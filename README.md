# java-transaction

> @MyTransactional 애노테이션 커스텀하기

## 커스텀 애노테이션(`@Transactional`) 적용 전

<details>

<summary>MemberService</summary>

```java
public class MemberServiceV1 {

  private final DataSource dataSource;
  private final MemberRepositoryV1 memberRepository;

  private static final Logger logger = LoggerFactory.getLogger(MemberServiceV1.class);

  public MemberServiceV1(DataSource dataSource, MemberRepositoryV1 memberRepository) {
    this.dataSource = dataSource;
    this.memberRepository = memberRepository;
  }

  public void accountTransfer(String fromId, String toId, int money) throws SQLException {
    Connection con = dataSource.getConnection();

    try {
      con.setAutoCommit(false); // 트랜잭션 시작

      // 비즈니스 로직
      bizLogic(con, fromId, toId, money);
      con.commit();
    } catch (Exception e) {
      con.rollback();
      throw new IllegalStateException(e.getMessage());
    } finally {
      release(con);
    }
  }

  private void release(Connection con) {
    if (con != null) {
      try {
        con.setAutoCommit(true); // 커넥션 풀 고려
        con.close();
      } catch (Exception e) {
        logger.error("error", e);
      }
    }
  }

  private void bizLogic(Connection con, String fromId, String toId, int money) throws SQLException {
    Member fromMember = memberRepository.findById(con, fromId);
    Member toMember = memberRepository.findById(con, toId);

    memberRepository.update(con, fromId, fromMember.getMoney() - money);
    validation(toMember);
    memberRepository.update(con, toId, toMember.getMoney() + money);
  }

  private void validation(Member toMember) {
    if (toMember.getMemberId().equals("ex")) {
      throw new IllegalStateException("이체중 예외 발생");
    }
  }
}
```

직접 트랜잭션을 시작하기 위해 con.setAutoCommit(false);를 통해 수동 커밋 모드로 시작해줘야 한다. 트랜잭션이 시작된 커넥션을 전달하면서 비즈니스 로직(bizLogic)을 수행한다. 그리고 비즈니스 로직이 정상 수행되면 트랜잭션을 커밋하고, 비즈니스 로직 수행 도중에 예외가 발생하면 트랜잭션을 롤백한다.

</details>

위 MemberService를 @Transactional 애노테이션이 있는 경우 JDK Dynamic Proxy를 사용해서 메서드에 대한 제어권을 프록시 객체에게 넘겨주고, 개발자는 비즈니스 로직에만 신경쓸 수 있도록 만들어보자.

## Reflection과 JDK Dynamic Proxy 활용해서 런타임에 동적으로 생성하는 프록시 패턴 만들기

### `@Transactional` 생성

```java
@Retention(RetentionPolicy.RUNTIME)
  public @interface MyTransactional {
}
```

런타임까지 애노테이션 정보를 유지하기 위해 애노테이션 설정을 다음과 같이 설정하고 `@Transactional` 애노테이션을 생성했다.

### InvocationHandler 구현

```java
public class MyTransactionalHandler implements InvocationHandler {

  private final Object target;

  public MyTransactionalHandler(Object target) {
    this.target = target;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Connection con = (Connection) args[3];

    try {
      con.setAutoCommit(false);

      Object result = method.invoke(target, args);
      con.commit();
      return result;
    } catch (Exception e) {
      con.rollback();
      Throwable cause = e.getCause();
      throw new IllegalStateException(cause.getMessage());
    } finally {
      release(con);
    }
  }

  private void release(Connection con) {
    if (con != null) {
      try {
        con.setAutoCommit(true); // 커넥션 풀 고려
        con.close();
      } catch (Exception e) {
      }
    }
  }
}
```

JDK Dynamic Proxy를 사용하기 위해서는 invoke() 메서드를 가지고 있는 InvocationHandler를 구현해야 한다. invoke() 메서드는 런타임 시점에 생긴 동적 프록시의 메서드가 호출되었을 때 실행되는 메서드이고, 어떤 메서드가 실행되었는지 메서드 정보와 메서드에 전달된 인자까지 invoke() 메서드의 인자로 들어오게 된다.
invoke() 메서드 내용으로는 MemberService의 비즈니스 로직을 제외한 트랜잭션 관련 로직들은 수행할 수 있도록 작성했다.

### Reflection과 JDK Dynamic Proxy 구현

```java
public class ContainerService {

  private static final DataSource dataSource = DBConnectionUtil.getDataSource();
  private static final MemberRepositoryV1 memberRepository = new MemberRepositoryV1(dataSource);


  private ContainerService() {
  }

  public static <T> T getObject(Class<T> classType) {
    T instance = createServiceInstance(classType);

    return Arrays.stream(classType.getDeclaredMethods())
            .filter(method -> method.getAnnotation(MyTransactional.class) != null)
            .findFirst()
            .map(m -> {
              MyTransactionalHandler handler = new MyTransactionalHandler(instance);
              return (T) Proxy.newProxyInstance(
                      instance.getClass().getClassLoader(),
                      new Class[]{MemberServiceV2.class},
                      handler
              );
            })
            .orElse(instance);
  }

  private static <T> T createServiceInstance(Class<T> classType) {
    try {
      return classType.getConstructor(MemberRepositoryV1.class).newInstance(memberRepository);
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
}
```

Java에서 런타임 시점에 프록시 클래스를 만들어주는 기능을 제공하는 Reflecion API의 newProxyInstance() 메서드를 사용했다.   
createServiceInstance() 구현하는 부분에 아쉬운 부분들이 많은데 먼저 객체들을 생성하는 제어권도 개발자가 아닌 프레임워크에게 줄 수 있도록 하고 싶지만 아직은 직접 생성하여 넘겨주는 방식으로 작성했다.   
그리고 JDK Dynamic Proxy 같은 경우에는 인터페이스가 반드시 필요하기 때문에 MemberServiceV2.class에 의존하게 되는 코드를 넣을 수 밖에 없었다. 따라서 다음에는 CGLIB 라이브러리를 사용해서 동적으로 상속을 통해 프록시 객체를 생성할 수 있도록 만들어보자.



## 커스텀 애노테이션(`@Transactional`) 적용 및 테스트

### 커스텀 애노테이션(`@Transactional`) 적용

```java
public class MemberServiceV2Impl implements MemberServiceV2 {

  private MemberRepositoryV1 memberRepository;

  public MemberServiceV2Impl(MemberRepositoryV1 memberRepository) {
    this.memberRepository = memberRepository;
  }

  @MyTransactional
  public void accountTransfer(String fromId, String toId, int money, Connection con) throws SQLException {
    bizLogic(con, fromId, toId, money);
  }

  private void bizLogic(Connection con, String fromId, String toId, int money) throws SQLException {
    Member fromMember = memberRepository.findById(con, fromId);
    Member toMember = memberRepository.findById(con, toId);

    memberRepository.update(con, fromId, fromMember.getMoney() - money);
    validation(toMember);
    memberRepository.update(con, toId, toMember.getMoney() + money);
  }

  private void validation(Member toMember) {
    if (toMember.getMemberId().equals("ex")) {
      throw new IllegalStateException("이체중 예외 발생");
    }
  }
}
```

accountTransfer() 메서드를 보면 @MyTransactional 애노테이션이 붙어있고, 트랜잭션 관련 로직은 없고 비즈니스 로직만 남을 것을 확인할 수 있다.

### 테스트

```java
class ContainerServiceTest {

  private DataSource dataSource;
  private MemberRepositoryV1 memberRepository;
  private MemberServiceV2 memberService;

  @BeforeEach
  void setUp() throws SQLException {
    dataSource = DBConnectionUtil.getDataSource();
    memberRepository = new MemberRepositoryV1(dataSource);
    memberService = ContainerService.getObject(MemberServiceV2Impl.class);
  }

  @AfterEach
  void tearDown() throws SQLException {
    memberRepository.delete("memberA");
    memberRepository.delete("memberB");
    memberRepository.delete("ex");
  }

  @Test
  @DisplayName("정상 이체")
  void getObject() throws SQLException {
    // given
    Member memberA = new Member("memberA", 10000);
    Member memberB = new Member("memberB", 10000);
    memberRepository.save(memberA);
    memberRepository.save(memberB);

    // when
    memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000, dataSource.getConnection());

    // memberService.accountTransfer();
    Member findMemberA = memberRepository.findById(memberA.getMemberId());
    Member findMemberB = memberRepository.findById(memberB.getMemberId());

    assertAll(
            () -> assertThat(findMemberA.getMoney()).isEqualTo(8000),
            () -> assertThat(findMemberB.getMoney()).isEqualTo(12000)
    );
  }

  @Test
  @DisplayName("이체중 예외 발생")
  void accountTransferEx() throws SQLException {
    // given
    Member memberA = new Member("memberA", 10000);
    Member memberEx = new Member("ex", 10000);
    memberRepository.save(memberA);
    memberRepository.save(memberEx);

    // when
    assertThatThrownBy(() -> memberService.accountTransfer("memberA", "ex", 2000, dataSource.getConnection()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이체중 예외 발생");

    // then
    Member findMemberA = memberRepository.findById(memberA.getMemberId());
    Member findMemberEx = memberRepository.findById(memberEx.getMemberId());

    assertAll(
            () -> assertThat(findMemberA.getMoney()).isEqualTo(10000),
            () -> assertThat(findMemberEx.getMoney()).isEqualTo(10000)
    );
  }
}
```

![image](https://github.com/oneny/TIL/assets/97153666/3f30fe4e-dfd6-4aa5-bbed-ef75d9bf5ca4)

트랜잭션 성공, 실패 시 모두 정상적으로 작동하는 것을 확인할 수 있다.

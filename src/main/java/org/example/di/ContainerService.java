package org.example.di;

import org.example.db.DBConnectionUtil;
import org.example.member.MemberRepositoryV1;
import org.example.member.MemberServiceV2;
import org.example.transactional.MyTransactional;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Arrays;

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

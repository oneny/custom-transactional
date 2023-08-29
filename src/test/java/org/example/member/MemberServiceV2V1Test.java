package org.example.member;

import org.example.db.DBConnectionUtil;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class MemberServiceV2V1Test {

  private DataSource dataSource;
  private MemberRepositoryV1 memberRepository;
  private MemberServiceV1 memberService;

  @BeforeEach
  void setUp() {
    dataSource = DBConnectionUtil.getDataSource();
    memberRepository = new MemberRepositoryV1(dataSource);
    memberService = new MemberServiceV1(dataSource, memberRepository);
  }

  @AfterEach
  void tearDown() throws SQLException {
    memberRepository.delete("memberA");
    memberRepository.delete("memberB");
    memberRepository.delete("ex");
  }

  @Test
  @DisplayName("정상 이제")
  void accountTransfer() throws SQLException {
    // given
    Member memberA = new Member("memberA", 10000);
    Member memberB = new Member("memberB", 10000);
    memberRepository.save(memberA);
    memberRepository.save(memberB);

    // when
    memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000);

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
    assertThatThrownBy(() -> memberService.accountTransfer("memberA", "ex", 2000))
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

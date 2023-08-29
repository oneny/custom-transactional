package org.example.di;

import org.assertj.core.api.Assertions;
import org.example.db.DBConnectionUtil;
import org.example.member.Member;
import org.example.member.MemberRepositoryV1;
import org.example.member.MemberServiceV2;
import org.example.member.MemberServiceV2Impl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

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

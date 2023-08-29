package org.example.member;

import org.example.di.Inject;
import org.example.transactional.MyTransactional;

import java.sql.Connection;
import java.sql.SQLException;

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

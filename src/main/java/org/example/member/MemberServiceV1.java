package org.example.member;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

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

package org.example.member;

import org.example.db.DBConnectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class MemberRepositoryV1Test {

  private DataSource dataSource;
  private MemberRepositoryV1 memberRepository;

  @BeforeEach
  void setUp() {
    dataSource = DBConnectionUtil.getDataSource();
    memberRepository = new MemberRepositoryV1(dataSource);
  }

  @Test
  void insert() throws SQLException {
    Member oneny = new Member("threeny", 30);
    memberRepository.save(oneny);
  }

  @Test
  void findBy() throws SQLException {
    Member threeny = memberRepository.findById("threeny");
    assertAll(
            () -> assertThat(threeny.getMemberId()).isEqualTo("threeny"),
            () -> assertThat(threeny.getMoney()).isEqualTo(30)
    );
  }

  @Test
  void update() throws SQLException {
    memberRepository.update("threeny", 31);
    Member threeny = memberRepository.findById("threeny");

    assertAll(
            () -> assertThat(threeny.getMemberId()).isEqualTo("threeny"),
            () -> assertThat(threeny.getMoney()).isEqualTo(31)
    );
  }

  @Test
  void delete() throws SQLException {
    memberRepository.delete("threeny");

    assertThatThrownBy(() -> memberRepository.findById("threeny"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("member not found memberId = threeny");
  }
}

package org.example;

import com.zaxxer.hikari.HikariDataSource;
import org.example.db.DBConnectionUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DBConnectionUtilTest {

  @Test
  void connection() {
    HikariDataSource connection = DBConnectionUtil.getDataSource();
    assertThat(connection).isNotNull();
  }
}

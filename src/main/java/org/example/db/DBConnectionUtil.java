package org.example.db;

import com.zaxxer.hikari.HikariDataSource;

import static org.example.db.ConnectionConst.*;

public class DBConnectionUtil {

  private static final HikariDataSource dataSource = new HikariDataSource();

  static {
    dataSource.setJdbcUrl(URL);
    dataSource.setUsername(USERNAME);
    dataSource.setPassword(PASSWORD);
    dataSource.setMaximumPoolSize(10);
    dataSource.setPoolName("MyPool");
  }

  private DBConnectionUtil() {
  }

  public static HikariDataSource getDataSource() {
    return dataSource;
  }
}

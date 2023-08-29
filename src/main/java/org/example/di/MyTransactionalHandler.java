package org.example.di;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;

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

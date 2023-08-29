package org.example.transactional;

import org.example.member.MemberServiceV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.sql.Connection;

public class ProxyInMemberService {

  private static final Logger logger = LoggerFactory.getLogger(ProxyInMemberService.class);

  public MemberServiceV2 getMemberServiceProxy(MemberServiceV2 target) {


    return (MemberServiceV2) Proxy.newProxyInstance(
            this.getClass().getClassLoader(),
            new Class[]{MemberServiceV2.class},
            (proxy, method, args) -> {
              Connection con = (Connection) args[3];

              try {
                con.setAutoCommit(false);

                method.invoke(target, args);
                con.commit();
              } catch (Exception e) {
                con.rollback();
                Throwable cause = e.getCause();
                throw new IllegalStateException(cause.getMessage());
              } finally {
                release(con);
              }

              return null;
            });
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
}

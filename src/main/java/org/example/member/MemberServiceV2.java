package org.example.member;

import java.sql.Connection;
import java.sql.SQLException;

public interface MemberServiceV2 {

  void accountTransfer(String fromId, String toId, int money, Connection con) throws SQLException;
}

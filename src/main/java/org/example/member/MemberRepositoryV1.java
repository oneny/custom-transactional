package org.example.member;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

public class MemberRepositoryV1 {

  private final DataSource dataSource;

  public MemberRepositoryV1(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Member save(Member member) throws SQLException {
    String sql = "insert into member(member_id, money) values(?,?)";
    Connection con = dataSource.getConnection();
    PreparedStatement pstmt = con.prepareStatement(sql);

    try (con; pstmt) {

      pstmt.setString(1, member.getMemberId());
      pstmt.setInt(2, member.getMoney());
      pstmt.executeUpdate();
      return member;
    } catch (SQLException e) {
      throw e;
    }
  }

  public Member findById(String memberId) throws SQLException {
    String sql = "select * from member where member_id = ?";
    Connection con = dataSource.getConnection();
    PreparedStatement pstmt = createPreparedStatement(con, sql, memberId);
    ResultSet rs = pstmt.executeQuery();

    try (con; pstmt; rs) {

      if (rs.next()) {
        return new Member(
                rs.getString("member_id"),
                rs.getInt("money"));
      } else {
        throw new NoSuchElementException("member not found memberId = " + memberId);
      }
    } catch (SQLException e) {
      throw e;
    }
  }

  public Member findById(Connection con, String memberId) throws SQLException {
    String sql = "select * from member where member_id = ?";
    PreparedStatement pstmt = createPreparedStatement(con, sql, memberId);
    ResultSet rs = pstmt.executeQuery();

    try (pstmt; rs) { // connection은 여기서 닫히지 않는다.

      if (rs.next()) {
        return new Member(
                rs.getString("member_id"),
                rs.getInt("money"));
      } else {
        throw new NoSuchElementException("member not found memberId = " + memberId);
      }
    } catch (SQLException e) {
      throw e;
    }
  }

  public void update(String memberId, int money) throws SQLException {
    String sql = "update member set money = ? where member_id = ?";

    Connection con = dataSource.getConnection();
    PreparedStatement pstmt = con.prepareStatement(sql);

    try (con; pstmt) {
      pstmt.setInt(1, money);
      pstmt.setString(2, memberId);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw e;
    }
  }

  public void update(Connection con, String memberId, int money) throws SQLException {
    String sql = "update member set money = ? where member_id = ?";

    PreparedStatement pstmt = con.prepareStatement(sql);

    try (pstmt) { // connection은 여기서 닫히지 않는다.
      pstmt.setInt(1, money);
      pstmt.setString(2, memberId);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw e;
    }
  }

  public void delete(String memberId) throws SQLException {
    String sql = "delete from member where member_id = ?";

    Connection con = dataSource.getConnection();
    PreparedStatement pstmt = con.prepareStatement(sql);

    try (con; pstmt) {
      pstmt.setString(1, memberId);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw e;
    }
  }

  private PreparedStatement createPreparedStatement(Connection con, String sql, String memberId) throws SQLException {
    PreparedStatement pstmt = con.prepareStatement(sql);
    pstmt.setString(1, memberId);
    return pstmt;
  }
}

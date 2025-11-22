package pos.dao;

import pos.db.Database;
import pos.model.CashSession;

import java.sql.*;
import java.time.LocalDateTime;

public class CashSessionDao {

    /** 🟢 ABRIR CAJA — Inserta una nueva fila con open_time y closed=0 */
    public void openSession(String usuario, int montoInicial) {

        final String sql = """
            INSERT INTO cash_sessions (user, open_time, monto_inicial, closed)
            VALUES (?, ?, ?, 0)
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, usuario);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setInt(3, montoInicial);

            ps.executeUpdate();
            System.out.println("[CashSessionDao] Caja abierta para " + usuario);

        } catch (SQLException e) {
            System.err.println("[CashSessionDao] Error al abrir caja: " + e.getMessage());
        }
    }

    /** 🔵 CERRAR CAJA — Actualiza la última sesión abierta usando subquery seguro */
    public void closeSession(String usuario, int montoFinal) {

        final String sql = """
            UPDATE cash_sessions
               SET close_time = ?,
                   monto_final = ?,
                   closed = 1
             WHERE id = (
                 SELECT id
                   FROM cash_sessions
                  WHERE user = ?
                    AND closed = 0
               ORDER BY datetime(open_time) DESC
                  LIMIT 1
             )
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, LocalDateTime.now().toString());
            ps.setInt(2, montoFinal);
            ps.setString(3, usuario);

            ps.executeUpdate();
            System.out.println("[CashSessionDao] Caja cerrada para " + usuario);

        } catch (SQLException e) {
            System.err.println("[CashSessionDao] Error al cerrar caja: " + e.getMessage());
        }
    }

    /** 🟣 Obtener sesión ABIERTA actual */
    public CashSession getOpenSession(String usuario) {

        final String sql = """
            SELECT id, user, open_time, close_time, monto_inicial, monto_final, closed
              FROM cash_sessions
             WHERE user = ?
               AND closed = 0
             ORDER BY datetime(open_time) DESC
             LIMIT 1
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, usuario);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    CashSession cs = new CashSession();

                    cs.setId(rs.getInt("id"));
                    cs.setUser(rs.getString("user"));
                    cs.setOpenTime(LocalDateTime.parse(rs.getString("open_time")));

                    String close = rs.getString("close_time");
                    if (close != null)
                        cs.setCloseTime(LocalDateTime.parse(close));

                    cs.setMontoInicial(rs.getInt("monto_inicial"));
                    cs.setMontoFinal(rs.getInt("monto_final"));
                    cs.setClosed(rs.getInt("closed") == 1);

                    return cs;
                }
            }

        } catch (SQLException e) {
            System.err.println("[CashSessionDao] Error getOpenSession: " + e.getMessage());
        }

        return null;
    }

    /** 🟡 Total vendido por usuario */
    public int calcularTotalVentasDelDia(String usuario) {

        final String sql = """
            SELECT IFNULL(SUM(total), 0)
              FROM sales
             WHERE user = ?
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, usuario);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }

        } catch (SQLException e) {
            System.err.println("[CashSessionDao] Error total ventas: " + e.getMessage());
        }

        return 0;
    }
}

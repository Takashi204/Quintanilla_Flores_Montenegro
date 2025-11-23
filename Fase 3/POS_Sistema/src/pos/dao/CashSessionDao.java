package pos.dao;

import pos.db.Database;                 // Acceso a la conexión con SQLite
import pos.model.CashSession;           // Modelo de datos de sesión de caja

import java.sql.*;                      // JDBC para consultas SQL
import java.time.LocalDateTime;         // Para fechas de apertura/cierre

/**
 * DAO encargado de manejar todo lo relacionado con la CAJA:
 *  - abrir caja
 *  - cerrar caja
 *  - obtener sesión abierta
 *  - calcular total de ventas del día
 */
public class CashSessionDao {

    // ===============================================================
    // 🔵 ABRIR CAJA
    // ===============================================================

    public void openSession(String usuario, int montoInicial) {

        final String sql = """
            INSERT INTO cash_sessions (user, open_time, monto_inicial, closed)
            VALUES (?, ?, ?, 0)
        """; 
        // INSERT crea una nueva sesión con closed = 0 (abierta)

        try (Connection cn = Database.get();          // obtiene conexión a BD
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, usuario);                 // usuario que abre la caja
            ps.setString(2, LocalDateTime.now().toString()); // hora actual en texto
            ps.setInt(3, montoInicial);               // monto inicial de caja

            ps.executeUpdate();                       // ejecuta el INSERT

        } catch (SQLException e) {
            System.err.println("[CashSessionDao] Error al abrir caja: " + e.getMessage());
        }
    }

    // ===============================================================
    // 🔴 CERRAR CAJA
    // ===============================================================

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
                    AND closed = 0            -- solo la sesión abierta
               ORDER BY datetime(open_time) DESC
                  LIMIT 1                     -- toma la última apertura
             )
        """;
        // Este UPDATE cierra la última sesión abierta del usuario

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, LocalDateTime.now().toString()); // hora de cierre
            ps.setInt(2, montoFinal);                        // monto final
            ps.setString(3, usuario);                        // usuario que cerró

            ps.executeUpdate();                              // ejecuta el UPDATE

        } catch (SQLException e) {
            System.err.println("[CashSessionDao] Error al cerrar caja: " + e.getMessage());
        }
    }

    // ===============================================================
    // 🟣 OBTENER LA SESIÓN ABIERTA ACTUAL
    // ===============================================================

    public CashSession getOpenSession(String usuario) {

        final String sql = """
            SELECT id, user, open_time, close_time, monto_inicial, monto_final, closed
              FROM cash_sessions
             WHERE user = ?
               AND closed = 0                   -- solo sesiones abiertas
             ORDER BY datetime(open_time) DESC
             LIMIT 1
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, usuario);            // filtra por usuario

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {                // si existe una sesión abierta

                    CashSession cs = new CashSession();   // crea objeto modelo

                    cs.setId(rs.getInt("id"));                           // id
                    cs.setUser(rs.getString("user"));                    // usuario
                    cs.setOpenTime(LocalDateTime.parse(rs.getString("open_time"))); // fecha apertura

                    String close = rs.getString("close_time");
                    if (close != null) {
                        cs.setCloseTime(LocalDateTime.parse(close));     // fecha cierre (si existe)
                    }

                    cs.setMontoInicial(rs.getInt("monto_inicial"));      // monto inicial
                    cs.setMontoFinal(rs.getInt("monto_final"));          // monto final (si existe)
                    cs.setClosed(rs.getInt("closed") == 1);              // estado (boolean)

                    return cs;                                           // devuelve sesión
                }
            }

        } catch (SQLException e) {
            System.err.println("[CashSessionDao] Error getOpenSession: " + e.getMessage());
        }

        return null;  // no hay sesión abierta
    }

    // ===============================================================
    // 🟡 TOTAL DE VENTAS DEL DÍA (POR USUARIO)
    // ===============================================================

    public int calcularTotalVentasDelDia(String usuario) {

        final String sql = """
            SELECT IFNULL(SUM(total), 0)
              FROM sales
             WHERE user = ?
        """;
        // Suma el campo "total" de todas las ventas pertenecientes al usuario

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, usuario);         // usuario dueño de las ventas

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;   // si no hay ventas -> 0
            }

        } catch (SQLException e) {
            System.err.println("[CashSessionDao] Error total ventas: " + e.getMessage());
        }

        return 0;  // retorno por defecto si hubo error
    }
}


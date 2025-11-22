package pos.dao;

import pos.db.Database;

import java.sql.*;
import java.time.LocalDate;

/**
 * DashboardDao: calcula totales de ventas
 * (día, semana y mes) directamente desde la tabla 'sales'.
 */
public class DashboardDao {

    /** Total vendido hoy */
    public static int totalHoy() {
        LocalDate hoy = LocalDate.now();
        return totalEntre(hoy, hoy);
    }

    /** Total vendido desde el lunes hasta hoy */
    public static int totalSemana() {
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(java.time.DayOfWeek.MONDAY);
        return totalEntre(lunes, hoy);
    }

    /** Total vendido desde el primer día del mes hasta hoy */
    public static int totalMes() {
        LocalDate hoy = LocalDate.now();
        LocalDate primero = hoy.withDayOfMonth(1);
        return totalEntre(primero, hoy);
    }

    // ==============================================================
    // MÉTODO BASE
    // ==============================================================

    private static int totalEntre(LocalDate desde, LocalDate hasta) {
        final String sql = """
            SELECT SUM(total) AS total
            FROM sales
            WHERE date(ts) BETWEEN ? AND ?
        """;
        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDao] Error al calcular totales: " + e.getMessage());
            return 0;
        }
    }
}

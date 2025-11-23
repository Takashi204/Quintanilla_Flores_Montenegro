package pos.dao;

import pos.db.Database;                 // Acceso a la BD SQLite

import java.sql.*;                      // JDBC para consultas SQL
import java.time.LocalDate;             // Fechas sin hora

/**
 * DashboardDao:
 * -------------
 * Calcula totales de ventas para:
 *   - Hoy
 *   - Semana actual
 *   - Mes actual
 *
 * Todos los cálculos se realizan consultando la tabla 'sales'.
 */
public class DashboardDao {

    /**
     * totalHoy()
     * ----------
     * Calcula la suma total vendida en la fecha actual.
     * Usa LocalDate.now() para obtener la fecha de hoy
     * y pasa esa misma fecha como inicio y fin al método totalEntre().
     */
    public static int totalHoy() {
        LocalDate hoy = LocalDate.now();        // fecha actual del sistema
        return totalEntre(hoy, hoy);            // rango: hoy → hoy
    }

    /**
     * totalSemana()
     * -------------
     * Calcula el total vendido desde el lunes de esta semana hasta hoy.
     * with(DayOfWeek.MONDAY) obtiene el lunes correspondiente a la semana actual.
     */
    public static int totalSemana() {
        LocalDate hoy = LocalDate.now();        // fecha actual
        LocalDate lunes = hoy.with(java.time.DayOfWeek.MONDAY); // inicio semana
        return totalEntre(lunes, hoy);          // rango: lunes → hoy
    }

    /**
     * totalMes()
     * ----------
     * Calcula la suma de ventas desde el primer día del mes hasta hoy.
     */
    public static int totalMes() {
        LocalDate hoy = LocalDate.now();        // fecha actual
        LocalDate primero = hoy.withDayOfMonth(1);  // día 1 del mes
        return totalEntre(primero, hoy);        // rango: 1 del mes → hoy
    }

    // ==================================================================
    // MÉTODO BASE — Todas las funciones llaman a este método.
    // ==================================================================

    /**
     * totalEntre(desde, hasta)
     * -------------------------
     * Calcula el total de ventas en un rango de fechas.
     *
     * Parámetros:
     *   - desde: fecha inicial
     *   - hasta: fecha final
     *
     * SQL:
     *   SUM(total) → suma el campo 'total' de cada venta
     *   date(ts)   → extrae solo la fecha del timestamp 'ts'
     *   BETWEEN    → filtra ventas entre las dos fechas
     */
    private static int totalEntre(LocalDate desde, LocalDate hasta) {

        final String sql = """
            SELECT SUM(total) AS total
            FROM sales
            WHERE date(ts) BETWEEN ? AND ?
        """; 
        // Consulta SQL que suma las ventas dentro del rango

        try (Connection cn = Database.get();               // obtiene conexión
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, desde.toString());             // fecha inicio (AAAA-MM-DD)
            ps.setString(2, hasta.toString());             // fecha fin    (AAAA-MM-DD)

            try (ResultSet rs = ps.executeQuery()) {

                // Si hay resultado, devuelve el total; si no, devuelve 0
                return rs.next() ? rs.getInt("total") : 0;
            }

        } catch (SQLException e) {
            System.err.println("[DashboardDao] Error al calcular totales: " + e.getMessage());
            return 0;                                       // valor por defecto
        }
    }
}


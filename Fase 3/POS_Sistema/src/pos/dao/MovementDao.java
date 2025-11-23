package pos.dao;

import pos.db.Database;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovementDao {

    /** Inserta un movimiento en el historial de inventario. */
    public void insert(String code, String type, int qty, int prev, int now,
                       String reason, String user, LocalDateTime ts) {

        final String sql = """
            INSERT INTO inventory_movements
                (code, type, qty, reason, prev_stock, new_stock, user, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """; // consulta SQL para registrar el movimiento

        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, code);                // código del producto
            ps.setString(2, type);                // tipo de movimiento (venta, add, etc.)
            ps.setInt(3, qty);                    // cantidad movida
            ps.setString(4, reason == null ? "" : reason); // motivo (si es null, deja "")
            ps.setInt(5, prev);                   // stock antes del cambio
            ps.setInt(6, now);                    // stock después del cambio
            ps.setString(7, user);                // usuario que hizo la acción
            ps.setString(8, ts.toString());       // timestamp
            ps.executeUpdate();                   // ejecutar insert

        } catch (SQLException e) {
            System.err.println("[MovementDao] Error al insertar movimiento: " + e.getMessage());
        }
    }

    /** Lista los últimos movimientos globales. */
    public List<String[]> listRecent(int limit) {

        final String sql = """
            SELECT m.code, i.name AS product_name, m.type, m.qty, m.reason,
                   m.prev_stock, m.new_stock, m.user, m.created_at
              FROM inventory_movements m
              LEFT JOIN inventory i ON i.code = m.code
             ORDER BY datetime(m.created_at) DESC
             LIMIT ?
        """; // obtiene los movimientos más recientes

        List<String[]> out = new ArrayList<>();

        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, limit); // límite de filas

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rowToArray(rs)); // convertir fila → arreglo
                }
            }

        } catch (SQLException e) {
            System.err.println("[MovementDao] Error al listar movimientos: " + e.getMessage());
        }

        return out;
    }

    /** Lista los movimientos de un producto específico. */
    public List<String[]> listByCode(String code, int limit) {

        final String sql = """
            SELECT m.code, i.name AS product_name, m.type, m.qty, m.reason,
                   m.prev_stock, m.new_stock, m.user, m.created_at
              FROM inventory_movements m
              LEFT JOIN inventory i ON i.code = m.code
             WHERE m.code = ?
             ORDER BY datetime(m.created_at) DESC
             LIMIT ?
        """; // movimientos filtrados por código

        List<String[]> out = new ArrayList<>();

        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, code);  // código del producto
            ps.setInt(2, limit);    // límite

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rowToArray(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[MovementDao] Error al listar por código: " + e.getMessage());
        }

        return out;
    }

    /** Obtiene solo el último movimiento del producto. */
    public String[] findLatestByCode(String code) {

        final String sql = """
            SELECT m.code, i.name AS product_name, m.type, m.qty, m.reason,
                   m.prev_stock, m.new_stock, m.user, m.created_at
              FROM inventory_movements m
              LEFT JOIN inventory i ON i.code = m.code
             WHERE m.code = ?
             ORDER BY datetime(m.created_at) DESC
             LIMIT 1
        """; // busca el movimiento más reciente por código

        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, code); // código

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rowToArray(rs); // si existe, convertirlo
            }

        } catch (SQLException e) {
            System.err.println("[MovementDao] Error al obtener último movimiento: " + e.getMessage());
        }

        return null; // si no hay movimientos
    }

    /** Convierte una fila del ResultSet en un arreglo de Strings. */
    private String[] rowToArray(ResultSet rs) throws SQLException {

        return new String[]{
                rs.getString("code"),                      // código
                rs.getString("product_name"),              // nombre producto
                rs.getString("type"),                      // tipo movimiento
                Integer.toString(rs.getInt("qty")),        // cantidad
                rs.getString("reason"),                    // motivo
                Integer.toString(rs.getInt("prev_stock")), // stock antes
                Integer.toString(rs.getInt("new_stock")),  // stock después
                rs.getString("user"),                      // usuario
                rs.getString("created_at")                 // fecha/hora
        };
    }
}



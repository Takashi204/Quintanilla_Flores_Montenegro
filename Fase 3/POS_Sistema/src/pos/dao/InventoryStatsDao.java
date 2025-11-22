package pos.dao;

import pos.db.Database;
import pos.model.Product;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de solo lectura para estadísticas de inventario.
 * Usado exclusivamente por DashboardPanel.
 * No afecta a InventoryDao, CajeroPanel ni InventarioPanel.
 */
public class InventoryStatsDao {

    /**
     * Devuelve una lista de productos con stock menor o igual al umbral.
     * @param threshold cantidad límite de stock
     */
    public static List<Product> lowStock(int threshold) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory WHERE stock <= ? ORDER BY stock ASC";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, threshold);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));

        } catch (Exception e) {
            System.err.println("[InventoryStatsDao] Error al obtener stock bajo: " + e.getMessage());
        }
        return list;
    }

    /**
     * Devuelve una lista de productos que vencen dentro de los próximos 'days' días.
     * Si el campo expiry está nulo, se ignora.
     */
    public static List<Product> expiringInDays(int days) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory WHERE expiry IS NOT NULL AND expiry <= date('now', ? || ' days')";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));

        } catch (Exception e) {
            System.err.println("[InventoryStatsDao] Error al obtener productos por vencer: " + e.getMessage());
        }
        return list;
    }

    // =======================================================
    // 🔹 Función auxiliar para convertir filas a objetos Product
    // =======================================================
    private static Product map(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getInt("price"),
                rs.getInt("stock"),
                parseDate(rs.getString("expiry"))
        );
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); }
        catch (Exception e) { return null; }
    }
}

package pos.dao;

import pos.db.Database;          // conexión a SQLite
import pos.model.Product;        // modelo de producto

import java.sql.*;               // JDBC
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * InventoryStatsDao
 * -----------------
 * DAO dedicado únicamente a *estadísticas de inventario*.
 *
 * ❗ Importante:
 *  - NO modifica datos
 *  - Solo consulta información
 *  - Se usa principalmente en DashboardPanel
 */
public class InventoryStatsDao {

    /**
     * lowStock(int threshold)
     * -----------------------
     * Retorna productos cuyo stock es ≤ al threshold (ej: <=5).
     */
    public static List<Product> lowStock(int threshold) {
        List<Product> list = new ArrayList<>();

        String sql = "SELECT * FROM inventory WHERE stock <= ? ORDER BY stock ASC"; // ordenado por menor stock

        try (Connection c = Database.get();                     // abre conexión
             PreparedStatement ps = c.prepareStatement(sql)) { // prepara SQL

            ps.setInt(1, threshold);                           // establece límite (ej: 5)

            ResultSet rs = ps.executeQuery();                  // ejecuta SELECT

            while (rs.next())
                list.add(map(rs));                             // convierte cada fila a Product

        } catch (Exception e) {
            System.err.println("[InventoryStatsDao] Error al obtener stock bajo: " + e.getMessage());
        }

        return list;                                           // retorna lista final
    }

    /**
     * expiringInDays(int days)
     * ------------------------
     * Busca productos que vencen dentro de N días.
     * Ignora productos con expiry NULL.
     */
    public static List<Product> expiringInDays(int days) {
        List<Product> list = new ArrayList<>();

        // expiry <= fecha actual + días
        String sql = "SELECT * FROM inventory WHERE expiry IS NOT NULL AND expiry <= date('now', ? || ' days')";

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, days);                                // cuántos días hacia adelante

            ResultSet rs = ps.executeQuery();

            while (rs.next())
                list.add(map(rs));                             // mapea cada fila → Product

        } catch (Exception e) {
            System.err.println("[InventoryStatsDao] Error al obtener productos por vencer: " + e.getMessage());
        }

        return list;
    }

    // =======================================================
    // MAPEO RESULTSET → PRODUCT
    // =======================================================

    /**
     * map(ResultSet rs)
     * -----------------
     * Convierte una fila del SELECT en un objeto Product.
     */
    private static Product map(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),                               // ID interno
                rs.getString("code"),                          // código del producto
                rs.getString("name"),                          // nombre
                rs.getString("category"),                      // categoría
                rs.getInt("price"),                            // precio unitario
                rs.getInt("stock"),                            // stock disponible
                parseDate(rs.getString("expiry"))              // fecha de vencimiento (nullable)
        );
    }

    /**
     * parseDate(String s)
     * -------------------
     * Convierte un String (YYYY-MM-DD) en LocalDate.
     * Si está vacío o está mal formateado → retorna null.
     */
    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;

        try {
            return LocalDate.parse(s);                         // intenta parsear
        } catch (Exception e) {
            return null;                                       // si falla → null
        }
    }
}

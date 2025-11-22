package pos.dao;

import pos.db.Database;
import pos.model.Product;
import pos.util.DataSync;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO central del inventario.
 * Compatible con VentasPanel, CajeroPanel e InventarioPanel.
 * Ahora notifica cambios globales vía DataSync.
 */
public class InventoryDao {

    // ==== Crear producto ====
    public static void insert(Product p) {
        final String sql = """
            INSERT INTO inventory (code, name, category, price, stock, expiry)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getCode());
            ps.setString(2, p.getName());
            ps.setString(3, p.getCategory());
            ps.setInt(4, p.getPrice());
            ps.setInt(5, p.getStock());
            if (p.getExpiry() != null)
                ps.setString(6, p.getExpiry().toString());
            else
                ps.setNull(6, Types.VARCHAR);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }

            // 🔔 Notifica cambio global
            DataSync.notifyChange("inventory");

        } catch (SQLException e) {
            System.err.println("[InventoryDao.insert] " + e.getMessage());
        }
    }

    // ==== Listar productos ====
    public static List<Product> listAll() {
        return getAll();
    }

    public static List<Product> getAll() {
        final String sql = "SELECT * FROM inventory ORDER BY name";
        List<Product> list = new ArrayList<>();
        try (Connection cn = Database.get();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(fromRow(rs));
        } catch (SQLException e) {
            System.err.println("[InventoryDao.getAll] " + e.getMessage());
        }
        return list;
    }

    // ==== Buscar por código ====
    public static Product findByCode(String code) {
        if (code == null || code.isBlank()) return null;
        final String sql = "SELECT * FROM inventory WHERE code = ?";
        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("[InventoryDao.findByCode] " + e.getMessage());
        }
        return null;
    }

    // ==== Actualizar producto completo ====
    public static void update(Product p) {
        final String sql = """
            UPDATE inventory SET
              name=?, category=?, price=?, stock=?, expiry=?
            WHERE code=?
        """;
        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getCategory());
            ps.setInt(3, p.getPrice());
            ps.setInt(4, p.getStock());
            if (p.getExpiry() != null)
                ps.setString(5, p.getExpiry().toString());
            else
                ps.setNull(5, Types.VARCHAR);
            ps.setString(6, p.getCode());
            ps.executeUpdate();

            // 🔔 Notifica actualización global
            DataSync.notifyChange("inventory");

        } catch (SQLException e) {
            System.err.println("[InventoryDao.update] " + e.getMessage());
        }
    }

    // ==== Actualizar solo stock ====
    public static void updateStock(String code, int newStock) {
        final String sql = "UPDATE inventory SET stock=? WHERE code=?";
        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setString(2, code);
            ps.executeUpdate();

            // 🔔 Notifica cambio de stock en tiempo real
            DataSync.notifyChange("inventory");

        } catch (SQLException e) {
            System.err.println("[InventoryDao.updateStock] " + e.getMessage());
        }
    }

    // ==== Eliminar producto ====
    public static void delete(String code) {
        final String sql = "DELETE FROM inventory WHERE code=?";
        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.executeUpdate();

            // 🔔 Notifica eliminación
            DataSync.notifyChange("inventory");

        } catch (SQLException e) {
            System.err.println("[InventoryDao.delete] " + e.getMessage());
        }
    }

    // ==== Convertir fila SQL → Product ====
    private static Product fromRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String code = rs.getString("code");
        String name = rs.getString("name");
        String cat = rs.getString("category");
        int price = rs.getInt("price");
        int stock = rs.getInt("stock");
        String expStr = rs.getString("expiry");
        LocalDate expiry = (expStr == null || expStr.isBlank()) ? null : LocalDate.parse(expStr);
        return new Product(id, code, name, cat, price, stock, expiry);
    }
}

package pos.dao;

import pos.db.Database;                 // Clase para obtener la conexión SQLite
import pos.model.Product;               // Modelo de producto
import pos.util.DataSync;               // Notificaciones en tiempo real

import java.sql.*;                      // JDBC
import java.time.LocalDate;             // Manejo de fechas
import java.util.ArrayList;
import java.util.List;

/**
 * InventoryDao:
 * -------------
 * Maneja todas las operaciones CRUD del inventario.
 * Utilizado en:
 *  - VentasPanel
 *  - CajeroPanel
 *  - InventarioPanel
 *
 * Notifica cambios usando DataSync para refrescar otras vistas.
 */
public class InventoryDao {

    // ======================================================================
    // INSERTAR PRODUCTO
    // ======================================================================

    public static void insert(Product p) {

        final String sql = """
            INSERT INTO inventory (code, name, category, price, stock, expiry)
            VALUES (?, ?, ?, ?, ?, ?)
        """; // SQL para insertar un registro nuevo en inventario

        try (Connection cn = Database.get();                                   // obtener conexión
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getCode());                                      // código
            ps.setString(2, p.getName());                                      // nombre
            ps.setString(3, p.getCategory());                                  // categoría
            ps.setInt(4, p.getPrice());                                        // precio
            ps.setInt(5, p.getStock());                                        // stock

            if (p.getExpiry() != null)                                         // fecha opcional
                ps.setString(6, p.getExpiry().toString());
            else
                ps.setNull(6, Types.VARCHAR);

            ps.executeUpdate();                                                // ejecutar INSERT

            // capturar ID autogenerado por SQLite
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }

            DataSync.notifyChange("inventory");                                // notifica cambios

        } catch (SQLException e) {
            System.err.println("[InventoryDao.insert] " + e.getMessage());
        }
    }

    // ======================================================================
    // LISTAR TODOS LOS PRODUCTOS
    // ======================================================================

    public static List<Product> listAll() {
        return getAll();                                                       // alias
    }

    public static List<Product> getAll() {

        final String sql = "SELECT * FROM inventory ORDER BY name";            // ordenar por nombre

        List<Product> list = new ArrayList<>();

        try (Connection cn = Database.get();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {                                                // recorre filas
                list.add(fromRow(rs));                                         // convierte cada fila a Product
            }

        } catch (SQLException e) {
            System.err.println("[InventoryDao.getAll] " + e.getMessage());
        }

        return list;                                                           // lista completa
    }

    // ======================================================================
    // BUSCAR PRODUCTO POR CÓDIGO
    // ======================================================================

    public static Product findByCode(String code) {

        if (code == null || code.isBlank()) return null;                       // validación simple

        final String sql = "SELECT * FROM inventory WHERE code = ?";           // búsqueda exacta

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, code.trim());                                      // limpia espacios

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromRow(rs);                             // encontrado → convertir
            }

        } catch (SQLException e) {
            System.err.println("[InventoryDao.findByCode] " + e.getMessage());
        }

        return null;                                                           // no encontrado
    }

    // ======================================================================
    // ACTUALIZAR PRODUCTO COMPLETO
    // ======================================================================

    public static void update(Product p) {

        final String sql = """
            UPDATE inventory SET
              name=?, category=?, price=?, stock=?, expiry=?
            WHERE code=?
        """; // actualiza todos los campos del producto

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, p.getName());                                      // nuevo nombre
            ps.setString(2, p.getCategory());                                  // nueva categoría
            ps.setInt(3, p.getPrice());                                        // nuevo precio
            ps.setInt(4, p.getStock());                                        // nuevo stock

            if (p.getExpiry() != null)                                         // fecha opcional
                ps.setString(5, p.getExpiry().toString());
            else
                ps.setNull(5, Types.VARCHAR);

            ps.setString(6, p.getCode());                                      // código a actualizar

            ps.executeUpdate();                                                // ejecutar UPDATE

            DataSync.notifyChange("inventory");                                // notificar cambio

        } catch (SQLException e) {
            System.err.println("[InventoryDao.update] " + e.getMessage());
        }
    }

    // ======================================================================
    // ACTUALIZAR SOLO STOCK
    // ======================================================================

    public static void updateStock(String code, int newStock) {

        final String sql = "UPDATE inventory SET stock=? WHERE code=?";        // solo stock

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, newStock);                                            // nuevo stock
            ps.setString(2, code);                                             // código
            ps.executeUpdate();                                                // ejecutar

            DataSync.notifyChange("inventory");                                // notificar

        } catch (SQLException e) {
            System.err.println("[InventoryDao.updateStock] " + e.getMessage());
        }
    }

    // ======================================================================
    // ELIMINAR PRODUCTO
    // ======================================================================

    public static void delete(String code) {

        final String sql = "DELETE FROM inventory WHERE code=?";               // eliminar por código

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, code);                                             // código del producto
            ps.executeUpdate();                                                // borrar registro

            DataSync.notifyChange("inventory");                                // notificar

        } catch (SQLException e) {
            System.err.println("[InventoryDao.delete] " + e.getMessage());
        }
    }

    // ======================================================================
    // CONVERTIR ResultSet → OBJETO Product
    // ======================================================================

    private static Product fromRow(ResultSet rs) throws SQLException {

        int id = rs.getInt("id");                                              // id autoincremental
        String code = rs.getString("code");                                    // código único
        String name = rs.getString("name");                                    // nombre
        String cat = rs.getString("category");                                 // categoría
        int price = rs.getInt("price");                                        // precio
        int stock = rs.getInt("stock");                                        // stock actual

        String expStr = rs.getString("expiry");                                // fecha como string
        LocalDate expiry =
                (expStr == null || expStr.isBlank())
                        ? null                                                 // sin fecha
                        : LocalDate.parse(expStr);                              // parsear fecha

        return new Product(id, code, name, cat, price, stock, expiry);          // crear objeto
    }
}

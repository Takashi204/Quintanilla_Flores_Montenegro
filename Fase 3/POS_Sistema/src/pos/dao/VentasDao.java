package pos.dao;

import pos.db.Database;
import pos.model.Product;
import pos.model.Sale;
import pos.model.SaleItem;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class VentasDao {

    /** Crea las tablas si no existen */
    public static void ensureSchema() {
        try (Connection cn = Database.get(); Statement st = cn.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS sales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT NOT NULL,
                    name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    price INTEGER NOT NULL,
                    total INTEGER NOT NULL,
                    fecha TEXT NOT NULL,
                    user TEXT NOT NULL,
                    metodo TEXT NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS sale_items (
                    sale_id INTEGER,
                    code TEXT,
                    name TEXT,
                    qty INTEGER,
                    price INTEGER,
                    subtotal INTEGER,
                    FOREIGN KEY(sale_id) REFERENCES sales(id)
                )
            """);

        } catch (SQLException e) {
            System.err.println("[VentasDao.ensureSchema] " + e.getMessage());
        }
    }

    /** Guarda una venta con sus ítems y método de pago */
    public static void save(Sale sale) {
        ensureSchema();

        try (Connection cn = Database.get()) {
            cn.setAutoCommit(false);

            try (PreparedStatement ps = cn.prepareStatement("""
                INSERT INTO sales (code, name, quantity, price, total, fecha, user, metodo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """)) {

                for (SaleItem it : sale.getItems()) {

                    Product p = it.getProduct();

                    ps.setString(1, p.getCode());
                    ps.setString(2, p.getName());
                    ps.setInt(3, it.getQty());
                    ps.setInt(4, p.getPrice());
                    ps.setInt(5, it.getSubtotal());
                    ps.setString(6, LocalDateTime.now().toString());
                    ps.setString(7, sale.getUser());
                    ps.setString(8, sale.getPaymentMethod()); // 🔥 método real

                    ps.addBatch();
                }

                ps.executeBatch();
            }

            cn.commit();

        } catch (SQLException e) {
            System.err.println("[VentasDao.save] Error: " + e.getMessage());
        }
    }

    /** Devuelve todas las ventas */
    public static List<Sale> listAll() {
        ensureSchema();
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT * FROM sales ORDER BY fecha DESC";

        try (Connection cn = Database.get();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next())
                list.add(fromRow(rs));

        } catch (SQLException e) {
            System.err.println("[VentasDao.listAll] " + e.getMessage());
        }
        return list;
    }

    /** Ventas recientes */
    public static List<Sale> listRecent(int limit) {
        ensureSchema();
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT * FROM sales ORDER BY fecha DESC LIMIT ?";

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
                list.add(fromRow(rs));

        } catch (SQLException e) {
            System.err.println("[VentasDao.listRecent] " + e.getMessage());
        }

        return list;
    }

    /** Total vendido entre fechas */
    public static int total(LocalDate desde, LocalDate hasta) {
        ensureSchema();

        String sql = """
            SELECT SUM(total) AS sum FROM sales
            WHERE DATE(fecha) BETWEEN DATE(?) AND DATE(?)
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getInt("sum") : 0;

        } catch (SQLException e) {
            System.err.println("[VentasDao.total] " + e.getMessage());
            return 0;
        }
    }

    public static int totalHoy() {
        LocalDate hoy = LocalDate.now();
        return total(hoy, hoy);
    }

    public static int totalSemana() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1);
        return total(inicio, hoy);
    }

    public static int totalMes() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.withDayOfMonth(1);
        return total(inicio, hoy);
    }

    // =========================================
    //              LECTOR DE BD
    // =========================================
    private static Sale fromRow(ResultSet rs) throws SQLException {

        String rawFecha = rs.getString("fecha");
        LocalDateTime fechaParsed;

        try {
            if (rawFecha.contains("T")) {
                fechaParsed = LocalDateTime.parse(rawFecha);
            } else {
                fechaParsed = LocalDate.parse(rawFecha).atStartOfDay();
            }
        } catch (Exception e) {
            fechaParsed = LocalDateTime.now();
        }

        return new Sale(
                rs.getString("id"),
                "BOLETA",
                fechaParsed,
                List.of(),
                rs.getString("metodo"),    // 🔥 método real leído desde BD
                0, 0, 0,
                rs.getInt("total"),
                0,
                rs.getInt("total"),
                null,
                rs.getString("user")
        );
    }

    private VentasDao() {}
}

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

    /** Crea las tablas necesarias si no existen */
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
            """); // tabla que guarda cada ítem vendido (no ventas agrupadas)

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
            """); // tabla opcional si se quiere agrupar ventas en encabezados

        } catch (SQLException e) {
            System.err.println("[VentasDao.ensureSchema] " + e.getMessage());
        }
    }

    /** Guarda una venta completa (varios ítems) */
    public static void save(Sale sale) {
        ensureSchema(); // asegurar que tabla exista

        try (Connection cn = Database.get()) {
            cn.setAutoCommit(false); // iniciar transacción

            try (PreparedStatement ps = cn.prepareStatement("""
                INSERT INTO sales (code, name, quantity, price, total, fecha, user, metodo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """)) {

                for (SaleItem it : sale.getItems()) {

                    Product p = it.getProduct();       // producto del ítem

                    ps.setString(1, p.getCode());       // code del producto
                    ps.setString(2, p.getName());       // nombre
                    ps.setInt(3, it.getQty());          // cantidad vendida
                    ps.setInt(4, p.getPrice());         // precio unitario
                    ps.setInt(5, it.getSubtotal());     // precio * cantidad
                    ps.setString(6, LocalDateTime.now().toString()); // fecha de venta
                    ps.setString(7, sale.getUser());    // usuario que vende
                    ps.setString(8, sale.getPaymentMethod()); // método pago

                    ps.addBatch();                       // agregar ítem al batch
                }

                ps.executeBatch();                       // guardar todos los ítems juntos
            }

            cn.commit();                                  // confirmar transacción

        } catch (SQLException e) {
            System.err.println("[VentasDao.save] Error: " + e.getMessage());
        }
    }

    /** Lista todas las ventas registradas (cada producto contado como fila) */
    public static List<Sale> listAll() {
        ensureSchema();
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT * FROM sales ORDER BY fecha DESC"; // orden más recientes primero

        try (Connection cn = Database.get();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next())
                list.add(fromRow(rs)); // convertir fila → Sale

        } catch (SQLException e) {
            System.err.println("[VentasDao.listAll] " + e.getMessage());
        }
        return list;
    }

    /** Lista ventas recientes (limit) */
    public static List<Sale> listRecent(int limit) {
        ensureSchema();
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT * FROM sales ORDER BY fecha DESC LIMIT ?";

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, limit); // límite enviado por parámetro
            ResultSet rs = ps.executeQuery();

            while (rs.next())
                list.add(fromRow(rs));

        } catch (SQLException e) {
            System.err.println("[VentasDao.listRecent] " + e.getMessage());
        }

        return list;
    }

    /** Total vendido entre dos fechas */
    public static int total(LocalDate desde, LocalDate hasta) {
        ensureSchema();

        String sql = """
            SELECT SUM(total) AS sum FROM sales
            WHERE DATE(fecha) BETWEEN DATE(?) AND DATE(?)
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, desde.toString());  // fecha inicio
            ps.setString(2, hasta.toString());  // fecha fin

            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getInt("sum") : 0; // si no hay datos, retorna 0

        } catch (SQLException e) {
            System.err.println("[VentasDao.total] " + e.getMessage());
            return 0;
        }
    }

    // Total vendido hoy
    public static int totalHoy() {
        LocalDate hoy = LocalDate.now();
        return total(hoy, hoy);
    }

    // Total vendido esta semana
    public static int totalSemana() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1); // lunes
        return total(inicio, hoy);
    }

    // Total vendido este mes
    public static int totalMes() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.withDayOfMonth(1); // día 1 del mes
        return total(inicio, hoy);
    }

    // Convierte una fila SQL → objeto Sale
    private static Sale fromRow(ResultSet rs) throws SQLException {

        String rawFecha = rs.getString("fecha"); // string de fecha almacenado
        LocalDateTime fechaParsed;

        try {
            if (rawFecha.contains("T")) {
                fechaParsed = LocalDateTime.parse(rawFecha);  // formato ISO con hora
            } else {
                fechaParsed = LocalDate.parse(rawFecha).atStartOfDay(); // solo fecha
            }
        } catch (Exception e) {
            fechaParsed = LocalDateTime.now(); // fallback en caso de error
        }

        return new Sale(
                rs.getString("id"),          // ID de la fila
                "BOLETA",                    // tipo de comprobante (fijo por ahora)
                fechaParsed,                 // fecha venta
                List.of(),                   // items no se recuperan aquí
                rs.getString("metodo"),      // método de pago
                0, 0, 0,                     // campos no utilizados
                rs.getInt("total"),          // total del ítem
                0,                           // subtotal no usado
                rs.getInt("total"),          // total venta
                null,                        // caja no se asocia aquí
                rs.getString("user")         // usuario
        );
    }

    // Constructor privado: evita instanciar clase
    private VentasDao() {}
}


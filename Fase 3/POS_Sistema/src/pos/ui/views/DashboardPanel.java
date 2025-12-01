package pos.ui.views;

import org.json.JSONArray;
import org.json.JSONObject;
import pos.services.ReportApi;
import pos.model.Product;
import pos.model.Sale;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class DashboardPanel extends JPanel {

    private final JLabel lblHoy = new JLabel("$0");
    private final JLabel lblSemana = new JLabel("$0");
    private final JLabel lblMes = new JLabel("$0");
    private final JLabel lblProductos = new JLabel("0");

    private final JTable tblLow = new JTable(new LowModel());
    private final JTable tblExp = new JTable(new ExpModel());
    private final JTable tblVentas = new JTable(new VentasModel());

    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
    private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public DashboardPanel() {

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(0xF9FAFB));

        JLabel title = new JLabel("Dashboard — Resumen general");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
        add(title, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 15, 0));
        cards.setOpaque(false);
        cards.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        cards.add(card(lblHoy, "Ventas hoy"));
        cards.add(card(lblSemana, "Ventas 7 días"));
        cards.add(card(lblMes, "Ventas 30 días"));
        cards.add(card(lblProductos, "Productos totales"));
        add(cards, BorderLayout.PAGE_START);

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        refreshPanel.setOpaque(false);
        JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> recargar());
        refreshPanel.add(btnActualizar);
        add(refreshPanel, BorderLayout.AFTER_LAST_LINE);

        JPanel center = new JPanel(new GridLayout(1, 3, 15, 0));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        center.add(wrap("⚠️ Stock bajo (≤3)", new JScrollPane(tblLow)));
        center.add(wrap("⏰ Vencen pronto (≤30 días)", new JScrollPane(tblExp)));
        center.add(wrap("🧾 Ventas recientes", new JScrollPane(tblVentas)));
        add(center, BorderLayout.CENTER);

        configurarTablas();
        recargar();
    }

    private JPanel card(JLabel value, String label) {
        value.setFont(value.getFont().deriveFont(Font.BOLD, 18f));
        value.setForeground(new Color(0x111827));

        JLabel cap = new JLabel(label);
        cap.setForeground(new Color(0x6B7280));
        cap.setFont(cap.getFont().deriveFont(Font.PLAIN, 13f));

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        p.add(value);
        p.add(Box.createVerticalStrut(4));
        p.add(cap);
        return p;
    }

    private JPanel wrap(String title, JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13f));
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        p.add(t, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void configurarTablas() {
        tblLow.setRowHeight(22);
        tblExp.setRowHeight(22);
        tblVentas.setRowHeight(22);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        tblLow.getColumnModel().getColumn(2).setCellRenderer(right);

        tblExp.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.LEFT);

                if (value instanceof LocalDate e) {
                    setText(DF.format(e));
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), e);
                    if (!isSelected) {
                        if (dias <= 7) c.setForeground(new Color(0xDC2626));
                        else if (dias <= 30) c.setForeground(new Color(0xD97706));
                        else c.setForeground(new Color(0x111827));
                    }
                } else setText("-");
                return c;
            }
        });
    }

    // ============================================================
    // 🔥 RECARGAR DESDE API
    // ============================================================
    public void recargar() {
        JSONObject data = ReportApi.getDashboard();
        if (data == null) return;

        // ----- KPIs -----
        JSONObject kpis = data.getJSONObject("kpis");
        lblHoy.setText(CLP.format(kpis.getDouble("sales_today")));
        lblSemana.setText(CLP.format(kpis.getDouble("sales_7d")));
        lblMes.setText(CLP.format(kpis.getDouble("sales_30d")));
        lblProductos.setText(String.valueOf(kpis.getInt("products_in_inventory")));

        // ----- STOCK BAJO -----
        JSONArray low = data.getJSONArray("low_stock");
        ((LowModel) tblLow.getModel()).set(fromJsonToProducts(low));

        // ----- VENCIMIENTO -----
        JSONArray exp = data.getJSONArray("expiring_soon");
        ((ExpModel) tblExp.getModel()).set(fromJsonToProducts(exp));

        // ----- VENTAS RECIENTES -----
        JSONArray rec = data.getJSONArray("recent_sales");
        ((VentasModel) tblVentas.getModel()).set(fromJsonToSales(rec));
    }

    // ============================================================
    // JSON → PRODUCTOS
    // ============================================================
    private static List<Product> fromJsonToProducts(JSONArray arr) {
        List<Product> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);

            Product p = new Product(
                    o.optInt("id", 0),
                    o.optString("code", ""),
                    o.optString("name", ""),
                    o.optInt("price", 0),
                    o.optInt("stock", 0),
                    o.optInt("category_id", 0),
                    o.optBoolean("active", true)
            );

            list.add(p);
        }
        return list;
    }

    // ============================================================
    // JSON → VENTAS
    // ============================================================
    private static List<Sale> fromJsonToSales(JSONArray arr) {
        List<Sale> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);

            Sale s = new Sale(
                    o.optString("id", ""),
                    "BOLETA",
                    LocalDateTime.parse(o.getString("ts")),
                    List.of(),
                    o.optString("payment_method", "cash"),
                    0, 0, 0,
                    0, 0,
                    o.optInt("total", 0),
                    null,
                    "API"
            );

            list.add(s);
        }
        return list;
    }

    // ============================================================
    // MODELOS DE TABLAS
    // ============================================================
    private static class LowModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Stock"};
        private List<Product> data = List.of();
        public void set(List<Product> rows) { data = rows; fireTableDataChanged(); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            Product p = data.get(r);
            return switch (c) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getStock();
                default -> "";
            };
        }
    }

    private static class ExpModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Vence"};
        private List<Product> data = List.of();
        public void set(List<Product> rows) { data = rows; fireTableDataChanged(); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            Product p = data.get(r);
            return switch (c) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getExpiry();
                default -> "";
            };
        }
    }

    private static class VentasModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Fecha", "Total"};
        private List<Sale> data = List.of();
        private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM HH:mm");
        private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        public void set(List<Sale> rows) { data = rows; fireTableDataChanged(); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            Sale s = data.get(r);
            return switch (c) {
                case 0 -> s.getId();
                case 1 -> DF.format(s.getTs());
                case 2 -> CLP.format(s.getTotal());
                default -> "";
            };
        }
    }
}

package pos.ui.views;

import org.json.JSONArray;
import org.json.JSONObject;
import pos.services.ReportApi;
import pos.model.Sale;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportesPanel extends JPanel {

    private final JSpinner dpDesde;
    private final JSpinner dpHasta;

    private final JLabel lblTotalVendido = new JLabel("$0");
    private final JLabel lblVentas = new JLabel("0");
    private final JLabel lblPromedio = new JLabel("$0");
    private final JLabel lblUltimaVenta = new JLabel("-");

    private final JTable tblVentas;
    private final JTable tblMovimientos;

    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
    private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public ReportesPanel() {

        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(0xF9FAFB));

        JLabel titulo = new JLabel("Reportes");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        titulo.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        add(titulo, BorderLayout.NORTH);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);

        dpDesde = new JSpinner(new SpinnerDateModel(cal.getTime(), null, null, Calendar.DAY_OF_MONTH));
        dpHasta = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));

        dpDesde.setEditor(new JSpinner.DateEditor(dpDesde, "yyyy-MM-dd"));
        dpHasta.setEditor(new JSpinner.DateEditor(dpHasta, "yyyy-MM-dd"));

        JButton btnFiltrar = new JButton("Filtrar");
        JButton btnActualizar = new JButton("Actualizar");
        JButton btnExportar = new JButton("Exportar CSV");

        filtros.add(new JLabel("Desde:"));
        filtros.add(dpDesde);
        filtros.add(new JLabel("Hasta:"));
        filtros.add(dpHasta);
        filtros.add(btnFiltrar);
        filtros.add(btnActualizar);
        filtros.add(btnExportar);

        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        metrics.setOpaque(false);
        metrics.add(card(lblTotalVendido, "Total vendido"));
        metrics.add(card(lblVentas, "Ventas"));
        metrics.add(card(lblPromedio, "Promedio ticket"));
        metrics.add(card(lblUltimaVenta, "Última venta"));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        topPanel.add(filtros, BorderLayout.NORTH);
        topPanel.add(metrics, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.setOpaque(false);

        tblVentas = new JTable(new VentasModel());
        tblMovimientos = new JTable(new MovimientosModel());

        center.add(wrap("Ventas en el rango", new JScrollPane(tblVentas)));
        center.add(wrap("Movimientos de caja (Aperturas / Cierres)", new JScrollPane(tblMovimientos)));
        add(center, BorderLayout.CENTER);

        btnFiltrar.addActionListener(e -> reload());
        btnActualizar.addActionListener(e -> reload());
        btnExportar.addActionListener(e -> exportarCSV());

        reload();
    }

    private JPanel card(JLabel value, String label) {
        value.setFont(value.getFont().deriveFont(Font.BOLD, 16f));
        JLabel cap = new JLabel(label);
        cap.setForeground(new Color(0x6B7280));

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        p.add(value);
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

    public void reload() {

        LocalDate desde = toLocalDate((Date) dpDesde.getValue());
        LocalDate hasta = toLocalDate((Date) dpHasta.getValue());

        // ============================================================
        // 🔥 1. VENTAS EN RANGO
        // ============================================================
        JSONArray ventasJson = ReportApi.getSalesRange(desde.toString(), hasta.toString());
        List<Sale> ventas = fromJsonToSales(ventasJson);

        int total = ventas.stream().mapToInt(Sale::getTotal).sum();
        int cantidad = ventas.size();
        int promedio = cantidad > 0 ? total / cantidad : 0;

        lblTotalVendido.setText(CLP.format(total));
        lblVentas.setText(String.valueOf(cantidad));
        lblPromedio.setText(CLP.format(promedio));

        lblUltimaVenta.setText(
                ventas.isEmpty()
                        ? "-"
                        : DF.format(ventas.get(0).getTs())
        );

        ((VentasModel) tblVentas.getModel()).setData(ventas);

        // ============================================================
        // 🔥 2. MOVIMIENTOS DE CAJA
        // ============================================================
        JSONArray movJson = ReportApi.getCashMovements(desde.toString(), hasta.toString());
        List<Object[]> movs = fromJsonToMovements(movJson);
        ((MovimientosModel) tblMovimientos.getModel()).setData(movs);
    }

    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // ============================================================
    // JSON → VENTAS
    // ============================================================
    private List<Sale> fromJsonToSales(JSONArray arr) {

        List<Sale> list = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);

            Sale s = new Sale(
                    o.optString("id", ""),
                    o.optString("doc_type", "BOLETA"),
                    LocalDateTime.parse(o.getString("ts")),
                    List.of(),
                    o.optString("payment_method", "cash"),
                    0, 0, 0,
                    0, 0,
                    o.optInt("total", 0),
                    null,
                    o.optString("user", "API")
            );

            list.add(s);
        }

        return list;
    }

    // ============================================================
    // JSON → MOVIMIENTOS CAJA
    // ============================================================
    private List<Object[]> fromJsonToMovements(JSONArray arr) {

        List<Object[]> list = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);

            list.add(new Object[]{
                    o.getString("type"),
                    o.getString("ts"),
                    CLP.format(o.getInt("amount"))
            });
        }
        return list;
    }

    private void exportarCSV() {
        try {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {

                FileWriter fw = new FileWriter(fc.getSelectedFile() + ".csv");

                fw.write("Tipo,Hora,Monto\n");

                for (Object[] row : ((MovimientosModel) tblMovimientos.getModel()).getData()) {
                    for (int i = 0; i < row.length; i++) {
                        fw.write(row[i].toString());
                        if (i < row.length - 1) fw.write(","); // separador
                    }
                    fw.write("\n");
                }

                fw.close();

                JOptionPane.showMessageDialog(this, "Archivo CSV exportado correctamente.");
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al exportar CSV: " + ex.getMessage());
        }
    }

    private static class VentasModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Documento", "Método", "Total"};
        private List<Sale> data = new ArrayList<>();

        public void setData(List<Sale> rows) {
            data = rows;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Sale s = data.get(r);
            return switch (c) {
                case 0 -> s.getId();
                case 1 -> s.getDocType();
                case 2 -> s.getPaymentMethod();
                case 3 -> NumberFormat.getCurrencyInstance(new Locale("es", "CL")).format(s.getTotal());
                default -> "";
            };
        }
    }

    private static class MovimientosModel extends AbstractTableModel {

        private final String[] cols = {"Tipo", "Hora", "Monto"};
        private List<Object[]> data = new ArrayList<>();

        public void setData(List<Object[]> rows) {
            data = rows;
            fireTableDataChanged();
        }

        public List<Object[]> getData() { return data; }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            return data.get(r)[c];
        }
    }
}
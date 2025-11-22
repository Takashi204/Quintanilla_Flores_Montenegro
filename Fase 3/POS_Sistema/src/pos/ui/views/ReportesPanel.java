package pos.ui.views;

import java.awt.*;
import java.io.FileWriter;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import pos.dao.VentasDao;
import pos.model.Sale;

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

        // === Título ===
        JLabel titulo = new JLabel("Reportes");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        titulo.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        add(titulo, BorderLayout.NORTH);

        // === Filtros ===
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

        // === Métricas ===
        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        metrics.setOpaque(false);
        metrics.add(card(lblTotalVendido, "Total vendido"));
        metrics.add(card(lblVentas, "Ventas"));
        metrics.add(card(lblPromedio, "Promedio ticket"));
        metrics.add(card(lblUltimaVenta, "Última venta"));

        // === Panel superior combinado ===
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        topPanel.add(filtros, BorderLayout.NORTH);
        topPanel.add(metrics, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // === Tablas ===
        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.setOpaque(false);
        tblVentas = new JTable(new VentasModel());
        tblMovimientos = new JTable(new MovimientosModel());

        center.add(wrap("Ventas en el rango", new JScrollPane(tblVentas)));
        center.add(wrap("Movimientos de caja (Aperturas / Cierres)", new JScrollPane(tblMovimientos)));
        add(center, BorderLayout.CENTER);

        // === Acciones ===
        btnFiltrar.addActionListener(e -> reload());
        btnActualizar.addActionListener(e -> reload());
        btnExportar.addActionListener(e -> exportarCSV());

        reload();
    }

    // ------------ UI helpers ------------

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

    // ------------ Lógica principal ------------

    public void reload() {
        LocalDate desde = toLocalDate((Date) dpDesde.getValue());
        LocalDate hasta = toLocalDate((Date) dpHasta.getValue());

        // Ventas desde VentasDao
        List<Sale> ventas = VentasDao.listAll();

        List<Sale> filtradas = ventas.stream()
                .filter(v -> {
                    LocalDate d = v.getTs().toLocalDate();
                    return (d.isEqual(desde) || d.isAfter(desde)) &&
                           (d.isEqual(hasta) || d.isBefore(hasta));
                })
                .collect(Collectors.toList());   // <- compatible con Java 8

        int total = filtradas.stream().mapToInt(Sale::getTotal).sum();
        int cantidad = filtradas.size();
        int promedio = (cantidad > 0) ? total / cantidad : 0;

        lblTotalVendido.setText(CLP.format(total));
        lblVentas.setText(String.valueOf(cantidad));
        lblPromedio.setText(CLP.format(promedio));
        lblUltimaVenta.setText(filtradas.isEmpty()
                ? "-"
                : DF.format(filtradas.get(0).getTs()));

        ((VentasModel) tblVentas.getModel()).setData(filtradas);
        ((MovimientosModel) tblMovimientos.getModel()).setData(listarMovimientos(desde, hasta));
    }

    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // ------------ MOVIMIENTOS DE CAJA (FIX) ------------

    /**
     * Por cada fila de cash_sessions se generan:
     *  - 1 fila "Apertura" (open_time, monto_inicial)
     *  - 1 fila "Cierre"  (close_time, monto_final) SOLO si closed = 1
     */
    private List<Object[]> listarMovimientos(LocalDate desde, LocalDate hasta) {
        List<Object[]> data = new ArrayList<>();

        try (Connection cn = pos.db.Database.get();
             PreparedStatement ps = cn.prepareStatement("""
                 SELECT id, user, open_time, close_time,
                        monto_inicial, monto_final, closed
                   FROM cash_sessions
                  WHERE DATE(open_time) BETWEEN DATE(?) AND DATE(?)
                     OR (close_time IS NOT NULL AND DATE(close_time) BETWEEN DATE(?) AND DATE(?))
                  ORDER BY datetime(open_time) DESC
             """)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            ps.setString(3, desde.toString());
            ps.setString(4, hasta.toString());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String openStr = rs.getString("open_time");
                LocalDateTime openTime = LocalDateTime.parse(openStr);
                int montoInicial = rs.getInt("monto_inicial");

                // Siempre agregamos una fila de APERTURA
                data.add(new Object[]{
                        "Apertura",
                        DF.format(openTime),
                        CLP.format(montoInicial)
                });

                // Si está cerrada, agregamos también la fila de CIERRE
                int closed = rs.getInt("closed");
                String closeStr = rs.getString("close_time");
                if (closed == 1 && closeStr != null) {
                    LocalDateTime closeTime = LocalDateTime.parse(closeStr);
                    int montoFinal = rs.getInt("monto_final");

                    data.add(new Object[]{
                            "Cierre",
                            DF.format(closeTime),
                            CLP.format(montoFinal)
                    });
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReportesPanel.listarMovimientos] " + e.getMessage());
        }

        return data;
    }

    // ------------ Exportar CSV ------------

    private void exportarCSV() {
        try {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (FileWriter fw = new FileWriter(fc.getSelectedFile() + ".csv")) {

                    fw.write("Tipo,Hora,Monto\n");

                    for (Object[] row : ((MovimientosModel) tblMovimientos.getModel()).getData()) {
                        fw.write(String.join(",",
                                Arrays.stream(row)
                                        .map(Object::toString)
                                        .toArray(String[]::new)));
                        fw.write("\n");
                    }
                }
                JOptionPane.showMessageDialog(this, "Archivo CSV exportado correctamente.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al exportar CSV: " + ex.getMessage());
        }
    }

    // ------------ Modelos de tabla ------------

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
            switch (c) {
                case 0: return s.getId();
                case 1: return s.getDocType();
                case 2: return s.getPaymentMethod();
                case 3:
                    return NumberFormat
                            .getCurrencyInstance(new Locale("es", "CL"))
                            .format(s.getTotal());
                default: return "";
            }
        }
    }

    private static class MovimientosModel extends AbstractTableModel {
        private final String[] cols = {"Tipo", "Hora", "Monto"};
        private List<Object[]> data = new ArrayList<>();

        public void setData(List<Object[]> rows) {
            data = rows;
            fireTableDataChanged();
        }

        public List<Object[]> getData() {
            return data;
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            return data.get(r)[c];
        }
    }
}

package pos.ui.views; // Panel de reportes dentro de las vistas UI

import java.awt.*; // Layouts y colores
import java.io.FileWriter; // Para exportar CSV
import java.sql.*; // Conexión SQL para movimientos de caja
import java.text.NumberFormat; // Formato moneda CLP
import java.time.LocalDate; // Fechas (solo día)
import java.time.LocalDateTime; // Fechas con hora
import java.time.ZoneId; // Zona horaria para conversión Date -> LocalDate
import java.time.format.DateTimeFormatter; // Formato de fechas
import java.util.ArrayList; // Listas dinámicas
import java.util.Arrays; // Util para exportación CSV
import java.util.Calendar; // Manipular fechas en Spinner
import java.util.Date; // Tipo del Spinner
import java.util.List; // Collections
import java.util.Locale; // Localización CLP
import java.util.stream.Collectors; // Filtrado de listas

import javax.swing.*; // Componentes Swing
import javax.swing.table.AbstractTableModel; // Modelos de tabla

import pos.dao.VentasDao; // DAO de ventas
import pos.model.Sale; // Modelo de una venta

public class ReportesPanel extends JPanel { // Panel principal de reportes

    private final JSpinner dpDesde; // Selector de fecha "desde"
    private final JSpinner dpHasta; // Selector de fecha "hasta"
    private final JLabel lblTotalVendido = new JLabel("$0"); // Métrica total vendido
    private final JLabel lblVentas = new JLabel("0"); // Cantidad de ventas
    private final JLabel lblPromedio = new JLabel("$0"); // Ticket promedio
    private final JLabel lblUltimaVenta = new JLabel("-"); // Fecha última venta
    private final JTable tblVentas; // Tabla de ventas filtradas
    private final JTable tblMovimientos; // Tabla de movimientos de caja

    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es", "CL")); // Formato CLP
    private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"); // Formato fecha completa

    public ReportesPanel() { // Constructor
        setLayout(new BorderLayout(12, 12)); // Margen general
        setBackground(new Color(0xF9FAFB)); // Fondo elegante

        JLabel titulo = new JLabel("Reportes"); // Título
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f)); // Estilo grande
        titulo.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12)); // Padding
        add(titulo, BorderLayout.NORTH); // Arriba

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); // Panel filtros
        filtros.setOpaque(false); // Transparente

        Calendar cal = Calendar.getInstance(); // Fecha actual
        cal.add(Calendar.DAY_OF_MONTH, -7); // 7 días atrás para fecha inicial

        dpDesde = new JSpinner(new SpinnerDateModel(cal.getTime(), null, null, Calendar.DAY_OF_MONTH)); // Fecha desde por defecto
        dpHasta = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH)); // Fecha hasta hoy

        dpDesde.setEditor(new JSpinner.DateEditor(dpDesde, "yyyy-MM-dd")); // Formato visible
        dpHasta.setEditor(new JSpinner.DateEditor(dpHasta, "yyyy-MM-dd")); // Formato visible

        JButton btnFiltrar = new JButton("Filtrar"); // Botón filtrar rango
        JButton btnActualizar = new JButton("Actualizar"); // Refrescar
        JButton btnExportar = new JButton("Exportar CSV"); // Exportar CSV

        filtros.add(new JLabel("Desde:")); filtros.add(dpDesde); // Agregar inputs
        filtros.add(new JLabel("Hasta:")); filtros.add(dpHasta);
        filtros.add(btnFiltrar); filtros.add(btnActualizar); filtros.add(btnExportar);

        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8)); // Métricas superiores
        metrics.setOpaque(false);
        metrics.add(card(lblTotalVendido, "Total vendido")); // Card 1
        metrics.add(card(lblVentas, "Ventas")); // Card 2
        metrics.add(card(lblPromedio, "Promedio ticket")); // Card 3
        metrics.add(card(lblUltimaVenta, "Última venta")); // Card 4

        JPanel topPanel = new JPanel(new BorderLayout()); // Panel superior global
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12)); // Margen
        topPanel.add(filtros, BorderLayout.NORTH); // Filtros arriba
        topPanel.add(metrics, BorderLayout.SOUTH); // Métricas abajo
        add(topPanel, BorderLayout.NORTH); // Panel superior

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10)); // Parte central
        center.setOpaque(false);

        tblVentas = new JTable(new VentasModel()); // Tabla ventas
        tblMovimientos = new JTable(new MovimientosModel()); // Tabla movimientos

        center.add(wrap("Ventas en el rango", new JScrollPane(tblVentas))); // Izquierda
        center.add(wrap("Movimientos de caja (Aperturas / Cierres)", new JScrollPane(tblMovimientos))); // Derecha
        add(center, BorderLayout.CENTER); // Agregar al panel

        btnFiltrar.addActionListener(e -> reload()); // Filtrar
        btnActualizar.addActionListener(e -> reload()); // Actualizar
        btnExportar.addActionListener(e -> exportarCSV()); // Exportar CSV

        reload(); // Carga inicial
    }

    private JPanel card(JLabel value, String label) { // Card métrica
        value.setFont(value.getFont().deriveFont(Font.BOLD, 16f)); // Valor grande
        JLabel cap = new JLabel(label); // Texto pequeño
        cap.setForeground(new Color(0x6B7280)); // Gris suave

        JPanel p = new JPanel(); // Card contenedor
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); // Vertical
        p.setBackground(Color.WHITE); // Fondo blanco
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)), // Borde gris
                BorderFactory.createEmptyBorder(8, 14, 8, 14) // Padding
        ));
        p.add(value); // Agrega valor
        p.add(cap); // Agrega etiqueta
        return p;
    }

    private JPanel wrap(String title, JComponent c) { // Encabezado de tabla
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = new JLabel(title); // Título
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13f)); // Estilo
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0)); // Margin bottom
        p.add(t, BorderLayout.NORTH); // Arriba
        p.add(c, BorderLayout.CENTER); // Tabla
        return p;
    }

    public void reload() { // Recargar datos según rango
        LocalDate desde = toLocalDate((Date) dpDesde.getValue()); // Convierte date → LocalDate
        LocalDate hasta = toLocalDate((Date) dpHasta.getValue());

        List<Sale> ventas = VentasDao.listAll(); // Todas las ventas

        List<Sale> filtradas = ventas.stream() // Filtrar rango
                .filter(v -> {
                    LocalDate d = v.getTs().toLocalDate(); // Fecha venta
                    return (d.isEqual(desde) || d.isAfter(desde)) &&
                           (d.isEqual(hasta) || d.isBefore(hasta));
                })
                .collect(Collectors.toList()); // Compatible con Java 8

        int total = filtradas.stream().mapToInt(Sale::getTotal).sum(); // Total ventas
        int cantidad = filtradas.size(); // Número de ventas
        int promedio = (cantidad > 0) ? total / cantidad : 0; // Ticket promedio

        lblTotalVendido.setText(CLP.format(total)); // Mostrar total
        lblVentas.setText(String.valueOf(cantidad)); // Mostrar cantidad
        lblPromedio.setText(CLP.format(promedio)); // Mostrar ticket promedio
        lblUltimaVenta.setText(filtradas.isEmpty()
                ? "-"
                : DF.format(filtradas.get(0).getTs())); // Última venta

        ((VentasModel) tblVentas.getModel()).setData(filtradas); // Recargar tabla ventas
        ((MovimientosModel) tblMovimientos.getModel()).setData(listarMovimientos(desde, hasta)); // Recargar caja
    }

    private static LocalDate toLocalDate(Date d) { // Convertir Date → LocalDate
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private List<Object[]> listarMovimientos(LocalDate desde, LocalDate hasta) { // Movimientos caja
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

                data.add(new Object[]{
                        "Apertura",
                        DF.format(openTime),
                        CLP.format(montoInicial)
                });

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

    private void exportarCSV() { // Exportar CSV
        try {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (FileWriter fw = new FileWriter(fc.getSelectedFile() + ".csv")) {

                    fw.write("Tipo,Hora,Monto\n"); // Encabezado

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

    private static class VentasModel extends AbstractTableModel { // Modelo tabla ventas
        private final String[] cols = {"ID", "Documento", "Método", "Total"}; // Encabezados
        private List<Sale> data = new ArrayList<>();

        public void setData(List<Sale> rows) { // Cargar filas
            data = rows;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) { // Datos por columna
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

    private static class MovimientosModel extends AbstractTableModel { // Modelo caja
        private final String[] cols = {"Tipo", "Hora", "Monto"};
        private List<Object[]> data = new ArrayList<>();

        public void setData(List<Object[]> rows) { // Cargar filas
            data = rows;
            fireTableDataChanged();
        }

        public List<Object[]> getData() { return data; } // Usado para exportar

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) { // Mostrar celda
            return data.get(r)[c];
        }
    }
}

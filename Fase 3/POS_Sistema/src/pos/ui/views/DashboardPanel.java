package pos.ui.views; // Vista principal del dashboard

import pos.dao.InventoryDao; // DAO para obtener productos
import pos.dao.VentasDao; // DAO para obtener ventas
import pos.model.Product; // Modelo de producto
import pos.model.Sale; // Modelo de venta

import javax.swing.*; // Componentes Swing
import javax.swing.table.AbstractTableModel; // Base para modelos de tabla
import javax.swing.table.DefaultTableCellRenderer; // Renderizado de celdas
import java.awt.*; // Layouts y UI general
import java.text.NumberFormat; // Formato de moneda CLP
import java.time.LocalDate; // Fechas
import java.time.format.DateTimeFormatter; // Formato fechas
import java.util.List; // Listas
import java.util.Locale; // Configuración regional para CLP
import java.util.stream.Collectors; // Filtrar listas con streams

public class DashboardPanel extends JPanel { // Panel del dashboard

    private final JLabel lblHoy = new JLabel("$0"); // Monto ventas del día
    private final JLabel lblSemana = new JLabel("$0"); // Monto ventas últimos 7 días
    private final JLabel lblMes = new JLabel("$0"); // Monto ventas últimos 30 días
    private final JLabel lblProductos = new JLabel("0"); // Cantidad total de productos

    private final JTable tblLow = new JTable(new LowModel()); // Tabla de bajo stock
    private final JTable tblExp = new JTable(new ExpModel()); // Tabla de productos pronto a vencer
    private final JTable tblVentas = new JTable(new VentasModel()); // Tabla de ventas recientes

    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es", "CL")); // Formateador CLP
    private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy"); // Formateador fecha dd-MM-YYYY

    public DashboardPanel() { // Constructor
        setLayout(new BorderLayout(15, 15)); // Margen entre paneles
        setBackground(new Color(0xF9FAFB)); // Gris clarito estilo moderno

        JLabel title = new JLabel("Dashboard — Resumen general"); // Título
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f)); // Negrita tamaño 18
        title.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16)); // Margen superior
        add(title, BorderLayout.NORTH); // Lo coloca arriba

        JPanel cards = new JPanel(new GridLayout(1, 4, 15, 0)); // 4 tarjetas horizontales
        cards.setOpaque(false); // Fondo transparente
        cards.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16)); // Margen
        cards.add(card(lblHoy, "Ventas hoy")); // Tarjeta hoy
        cards.add(card(lblSemana, "Ventas 7 días")); // Tarjeta semana
        cards.add(card(lblMes, "Ventas 30 días")); // Tarjeta mes
        cards.add(card(lblProductos, "Productos totales")); // Tarjeta total productos
        add(cards, BorderLayout.PAGE_START); // Ubica arriba

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0)); // Panel refresh
        refreshPanel.setOpaque(false);
        JButton btnActualizar = new JButton("Actualizar"); // Botón actualizar
        btnActualizar.setFont(new Font("SansSerif", Font.PLAIN, 13)); // Fuente
        btnActualizar.addActionListener(e -> recargar()); // Acción recargar dashboard
        refreshPanel.add(btnActualizar); // Agregar botón
        add(refreshPanel, BorderLayout.AFTER_LAST_LINE); // Lo pone debajo de las cards

        JPanel center = new JPanel(new GridLayout(1, 3, 15, 0)); // 3 columnas
        center.setOpaque(false); // Sin fondo
        center.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16)); // Margen
        center.add(wrap("⚠️ Stock bajo (≤3)", new JScrollPane(tblLow))); // Tabla stock bajo
        center.add(wrap("⏰ Vencen pronto (≤30 días)", new JScrollPane(tblExp))); // Tabla vencimientos
        center.add(wrap("🧾 Ventas recientes", new JScrollPane(tblVentas))); // Tabla ventas
        add(center, BorderLayout.CENTER); // Ubicación central

        configurarTablas(); // Ajustes visuales de tablas
        recargar(); // Cargar datos iniciales
    }

    private JPanel card(JLabel value, String label) { // Crea una tarjeta estilo resumen
        value.setFont(value.getFont().deriveFont(Font.BOLD, 18f)); // Valor grande
        value.setForeground(new Color(0x111827)); // Texto negro elegante

        JLabel cap = new JLabel(label); // Título de la tarjeta
        cap.setForeground(new Color(0x6B7280)); // Gris suave
        cap.setFont(cap.getFont().deriveFont(Font.PLAIN, 13f));

        JPanel p = new JPanel(); // Panel contenedor
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); // Vertical
        p.setBackground(Color.WHITE); // Fondo blanco
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)), // Borde gris
                BorderFactory.createEmptyBorder(10, 16, 10, 16) // Padding
        ));
        p.add(value); // Monto
        p.add(Box.createVerticalStrut(4)); // Espacio
        p.add(cap); // Etiqueta
        return p;
    }

    private JPanel wrap(String title, JComponent c) { // Envuelve una tabla con título
        JPanel p = new JPanel(new BorderLayout()); // Layout
        p.setOpaque(false); // Transparente
        JLabel t = new JLabel(title); // Título
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13f)); // Negrita
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0)); // Margin bottom
        p.add(t, BorderLayout.NORTH); // Arriba
        p.add(c, BorderLayout.CENTER); // Tabla
        return p;
    }

    private void configurarTablas() { // Configura estilos
        tblLow.setRowHeight(22); // Alto filas
        tblExp.setRowHeight(22);
        tblVentas.setRowHeight(22);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer(); // Renderer alineado derecha
        right.setHorizontalAlignment(SwingConstants.RIGHT); // Alineación
        tblLow.getColumnModel().getColumn(2).setCellRenderer(right); // Stock alineado derecha

        tblExp.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.LEFT); // Alinear fecha izquierda
                if (value instanceof LocalDate e) { // Si es fecha
                    setText(DF.format(e)); // Formatear fecha
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), e); // Días restantes
                    if (!isSelected) { // Si no está seleccionada
                        if (dias <= 7) c.setForeground(new Color(0xDC2626)); // Rojo (muy urgente)
                        else if (dias <= 30) c.setForeground(new Color(0xD97706)); // Naranjo (pronto)
                        else c.setForeground(new Color(0x111827)); // Normal
                    }
                } else setText("-"); // Sin fecha
                return c; // Retorna celda
            }
        });
    }

    public void recargar() { // Refrescar dashboard completo
        lblHoy.setText(CLP.format(VentasDao.totalHoy())); // Ventas de hoy
        lblSemana.setText(CLP.format(VentasDao.totalSemana())); // 7 días
        lblMes.setText(CLP.format(VentasDao.totalMes())); // 30 días

        List<Product> productos = InventoryDao.getAll(); // Obtener productos
        lblProductos.setText(String.valueOf(productos.size())); // Cantidad total

        ((LowModel) tblLow.getModel()).set(
                productos.stream().filter(Product::hasLowStock).collect(Collectors.toList()) // Stock <=3
        );

        ((ExpModel) tblExp.getModel()).set(
                productos.stream()
                        .filter(p -> p.getExpiry() != null && // Tiene fecha
                                !p.isExpired() && // No vencido
                                java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), p.getExpiry()) <= 30)
                        .collect(Collectors.toList())
        );

        ((VentasModel) tblVentas.getModel()).set(VentasDao.listRecent(10)); // Últimas 10 ventas
    }

    private static class LowModel extends AbstractTableModel { // Tabla stock bajo
        private final String[] cols = {"Código", "Nombre", "Stock"}; // Columnas
        private List<Product> data = List.of(); // Lista vacía
        public void set(List<Product> rows) { data = rows; fireTableDataChanged(); } // Cargar datos
        @Override public int getRowCount() { return data.size(); } // Cantidad filas
        @Override public int getColumnCount() { return cols.length; } // Cant columnas
        @Override public String getColumnName(int c) { return cols[c]; } // Nombre columna
        @Override public Object getValueAt(int r, int c) { // Datos celda
            Product p = data.get(r);
            return switch (c) {
                case 0 -> p.getCode(); // Código
                case 1 -> p.getName(); // Nombre
                case 2 -> p.getStock(); // Stock
                default -> "";
            };
        }
    }

    private static class ExpModel extends AbstractTableModel { // Tabla productos por vencer
        private final String[] cols = {"Código", "Nombre", "Vence"}; // Columnas
        private List<Product> data = List.of(); // Vacía
        public void set(List<Product> rows) { data = rows; fireTableDataChanged(); } // Cargar
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

    private static class VentasModel extends AbstractTableModel { // Tabla ventas recientes
        private final String[] cols = {"ID", "Fecha", "Total"}; // Columnas
        private List<Sale> data = List.of(); // Lista ventas
        private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM HH:mm"); // Fecha corta
        private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es", "CL")); // CLP
        public void set(List<Sale> rows) { data = rows; fireTableDataChanged(); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            Sale s = data.get(r);
            return switch (c) {
                case 0 -> s.getId(); // ID venta
                case 1 -> DF.format(s.getTs()); // Fecha formateada
                case 2 -> CLP.format(s.getTotal()); // Monto
                default -> "";
            };
        }
    }
}

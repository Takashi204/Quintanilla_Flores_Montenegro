package pos.ui.views;

import pos.model.Product;
import pos.store.InMemoryStore;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class InventarioPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final JTextField txtBuscar = new JTextField(20);
    private final JTable tabla = new JTable(new StockModel());
    private final StockModel modelo = (StockModel) tabla.getModel();

    public InventarioPanel() {
        setLayout(new BorderLayout(10,10));
        setBackground(new Color(0xF9FAFB));

        // Título
        JLabel title = new JLabel("Inventario");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(12,12,0,12));
        add(title, BorderLayout.NORTH);

        // Barra superior
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        barra.setOpaque(false);

        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar por código o nombre...");
        JButton btnBuscar  = new JButton("Buscar");
        JButton btnEntrada = new JButton("Entrada (+)");
        JButton btnSalida  = new JButton("Salida (–)");
        JButton btnAjuste  = new JButton("Ajuste");

        barra.add(txtBuscar);
        barra.add(btnBuscar);
        barra.add(btnEntrada);
        barra.add(btnSalida);
        barra.add(btnAjuste);
        add(barra, BorderLayout.PAGE_START);

        // Tabla
        tabla.setRowHeight(22);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Carga inicial
        recargar();

        // Acciones
        btnBuscar.addActionListener(e -> buscar());
        btnEntrada.addActionListener(e -> moverStock(+1));
        btnSalida.addActionListener(e -> moverStock(-1));
        btnAjuste.addActionListener(e -> ajustarStock());
    }

    private void recargar() {
        modelo.set(InMemoryStore.getAllProducts());
    }

    private void buscar() {
        String q = txtBuscar.getText().trim().toLowerCase();
        if (q.isEmpty()) { recargar(); return; }
        List<Product> filtrados = InMemoryStore.getAllProducts().stream()
                .filter(p -> p.getName().toLowerCase().contains(q) || p.getCode().toLowerCase().contains(q))
                .collect(Collectors.toList());
        modelo.set(filtrados);
    }

    /** Entrada (+) o Salida (–) según sign: +1 o -1 */
    private void moverStock(int sign) {
        int row = tabla.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto"); return; }
        Product p = modelo.getAt(row);

        String s = JOptionPane.showInputDialog(this,
                (sign > 0 ? "Cantidad a ingresar (+):" : "Cantidad a retirar (–):"),
                "0");
        if (s == null) return;
        try {
            int qty = Integer.parseInt(s);
            if (qty < 0) qty = -qty; // si escriben negativo
            int nuevo = p.getStock() + sign * qty;
            if (nuevo < 0) { JOptionPane.showMessageDialog(this, "No puede quedar negativo"); return; }
            p.setStock(nuevo);
            InMemoryStore.updateProduct(p); // upsert si no existiera
            recargar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida");
        }
    }

    /** Ajuste directo: fija el stock a un valor */
    private void ajustarStock() {
        int row = tabla.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto"); return; }
        Product p = modelo.getAt(row);

        String s = JOptionPane.showInputDialog(this,
                "Nuevo stock para " + p.getName() + ":",
                String.valueOf(p.getStock()));
        if (s == null) return;
        try {
            int nuevo = Integer.parseInt(s);
            if (nuevo < 0) { JOptionPane.showMessageDialog(this, "No puede ser negativo"); return; }
            p.setStock(nuevo);
            InMemoryStore.updateProduct(p);
            recargar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Valor inválido");
        }
    }

    // ===== Modelo de tabla (solo lectura) =====
    private static class StockModel extends AbstractTableModel {
        private final String[] cols = {"Código","Nombre","Categoría","Stock","Estado","Vence"};
        private List<Product> data = List.of();

        public void set(List<Product> rows) { data = rows; fireTableDataChanged(); }
        public Product getAt(int row) { return data.get(row); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Product p = data.get(r);
            switch (c) {
                case 0: return p.getCode();
                case 1: return p.getName();
                case 2: return p.getCategory();
                case 3: return p.getStock();
                case 4: return (p.getStock() <= 5 ? "Bajo" : "OK");
                case 5: return p.getExpiry()==null ? "-" : DF.format(p.getExpiry());
                default: return "";
            }
        }
    }
}

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

public class ProductosPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final JTextField txtBuscar = new JTextField(20);
    private final JTable tabla = new JTable(new ProductosModel());
    private final ProductosModel modelo = (ProductosModel) tabla.getModel();

    public ProductosPanel() {
        setLayout(new BorderLayout(10,10));
        setBackground(new Color(0xF9FAFB));

        // Título
        JLabel title = new JLabel("Gestión de Productos");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(12,12,0,12));
        add(title, BorderLayout.NORTH);

        // Barra superior
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        barra.setOpaque(false);

        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar por nombre o código...");
        JButton btnBuscar   = new JButton("Buscar");
        JButton btnNuevo    = new JButton("Nuevo");
        JButton btnEditar   = new JButton("Editar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnRapido   = new JButton("Registro rápido");

        barra.add(txtBuscar);
        barra.add(btnBuscar);
        barra.add(btnNuevo);
        barra.add(btnEditar);
        barra.add(btnEliminar);
        barra.add(btnRapido);
        add(barra, BorderLayout.PAGE_START);

        // Tabla
        tabla.setRowHeight(22);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Carga inicial
        recargar();

        // Acciones
        btnBuscar.addActionListener(e -> buscar());
        btnNuevo.addActionListener(e -> nuevoProducto());
        btnEditar.addActionListener(e -> editarProducto());
        btnEliminar.addActionListener(e -> eliminarProducto());
        btnRapido.addActionListener(e -> registroRapido());
    }

    // ===== Funciones =====
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

    private void nuevoProducto() {
        JTextField codigo    = new JTextField(InMemoryStore.nextCode());
        JTextField nombre    = new JTextField();
        JTextField categoria = new JTextField("General");
        JTextField precio    = new JTextField();
        JTextField stock     = new JTextField();
        JTextField fecha     = new JTextField(LocalDate.now().plusMonths(6).toString()); // AAAA-MM-DD

        JPanel panel = new JPanel(new GridLayout(0,2,6,6));
        panel.add(new JLabel("Código:"));      panel.add(codigo);
        panel.add(new JLabel("Nombre:"));      panel.add(nombre);
        panel.add(new JLabel("Categoría:"));   panel.add(categoria);
        panel.add(new JLabel("Precio:"));      panel.add(precio);
        panel.add(new JLabel("Stock:"));       panel.add(stock);
        panel.add(new JLabel("Vencimiento (AAAA-MM-DD):")); panel.add(fecha);

        int res = JOptionPane.showConfirmDialog(this, panel, "Nuevo producto", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                LocalDate exp = fecha.getText().isBlank() ? null : LocalDate.parse(fecha.getText());
                Product p = new Product(
                        codigo.getText(),
                        nombre.getText(),
                        categoria.getText(),
                        Integer.parseInt(precio.getText()),
                        Integer.parseInt(stock.getText()),
                        exp
                );
                InMemoryStore.addProduct(p);
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Datos inválidos");
            }
        }
    }

    private void editarProducto() {
        int row = tabla.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto para editar"); return; }
        Product p = modelo.getAt(row);

        JTextField nombre    = new JTextField(p.getName());
        JTextField categoria = new JTextField(p.getCategory());
        JTextField precio    = new JTextField(String.valueOf(p.getPrice()));
        JTextField stock     = new JTextField(String.valueOf(p.getStock()));
        JTextField fecha     = new JTextField(p.getExpiry()==null ? "" : p.getExpiry().toString());

        JPanel panel = new JPanel(new GridLayout(0,2,6,6));
        panel.add(new JLabel("Código:"));      panel.add(new JLabel(p.getCode()));
        panel.add(new JLabel("Nombre:"));      panel.add(nombre);
        panel.add(new JLabel("Categoría:"));   panel.add(categoria);
        panel.add(new JLabel("Precio:"));      panel.add(precio);
        panel.add(new JLabel("Stock:"));       panel.add(stock);
        panel.add(new JLabel("Vencimiento:")); panel.add(fecha);

        int res = JOptionPane.showConfirmDialog(this, panel, "Editar producto", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                p.setName(nombre.getText());
                p.setCategory(categoria.getText());
                p.setPrice(Integer.parseInt(precio.getText()));
                p.setStock(Integer.parseInt(stock.getText()));
                p.setExpiry(fecha.getText().isBlank() ? null : LocalDate.parse(fecha.getText()));
                InMemoryStore.updateProduct(p);
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al editar producto");
            }
        }
    }

    private void eliminarProducto() {
        int row = tabla.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto para eliminar"); return; }
        Product p = modelo.getAt(row);
        int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar " + p.getName() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            InMemoryStore.removeProduct(p.getCode());
            recargar();
        }
    }

    private void registroRapido() {
        JTextField codigo = new JTextField(InMemoryStore.nextCode());
        JTextField nombre = new JTextField();
        JTextField precio = new JTextField();
        JTextField stock  = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0,2,6,6));
        panel.add(new JLabel("Código:")); panel.add(codigo);
        panel.add(new JLabel("Nombre:")); panel.add(nombre);
        panel.add(new JLabel("Precio:")); panel.add(precio);
        panel.add(new JLabel("Stock:"));  panel.add(stock);

        int res = JOptionPane.showConfirmDialog(this, panel, "Registro rápido", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Product p = new Product(
                        codigo.getText(),
                        nombre.getText(),
                        "General",                             // Categoría por defecto
                        Integer.parseInt(precio.getText()),
                        Integer.parseInt(stock.getText()),
                        LocalDate.now().plusMonths(6)          // Vencimiento por defecto
                );
                InMemoryStore.addProduct(p);
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error en los datos ingresados");
            }
        }
    }

    // ===== Modelo de tabla =====
    private static class ProductosModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Categoría", "Precio", "Stock", "Vence"};
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
                case 3: return "$" + p.getPrice();
                case 4: return p.getStock();
                case 5: return p.getExpiry()==null ? "-" : DF.format(p.getExpiry());
                default: return "";
            }
        }
    }
}


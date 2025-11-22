package pos.ui.views;

import pos.dao.InventoryDao;
import pos.dao.MovementDao;
import pos.model.Product;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Panel principal de gestión de inventario.
 * Sin dependencias de InventoryMovement.
 */
public class InventarioPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // Barra superior
    private final JTextField txtBuscar = new JTextField(18);
    private final JButton btnBuscar    = new JButton("Buscar");
    private final JButton btnEntrada   = new JButton("Entrada (+)");
    private final JButton btnSalida    = new JButton("Salida (−)");
    private final JButton btnAjuste    = new JButton("Ajuste");
    private final JButton btnHistorial = new JButton("Historial");
    private final JButton btnEliminar  = new JButton("Eliminar");

    // Tabla
    private final JTable tabla = new JTable(new ProductosModel());
    private final ProductosModel modelo = (ProductosModel) tabla.getModel();

    // DAOs
    private final InventoryDao dao = new InventoryDao();
    private final MovementDao movementDao = new MovementDao();

    private final String currentUser = "admin"; // reemplazar por usuario logueado real

    public InventarioPanel() {
        setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar por nombre o código…");
        top.add(txtBuscar);
        top.add(btnBuscar);
        top.add(btnEntrada);
        top.add(btnSalida);
        top.add(btnAjuste);
        top.add(btnHistorial);
        top.add(btnEliminar);
        add(top, BorderLayout.NORTH);

        tabla.setRowHeight(22);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        recargar();

        btnBuscar.addActionListener(e -> filtrar());
        btnEntrada.addActionListener(e -> onEntrada());
        btnSalida.addActionListener(e -> onSalida());
        btnAjuste.addActionListener(e -> onAjuste());
        btnHistorial.addActionListener(e -> openHistorial());
        btnEliminar.addActionListener(e -> onDelete());
    }

    // ================== Utilidades ==================

    private void recargar() {
        modelo.set(dao.listAll());
    }

    private void filtrar() {
        String q = txtBuscar.getText().trim().toLowerCase();
        if (q.isEmpty()) { recargar(); return; }
        List<Product> base = dao.listAll();
        modelo.set(base.stream()
                .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q))
                        || (p.getCode() != null && p.getCode().toLowerCase().contains(q)))
                .collect(Collectors.toList()));
    }

    private Product getSeleccionado() {
        int row = tabla.getSelectedRow();
        if (row < 0) return null;
        return modelo.getAt(row);
    }

    private Product findByCode(String code) {
        return dao.findByCode(code);
    }

    private void warn(String msg) { JOptionPane.showMessageDialog(this, msg, "Atención", JOptionPane.WARNING_MESSAGE); }
    private void info(String msg) { JOptionPane.showMessageDialog(this, msg, "OK", JOptionPane.INFORMATION_MESSAGE); }

    private JPanel form2(String l1, JComponent c1, String l2, JComponent c2) {
        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel(l1)); p.add(c1);
        p.add(new JLabel(l2)); p.add(c2);
        return p;
    }

    // ================== Operaciones ==================

    private void onEntrada() {
        JTextField txtCode  = new JTextField();
        JTextField txtName  = new JTextField();
        JTextField txtCat   = new JTextField("General");
        JTextField txtPrice = new JTextField("0");
        JTextField txtExp   = new JTextField("");
        JSpinner spQty      = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
        JTextField txtReason= new JTextField("Entrada de stock");

        JPanel wrap = new JPanel(new GridLayout(0,1,4,4));
        wrap.add(form2("Código:", txtCode, "Nombre:", txtName));
        wrap.add(form2("Categoría:", txtCat, "Precio:", txtPrice));
        wrap.add(form2("Vencimiento (AAAA-MM-DD):", txtExp, "Cantidad (+):", spQty));
        wrap.add(form2("Motivo:", txtReason, "", new JLabel()));

        int ok = JOptionPane.showConfirmDialog(this, wrap, "Entrada de producto", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String code = txtCode.getText().trim();
        if (code.isEmpty()) { warn("El código es obligatorio."); return; }

        Product prod = findByCode(code);
        int qty = (Integer) spQty.getValue();

        if (prod == null) {
            String name = txtName.getText().trim();
            if (name.isEmpty()) { warn("El nombre es obligatorio."); return; }
            String cat = txtCat.getText().trim();
            int price;
            try { price = Integer.parseInt(txtPrice.getText().trim()); }
            catch (Exception e) { warn("Precio inválido."); return; }
            LocalDate exp = null;
            if (!txtExp.getText().trim().isEmpty()) {
                try { exp = LocalDate.parse(txtExp.getText().trim()); }
                catch (Exception e) { warn("Fecha inválida."); return; }
            }
            prod = new Product(code, name, cat, price, qty, exp);
            dao.insert(prod);
        } else {
            int prev = prod.getStock();
            prod.setStock(prev + qty);
            dao.update(prod);
            movementDao.insert(code, "ENTRY", qty, prev, prod.getStock(),
                    txtReason.getText(), currentUser, LocalDateTime.now());
        }

        info("Entrada aplicada a " + prod.getName() + ". Nuevo stock: " + prod.getStock());
        recargar();
    }

    private void onSalida() {
        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        JSpinner spQty = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
        JTextField txtReason = new JTextField("Salida manual");
        JPanel form = form2("Cantidad (−):", spQty, "Motivo:", txtReason);

        int ok = JOptionPane.showConfirmDialog(this, form, "Salida de stock", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        int qty = (Integer) spQty.getValue();
        int prev = p.getStock();
        int newStock = Math.max(0, prev - qty);
        p.setStock(newStock);

        movementDao.insert(p.getCode(), "EXIT", qty, prev, newStock,
                txtReason.getText(), currentUser, LocalDateTime.now());
        dao.update(p);

        info("Salida aplicada. Stock: " + prev + " → " + newStock);
        recargar();
    }

    private void onAjuste() {
        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        JTextField txtName = new JTextField(p.getName());
        JTextField txtCat  = new JTextField(p.getCategory());
        JSpinner spPrice   = new JSpinner(new SpinnerNumberModel(p.getPrice(), 0, 1_000_000_000, 1));
        JTextField txtExp  = new JTextField(p.getExpiry() == null ? "" : p.getExpiry().toString());
        JSpinner spStock   = new JSpinner(new SpinnerNumberModel(p.getStock(), 0, 1_000_000, 1));
        JTextField txtReason = new JTextField("Ajuste / edición manual");

        JPanel form = new JPanel(new GridLayout(0,2,6,6));
        form.add(new JLabel("Código:")); form.add(new JLabel(p.getCode()));
        form.add(new JLabel("Nombre:")); form.add(txtName);
        form.add(new JLabel("Categoría:")); form.add(txtCat);
        form.add(new JLabel("Precio:")); form.add(spPrice);
        form.add(new JLabel("Vencimiento (AAAA-MM-DD):")); form.add(txtExp);
        form.add(new JLabel("Nuevo stock:")); form.add(spStock);
        form.add(new JLabel("Motivo:")); form.add(txtReason);

        int ok = JOptionPane.showConfirmDialog(this, form, "Ajuste de producto", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        p.setName(txtName.getText().trim());
        p.setCategory(txtCat.getText().trim());
        p.setPrice((Integer) spPrice.getValue());
        if (!txtExp.getText().trim().isEmpty()) {
            try { p.setExpiry(LocalDate.parse(txtExp.getText().trim())); }
            catch (Exception e) { warn("Fecha inválida."); return; }
        }
        int prev = p.getStock();
        int newStock = (Integer) spStock.getValue();
        p.setStock(newStock);

        if (newStock != prev) {
            movementDao.insert(p.getCode(), "ADJUST", Math.abs(newStock - prev),
                    prev, newStock, txtReason.getText(), currentUser, LocalDateTime.now());
        }
        dao.update(p);

        info("Ajuste aplicado. Stock: " + prev + " → " + newStock);
        recargar();
    }

    private void onDelete() {
        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        String extra = p.getStock() > 0
                ? "\n\n⚠️ Se registrará un movimiento DELETE y stock 0."
                : "";
        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el producto \"" + p.getName() + "\" (" + p.getCode() + ")?"
                        + "\nEsta acción no se puede deshacer." + extra,
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        movementDao.insert(p.getCode(), "DELETE", p.getStock(), p.getStock(), 0,
                "Eliminación de producto", currentUser, LocalDateTime.now());
        dao.delete(p.getCode());

        info("Producto eliminado correctamente.");
        recargar();
    }

    private void openHistorial() {
        String preset = "";
        Product sel = getSeleccionado();
        if (sel != null) preset = sel.getCode();

        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Historial de Movimientos", Dialog.ModalityType.MODELESS);
        d.setContentPane(new MovementsPanel(preset));
        d.setSize(900, 500);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    // ================== Modelo de tabla ==================

    private static class ProductosModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Categoría", "Precio", "Stock", "Estado", "Vence"};
        private List<Product> data = List.of();

        public void set(List<Product> rows) { data = Objects.requireNonNullElse(rows, List.of()); fireTableDataChanged(); }
        public Product getAt(int row) { return data.get(row); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Product p = data.get(r);
            return switch (c) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getCategory();
                case 3 -> "$" + p.getPrice();
                case 4 -> p.getStock();
                case 5 -> p.getStock() > 0 ? "OK" : "SIN STOCK";
                case 6 -> (p.getExpiry() == null) ? "-" : DF.format(p.getExpiry());
                default -> "";
            };
        }
    }
}

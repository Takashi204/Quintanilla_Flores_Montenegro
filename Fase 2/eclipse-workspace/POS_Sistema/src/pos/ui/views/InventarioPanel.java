package pos.ui.views;

import pos.model.InventoryMovement;
import pos.model.Product;
import pos.repo.InMemoryMovementRepository;
import pos.store.InMemoryStore;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// === NUEVO: imports para BD ===
import pos.db.ProductDao;
import java.sql.SQLException;

public class InventarioPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // Barra superior
    private final JTextField txtBuscar = new JTextField(18);
    private final JButton btnBuscar    = new JButton("Buscar");
    private final JButton btnEntrada   = new JButton("Entrada (+)");
    private final JButton btnSalida    = new JButton("Salida (−)");
    private final JButton btnAjuste    = new JButton("Ajuste");
    private final JButton btnHistorial = new JButton("Historial"); // ← ya lo tenías

    // Tabla
    private final JTable tabla = new JTable(new ProductosModel());
    private final ProductosModel modelo = (ProductosModel) tabla.getModel();

    // === NUEVO: DAO para persistencia ===
    private final ProductDao dao = new ProductDao();

    private final String currentUser = "admin"; // reemplaza por el usuario logueado si lo tienes

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
        add(top, BorderLayout.NORTH);

        tabla.setRowHeight(22);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Asegura tabla y carga datos
        try { dao.createTableIfNotExists(); } catch (SQLException e) { e.printStackTrace(); }
        recargar();

        btnBuscar.addActionListener(e -> filtrar());
        btnEntrada.addActionListener(e -> onEntradaCrearOSumar());
        btnSalida.addActionListener(e -> onSalida());
        btnAjuste.addActionListener(e -> onAjuste());
        btnHistorial.addActionListener(e -> openHistorial());
    }

    // ================== UI helpers ==================

    // === CAMBIO: si la memoria está vacía, trae desde BD y la rellena ===
    private void recargar() {
        if (InMemoryStore.getAllProducts().isEmpty()) {
            try {
                for (Product pr : dao.listAll()) {
                    InMemoryStore.addProduct(pr);
                }
            } catch (SQLException ex) {
                System.err.println("InventarioPanel: no se pudo leer BD: " + ex.getMessage());
            }
        }
        modelo.set(InMemoryStore.getAllProducts());
    }

    private void filtrar() {
        String q = txtBuscar.getText().trim().toLowerCase();
        if (q.isEmpty()) { recargar(); return; }
        List<Product> list = InMemoryStore.getAllProducts().stream()
                .filter(p -> p.getName().toLowerCase().contains(q) || p.getCode().toLowerCase().contains(q))
                .collect(Collectors.toList());
        modelo.set(list);
    }

    private Product getSeleccionado() {
        int row = tabla.getSelectedRow();
        if (row < 0) return null;
        return modelo.getAt(row);
    }

    private Product findByCode(String code) {
        if (code == null) return null;
        for (Product p : InMemoryStore.getAllProducts()) {
            if (code.equalsIgnoreCase(p.getCode())) return p;
        }
        return null;
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel form2(String l1, JComponent c1, String l2, JComponent c2) {
        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel(l1)); p.add(c1);
        p.add(new JLabel(l2)); p.add(c2);
        return p;
    }

    // ================== Operaciones ==================

    /** Entrada: si existe el código suma stock; si no existe, crea y luego entra stock. */
    private void onEntradaCrearOSumar() {
        // --- Campos del formulario ---
        JTextField txtCode  = new JTextField(InMemoryStore.nextCode());
        JTextField txtName  = new JTextField();
        JTextField txtCat   = new JTextField("General");
        JTextField txtPrice = new JTextField("0");
        JTextField txtExp   = new JTextField(""); // AAAA-MM-DD (opcional)
        JSpinner spQty      = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        JTextField txtReason= new JTextField("Compra a proveedor");

        // Armado del formulario
        JPanel p1 = form2("Código:", txtCode, "Nombre:", txtName);
        JPanel p2 = form2("Categoría:", txtCat, "Precio:", txtPrice);
        JPanel p3 = form2("Vencimiento (AAAA-MM-DD):", txtExp, "Cantidad (+):", spQty);
        JPanel p4 = form2("Motivo:", txtReason, " ", new JLabel("<html><i>Si el código existe, se usará ese producto.</i></html>"));
        JPanel wrap = new JPanel(new GridLayout(0,1,4,4));
        wrap.add(p1); wrap.add(p2); wrap.add(p3); wrap.add(p4);

        int ok = JOptionPane.showConfirmDialog(this, wrap, "Entrada de stock (crear o usar existente)", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String code = txtCode.getText().trim();
        if (code.isEmpty()) { warn("El código es obligatorio."); return; }

        Product prod = findByCode(code);

        // Si NO existe -> crear producto (nombre obligatorio)
        if (prod == null) {
            String name = txtName.getText().trim();
            if (name.isEmpty()) { warn("El nombre es obligatorio para nuevos productos."); return; }
            String cat  = txtCat.getText().trim();
            int price   = parseIntSafe(txtPrice.getText(), -1);
            if (price < 0) { warn("Precio inválido."); return; }

            LocalDate exp = null;
            String expRaw = txtExp.getText().trim();
            if (!expRaw.isEmpty()) {
                try { exp = LocalDate.parse(expRaw); }
                catch (Exception ex) { warn("Fecha de vencimiento inválida (usa AAAA-MM-DD)."); return; }
            }

            prod = new Product(code, name, cat.isEmpty() ? "General" : cat, price, 0, exp);
            InMemoryStore.addProduct(prod);
            // === NUEVO: persistir creación/edición ===
            try { dao.upsert(prod); } catch (SQLException e) { e.printStackTrace(); }
        }

        int qty = (int) spQty.getValue();
        String reason = txtReason.getText().isBlank() ? "Entrada" : txtReason.getText().trim();

        InventoryMovement m = InventoryMovement.entry(prod, qty, reason, currentUser);
        m.applyToProduct();
        InMemoryMovementRepository.getInstance().add(m);
        InMemoryStore.updateProduct(prod); // por si tu store no es por referencia
        // === NUEVO: persistir después de aplicar movimiento ===
        try { dao.upsert(prod); } catch (SQLException e) { e.printStackTrace(); }

        info("Entrada aplicada al producto " + prod.getName()
                + ". Stock: " + m.getPreviousStock() + " → " + m.getResultingStock());

        recargar();
    }

    private void onSalida() {
        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        JSpinner spQty      = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        JTextField txtReason= new JTextField("Salida manual");
        JPanel form = new JPanel(new GridLayout(0,2,6,6));
        form.add(new JLabel("Cantidad (−):")); form.add(spQty);
        form.add(new JLabel("Motivo:"));       form.add(txtReason);

        int ok = JOptionPane.showConfirmDialog(this, form, "Salida de stock", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        int qty = (int) spQty.getValue();
        if (qty > p.getStock()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "La cantidad supera el stock actual (" + p.getStock() + ").\n¿Continuar? (quedará en 0)",
                    "Advertencia", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
        }
        String reason = txtReason.getText().isBlank() ? "Salida" : txtReason.getText().trim();

        InventoryMovement m = InventoryMovement.exit(p, qty, reason, currentUser);
        m.applyToProduct();
        InMemoryMovementRepository.getInstance().add(m);
        InMemoryStore.updateProduct(p);
        // === NUEVO: persistir salida ===
        try { dao.upsert(p); } catch (SQLException e) { e.printStackTrace(); }

        info("Salida aplicada.\nStock: " + m.getPreviousStock() + " → " + m.getResultingStock());
        recargar();
    }

    /** AJUSTE que también permite EDITAR el producto (nombre, categoría, precio, vencimiento). */
    private void onAjuste() {
        Product p = getSeleccionado();
        if (p == null) { warn("Selecciona un producto."); return; }

        // Campos editables de PRODUCTO
        JTextField txtName = new JTextField(p.getName());
        JTextField txtCat  = new JTextField(p.getCategory());
        JSpinner spPrice   = new JSpinner(new SpinnerNumberModel(p.getPrice(), 0, 1_000_000_000, 1));
        JTextField txtExp  = new JTextField(p.getExpiry() == null ? "" : p.getExpiry().toString()); // AAAA-MM-DD

        // Campos del AJUSTE
        JSpinner spNew     = new JSpinner(new SpinnerNumberModel(p.getStock(), 0, 1_000_000, 1));
        JTextField txtReason = new JTextField("Ajuste / Edición");

        // Formulario
        JPanel form = new JPanel(new GridLayout(0,2,6,6));
        form.add(new JLabel("Código:"));          form.add(new JLabel(p.getCode()));      // no editable
        form.add(new JLabel("Nombre:"));          form.add(txtName);
        form.add(new JLabel("Categoría:"));       form.add(txtCat);
        form.add(new JLabel("Precio:"));          form.add(spPrice);
        form.add(new JLabel("Vencimiento (AAAA-MM-DD):")); form.add(txtExp);
        form.add(new JLabel("Nuevo stock:"));     form.add(spNew);
        form.add(new JLabel("Motivo:"));          form.add(txtReason);

        int ok = JOptionPane.showConfirmDialog(this, form, "Ajuste de stock y edición de producto",
                JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        // Validaciones
        String name = txtName.getText().trim();
        if (name.isEmpty()) { warn("El nombre no puede estar vacío."); return; }
        String cat = txtCat.getText().trim();
        int price = (Integer) spPrice.getValue();

        LocalDate exp = null;
        String expRaw = txtExp.getText().trim();
        if (!expRaw.isEmpty()) {
            try { exp = LocalDate.parse(expRaw); }
            catch (Exception ex) { warn("Fecha inválida. Usa AAAA-MM-DD."); return; }
        }

        int newStock = (Integer) spNew.getValue();
        String reason = txtReason.getText().isBlank() ? "Ajuste/Edición" : txtReason.getText().trim();

        // Detectar cambios
        boolean productChanged =
                !name.equals(p.getName()) ||
                !cat.equals(p.getCategory()) ||
                price != p.getPrice() ||
                !Objects.equals(exp, p.getExpiry());

        boolean stockChanged = newStock != p.getStock();

        // Aplicar cambios del producto
        if (productChanged) {
            p.setName(name);
            p.setCategory(cat);
            p.setPrice(price);
            p.setExpiry(exp);
        }

        // Aplicar ajuste de stock (con movimiento) si corresponde
        if (stockChanged) {
            InventoryMovement m = InventoryMovement.adjustment(p, newStock, reason, currentUser);
            m.applyToProduct();
            InMemoryMovementRepository.getInstance().add(m);
        }

        // Persistir y refrescar
        InMemoryStore.updateProduct(p);
        try { dao.upsert(p); } catch (SQLException e) { e.printStackTrace(); } // === NUEVO ===
        recargar();

        // Feedback
        if (productChanged && stockChanged) {
            info("Producto actualizado y ajuste aplicado. Nuevo stock: " + newStock);
        } else if (productChanged) {
            info("Producto actualizado (sin cambio de stock).");
        } else if (stockChanged) {
            info("Ajuste aplicado. Nuevo stock: " + newStock);
        } else {
            info("No hubo cambios.");
        }
    }

    // ==== abrir historial de movimientos ====
    private void openHistorial() {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Historial de Movimientos", Dialog.ModalityType.MODELESS);
        d.setContentPane(new MovementsPanel());
        d.setSize(900, 500);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    // ================== Tabla ==================

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
            switch (c) {
                case 0: return p.getCode();
                case 1: return p.getName();
                case 2: return p.getCategory();
                case 3: return "$" + p.getPrice();
                case 4: return p.getStock();
                case 5: return p.getStock() > 0 ? "OK" : "SIN STOCK";
                case 6: {
                    LocalDate exp = p.getExpiry();
                    return exp == null ? "-" : DF.format(exp);
                }
                default: return "";
            }
        }
    }
}


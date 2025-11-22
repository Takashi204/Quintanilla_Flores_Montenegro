package pos.ui.views;

import pos.dao.InventoryDao;
import pos.dao.VentasDao;
import pos.model.Product;
import pos.model.Sale;
import pos.model.SaleItem;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.Locale;

public class VentasPanel extends JPanel {

    private static class SaleLine {
        private final Product product;
        private int qty;

        SaleLine(Product product, int qty) {
            this.product = product;
            this.qty = Math.max(1, qty);
        }

        public Product getProduct() { return product; }
        public int getQty() { return qty; }
        public void setQty(int q) { this.qty = Math.max(1, q); }
        public int getSubtotal() { return product.getPrice() * qty; }
    }

    private static class ItemsModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Precio", "Cantidad", "Subtotal"};
        private final java.util.List<SaleLine> data = new ArrayList<>();

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 2,3,4 -> Integer.class;
                default -> String.class;
            };
        }
        @Override public boolean isCellEditable(int r, int c) { return c == 3; }

        @Override public Object getValueAt(int r, int c) {
            SaleLine it = data.get(r);
            Product p = it.getProduct();
            return switch (c) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getPrice();
                case 3 -> it.getQty();
                case 4 -> it.getSubtotal();
                default -> "";
            };
        }

        @Override public void setValueAt(Object v, int r, int c) {
            if (c == 3) {
                try {
                    int q = Integer.parseInt(String.valueOf(v));
                    if (q < 1) q = 1;
                    data.get(r).setQty(q);
                    fireTableRowsUpdated(r, r);
                } catch (Exception ignored) {}
            }
        }

        public void addOrIncrement(Product p, int qty) {
            for (int i = 0; i < data.size(); i++) {
                SaleLine it = data.get(i);
                if (it.getProduct().getCode().equals(p.getCode())) {
                    it.setQty(it.getQty() + qty);
                    fireTableRowsUpdated(i, i);
                    return;
                }
            }
            data.add(new SaleLine(p, qty));
            fireTableRowsInserted(data.size()-1, data.size()-1);
        }

        public void removeAt(int idx) {
            if (idx >= 0 && idx < data.size()) {
                data.remove(idx);
                fireTableDataChanged();
            }
        }

        public void clear() {
            data.clear();
            fireTableDataChanged();
        }

        public List<SaleLine> getItems() { return new ArrayList<>(data); }
    }

    // ==== Campos UI ====
    private JTextField txtCodigo;
    private JTable tbl;
    private final JLabel lblInfo = new JLabel("Listo.");
    private final ItemsModel itemsModel = new ItemsModel();
    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es","CL"));

    private final Map<String, Product> inventory = new HashMap<>();

    public VentasPanel() {
        setLayout(new BorderLayout(10,10));
        setBackground(Color.WHITE);

        // ==== Barra superior ====
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        txtCodigo = new JTextField(16);
        JButton btnAgregar = new JButton("➕ Agregar");
        JButton btnServicio = new JButton("🧾 Servicio/Precio abierto");
        JButton btnAdmin = new JButton("⚙️ Productos (CRUD)");
        barra.add(new JLabel("Código:"));
        barra.add(txtCodigo);
        barra.add(btnAgregar);
        barra.add(btnServicio);
        barra.add(btnAdmin);
        add(barra, BorderLayout.NORTH);

        // ==== Tabla ====
        tbl = new JTable(itemsModel);
        tbl.setRowHeight(26);
        add(new JScrollPane(tbl), BorderLayout.CENTER);

        // ==== Pie ====
        JPanel pie = new JPanel(new BorderLayout());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnQuitar = new JButton("🗑 Quitar ítem");
        JButton btnCobrar = new JButton("💵 Cobrar (F9)");
        btnCobrar.setMnemonic(KeyEvent.VK_F9);
        acciones.add(btnQuitar);
        acciones.add(btnCobrar);
        pie.add(lblInfo, BorderLayout.WEST);
        pie.add(acciones, BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);

        // ==== Acciones ====
        txtCodigo.addActionListener(e -> agregarPorCodigo());
        btnAgregar.addActionListener(e -> agregarPorCodigo());
        btnServicio.addActionListener(e -> agregarServicioManual());
        btnQuitar.addActionListener(e -> quitarSeleccion());
        btnCobrar.addActionListener(e -> cobrar());
        btnAdmin.addActionListener(e -> abrirAdminProductos());

        // Cargar inventario desde BD
        recargarInventario();
    }

    // ==== Cargar inventario ====
    private void recargarInventario() {
        inventory.clear();
        for (Product p : InventoryDao.getAll()) {
            inventory.put(p.getCode(), p);
        }
    }

    // ==== Agregar producto por código ====
    private void agregarPorCodigo() {
        String code = txtCodigo.getText().trim();
        if (code.isEmpty()) return;

        Product p = inventory.get(code);
        if (p == null) {
            p = InventoryDao.findByCode(code);
            if (p != null) inventory.put(p.getCode(), p);
        }

        if (p == null) {
            JOptionPane.showMessageDialog(this, "Código no encontrado: " + code);
            info("Código no encontrado.");
            return;
        }
        if (p.getStock() <= 0) {
            JOptionPane.showMessageDialog(this, "Sin stock para: " + p.getName());
            return;
        }

        itemsModel.addOrIncrement(p, 1);
        txtCodigo.setText("");
        txtCodigo.requestFocusInWindow();
        info("Agregado: " + p.getName());
    }

    private void agregarServicioManual() {
        String desc = JOptionPane.showInputDialog(this, "Descripción del servicio:");
        if (desc == null || desc.isBlank()) return;
        String precioStr = JOptionPane.showInputDialog(this, "Precio (CLP):");
        if (precioStr == null || precioStr.isBlank()) return;

        int precio;
        try { precio = Integer.parseInt(precioStr.trim()); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, "Precio inválido"); return; }

        Product temp = new Product("SERV-" + System.currentTimeMillis(), desc, "SERVICIO", precio, 1);
        itemsModel.addOrIncrement(temp, 1);
        info("Servicio agregado: " + desc);
    }

    private void quitarSeleccion() {
        int row = tbl.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un ítem."); return; }
        itemsModel.removeAt(tbl.convertRowIndexToModel(row));
        info("Ítem eliminado.");
    }

    private void cobrar() {
        if (itemsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay ítems en la venta.");
            return;
        }

        List<SaleLine> items = itemsModel.getItems();
        int total = items.stream().mapToInt(SaleLine::getSubtotal).sum();
        String[] tipos = {"Boleta", "Factura"};
        String tipoDoc = (String) JOptionPane.showInputDialog(this, "Tipo de documento:", "Documento",
                JOptionPane.QUESTION_MESSAGE, null, tipos, tipos[0]);
        if (tipoDoc == null) return;

        String rut = null;
        if ("Factura".equals(tipoDoc)) {
            rut = JOptionPane.showInputDialog(this, "RUT/Razón Social (opcional):");
        }

        String[] pagos = {"Efectivo", "Tarjeta", "Transferencia", "Mixto"};
        String medioPago = (String) JOptionPane.showInputDialog(this, "Medio de pago:", "Cobro",
                JOptionPane.QUESTION_MESSAGE, null, pagos, pagos[0]);
        if (medioPago == null) return;

        int ef=0, tj=0, tr=0;
        if ("Mixto".equals(medioPago)) {
            try {
                ef = parseIntSafe(JOptionPane.showInputDialog(this, "Monto Efectivo:", "0"));
                tj = parseIntSafe(JOptionPane.showInputDialog(this, "Monto Tarjeta:", "0"));
                tr = parseIntSafe(JOptionPane.showInputDialog(this, "Monto Transferencia:", "0"));
                if (ef + tj + tr != total) {
                    JOptionPane.showMessageDialog(this, "La suma no coincide con el total.");
                    return;
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Montos inválidos."); return; }
        }

        int neto = (int)Math.round(total / 1.19);
        int iva = total - neto;

        // Actualizar stock en BD
        for (SaleLine it : items) {
            Product p = it.getProduct();
            int nuevoStock = Math.max(0, p.getStock() - it.getQty());
            p.setStock(nuevoStock);
            InventoryDao.updateStock(p.getCode(), nuevoStock);
        }

        // Guardar venta real
        List<SaleItem> saleItems = new ArrayList<>();
        for (SaleLine it : items) {
            saleItems.add(new SaleItem(it.getProduct(), it.getQty()));
        }

        Sale sale = new Sale(
                String.valueOf(System.currentTimeMillis()),
                tipoDoc,
                LocalDateTime.now(),
                saleItems,
                medioPago,
                ef, tj, tr,
                neto, iva, total,
                rut
        );

        VentasDao.save(sale);
        imprimirTicket(sale);
        itemsModel.clear();
        info("Venta completada: " + CLP.format(total));
        JOptionPane.showMessageDialog(this, "Venta registrada.\nTotal: " + CLP.format(total));
    }

    private int parseIntSafe(String s) { return (s == null || s.isBlank()) ? 0 : Integer.parseInt(s.trim()); }

    private void imprimirTicket(Sale sale) {
        StringBuilder sb = new StringBuilder();
        sb.append(sale.getDocType()).append(" #").append(sale.getId())
                .append(" — ").append(sale.getTs()).append("\n");
        if (sale.getCustomerId() != null) sb.append("Cliente: ").append(sale.getCustomerId()).append("\n");
        sb.append("--------------------------------\n");
        for (SaleItem it : sale.getItems()) {
            sb.append(it.getProduct().getName()).append(" x").append(it.getQty())
              .append("  ").append(CLP.format(it.getSubtotal())).append("\n");
        }
        sb.append("--------------------------------\n");
        sb.append("Neto: ").append(CLP.format(sale.getNeto())).append("\n");
        sb.append("IVA: ").append(CLP.format(sale.getIva())).append("\n");
        sb.append("TOTAL: ").append(CLP.format(sale.getTotal())).append("\n");
        sb.append("Pago: ").append(sale.getPaymentMethod()).append("\n");

        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        try { ta.print(); } catch (Exception ignored) {}
    }

    private void abrirAdminProductos() {
        new ProductAdminDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true);
        recargarInventario();
    }

    private void info(String msg) { lblInfo.setText(msg); }

    // ==== CRUD de productos ====
    private static class ProductAdminDialog extends JDialog {
        private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es","CL"));
        private java.util.List<Product> productos = new ArrayList<>();
        private final ProductModel model = new ProductModel();

        ProductAdminDialog(Window owner) {
            super(owner, "Productos — Admin", ModalityType.APPLICATION_MODAL);
            productos = InventoryDao.getAll();
            model.set(productos);
            buildUI();
            setSize(740, 440);
            setLocationRelativeTo(owner);
        }

        private void buildUI() {
            setLayout(new BorderLayout(8,8));
            JTable table = new JTable(model);
            table.setRowHeight(24);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            JButton btnAdd = new JButton("➕ Agregar");
            JButton btnEdit = new JButton("✏️ Editar");
            JButton btnDel = new JButton("🗑 Eliminar");
            JButton btnClose = new JButton("Cerrar");
            acciones.add(btnAdd); acciones.add(btnEdit); acciones.add(btnDel); acciones.add(btnClose);
            add(acciones, BorderLayout.SOUTH);

            btnAdd.addActionListener(e -> agregar());
            btnEdit.addActionListener(e -> editar(table));
            btnDel.addActionListener(e -> eliminar(table));
            btnClose.addActionListener(e -> dispose());
        }

        private void agregar() {
            JTextField code = new JTextField();
            JTextField name = new JTextField();
            JTextField cat  = new JTextField();
            JTextField price= new JTextField();
            JTextField stock= new JTextField();

            JPanel p = new JPanel(new GridLayout(0,2,6,6));
            p.add(new JLabel("Código:")); p.add(code);
            p.add(new JLabel("Nombre:")); p.add(name);
            p.add(new JLabel("Categoría:")); p.add(cat);
            p.add(new JLabel("Precio:")); p.add(price);
            p.add(new JLabel("Stock:")); p.add(stock);

            if (JOptionPane.showConfirmDialog(this, p, "Nuevo producto", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                return;

            try {
                Product prod = new Product(code.getText(), name.getText(), cat.getText(),
                        Integer.parseInt(price.getText()), Integer.parseInt(stock.getText()));
                InventoryDao.insert(prod);
                model.set(InventoryDao.getAll());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }

        private void editar(JTable table) {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto."); return; }
            Product p = model.getAt(row);

            JTextField name = new JTextField(p.getName());
            JTextField cat  = new JTextField(p.getCategory());
            JTextField price= new JTextField(String.valueOf(p.getPrice()));
            JTextField stock= new JTextField(String.valueOf(p.getStock()));

            JPanel form = new JPanel(new GridLayout(0,2,6,6));
            form.add(new JLabel("Código:")); form.add(new JLabel(p.getCode()));
            form.add(new JLabel("Nombre:")); form.add(name);
            form.add(new JLabel("Categoría:")); form.add(cat);
            form.add(new JLabel("Precio:")); form.add(price);
            form.add(new JLabel("Stock:")); form.add(stock);

            if (JOptionPane.showConfirmDialog(this, form, "Editar producto", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                return;

            try {
                p.setName(name.getText());
                p.setCategory(cat.getText());
                p.setPrice(Integer.parseInt(price.getText()));
                p.setStock(Integer.parseInt(stock.getText()));
                InventoryDao.update(p);
                model.set(InventoryDao.getAll());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar.");
            }
        }

        private void eliminar(JTable table) {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto."); return; }
            Product p = model.getAt(row);
            if (JOptionPane.showConfirmDialog(this, "¿Eliminar " + p.getName() + "?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                InventoryDao.delete(p.getCode());
                model.set(InventoryDao.getAll());
            }
        }

        private static class ProductModel extends AbstractTableModel {
            private final String[] cols = {"Código","Nombre","Categoría","Precio","Stock"};
            private List<Product> data = new ArrayList<>();

            void set(List<Product> rows){ data = rows; fireTableDataChanged(); }
            Product getAt(int r){ return data.get(r); }

            @Override public int getRowCount(){ return data.size(); }
            @Override public int getColumnCount(){ return cols.length; }
            @Override public String getColumnName(int c){ return cols[c]; }
            @Override public Object getValueAt(int r, int c){
                Product p = data.get(r);
                return switch(c){
                    case 0 -> p.getCode();
                    case 1 -> p.getName();
                    case 2 -> p.getCategory();
                    case 3 -> p.getPrice();
                    case 4 -> p.getStock();
                    default -> "";
                };
            }
        }
    }
}


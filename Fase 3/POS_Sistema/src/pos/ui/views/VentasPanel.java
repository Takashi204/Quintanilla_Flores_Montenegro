package pos.ui.views;

import pos.services.InventoryService;
import pos.services.CashService;
import pos.services.SaleService;
import pos.model.Product;
import pos.model.Sale;
import pos.model.SaleItem;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.Locale;

public class VentasPanel extends JPanel {

    // ============================================================
    //  ÍTEM DE LÍNEA
    // ============================================================
    public static class SaleLine {
        private final Product product;
        private int qty;

        public SaleLine(Product product, int qty) {
            this.product = product;
            this.qty = Math.max(1, qty);
        }

        public Product getProduct() { return product; }
        public int getQty() { return qty; }
        public void setQty(int q) { this.qty = Math.max(1, q); }
        public int getSubtotal() { return product.getPrice() * qty; }
    }

    // ============================================================
    //  MODELO TABLA
    // ============================================================
    private static class ItemsModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Precio", "Cantidad", "Subtotal"};
        private final List<SaleLine> data = new ArrayList<>();

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
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        public List<SaleLine> getItems() { return new ArrayList<>(data); }
        public void removeAt(int i){ if (i >= 0 && i < data.size()) { data.remove(i); fireTableDataChanged(); } }
        public void clear(){ data.clear(); fireTableDataChanged(); }
    }

    // ============================================================
    //  CAMPOS UI
    // ============================================================
    private JTextField txtCodigo;
    private JTable tbl;
    private final JLabel lblInfo = new JLabel("Listo.");
    private final ItemsModel itemsModel = new ItemsModel();

    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es","CL"));
    private final Map<String, Product> inventory = new HashMap<>();

    private final InventoryService inventoryService = new InventoryService();
    private final SaleService saleService = new SaleService();

    // ============================================================
    //  CONSTRUCTOR
    // ============================================================
    public VentasPanel() {
        setLayout(new BorderLayout(10,10));
        setBackground(Color.WHITE);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        txtCodigo = new JTextField(16);
        JButton btnAgregar = new JButton("➕ Agregar");
        JButton btnServicio = new JButton("🧾 Servicio/Precio abierto");

        barra.add(new JLabel("Código:"));
        barra.add(txtCodigo);
        barra.add(btnAgregar);
        barra.add(btnServicio);
        add(barra, BorderLayout.NORTH);

        tbl = new JTable(itemsModel);
        tbl.setRowHeight(26);
        add(new JScrollPane(tbl), BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));

        JButton btnQuitar = new JButton("🗑 Quitar ítem");
        JButton btnCobrar = new JButton("💵 Cobrar");

        acciones.add(btnQuitar);
        acciones.add(btnCobrar);
        pie.add(lblInfo, BorderLayout.WEST);
        pie.add(acciones, BorderLayout.EAST);

        add(pie, BorderLayout.SOUTH);

        txtCodigo.addActionListener(e -> agregarPorCodigo());
        btnAgregar.addActionListener(e -> agregarPorCodigo());
        btnServicio.addActionListener(e -> agregarServicioManual());
        btnQuitar.addActionListener(e -> quitarSeleccion());
        btnCobrar.addActionListener(e -> cobrar());

        recargarInventario();
    }

    // ============================================================
    //  INVENTARIO DESDE API
    // ============================================================
    private void recargarInventario() {
        try {
            inventory.clear();
            List<Product> productos = InventoryService.getAll();
            for (Product p : productos) inventory.put(p.getCode(), p);
            lblInfo.setText("Inventario cargado desde API.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando inventario desde la API\n" + e.getMessage());
        }
    }

    // ============================================================
    //  AGREGAR PRODUCTO
    // ============================================================
    private void agregarPorCodigo() {
        String code = txtCodigo.getText().trim();
        if (code.isEmpty()) return;

        Product p = inventory.get(code);

        if (p == null) {
            try {
                p = inventoryService.getByCode(code);
                if (p != null) inventory.put(p.getCode(), p);
            } catch (Exception ignored) {}
        }

        if (p == null) {
            JOptionPane.showMessageDialog(this, "Código no encontrado.");
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

    // ============================================================
    //  SERVICIO MANUAL
    // ============================================================
    private void agregarServicioManual() {
        String desc = JOptionPane.showInputDialog(this, "Descripción del servicio:");
        if (desc == null || desc.isBlank()) return;

        String precioStr = JOptionPane.showInputDialog(this, "Precio:");
        if (precioStr == null || precioStr.isBlank()) return;

        int precio;
        try { precio = Integer.parseInt(precioStr.trim()); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this,"Precio inválido"); return; }

        Product temp = new Product("SV-"+System.currentTimeMillis(), desc, "SERVICIO", precio, 1);
        itemsModel.addOrIncrement(temp, 1);
        info("Servicio agregado");
    }

    // ============================================================
    //  QUITAR
    // ============================================================
    private void quitarSeleccion() {
        int row = tbl.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this,"Selecciona un ítem."); return; }
        itemsModel.removeAt(tbl.convertRowIndexToModel(row));
        info("Ítem eliminado.");
    }

    // ============================================================
    //  COBRAR (API)
    // ============================================================
    private void cobrar() {

        if (itemsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay ítems en la venta.");
            return;
        }

        try {
            // 1️⃣ Obtener session_id REAL
            int sessionId = CashService.getActiveSessionId();

            // 2️⃣ Documentos
            String[] tipos = {"Boleta", "Factura"};
            String tipoDoc = (String) JOptionPane.showInputDialog(
                    this, "Tipo de documento:", "Documento",
                    JOptionPane.QUESTION_MESSAGE, null, tipos, tipos[0]
            );
            if (tipoDoc == null) return;

            // 3️⃣ RUT si factura
            String rut = null;
            if ("Factura".equals(tipoDoc)) {
                rut = JOptionPane.showInputDialog(this, "RUT/Razón Social (opcional):");
                if (rut == null) rut = "";
            }

            // 4️⃣ Medio de pago
            String[] pagos = {"Efectivo", "Tarjeta", "Transferencia"};
            String medioPago = (String) JOptionPane.showInputDialog(
                    this, "Medio de pago:", "Cobro",
                    JOptionPane.QUESTION_MESSAGE, null, pagos, pagos[0]
            );
            if (medioPago == null) return;

            // 5️⃣ Convertir SaleLine → Sale y agregar session_id
            List<SaleLine> lines = itemsModel.getItems();
            Sale venta = Sale.fromPOS(lines, tipoDoc, medioPago, rut);
            venta.setSession_id(sessionId);

            // 6️⃣ Enviar venta a la API
            boolean ok = saleService.enviarVenta(venta);

            if (ok) {
                JOptionPane.showMessageDialog(this, "Venta registrada correctamente.");
                itemsModel.clear();
                recargarInventario();
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error enviando venta a la API:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void info(String msg) {
        lblInfo.setText(msg);
    }
}
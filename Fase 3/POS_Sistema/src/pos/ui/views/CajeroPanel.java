package pos.ui.views;

import pos.services.InventoryService;
import pos.services.CashService;
import pos.dao.MovementDao;


import pos.model.Product;
import pos.model.SaleItem;
import pos.model.Sale;

import pos.services.SaleService;

import pos.util.DataSync;
import pos.util.TicketPrinter;
import pos.util.AuthState;

import pos.ui.dialogs.CheckoutDialog;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import java.awt.*;
import java.awt.event.*;

import java.math.BigDecimal;
import java.text.NumberFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CajeroPanel extends JPanel {

    private final JTextField txtBuscarCodigo = new JTextField(14);
    private final JTextField txtBuscarNombre = new JTextField(16);
    private final JTable tblInv = new JTable(new InvModel());
    private final ItemsModel itemsModel = new ItemsModel();
    private final JTable tblCarrito = new JTable(itemsModel);
    private final JLabel lblTotal = new JLabel("$0");

    private final NumberFormat CLP = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
    private boolean cajaAbierta = false;

    private final String currentUser;

    public CajeroPanel(String currentUser) {
        this.currentUser = currentUser;

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(0xF3F4F6));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        split.setLeftComponent(buildLeft());
        split.setRightComponent(buildRight());
        add(split, BorderLayout.CENTER);

        ((InvModel) tblInv.getModel()).reload();

        tblInv.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && cajaAbierta) agregarProductoSeleccionado();
            }
        });

        txtBuscarCodigo.addActionListener(e -> {
            if (cajaAbierta) agregarPorCodigo(txtBuscarCodigo.getText().trim());
        });

        registerShortcuts();

        // 🔥 IMPORTANTÍSIMO: sincroniza la caja con la API
        syncCajaDesdeAPI();

        DataSync.addListener("inventory", () ->
                SwingUtilities.invokeLater(() ->
                        ((InvModel) tblInv.getModel()).reload()
                )
        );
    }
    
    private void syncCajaDesdeAPI() {
        try {
            int sessionId = CashService.getActiveSessionId();
            cajaAbierta = true;
            System.out.println("Caja activa API detectada: #" + sessionId);
        } catch (Exception e) {
            cajaAbierta = false;
            System.out.println("No hay caja activa en API.");
        }
    }

    private void registerShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "abrirCaja");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "cerrarCaja");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "cobrar");

        am.put("abrirCaja", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { abrirCaja(); }
        });

        am.put("cerrarCaja", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { cerrarCaja(); }
        });

        am.put("cobrar", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (cajaAbierta) abrirModalCobro();
                else JOptionPane.showMessageDialog(CajeroPanel.this, "Debe abrir la caja antes de cobrar.");
            }
        });
    }

    private JComponent buildLeft() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnBuscar = new JButton("Buscar");
        JButton btnTodos = new JButton("Todos");

        top.add(new JLabel("Código:"));
        top.add(txtBuscarCodigo);
        top.add(new JLabel("Nombre:"));
        top.add(txtBuscarNombre);
        top.add(btnBuscar);
        top.add(btnTodos);

        p.add(top, BorderLayout.NORTH);

        tblInv.setRowHeight(24);
        p.add(new JScrollPane(tblInv), BorderLayout.CENTER);

        btnBuscar.addActionListener(e ->
                ((InvModel) tblInv.getModel()).search(txtBuscarCodigo.getText(), txtBuscarNombre.getText()));

        btnTodos.addActionListener(e ->
                ((InvModel) tblInv.getModel()).reload());

        return p;
    }

    private JComponent buildRight() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnAbrir = new JButton("Abrir Caja (F1)");
        JButton btnCerrar = new JButton("Cerrar Caja (F2)");
        top.add(btnNuevo);
        top.add(btnAbrir);
        top.add(btnCerrar);
        p.add(top, BorderLayout.NORTH);

        tblCarrito.setRowHeight(26);
        p.add(new JScrollPane(tblCarrito), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        JPanel tot = new JPanel(new GridLayout(0, 2, 8, 4));
        tot.setBorder(BorderFactory.createTitledBorder("Totales"));
        tot.add(new JLabel("TOTAL:"));
        tot.add(lblTotal);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnQuitar = new JButton("Quitar ítem");
        JButton btnCobrar = new JButton("Cobrar (F9)");
        acciones.add(btnQuitar);
        acciones.add(btnCobrar);

        bottom.add(tot, BorderLayout.NORTH);
        bottom.add(acciones, BorderLayout.SOUTH);
        p.add(bottom, BorderLayout.SOUTH);

        btnNuevo.addActionListener(e -> limpiarCarrito());
        btnQuitar.addActionListener(e -> quitarItem());
        btnCobrar.addActionListener(e -> abrirModalCobro());
        btnAbrir.addActionListener(e -> abrirCaja());
        btnCerrar.addActionListener(e -> cerrarCaja());

        return p;
    }

    

    private void abrirCaja() {
        if (cajaAbierta) {
            JOptionPane.showMessageDialog(this, "La caja ya está abierta.");
            return;
        }

        try {
            // register_id = 1, opening_amount = 0
            int sessionId = CashService.openCash(1, 0);

            cajaAbierta = true;

            JOptionPane.showMessageDialog(
                this,
                "Caja abierta correctamente (API).\nSesión #" + sessionId
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Error al abrir la caja en API:\n" + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    private void cerrarCaja() {
        if (!cajaAbierta) {
            JOptionPane.showMessageDialog(this, "No hay caja abierta.");
            return;
        }

        try {
            int sessionId = CashService.getActiveSessionId();
            CashService.closeCash(sessionId);

            cajaAbierta = false;

            JOptionPane.showMessageDialog(this, "Caja cerrada correctamente (API).");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Error al cerrar caja en API:\n" + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    private void limpiarCarrito() {
        itemsModel.data.clear();
        itemsModel.fireTableDataChanged();
        lblTotal.setText("$0");
    }

    private void quitarItem() {
        int r = tblCarrito.getSelectedRow();
        if (r >= 0) {
            itemsModel.data.remove(r);
            itemsModel.fireTableDataChanged();
            actualizarTotal();
        }
    }

    private void actualizarTotal() {
        lblTotal.setText(CLP.format(itemsModel.total()));
    }

    private void agregarPorCodigo(String code) {
        if (!cajaAbierta) return;

        Product p = null;

        // Buscar en la tabla lo que ya cargó la API
        InvModel model = (InvModel) tblInv.getModel();

        for (Product pr : model.data) {
            if (pr.getCode().equalsIgnoreCase(code)) {
                p = pr;
                break;
            }
        }

        if (p == null) {
            JOptionPane.showMessageDialog(this, "Producto no encontrado.");
            return;
        }

        if (p.getStock() <= 0) {
            JOptionPane.showMessageDialog(this, "Sin stock.");
            return;
        }

        itemsModel.addOrInc(p, 1);
        actualizarTotal();
        txtBuscarCodigo.setText("");
    }
    private void agregarProductoSeleccionado() {
        if (!cajaAbierta) return;

        int r = tblInv.getSelectedRow();
        if (r < 0) return;

        InvModel model = (InvModel) tblInv.getModel();
        Product p = model.getAt(tblInv.convertRowIndexToModel(r));

        if (p.getStock() <= 0) {
            JOptionPane.showMessageDialog(this, "Sin stock.");
            return;
        }

        itemsModel.addOrInc(p, 1);
        actualizarTotal();
    }


    private void abrirModalCobro() {
        if (!cajaAbierta) {
            JOptionPane.showMessageDialog(this, "Debe abrir la caja.");
            return;
        }

        if (itemsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Carrito vacío.");
            return;
        }

        int total = itemsModel.total();

        CheckoutDialog dlg = new CheckoutDialog(
                SwingUtilities.getWindowAncestor(this),
                new BigDecimal(total),
                BigDecimal.ZERO
        );

        dlg.setVisible(true);
        CheckoutDialog.Result r = dlg.getResult();

        if (r == null) return;

        cobrarVenta(total, r.paymentMethod);
    }

    private void cobrarVenta(int total, String metodoPagoPOS) {

        // 1) Convertir medio POS → API
        String metodoAPI = mapMetodoPagoAPI(metodoPagoPOS);

        try {

            // 2) Construir lista de items API
            List<SaleItem> apiItems = new ArrayList<>();
            for (Item it : itemsModel.data) {
                apiItems.add(new SaleItem(it.product, it.qty));
            }

            // 3) Obtener el session_id REAL desde la API
            int sessionId = CashService.getActiveSessionId();

            // 4) Crear la venta para la API usando el session_id correcto
            Sale ventaApi = new Sale(
                    sessionId,   // SESIÓN REAL ENTREGADA POR LA API
                    metodoAPI,   // "cash", "card", "transfer"
                    apiItems
            );

            // 5) Enviar venta a la API
            SaleService service = new SaleService();
            boolean ok = service.enviarVenta(ventaApi);

            if (!ok) {
                JOptionPane.showMessageDialog(this, "⚠ La API no registró la venta.");
                return;
            }

            // 6) Si todo fue bien → limpiar carrito
            JOptionPane.showMessageDialog(this, "Venta realizada correctamente (API).");
            limpiarCarrito();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al enviar venta a la API: " + ex.getMessage());
        }
    }
    
    private String mapMetodoPagoAPI(String posMetodo) {
        if (posMetodo == null) return "other";

        switch (posMetodo.toLowerCase()) {
            case "efectivo": return "cash";
            case "tarjeta": return "card";
            case "transferencia": return "transfer";
            case "mixto": return "other"; // la API NO soporta mixto
            default: return "other";
        }
    }

    // ======================================
    //             MODELOS
    // ======================================

    private class InvModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Categoría", "Precio", "Stock"};
        private List<Product> data = new ArrayList<>();

        void reload() {
            try {
                data = InventoryService.getAll();  // ← API
                fireTableDataChanged();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error al obtener inventario desde la API");
                data = new ArrayList<>();
                fireTableDataChanged();
            }
        }

        

        void search(String c, String n) {
            List<Product> base = data; 
            List<Product> filtrado = new ArrayList<>();

            for (Product p : base) {
                boolean ok = true;

                if (c != null && !c.isBlank())
                    ok &= p.getCode().toLowerCase().contains(c.toLowerCase());

                if (n != null && !n.isBlank())
                    ok &= p.getName().toLowerCase().contains(n.toLowerCase());

                if (ok) filtrado.add(p);
            }

            data = filtrado;
            fireTableDataChanged();
        }


        Product getAt(int r) { return data.get(r); }

        public int getRowCount() { return data.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }

        public Object getValueAt(int r, int c) {
            Product p = data.get(r);
            return switch (c) {
                case 0 -> p.getCode();
                case 1 -> p.getName();
                case 2 -> p.getCategory();
                case 3 -> p.getPrice();
                case 4 -> p.getStock();
                default -> "";
            };
        }
    }

    private static class Item {
        final Product product;
        int qty;
        Item(Product p, int q) { product = p; qty = q; }
    }

    private static class ItemsModel extends AbstractTableModel {
        private final String[] cols = {"Código", "Nombre", "Precio", "Cantidad", "Subtotal"};
        final List<Item> data = new ArrayList<>();

        public int getRowCount() { return data.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }

        public Object getValueAt(int r, int c) {
            Item it = data.get(r);
            return switch (c) {
                case 0 -> it.product.getCode();
                case 1 -> it.product.getName();
                case 2 -> it.product.getPrice();
                case 3 -> it.qty;
                case 4 -> it.product.getPrice() * it.qty;
                default -> "";
            };
        }

        public boolean isCellEditable(int r, int c) { return c == 3; }

        public void setValueAt(Object v, int r, int c) {
            if (c == 3) {
                try {
                    int q = Math.max(1, Integer.parseInt(v.toString().trim()));
                    if (q > data.get(r).product.getStock()) {
                        JOptionPane.showMessageDialog(null, "Stock insuficiente.");
                        return;
                    }
                    data.get(r).qty = q;
                    fireTableRowsUpdated(r, r);
                } catch (Exception ignored) {}
            }
        }

        void addOrInc(Product p, int q) {
            for (Item it : data) {
                if (it.product.getCode().equals(p.getCode())) {
                    if (it.qty + q > p.getStock()) {
                        JOptionPane.showMessageDialog(null, "Stock insuficiente.");
                        return;
                    }
                    it.qty += q;
                    fireTableDataChanged();
                    return;
                }
            }
            data.add(new Item(p, q));
            fireTableDataChanged();
        }

        int total() {
            int sum = 0;
            for (Item it : data) sum += it.product.getPrice() * it.qty;
            return sum;
        }
    }
}


package pos.ui.dialogs;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.awt.event.KeyAdapter;

/**
 * Diálogo de cobro. Permite:
 *  - Mostrar subtotal, impuesto y total
 *  - Elegir medio de pago (efectivo/tarjeta/transferencia/mixto)
 *  - Calcular vuelto en efectivo
 *  - Validar suma exacta en modo mixto
 */
public class CheckoutDialog extends JDialog {

    /**
     * Objeto resultado que se devuelve al cerrar el diálogo.
     */
    public static class Result {
        public final BigDecimal subtotal;     // subtotal sin impuestos
        public final BigDecimal taxAmount;    // impuesto calculado
        public final BigDecimal total;        // total final a cobrar

        public final String paymentMethod;    // método seleccionado
        public final BigDecimal cash;         // efectivo
        public final BigDecimal card;         // tarjeta
        public final BigDecimal transfer;     // transferencia
        public final BigDecimal change;       // vuelto

        public Result(BigDecimal subtotal, BigDecimal taxAmount, BigDecimal total,
                      String paymentMethod, BigDecimal cash, BigDecimal card,
                      BigDecimal transfer, BigDecimal change) {
            this.subtotal = subtotal;
            this.taxAmount = taxAmount;
            this.total = total;
            this.paymentMethod = paymentMethod;
            this.cash = cash;
            this.card = card;
            this.transfer = transfer;
            this.change = change;
        }
    }

    // Labels para mostrar totales
    private final JLabel lblSubtotal = new JLabel();
    private final JLabel lblImpuesto = new JLabel();
    private final JLabel lblTotal = new JLabel();

    // Combo con los medios de pago disponibles
    private final JComboBox<String> cbMedio =
            new JComboBox<>(new String[]{"Efectivo","Tarjeta","Transferencia","Mixto"});

    // Panel: EFECTIVO
    private final JPanel pnlEfectivo = new JPanel(new GridBagLayout());
    private final JTextField txtRecibido = new JTextField(); // cuanto entrega el cliente
    private final JLabel lblVuelto = new JLabel();           // vuelto calculado

    // Panel simple: TARJETA / TRANSFERENCIA (no requiere montos extra)
    private final JPanel pnlSimple = new JPanel(new BorderLayout());

    // Panel: MIXTO
    private final JPanel pnlMixto = new JPanel(new GridBagLayout());
    private final JTextField txtMixEf = new JTextField(); // efectivo
    private final JTextField txtMixTj = new JTextField(); // tarjeta
    private final JTextField txtMixTr = new JTextField(); // transferencia
    private final JLabel lblMixHint = new JLabel("La suma debe ser EXACTA al total.");

    // Botones del diálogo
    private final JButton btnConfirmar = new JButton("Cobrar (Enter)");
    private final JButton btnCancelar = new JButton("Cancelar (Esc)");

    // Datos base
    private final BigDecimal subtotal;
    private final BigDecimal taxRate; // porcentaje en decimal (ej: 0.19)
    private Result result;            // resultado final

    public CheckoutDialog(Window parent, BigDecimal subtotal, BigDecimal taxRate) {
        super(parent, "Cobro", ModalityType.APPLICATION_MODAL);
        this.subtotal = nvl(subtotal); // evita nulls
        this.taxRate  = nvl(taxRate);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 430);
        setLocationRelativeTo(parent);

        // Crear la interfaz
        initUI();
        refreshTotals(); // calcular totales
        wireEvents();    // conectar eventos
    }

    public Result getResult() { return result; }

    // ---------------- UI ----------------
    private void initUI() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0;

        // ---- Subtotal ----
        content.add(new JLabel("Subtotal:"), c);
        c.gridx = 1;
        lblSubtotal.setHorizontalAlignment(SwingConstants.RIGHT);
        content.add(lblSubtotal, c);

        // ---- Impuesto ----
        c.gridy++; c.gridx = 0;
        content.add(new JLabel("Impuesto (" + percent(taxRate) + "%):"), c);
        c.gridx = 1;
        lblImpuesto.setHorizontalAlignment(SwingConstants.RIGHT);
        content.add(lblImpuesto, c);

        // ---- Total ----
        c.gridy++; c.gridx = 0;
        JLabel lblt = new JLabel("TOTAL a pagar:");
        lblt.setFont(lblt.getFont().deriveFont(Font.BOLD));
        content.add(lblt, c);

        c.gridx = 1;
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD));
        content.add(lblTotal, c);

        // ---- Medio de pago ----
        c.gridy++; c.gridx = 0;
        content.add(new JLabel("Medio de pago:"), c);
        c.gridx = 1;
        content.add(cbMedio, c);

        // ---- Paneles dinámicos (card layout) ----
        JPanel stacked = new JPanel(new CardLayout());
        stacked.add(buildEfectivoPanel(), "EFECTIVO");
        stacked.add(buildSimplePanel(),   "SIMPLE");
        stacked.add(buildMixtoPanel(),    "MIXTO");

        c.gridy++; c.gridx = 0; c.gridwidth = 2;
        content.add(stacked, c);

        // ---- Botones ----
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnCancelar);
        buttons.add(btnConfirmar);

        // Agregar todo al diálogo
        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        // Vista inicial
        showCard(stacked, "EFECTIVO");
        getRootPane().setDefaultButton(btnConfirmar); // Enter confirma
    }

    // Panel para EFECTIVO
    private JPanel buildEfectivoPanel() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);

        pnlEfectivo.removeAll();
        c.gridx = 0; c.gridy = 0;
        pnlEfectivo.add(new JLabel("Efectivo recibido:"), c);

        c.gridx = 1;
        txtRecibido.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlEfectivo.add(txtRecibido, c);

        c.gridy++; c.gridx = 0;
        pnlEfectivo.add(new JLabel("Vuelto:"), c);

        c.gridx = 1;
        lblVuelto.setHorizontalAlignment(SwingConstants.RIGHT);
        lblVuelto.setFont(lblVuelto.getFont().deriveFont(Font.BOLD));
        pnlEfectivo.add(lblVuelto, c);
        return pnlEfectivo;
    }

    // Panel simple (tarjeta o transferencia)
    private JPanel buildSimplePanel() {
        pnlSimple.removeAll();
        pnlSimple.add(new JLabel("No se requieren montos adicionales."), BorderLayout.WEST);
        return pnlSimple;
    }

    // Panel para MIxTO
    private JPanel buildMixtoPanel() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);

        pnlMixto.removeAll();

        // efectivo mixto
        c.gridx = 0; c.gridy = 0;
        pnlMixto.add(new JLabel("Efectivo:"), c);
        c.gridx = 1;
        txtMixEf.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlMixto.add(txtMixEf, c);

        // tarjeta
        c.gridy++; c.gridx = 0;
        pnlMixto.add(new JLabel("Tarjeta:"), c);
        c.gridx = 1;
        txtMixTj.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlMixto.add(txtMixTj, c);

        // transferencia
        c.gridy++; c.gridx = 0;
        pnlMixto.add(new JLabel("Transferencia:"), c);
        c.gridx = 1;
        txtMixTr.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlMixto.add(txtMixTr, c);

        // mensaje ayuda
        c.gridy++; c.gridx = 0; c.gridwidth = 2;
        lblMixHint.setForeground(new Color(0x888888));
        pnlMixto.add(lblMixHint, c);

        return pnlMixto;
    }

    // Manejo de eventos
    private void wireEvents() {

        // Cambiar panel según medio
        cbMedio.addActionListener(e -> {
            String m = String.valueOf(cbMedio.getSelectedItem());
            Container parent = pnlEfectivo.getParent();

            if (m.equals("Efectivo")) showCard(parent, "EFECTIVO");
            else if (m.equals("Tarjeta") || m.equals("Transferencia")) showCard(parent, "SIMPLE");
            else showCard(parent, "MIXTO");

            refreshChange();
        });

        // Cambios en efectivo
        txtRecibido.getDocument().addDocumentListener(new Doc(this::refreshChange));

        // Validación automática del mixto
        Doc mixDoc = new Doc(() -> {
            BigDecimal total = calcTotal();
            BigDecimal ef = parseMoney(txtMixEf.getText());
            BigDecimal tj = parseMoney(txtMixTj.getText());
            BigDecimal tr = parseMoney(txtMixTr.getText());

            BigDecimal sum = ef.add(tj).add(tr);
            boolean ok = sum.compareTo(total) == 0;

            lblMixHint.setForeground(ok ? new Color(0x10b981) : new Color(0xef4444));
            lblMixHint.setText(ok ? "OK: suma exacta." : "La suma debe ser EXACTA al total.");
        });

        txtMixEf.getDocument().addDocumentListener(mixDoc);
        txtMixTj.getDocument().addDocumentListener(mixDoc);
        txtMixTr.getDocument().addDocumentListener(mixDoc);

        // Confirmar compra
        btnConfirmar.addActionListener(e -> confirmar());

        // Cancelar
        btnCancelar.addActionListener(e -> {
            result = null;
            dispose();
        });

        // ESC = cerrar
        getRootPane().registerKeyboardAction(
                e -> { result = null; dispose(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Enter en campos confirma
        KeyAdapter enterToConfirm = new KeyAdapter(){
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) confirmar();
            }
        };

        txtRecibido.addKeyListener(enterToConfirm);
        txtMixEf.addKeyListener(enterToConfirm);
        txtMixTj.addKeyListener(enterToConfirm);
        txtMixTr.addKeyListener(enterToConfirm);
    }

    // Lógica para confirmar pago
    private void confirmar() {
        BigDecimal total = calcTotal();
        String m = String.valueOf(cbMedio.getSelectedItem());

        BigDecimal ef = BigDecimal.ZERO, tj = BigDecimal.ZERO,
                   tr = BigDecimal.ZERO, change = BigDecimal.ZERO;

        // EFECTIVO
        if (m.equals("Efectivo")) {
            BigDecimal recibido = parseMoney(txtRecibido.getText());

            if (recibido.compareTo(total) < 0) {
                warn("El efectivo recibido es menor al total.");
                return;
            }

            ef = total;
            change = recibido.subtract(total).max(BigDecimal.ZERO);

        // TARJETA
        } else if (m.equals("Tarjeta")) {
            tj = total;

        // TRANSFERENCIA
        } else if (m.equals("Transferencia")) {
            tr = total;

        // MIXTO
        } else {
            ef = parseMoney(txtMixEf.getText());
            tj = parseMoney(txtMixTj.getText());
            tr = parseMoney(txtMixTr.getText());

            BigDecimal sum = ef.add(tj).add(tr);

            if (sum.compareTo(total) != 0) {
                warn("En Mixto, la suma debe ser EXACTA al total.");
                return;
            }
        }

        // Guardar resultado final
        result = new Result(subtotal, calcTax(), total, m, ef, tj, tr, change);
        dispose();
    }

    // Actualiza números en pantalla
    private void refreshTotals() {
        lblSubtotal.setText(formatCurrency(subtotal));
        lblImpuesto.setText(formatCurrency(calcTax()));
        lblTotal.setText(formatCurrency(calcTotal()));
        refreshChange();
    }

    // Calcula vuelto en modo efectivo
    private void refreshChange() {
        String m = String.valueOf(cbMedio.getSelectedItem());
        if (!m.equals("Efectivo")) {
            lblVuelto.setText("—");
            return;
        }

        BigDecimal total = calcTotal();
        BigDecimal recibido = parseMoney(txtRecibido.getText());
        BigDecimal vuelto = recibido.subtract(total);

        lblVuelto.setText(vuelto.compareTo(BigDecimal.ZERO) < 0 ? "—" : formatCurrency(vuelto));
    }

    // ---------------- helpers ----------------
    private BigDecimal calcTax() {
        return subtotal.multiply(taxRate).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calcTotal() {
        return subtotal.add(calcTax()).setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static String percent(BigDecimal r){
        return r.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private static BigDecimal parseMoney(String s) {
        if (s == null || s.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            String clean = s.replace(".", "").replace(",", "").replace("$", "").trim();
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static String formatCurrency(BigDecimal v) {
        return "$" + v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static void showCard(Container parent, String key){
        CardLayout cl = (CardLayout) ((JPanel)parent).getLayout();
        cl.show((JPanel)parent, key);
    }

    private static void warn(String msg){
        JOptionPane.showMessageDialog(null, msg, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    // DocumentListener simple para refrescar cálculos
    private static class Doc implements DocumentListener {
        private final Runnable r;
        Doc(Runnable r) { this.r = r; }
        public void insertUpdate(DocumentEvent e) { r.run(); }
        public void removeUpdate(DocumentEvent e) { r.run(); }
        public void changedUpdate(DocumentEvent e) { r.run(); }
    }
}

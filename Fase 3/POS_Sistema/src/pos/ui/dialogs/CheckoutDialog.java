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
 * Dialogo de cobro con medios:
 *  - Muestra Subtotal, Impuesto y Total
 *  - Medios: Efectivo, Tarjeta, Transferencia, Mixto
 *  - Vuelto si hay efectivo (no en mixto)
 *
 * Uso:
 *   CheckoutDialog dlg = new CheckoutDialog(parent, new BigDecimal("10000"), new BigDecimal("0.19"));
 *   dlg.setVisible(true);
 *   CheckoutDialog.Result r = dlg.getResult();
 *   if (r != null) {
 *       // r.total, r.cash, r.card, r.transfer, r.change...
 *   }
 */
public class CheckoutDialog extends JDialog {

    public static class Result {
        public final BigDecimal subtotal;
        public final BigDecimal taxAmount;
        public final BigDecimal total;

        public final String paymentMethod;     // "Efectivo" | "Tarjeta" | "Transferencia" | "Mixto"
        public final BigDecimal cash;          // efectivo (puede ser 0)
        public final BigDecimal card;          // tarjeta (puede ser 0)
        public final BigDecimal transfer;      // transferencia (0)
        public final BigDecimal change;        // vuelto (solo efectivo sin mixto)

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

    // UI comunes
    private final JLabel lblSubtotal = new JLabel();
    private final JLabel lblImpuesto = new JLabel();
    private final JLabel lblTotal = new JLabel();

    // Selector de medio
    private final JComboBox<String> cbMedio =
            new JComboBox<>(new String[]{"Efectivo","Tarjeta","Transferencia","Mixto"});

    // Panel efectivo simple
    private final JPanel pnlEfectivo = new JPanel(new GridBagLayout());
    private final JTextField txtRecibido = new JTextField();
    private final JLabel lblVuelto = new JLabel();

    // Panel sin campos (tarjeta/transferencia)
    private final JPanel pnlSimple = new JPanel(new BorderLayout());

    // Panel mixto
    private final JPanel pnlMixto = new JPanel(new GridBagLayout());
    private final JTextField txtMixEf = new JTextField();
    private final JTextField txtMixTj = new JTextField();
    private final JTextField txtMixTr = new JTextField();
    private final JLabel lblMixHint = new JLabel("La suma debe ser EXACTA al total.");

    // Botones
    private final JButton btnConfirmar = new JButton("Cobrar (Enter)");
    private final JButton btnCancelar = new JButton("Cancelar (Esc)");

    // Datos
    private final BigDecimal subtotal;
    private final BigDecimal taxRate; // ej: 0.19
    private Result result;

    public CheckoutDialog(Window parent, BigDecimal subtotal, BigDecimal taxRate) {
        super(parent, "Cobro", ModalityType.APPLICATION_MODAL);
        this.subtotal = nvl(subtotal);
        this.taxRate  = nvl(taxRate);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 430);
        setLocationRelativeTo(parent);

        initUI();
        refreshTotals();
        wireEvents();
    }

    public Result getResult() { return result; }

    // ---------------- UI ----------------
    private void initUI() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; c.weightx = 0;

        // Totales
        content.add(new JLabel("Subtotal:"), c);
        c.gridx = 1; c.weightx = 1;
        lblSubtotal.setHorizontalAlignment(SwingConstants.RIGHT);
        content.add(lblSubtotal, c);

        c.gridy++; c.gridx = 0; c.weightx = 0;
        content.add(new JLabel("Impuesto (" + percent(taxRate) + "%):"), c);
        c.gridx = 1; c.weightx = 1;
        lblImpuesto.setHorizontalAlignment(SwingConstants.RIGHT);
        content.add(lblImpuesto, c);

        c.gridy++; c.gridx = 0; c.weightx = 0;
        JLabel lblt = new JLabel("TOTAL a pagar:");
        lblt.setFont(lblt.getFont().deriveFont(Font.BOLD));
        content.add(lblt, c);
        c.gridx = 1; c.weightx = 1;
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD));
        content.add(lblTotal, c);

        // Medio
        c.gridy++; c.gridx = 0; c.weightx = 0;
        content.add(new JLabel("Medio de pago:"), c);
        c.gridx = 1; c.weightx = 1;
        content.add(cbMedio, c);

        // Stacked panels
        JPanel stacked = new JPanel(new CardLayout());
        stacked.add(buildEfectivoPanel(), "EFECTIVO");
        stacked.add(buildSimplePanel(),   "SIMPLE");
        stacked.add(buildMixtoPanel(),    "MIXTO");

        c.gridy++; c.gridx = 0; c.gridwidth = 2; c.weightx = 1;
        content.add(stacked, c);

        // Botones
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(btnCancelar);
        buttons.add(btnConfirmar);

        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        // Modo inicial
        showCard(stacked, "EFECTIVO");
        getRootPane().setDefaultButton(btnConfirmar);
    }

    private JPanel buildEfectivoPanel() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; c.weightx = 0;

        pnlEfectivo.removeAll();
        pnlEfectivo.add(new JLabel("Efectivo recibido:"), c);
        c.gridx = 1; c.weightx = 1;
        txtRecibido.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlEfectivo.add(txtRecibido, c);

        c.gridy++; c.gridx = 0; c.weightx = 0;
        pnlEfectivo.add(new JLabel("Vuelto:"), c);
        c.gridx = 1; c.weightx = 1;
        lblVuelto.setHorizontalAlignment(SwingConstants.RIGHT);
        lblVuelto.setFont(lblVuelto.getFont().deriveFont(Font.BOLD));
        pnlEfectivo.add(lblVuelto, c);
        return pnlEfectivo;
    }

    private JPanel buildSimplePanel() {
        pnlSimple.removeAll();
        pnlSimple.add(new JLabel("No se requieren montos adicionales para este medio."), BorderLayout.WEST);
        return pnlSimple;
    }

    private JPanel buildMixtoPanel() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; c.weightx = 0;

        pnlMixto.removeAll();
        pnlMixto.add(new JLabel("Efectivo:"), c);
        c.gridx = 1; c.weightx = 1;
        txtMixEf.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlMixto.add(txtMixEf, c);

        c.gridy++; c.gridx = 0; c.weightx = 0;
        pnlMixto.add(new JLabel("Tarjeta:"), c);
        c.gridx = 1; c.weightx = 1;
        txtMixTj.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlMixto.add(txtMixTj, c);

        c.gridy++; c.gridx = 0; c.weightx = 0;
        pnlMixto.add(new JLabel("Transferencia:"), c);
        c.gridx = 1; c.weightx = 1;
        txtMixTr.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlMixto.add(txtMixTr, c);

        c.gridy++; c.gridx = 0; c.gridwidth = 2;
        lblMixHint.setForeground(new Color(0x888888));
        pnlMixto.add(lblMixHint, c);

        return pnlMixto;
    }

    // --------------- Eventos / lógica ---------------
    private void wireEvents() {
        // Cambia panel por medio
        cbMedio.addActionListener(e -> {
            String m = String.valueOf(cbMedio.getSelectedItem());
            Container parent = pnlEfectivo.getParent();
            if (m.equals("Efectivo"))       showCard(parent, "EFECTIVO");
            else if (m.equals("Tarjeta") || m.equals("Transferencia")) showCard(parent, "SIMPLE");
            else                            showCard(parent, "MIXTO");
            refreshChange();
        });

        // Recalcular vuelto al escribir (efectivo)
        txtRecibido.getDocument().addDocumentListener(new Doc(this::refreshChange));

        // Validación “mixto”: sugerencia visual
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

        // Confirmar
        btnConfirmar.addActionListener(e -> confirmar());

        // Cancelar
        btnCancelar.addActionListener(e -> {
            result = null;
            dispose();
        });

        // ESC para cerrar
        getRootPane().registerKeyboardAction(e -> {
            result = null;
            dispose();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Enter en campos = confirmar
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

    private void confirmar() {
        BigDecimal total = calcTotal();
        String m = String.valueOf(cbMedio.getSelectedItem());

        BigDecimal ef = BigDecimal.ZERO, tj = BigDecimal.ZERO, tr = BigDecimal.ZERO, change = BigDecimal.ZERO;

        if (m.equals("Efectivo")) {
            BigDecimal recibido = parseMoney(txtRecibido.getText());
            if (recibido.compareTo(total) < 0) {
                warn("El efectivo recibido es menor al total.");
                return;
            }
            ef = total;
            change = recibido.subtract(total).max(BigDecimal.ZERO);
        } else if (m.equals("Tarjeta")) {
            tj = total;
        } else if (m.equals("Transferencia")) {
            tr = total;
        } else { // Mixto
            ef = parseMoney(txtMixEf.getText());
            tj = parseMoney(txtMixTj.getText());
            tr = parseMoney(txtMixTr.getText());
            BigDecimal sum = ef.add(tj).add(tr);
            if (sum.compareTo(total) != 0) {
                warn("En Mixto, la suma Efectivo+Tarjeta+Transferencia debe ser EXACTA al total.");
                return;
            }
            change = BigDecimal.ZERO; // sin vuelto en mixto
        }

        result = new Result(subtotal, calcTax(), total, m, ef, tj, tr, change);
        dispose();
    }

    private void refreshTotals() {
        lblSubtotal.setText(formatCurrency(subtotal));
        lblImpuesto.setText(formatCurrency(calcTax()));
        lblTotal.setText(formatCurrency(calcTotal()));
        refreshChange();
    }

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
        return r.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
    private static BigDecimal parseMoney(String s) {
        if (s == null || s.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            String clean = s.replace(".", "").replace(",", "").replace("$", "").trim();
            return new BigDecimal(clean);
        } catch (Exception e) { return BigDecimal.ZERO; }
    }
    private static String formatCurrency(BigDecimal v) {
        if (v == null) return "$0";
        return "$" + v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
    private static void showCard(Container parent, String key){
        CardLayout cl = (CardLayout) ((JPanel)parent).getLayout();
        if (key.equals("EFECTIVO")) cl.show((JPanel)parent, "EFECTIVO");
        else if (key.equals("SIMPLE")) cl.show((JPanel)parent, "SIMPLE");
        else cl.show((JPanel)parent, "MIXTO");
    }
    private static void warn(String msg){
        JOptionPane.showMessageDialog(null, msg, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    private static class Doc implements DocumentListener {
        private final Runnable r;
        Doc(Runnable r) { this.r = r; }
        public void insertUpdate(DocumentEvent e) { r.run(); }
        public void removeUpdate(DocumentEvent e) { r.run(); }
        public void changedUpdate(DocumentEvent e) { r.run(); }
    }
}

package pos.login;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// ⬅️ IMPORTA el Main
import pos.ui.MainFrame;

public class LoginFrame extends JFrame {

    private JTextField txtUser;
    private JPasswordField txtPass;
    private JRadioButton rAdmin, rCashier;
    private JLabel lblError;
    private RoundedButton btnLogin;

    public LoginFrame() {
        setTitle("Almacén Sonia — Login");
        setSize(820, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Fondo principal (gris claro)
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(new Color(0xF5F7FA));
        add(bg, BorderLayout.CENTER);

        // Panel blanco para el login (card)
        JPanel card = new JPanel(null);
        card.setPreferredSize(new Dimension(380, 340));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB), 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JLabel lblTitle = new JLabel("Iniciar sesión", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitle.setForeground(new Color(0x111827));
        lblTitle.setBounds(100, 18, 180, 30);
        card.add(lblTitle);

        // ----- Usuario
        JLabel lblUser = new JLabel("Usuario");
        lblUser.setForeground(new Color(0x374151));
        lblUser.setBounds(40, 64, 300, 18);
        card.add(lblUser);

        txtUser = new JTextField();
        txtUser.setBounds(40, 84, 300, 30);
        styleField(txtUser);
        card.add(txtUser);

        // ----- Contraseña
        JLabel lblPass = new JLabel("Contraseña");
        lblPass.setForeground(new Color(0x374151));
        lblPass.setBounds(40, 122, 300, 18);
        card.add(lblPass);

        txtPass = new JPasswordField();
        txtPass.setBounds(40, 142, 300, 30);
        styleField(txtPass);
        txtPass.setEchoChar('\u2022');
        card.add(txtPass);

        JCheckBox show = new JCheckBox("Mostrar contraseña");
        show.setOpaque(false);
        show.setForeground(new Color(0x6B7280));
        show.setBounds(40, 176, 180, 22);
        show.addActionListener(e -> txtPass.setEchoChar(show.isSelected() ? 0 : '\u2022'));
        card.add(show);

        // ----- Rol
        rAdmin   = new JRadioButton("Administrador");
        rCashier = new JRadioButton("Cajero");
        for (JRadioButton rb : new JRadioButton[]{rAdmin, rCashier}) {
            rb.setOpaque(false);
            rb.setForeground(new Color(0x333333));
        }
        rAdmin.setBounds(60, 202, 130, 24);
        rCashier.setBounds(210, 202, 100, 24);

        ButtonGroup group = new ButtonGroup();
        group.add(rAdmin);
        group.add(rCashier);
        rCashier.setSelected(true); // por defecto Cajero

        card.add(rAdmin);
        card.add(rCashier);

        // ----- Mensaje de error
        lblError = new JLabel(" ", SwingConstants.CENTER);
        lblError.setForeground(new Color(0xDC2626)); // rojo 600
        lblError.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblError.setBounds(40, 226, 300, 16);
        card.add(lblError);

        // ----- Botón redondeado
        btnLogin = new RoundedButton("Ingresar");
        btnLogin.setBounds(120, 252, 140, 40);
        btnLogin.setEnabled(false); // se habilita con validación
        card.add(btnLogin);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Columna izquierda (branding)
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel t1 = new JLabel("Almacén Sonia");
        t1.setFont(new Font("SansSerif", Font.BOLD, 28));
        t1.setForeground(new Color(0x111827));

        JLabel t2 = new JLabel("Sistema de ventas rápido y sencillo");
        t2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t2.setForeground(new Color(0x374151));

        left.add(t1);
        left.add(Box.createVerticalStrut(8));
        left.add(t2);

        // Añadir al fondo
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(20, 40, 20, 40);
        gc.gridx = 0; gc.gridy = 0;
        bg.add(left, gc);

        gc.gridx = 1;
        bg.add(card, gc);

        // Acciones
        DocumentListenerSimple dl = new DocumentListenerSimple(this::validateForm);
        txtUser.getDocument().addDocumentListener(dl);
        txtPass.getDocument().addDocumentListener(dl);

        getRootPane().setDefaultButton(btnLogin);
        btnLogin.addActionListener(e -> doLogin());
    }

    /** Valida campos y habilita/deshabilita el botón. */
    private void validateForm() {
        boolean ok = !txtUser.getText().trim().isEmpty()
                  && txtPass.getPassword().length > 0;
        btnLogin.setEnabled(ok);
        lblError.setText(" "); // limpio el error si el usuario corrige
    }

    /** DEMO front-end: acepta cualquier usuario/clave y abre el Main. */
    private void doLogin() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            lblError.setText("Debe ingresar usuario y contraseña.");
            return;
        }

        // Etiqueta para el mensaje y código para la app
        String roleCode  = rAdmin.isSelected() ? "ADMIN" : "CAJERO";
        String roleLabel = rAdmin.isSelected() ? "Administrador" : "Cajero";

        JOptionPane.showMessageDialog(this,
                "Bienvenido " + user + " (" + roleLabel + ")",
                "Login correcto",
                JOptionPane.PLAIN_MESSAGE);

        // 👉 Abrir Main y cerrar login (solo front-end, sin BD)
        SwingUtilities.invokeLater(() -> {
            dispose(); // cierra login
            new MainFrame(user, roleCode).setVisible(true); // abre el Main
        });
    }

    // ---------- helpers de estilo ----------
    private void styleField(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD1D5DB), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x2563EB), 2, true),
                        BorderFactory.createEmptyBorder(5, 9, 5, 9)
                ));
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xD1D5DB), 1, true),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));
            }
        });
    }

    /** Botón redondeado con colores y estados (normal/hover/pressed/disabled). */
    private static class RoundedButton extends JButton {
        private Color normal   = new Color(0x2563EB); // azul
        private Color hover    = new Color(0x1D4ED8);
        private Color pressed  = new Color(0x1E40AF);
        private Color disabled = new Color(0x9CA3AF); // gris

        private int arc = 12;
        private boolean isHover = false;
        private boolean isPress = false;

        RoundedButton(String text) {
            super(text);
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 15));
            setMargin(new Insets(8, 18, 8, 18));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isHover = true; repaint(); }
                @Override public void mouseExited (MouseEvent e) { isHover = false; isPress = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { isPress = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { isPress = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bg;
            if (!isEnabled()) bg = disabled;
            else if (isPress) bg = pressed;
            else if (isHover) bg = hover;
            else bg = normal;

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            // Texto centrado
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(Color.WHITE);
            g2.drawString(getText(), x, y);

            g2.dispose();
        }
    }

    /** Listener simple para no repetir código. */
    private static class DocumentListenerSimple implements javax.swing.event.DocumentListener {
        private final Runnable cb;
        DocumentListenerSimple(Runnable cb) { this.cb = cb; }
        public void insertUpdate(javax.swing.event.DocumentEvent e) { cb.run(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { cb.run(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { cb.run(); }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignore) {}
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}


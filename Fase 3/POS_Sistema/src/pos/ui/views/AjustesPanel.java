package pos.ui.views;

import pos.dao.UserDao;
import pos.model.User;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AjustesPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final JComboBox<UserItem> cmbUsuarios = new JComboBox<>();

    public AjustesPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(0xF9FAFB));

        // === TÍTULO ===
        JLabel title = new JLabel("⚙️ Ajustes del Sistema");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(0x111827));
        title.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));
        add(title, BorderLayout.NORTH);

        // === CONTENEDOR DE TARJETAS ===
        JPanel content = new JPanel(new GridLayout(1, 3, 18, 0));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        add(content, BorderLayout.CENTER);

        // === INFO ===
        JPanel infoCard = card("🏪 Información del sistema");
        infoCard.add(labelRow("Negocio", "Almacén Sonia"));
        infoCard.add(labelRow("Fecha", DF.format(LocalDate.now())));
        infoCard.add(labelRow("Versión", "v1.0 Beta"));
        infoCard.add(Box.createVerticalGlue());
        content.add(infoCard);

        // === CUENTA ===
        JPanel cuentaCard = card("👤 Cuenta y seguridad");
        cargarUsuarios();

        cuentaCard.add(fieldRow("Usuario", cmbUsuarios));

        JPasswordField txtNueva = new JPasswordField();
        txtNueva.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD1D5DB)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        cuentaCard.add(fieldRow("Nueva contraseña", txtNueva));

        JButton btnCambiar = botonPrincipal("Actualizar contraseña");
        btnCambiar.addActionListener(e -> {
            UserItem sel = (UserItem) cmbUsuarios.getSelectedItem();
            if (sel == null) {
                JOptionPane.showMessageDialog(this, "Selecciona un usuario.");
                return;
            }
            String nueva = new String(txtNueva.getPassword()).trim();
            if (nueva.isBlank()) {
                JOptionPane.showMessageDialog(this, "Ingresa una contraseña válida.");
                return;
            }

            boolean ok = UserDao.resetPassword(sel.id, nueva);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Contraseña actualizada para " + sel.username);
                txtNueva.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar la contraseña.");
            }
        });
        cuentaCard.add(wrapCenter(btnCambiar));
        content.add(cuentaCard);

        // === SESIÓN ===
        JPanel sesionCard = card("🔒 Sesión y consejos");

        JTextArea tips = new JTextArea(
                "💡 Recomendaciones:\n\n" +
                "• Guarda tu trabajo antes de cerrar sesión.\n" +
                "• Cambia tu contraseña regularmente.\n" +
                "• Usa roles adecuados (ADMIN / CAJERO)."
        );
        tips.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tips.setForeground(new Color(0x374151));
        tips.setOpaque(false);
        tips.setEditable(false);
        tips.setLineWrap(true);
        tips.setWrapStyleWord(true);
        sesionCard.add(wrap(tips));

        JButton btnLogout = botonSecundario("Cerrar sesión");
        btnLogout.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this, "¿Deseas cerrar sesión ahora?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) System.exit(0);
        });
        sesionCard.add(wrapCenter(btnLogout));
        content.add(sesionCard);
    }

    // === Helpers visuales ===

    private JPanel card(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 15));
        t.setForeground(new Color(0x111827));
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        card.add(t);
        return card;
    }

    private JPanel labelRow(String label, String value) {
        JPanel p = new JPanel(new GridLayout(1, 2, 8, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label + ":");
        l.setForeground(new Color(0x4B5563));
        JLabel v = new JLabel(value);
        v.setForeground(new Color(0x111827));
        v.setFont(v.getFont().deriveFont(Font.BOLD, 13f));
        p.add(l);
        p.add(v);
        return p;
    }

    private JPanel fieldRow(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label);
        l.setForeground(new Color(0x4B5563));
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 13f));
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setBackground(Color.WHITE);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD1D5DB)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        p.add(Box.createVerticalStrut(10));
        return p;
    }

    private JButton botonPrincipal(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(new Color(0x2563EB));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b.setBackground(new Color(0x1E40AF));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b.setBackground(new Color(0x2563EB));
            }
        });
        return b;
    }

    private JButton botonSecundario(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(Color.WHITE);
        b.setForeground(new Color(0x1F2937));
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(0xD1D5DB)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b.setBackground(new Color(0xF3F4F6));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b.setBackground(Color.WHITE);
            }
        });
        return b;
    }

    private JPanel wrap(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private JPanel wrapCenter(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    private void cargarUsuarios() {
        cmbUsuarios.removeAllItems();
        List<User> users = UserDao.getAll();
        for (User u : users) cmbUsuarios.addItem(new UserItem(u.getId(), u.getUsername()));
        if (cmbUsuarios.getItemCount() > 0) cmbUsuarios.setSelectedIndex(0);
    }

    private static class UserItem {
        final String id;
        final String username;
        UserItem(String id, String username) { this.id = id; this.username = username; }
        @Override public String toString() { return username + " (" + id + ")"; }
    }
}

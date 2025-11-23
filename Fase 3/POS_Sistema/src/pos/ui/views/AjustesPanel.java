package pos.ui.views;                       // Paquete donde se ubica esta vista

import pos.dao.UserDao;                     // DAO para usuarios (cambiar contraseña, listar)
import pos.model.User;                      // Modelo de usuario

import javax.swing.*;                       // Componentes Swing
import java.awt.*;                          // Layouts y estilos
import java.time.LocalDate;                 // Fecha actual
import java.time.format.DateTimeFormatter;  // Formato dd-MM-yyyy
import java.util.List;                      // Listado de usuarios

public class AjustesPanel extends JPanel {  // Panel de ajustes del sistema (solo Admin)

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");  
    // Formateador de fecha para mostrarla bonita

    private final JComboBox<UserItem> cmbUsuarios = new JComboBox<>(); 
    // ComboBox donde se muestran los usuarios existentes

    public AjustesPanel() {                 // Constructor del panel
        setLayout(new BorderLayout(16, 16));      
        setBackground(new Color(0xF9FAFB));       // Fondo gris suave estilo Tailwind

        // === TÍTULO PRINCIPAL ===
        JLabel title = new JLabel("⚙️ Ajustes del Sistema");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));   // Título grande
        title.setForeground(new Color(0x111827));              // Color gris oscuro
        title.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20)); 
        add(title, BorderLayout.NORTH);                        // Lo agregamos arriba

        // === CONTENEDOR DE TARJETAS (3 columnas) ===
        JPanel content = new JPanel(new GridLayout(1, 3, 18, 0)); // 3 tarjetas horizontales
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        add(content, BorderLayout.CENTER);                    // Lo agregamos al centro del panel

        // ===================
        //     TARJETA 1
        //   INFO DEL SISTEMA
        // ===================
        JPanel infoCard = card("🏪 Información del sistema"); // Crea tarjeta visual básica
        infoCard.add(labelRow("Negocio", "Almacén Sonia"));  // Fila: nombre del negocio
        infoCard.add(labelRow("Fecha", DF.format(LocalDate.now()))); // Fila: fecha actual
        infoCard.add(labelRow("Versión", "v1.0 Beta"));      // Fila: versión del programa
        infoCard.add(Box.createVerticalGlue());              // Empuja contenido hacia arriba
        content.add(infoCard);                               // Agrega la tarjeta

        // ===================
        //     TARJETA 2
        //   CUENTA / SEGURIDAD
        // ===================
        JPanel cuentaCard = card("👤 Cuenta y seguridad");    // Segunda tarjeta
        cargarUsuarios();                                     // Carga usuarios desde BD

        cuentaCard.add(fieldRow("Usuario", cmbUsuarios));     // ComboBox para elegir usuario

        // Campo de nueva contraseña
        JPasswordField txtNueva = new JPasswordField();
        txtNueva.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD1D5DB)),       // Borde gris claro
                BorderFactory.createEmptyBorder(6, 8, 6, 8)                // Padding interno
        ));
        cuentaCard.add(fieldRow("Nueva contraseña", txtNueva));            // Agrega campo

        // Botón para actualizar contraseña
        JButton btnCambiar = botonPrincipal("Actualizar contraseña");
        btnCambiar.addActionListener(e -> {                                // Acción al hacer click
            UserItem sel = (UserItem) cmbUsuarios.getSelectedItem();       // Usuario seleccionado
            if (sel == null) {                                             // Validación
                JOptionPane.showMessageDialog(this, "Selecciona un usuario.");
                return;
            }

            String nueva = new String(txtNueva.getPassword()).trim();      // Nueva contraseña
            if (nueva.isBlank()) {
                JOptionPane.showMessageDialog(this, "Ingresa una contraseña válida.");
                return;
            }

            boolean ok = UserDao.resetPassword(sel.id, nueva);             // Actualiza en BD
            if (ok) {
                JOptionPane.showMessageDialog(this, "Contraseña actualizada para " + sel.username);
                txtNueva.setText("");                                      // Limpia campo
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar la contraseña.");
            }
        });
        cuentaCard.add(wrapCenter(btnCambiar));                            // Centra el botón
        content.add(cuentaCard);                                            // Agrega tarjeta

        // ===================
        //     TARJETA 3
        //    SESIÓN / TIPS
        // ===================
        JPanel sesionCard = card("🔒 Sesión y consejos");     // Tercera tarjeta

        // Texto de consejos
        JTextArea tips = new JTextArea(
                "💡 Recomendaciones:\n\n" +
                "• Guarda tu trabajo antes de cerrar sesión.\n" +
                "• Cambia tu contraseña regularmente.\n" +
                "• Usa roles adecuados (ADMIN / CAJERO)."
        );
        tips.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tips.setForeground(new Color(0x374151));             // Gris oscuro
        tips.setOpaque(false);                               // Fondo transparente
        tips.setEditable(false);
        tips.setLineWrap(true);
        tips.setWrapStyleWord(true);
        sesionCard.add(wrap(tips));                          // Agrega tips a la tarjeta

        // Botón de cerrar sesión
        JButton btnLogout = botonSecundario("Cerrar sesión");
        btnLogout.addActionListener(e -> {                    // Acción del botón
            int r = JOptionPane.showConfirmDialog(this,
                    "¿Deseas cerrar sesión ahora?", "Confirmar",
                    JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) System.exit(0); // Cierra el programa
        });
        sesionCard.add(wrapCenter(btnLogout));               // Lo centra
        content.add(sesionCard);                             // Agrega tarjeta final
    }

    // ===========================
    //      HELPERS VISUALES
    // ===========================

    private JPanel card(String title) {                       // Crea una tarjeta con borde y título
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);                      // Fondo blanco
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),        // Borde gris suave
                BorderFactory.createEmptyBorder(16, 18, 16, 18)             // Padding
        ));

        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 15));
        t.setForeground(new Color(0x111827));                 // Gris oscuro
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        card.add(t);
        return card;
    }

    private JPanel labelRow(String label, String value) {     // Fila simple L:V
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

    private JPanel fieldRow(String label, JComponent field) { // Fila de formulario con input
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

    private JButton botonPrincipal(String texto) {            // Botón azul
        JButton b = new JButton(texto);
        b.setBackground(new Color(0x2563EB));                 // Azul
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.addMouseListener(new java.awt.event.MouseAdapter() { // Efecto hover
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b.setBackground(new Color(0x1E40AF));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b.setBackground(new Color(0x2563EB));
            }
        });

        return b;
    }

    private JButton botonSecundario(String texto) {           // Botón blanco
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

    private JPanel wrap(JComponent c) {                       // Envuelve componente ocupando todo el espacio
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private JPanel wrapCenter(JComponent c) {                 // Centra un componente
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    private void cargarUsuarios() {                           // Carga usuarios desde BD
        cmbUsuarios.removeAllItems();
        List<User> users = UserDao.getAll();                  // Llama al DAO
        for (User u : users)
            cmbUsuarios.addItem(new UserItem(u.getId(), u.getUsername())); // Agrega al combo
        if (cmbUsuarios.getItemCount() > 0)
            cmbUsuarios.setSelectedIndex(0);                  // Selección por defecto
    }

    private static class UserItem {                           // Item mostrado en JComboBox
        final String id;
        final String username;
        UserItem(String id, String username) {
            this.id = id;
            this.username = username;
        }
        @Override public String toString() {                  // Lo que aparece en la lista
            return username + " (" + id + ")";
        }
    }
}

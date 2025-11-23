package pos.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

import pos.login.LoginFrame;

public class Sidebar extends JPanel {

    public Sidebar(Consumer<String> onNavigate, String role, String username) {
        // Tamaño fijo del sidebar + layout vertical con GridBagLayout
        setPreferredSize(new Dimension(230, 720));
        setLayout(new GridBagLayout());
        setBackground(new Color(0x111827)); // fondo gris-oscuro estilo Tailwind Gray 900

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;                             // siempre en columna 0
        c.fill = GridBagConstraints.HORIZONTAL; // los botones ocupan todo el ancho
        c.insets = new Insets(6, 12, 6, 12);     // margen alrededor

        // ==========================================
        // 🔵 TÍTULO DEL MENÚ (arriba del sidebar)
        // ==========================================
        JLabel title = new JLabel("Almacén Sonia");
        title.setForeground(Color.WHITE);                // texto blanco
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        addAt(c, title); // helper que incrementa fila

        // ==========================================
        // 🔵 BOTONES GENERALES (visibles para ADMIN y CAJERO)
        // ==========================================
        addBtn(c, "Dashboard",   () -> onNavigate.accept(MainFrame.DASHBOARD));
        addBtn(c, "Ventas",      () -> onNavigate.accept(MainFrame.VENTAS));
        addBtn(c, "Proveedores", () -> onNavigate.accept(MainFrame.PROVEEDORES));
        addBtn(c, "Inventario",  () -> onNavigate.accept(MainFrame.INVENTARIO));
        addBtn(c, "Reportes",    () -> onNavigate.accept(MainFrame.REPORTES));

        // ==========================================
        // 🔵 OPCIONES SOLO PARA ADMINISTRADOR
        // ==========================================
        if ("ADMIN".equalsIgnoreCase(role)) {
            addSeparator(c); // línea divisoria

            addBtn(c, "Usuarios", () -> onNavigate.accept(MainFrame.USUARIOS));
            addBtn(c, "Ajustes",  () -> onNavigate.accept(MainFrame.AJUSTES));
        }

        // ==========================================
        // 🔴 CERRAR SESIÓN
        // ==========================================
        addSeparator(c);
        addBtn(c, "Cerrar sesión", () -> {
            Window w = SwingUtilities.getWindowAncestor(this); // obtiene ventana actual
            if (w != null) w.dispose();                        // la cierra

            // Vuelve al LoginFrame
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });

        // ==========================================
        // Empuja el resto hacia abajo para que el footer quede al fondo
        // ==========================================
        c.weighty = 1;
        addAt(c, Box.createVerticalGlue()); // espacio expansible

        // ==========================================
        // 🔵 FOOTER (muestra usuario y rol)
        // ==========================================
        JLabel info = new JLabel("Usuario: " + username + "  |  Rol: " + role);
        info.setForeground(new Color(0x9CA3AF));                   // gris claro
        info.setFont(info.getFont().deriveFont(Font.PLAIN, 12f)); // letra más pequeña
        addAt(c, info);
    }

    // ==========================================
    // 🔧 MÉTODOS HELPER PARA CREAR BOTONES
    // ==========================================
    private void addBtn(GridBagConstraints c, String text, Runnable action) {
        JButton b = new JButton(text);

        b.setFocusPainted(false);                     // sin borde de enfoque
        b.setBorderPainted(false);                    // sin borde visual
        b.setForeground(Color.WHITE);                 // texto blanco
        b.setBackground(new Color(0x1F2937));         // gris oscuro (Tailwind Gray 800)
        b.setPreferredSize(new Dimension(190, 40));   // tamaño uniforme
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 14f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // cursor apuntador

        b.addActionListener(e -> action.run());       // ejecuta la acción asociada
        addAt(c, b);                                  // agregar al layout
    }

    // Línea separadora para organizar visualmente
    private void addSeparator(GridBagConstraints c) {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x374151)); // gris suave
        addAt(c, sep);
    }

    // Agrega componentes incrementando la fila (gridy)
    private void addAt(GridBagConstraints c, Component comp) {
        c.gridy++;         // pasa a la siguiente fila
        add(comp, c);      // agrega el componente con esas reglas
    }
}


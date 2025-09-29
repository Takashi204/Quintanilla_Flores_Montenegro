package pos.ui;

import pos.ui.views.*;   // DashboardPanel, VentasPanel, etc.
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    // --- Campos / estado ---
    private final CardLayout card = new CardLayout();
    private final JPanel content = new JPanel(card);
    private final String roleGlobal;  // rol del usuario activo

    // --- Claves de vistas ---
    public static final String DASHBOARD   = "dashboard";
    public static final String VENTAS      = "ventas";
    public static final String PRODUCTOS   = "productos";
    public static final String CLIENTES    = "clientes";
    public static final String INVENTARIO  = "inventario";
    public static final String REPORTES    = "reportes";
    public static final String USUARIOS    = "usuarios";
    public static final String AJUSTES     = "ajustes";

    // --- Constructor ---
    public MainFrame(String username, String role) {
        // Guarda el rol ANTES de cualquier navegación
        this.roleGlobal = role;

        setTitle("Almacén Sonia — POS" + (isAdmin() ? " [ADMIN]" : " [CAJERO]"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Sidebar adaptado al rol
        Sidebar sidebar = new Sidebar(this::showView, roleGlobal, username);
        add(sidebar, BorderLayout.WEST);

        // Vistas comunes
        content.add(new DashboardPanel(), DASHBOARD);
        content.add(new VentasPanel(), VENTAS);
        content.add(new ProductosPanel(), PRODUCTOS);
        content.add(new ClientesPanel(), CLIENTES);
        content.add(new InventarioPanel(), INVENTARIO);
        content.add(new ReportesPanel(), REPORTES);

        // Vistas exclusivas de ADMIN
        if (isAdmin()) {
            content.add(new UsuariosPanel(), USUARIOS);
            content.add(new AjustesPanel(), AJUSTES);
        }

        add(content, BorderLayout.CENTER);

        // Aterrizaje por rol
        showView(isAdmin() ? DASHBOARD : VENTAS);
    }

    // --- Navegación con guardia por rol ---
    public void showView(String key) {
        if (!isAdmin() && (USUARIOS.equals(key) || AJUSTES.equals(key))) {
            JOptionPane.showMessageDialog(this,
                    "Permiso denegado (solo ADMIN).",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        card.show(content, key);
    }

    private boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(roleGlobal);
    }
}
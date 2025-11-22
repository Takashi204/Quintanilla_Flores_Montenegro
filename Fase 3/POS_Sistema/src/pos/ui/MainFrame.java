package pos.ui;

import pos.ui.views.*;
import javax.swing.*;
import java.awt.*;

import pos.login.LoginFrame;
import pos.db.DatabaseInit;
import pos.dao.UserDao;

/**
 * Ventana principal del sistema POS Pyme.
 * Controla el flujo de vistas (Dashboard, Cajero, Inventario, etc.)
 * e inicializa la base de datos.
 */
public class MainFrame extends JFrame {

    private final CardLayout card = new CardLayout();
    private final JPanel content = new JPanel(card);
    private final String roleGlobal;
    private final String usernameGlobal;

    // === Claves de vistas ===
    public static final String DASHBOARD   = "dashboard";
    public static final String VENTAS      = "ventas";
    public static final String PROVEEDORES = "proveedores";
    public static final String INVENTARIO  = "inventario";
    public static final String REPORTES    = "reportes";
    public static final String USUARIOS    = "usuarios";
    public static final String AJUSTES     = "ajustes";

    private ReportesPanel reportesPanel;

    public MainFrame(String username, String role) {
        this.roleGlobal = role;
        this.usernameGlobal = username;

        // ======================
        // ✅ Inicialización completa de la base de datos
        // ======================
        try {
            DatabaseInit.initialize(); // crea tablas de inventario, movimientos, caja
            UserDao.initTable();       // crea tabla de usuarios si no existe
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al inicializar base de datos:\n" + e.getMessage(),
                "Error crítico", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        boolean admin = isAdmin();
        setTitle("POS Pyme — " + (admin ? "ADMINISTRADOR" : "CAJERO"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ======================
        // === MODO ADMIN ===
        // ======================
        if (admin) {
            Sidebar sidebar = new Sidebar(this::showView, roleGlobal, usernameGlobal);
            add(sidebar, BorderLayout.WEST);

            // 🔹 Agregar los paneles principales
            content.add(new DashboardPanel(), DASHBOARD);
            content.add(new CajeroPanel(usernameGlobal), VENTAS);
            content.add(new ProveedoresPanel(), PROVEEDORES);
            content.add(new InventarioPanel(), INVENTARIO);

            // 🔹 Reportes y administración
            reportesPanel = new ReportesPanel();
            content.add(reportesPanel, REPORTES);
            content.add(new UsuariosPanel(), USUARIOS);
            content.add(new AjustesPanel(), AJUSTES);

            add(content, BorderLayout.CENTER);
            showView(DASHBOARD);

        // ======================
        // === MODO CAJERO ===
        // ======================
        } else {
            // ==== Encabezado ====
            JPanel top = new JPanel(new BorderLayout());
            JLabel title = new JLabel("Caja - Punto de Venta");
            title.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
            top.add(title, BorderLayout.WEST);

            JLabel user = new JLabel("Usuario: " + usernameGlobal + "  |  Rol: CAJERO");
            user.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            JButton btnLogout = new JButton("Cerrar sesión");
            btnLogout.addActionListener(e -> {
                this.dispose();
                SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
            });

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            right.setOpaque(false);
            right.add(user);
            right.add(btnLogout);
            top.add(right, BorderLayout.EAST);

            add(top, BorderLayout.NORTH);

            // ==== Cuerpo ====
            content.add(new CajeroPanel(usernameGlobal), VENTAS);
            add(content, BorderLayout.CENTER);
            showView(VENTAS);
        }
    }

    // ======================
    // === NAVEGACIÓN ===
    // ======================
    public void showView(String key) {
        if (!isAdmin() && (USUARIOS.equals(key) || AJUSTES.equals(key))) {
            JOptionPane.showMessageDialog(this,
                    "Permiso denegado (solo ADMIN).",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        card.show(content, key);

        // 🔁 Recargar reportes automáticamente al entrar
        if (isAdmin() && REPORTES.equals(key) && reportesPanel != null) {
            reportesPanel.reload();
        }
    }

    private boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(roleGlobal);
    }
}


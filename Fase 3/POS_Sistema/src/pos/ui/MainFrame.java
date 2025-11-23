package pos.ui;

import pos.ui.views.*;             // importa los paneles (Dashboard, Cajero, Inventario, etc.)
import javax.swing.*;
import java.awt.*;

import pos.login.LoginFrame;        // ventana de login
import pos.db.DatabaseInit;         // inicializa estructura completa de BD
import pos.dao.UserDao;             // inicializa tabla de usuarios

/**
 * Ventana principal del sistema POS.
 * Maneja:
 *  - navegación entre vistas usando CardLayout
 *  - carga inicial de la base de datos
 *  - control de acceso según rol (ADMIN o CAJERO)
 */
public class MainFrame extends JFrame {

    private final CardLayout card = new CardLayout(); // sistema de tarjetas para cambiar paneles
    private final JPanel content = new JPanel(card);  // contenedor de todas las vistas
    private final String roleGlobal;                  // guarda el rol del usuario
    private final String usernameGlobal;              // guarda el nombre de usuario

    // === Claves que identifican cada vista del sistema ===
    public static final String DASHBOARD   = "dashboard";
    public static final String VENTAS      = "ventas";
    public static final String PROVEEDORES = "proveedores";
    public static final String INVENTARIO  = "inventario";
    public static final String REPORTES    = "reportes";
    public static final String USUARIOS    = "usuarios";
    public static final String AJUSTES     = "ajustes";

    private ReportesPanel reportesPanel; // referencia para recargar datos cuando se muestra

    public MainFrame(String username, String role) {
        this.roleGlobal = role;          // asigna el rol (ADMIN/CAJERO)
        this.usernameGlobal = username;  // guarda el nombre del usuario logeado

        // =============================
        // 🔥 Inicialización de Base de Datos
        // =============================
        try {
            DatabaseInit.initialize(); // crea todas las tablas (inventory, movimientos, caja, providers, sales)
            UserDao.initTable();       // crea tabla users si no existe
        } catch (Exception e) {
            // si algo falla → error fatal
            JOptionPane.showMessageDialog(this,
                "Error al inicializar base de datos:\n" + e.getMessage(),
                "Error crítico", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        boolean admin = isAdmin();     // verifica si es admin

        setTitle("POS Pyme — " + (admin ? "ADMINISTRADOR" : "CAJERO")); // título dinámico
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);           // tamaño de ventana
        setLocationRelativeTo(null);  // centrar
        setLayout(new BorderLayout());

        // ===========================================
        // 🔵 MODO ADMINISTRADOR (acceso total)
        // ===========================================
        if (admin) {

            Sidebar sidebar = new Sidebar(this::showView, roleGlobal, usernameGlobal);
            add(sidebar, BorderLayout.WEST); // agrega menú lateral

            // Añade cada panel asociado a su clave
            content.add(new DashboardPanel(), DASHBOARD);
            content.add(new CajeroPanel(usernameGlobal), VENTAS);
            content.add(new ProveedoresPanel(), PROVEEDORES);
            content.add(new InventarioPanel(), INVENTARIO);

            // Paneles administrativos
            reportesPanel = new ReportesPanel();
            content.add(reportesPanel, REPORTES);
            content.add(new UsuariosPanel(), USUARIOS);
            content.add(new AjustesPanel(), AJUSTES);

            add(content, BorderLayout.CENTER);
            showView(DASHBOARD); // vista inicial

        // ===========================================
        // 🔵 MODO CAJERO — Solo ventas
        // ===========================================
        } else {

            // ---------- ENCABEZADO (barra superior) ----------
            JPanel top = new JPanel(new BorderLayout());

            JLabel title = new JLabel("Caja - Punto de Venta");
            title.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
            top.add(title, BorderLayout.WEST);

            JLabel user = new JLabel("Usuario: " + usernameGlobal + "  |  Rol: CAJERO");
            user.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            JButton btnLogout = new JButton("Cerrar sesión");
            btnLogout.addActionListener(e -> {
                this.dispose(); // cierra MainFrame
                SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true)); // vuelve al login
            });

            // Panel derecho (usuario + logout)
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            right.setOpaque(false);
            right.add(user);
            right.add(btnLogout);

            top.add(right, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            // --- CUERPO PRINCIPAL (solo panel ventas) ---
            content.add(new CajeroPanel(usernameGlobal), VENTAS);
            add(content, BorderLayout.CENTER);
            showView(VENTAS); // inicia directo en caja
        }
    }

    // ================================================
    // 🔄 CAMBIO DE VISTAS (Dashboard, Inventario, etc.)
    // ================================================
    public void showView(String key) {
        // Cajero no puede acceder a Usuarios ni Ajustes
        if (!isAdmin() && (USUARIOS.equals(key) || AJUSTES.equals(key))) {
            JOptionPane.showMessageDialog(this,
                    "Permiso denegado (solo ADMIN).",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        card.show(content, key); // cambia la tarjeta mostrada

        // Si el admin entra a REPORTES → recarga automáticamente
        if (isAdmin() && REPORTES.equals(key) && reportesPanel != null) {
            reportesPanel.reload(); // actualiza gráficos/tablas
        }
    }

    // ================================================
    // 🔍 Detección de rol
    // ================================================
    private boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(roleGlobal);
    }
}


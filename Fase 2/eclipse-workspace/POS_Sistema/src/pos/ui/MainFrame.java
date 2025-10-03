package pos.ui;

import pos.ui.views.*;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final CardLayout card = new CardLayout();
    private final JPanel content = new JPanel(card);
    private final String roleGlobal;

    public static final String DASHBOARD   = "dashboard";
    public static final String VENTAS      = "ventas";
    public static final String PRODUCTOS   = "productos";
    public static final String CLIENTES    = "clientes";
    public static final String INVENTARIO  = "inventario";
    public static final String REPORTES    = "reportes";
    public static final String USUARIOS    = "usuarios";
    public static final String AJUSTES     = "ajustes";

    // Solo la usaremos en ADMIN
    private ReportesPanel reportesPanel;

    public MainFrame(String username, String role) {
        this.roleGlobal = role;

        boolean admin = isAdmin();
        setTitle("Almacén Sonia — POS" + (admin ? " [ADMIN]" : " [CAJERO]"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        if (admin) {
            // ====== MODO ADMIN: con Sidebar y todas las vistas ======
            Sidebar sidebar = new Sidebar(this::showView, roleGlobal, username);
            add(sidebar, BorderLayout.WEST);

            content.add(new DashboardPanel(), DASHBOARD);
            content.add(new CajeroPanel(), VENTAS);        
            content.add(new ProductosPanel(), PRODUCTOS);
            content.add(new ClientesPanel(), CLIENTES);
            content.add(new InventarioPanel(), INVENTARIO);

            reportesPanel = new ReportesPanel();
            content.add(reportesPanel, REPORTES);

            content.add(new UsuariosPanel(), USUARIOS);
            content.add(new AjustesPanel(), AJUSTES);

            add(content, BorderLayout.CENTER);
            showView(DASHBOARD);

        } else {
            // ====== MODO CAJERO: SIN sidebar, pantalla solo Cajero ======
            // Barra superior muy simple (título + usuario)
            JPanel top = new JPanel(new BorderLayout());
            JLabel title = new JLabel("Cajero");
            title.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
            title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
            JLabel user = new JLabel("Usuario: " + username + "  |  Rol: CAJERO");
            user.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
            top.add(title, BorderLayout.WEST);
            top.add(user, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            // Solo registramos el CajeroPanel como "VENTAS"
            content.add(new CajeroPanel(), VENTAS);
            add(content, BorderLayout.CENTER);
            showView(VENTAS);
        }
    }

    public void showView(String key) {
        // En modo cajero no hay restricciones: solo existe VENTAS
        if (!isAdmin() && (USUARIOS.equals(key) || AJUSTES.equals(key))) {
            JOptionPane.showMessageDialog(this,
                    "Permiso denegado (solo ADMIN).",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        card.show(content, key);

        // Refrescar Reportes al abrir (solo si estamos en admin y existe el panel)
        if (isAdmin() && REPORTES.equals(key) && reportesPanel != null) {
            reportesPanel.reload();
        }
    }

    private boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(roleGlobal);
    }
}
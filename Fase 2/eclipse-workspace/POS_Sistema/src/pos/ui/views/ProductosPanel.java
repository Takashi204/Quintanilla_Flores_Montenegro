package pos.ui.views;
import javax.swing.*; import java.awt.*;
public class ProductosPanel extends JPanel {
  public ProductosPanel() {
    setLayout(new BorderLayout()); setBackground(new Color(0xF9FAFB));
    JLabel t=new JLabel("Gestión de Productos");
    t.setFont(t.getFont().deriveFont(Font.BOLD,18f));
    t.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
    add(t, BorderLayout.NORTH);
  }
}

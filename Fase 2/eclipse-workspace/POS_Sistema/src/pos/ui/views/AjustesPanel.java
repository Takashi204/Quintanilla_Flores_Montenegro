package pos.ui.views;
import javax.swing.*; import java.awt.*;
public class AjustesPanel extends JPanel {
  public AjustesPanel() {
    setLayout(new BorderLayout()); setBackground(new Color(0xF9FAFB));
    JLabel t=new JLabel("Ajustes del Sistema");
    t.setFont(t.getFont().deriveFont(Font.BOLD,18f));
    t.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
    add(t, BorderLayout.NORTH);
  }
}
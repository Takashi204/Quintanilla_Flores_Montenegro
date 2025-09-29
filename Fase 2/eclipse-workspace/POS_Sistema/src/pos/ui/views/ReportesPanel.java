package pos.ui.views;
import javax.swing.*; import java.awt.*;
public class ReportesPanel extends JPanel {
  public ReportesPanel() {
    setLayout(new BorderLayout()); setBackground(new Color(0xF9FAFB));
    JLabel t=new JLabel("Reportes");
    t.setFont(t.getFont().deriveFont(Font.BOLD,18f));
    t.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
    add(t, BorderLayout.NORTH);
  }
}
package pos.ui.views;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(0xF9FAFB));
        JLabel t = new JLabel("Dashboard — Resumen");
        t.setFont(t.getFont().deriveFont(Font.BOLD, 20f));
        t.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        add(t, BorderLayout.NORTH);
    }
}
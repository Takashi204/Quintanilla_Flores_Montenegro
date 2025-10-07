package pos.ui.views;

import pos.model.InventoryMovement;
import pos.model.Product;
import pos.repo.InMemoryMovementRepository;

import javax.swing.*;
import java.awt.*;

/**
 * Panel sencillo para realizar AJUSTES de stock (valor absoluto).
 * NOTA: Debes cargar el JComboBox con tus productos desde tu store/repositorio.
 */
public class InventoryAdjustmentPanel extends JPanel {

    private final JComboBox<Product> cmbProduct = new JComboBox<>();
    private final JSpinner spNewStock = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
    private final JTextField txtReason = new JTextField(18);
    private final JTextField txtUser = new JTextField(12);
    private final JButton btnApply = new JButton("Aplicar ajuste");

    public InventoryAdjustmentPanel() {
        setLayout(new BorderLayout(8,8));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        addField(form, gc, y++, "Producto:", cmbProduct);
        addField(form, gc, y++, "Nuevo stock:", spNewStock);
        addField(form, gc, y++, "Motivo:", txtReason);
        addField(form, gc, y++, "Usuario:", txtUser);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(btnApply);

        add(form, BorderLayout.NORTH);
        add(actions, BorderLayout.SOUTH);

        
        btnApply.addActionListener(e -> applyAdjustment());
    }

    private void addField(JPanel panel, GridBagConstraints gc, int y, String label, JComponent comp) {
        gc.gridx = 0; gc.gridy = y; gc.weightx = 0.2;
        panel.add(new JLabel(label), gc);
        gc.gridx = 1; gc.weightx = 0.8;
        panel.add(comp, gc);
    }

    private void applyAdjustment() {
        Product p = (Product) cmbProduct.getSelectedItem();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int newStock = (int) spNewStock.getValue();
        String reason = txtReason.getText().isBlank() ? "Ajuste manual" : txtReason.getText().trim();
        String user = txtUser.getText().isBlank() ? "system" : txtUser.getText().trim();

        InventoryMovement mov = InventoryMovement.adjustment(p, newStock, reason, user);
        mov.applyToProduct();
        InMemoryMovementRepository.getInstance().add(mov);

        JOptionPane.showMessageDialog(this,
                "Ajuste aplicado. Stock: " + mov.getPreviousStock() + " → " + mov.getResultingStock(),
                "OK", JOptionPane.INFORMATION_MESSAGE);
    }
}

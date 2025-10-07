package pos.repo;

import pos.model.InventoryMovement;
import java.util.List;

public interface InventoryMovementRepository {
    void add(InventoryMovement movement);
    List<InventoryMovement> findAll();
    List<InventoryMovement> findByProductCode(String code);
    List<InventoryMovement> findByType(InventoryMovement.MovementType type);
    void clear(); // opcional para pruebas
}
package pos.repo;

import pos.model.InventoryMovement;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryMovementRepository implements InventoryMovementRepository {

    private static final InMemoryMovementRepository INSTANCE = new InMemoryMovementRepository();
    public static InMemoryMovementRepository getInstance() { return INSTANCE; }

    private final List<InventoryMovement> data = new ArrayList<>();
    private int seq = 1;

    private InMemoryMovementRepository() {}

    @Override
    public synchronized void add(InventoryMovement m) {
        if (m == null) return;
        if (m.getId() <= 0) m.setId(seq++); // asigna id incremental si no viene de BD
        data.add(m);
    }

    @Override
    public synchronized List<InventoryMovement> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(data));
    }

    @Override
    public synchronized List<InventoryMovement> findByProductCode(String code) {
        if (code == null || code.isBlank()) return List.of();
        return data.stream()
                .filter(m -> m.getProduct() != null && code.equalsIgnoreCase(m.getProduct().getCode()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<InventoryMovement> findByType(InventoryMovement.MovementType type) {
        if (type == null) return List.of();
        return data.stream().filter(m -> m.getType() == type).collect(Collectors.toList());
    }

    @Override
    public synchronized void clear() { data.clear(); }
}

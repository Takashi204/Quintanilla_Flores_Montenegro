package pos.store;

import pos.model.Product;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fuente de datos EN MEMORIA para el front-end.
 * No hay base de datos real: solo datos de ejemplo y operaciones CRUD básicas.
 */
public class InMemoryStore {

    // “Base de datos” de productos
    private static final List<Product> PRODUCTS = new ArrayList<>();

    static {
        PRODUCTS.add(new Product("1001", "Arroz 5kg", "Alimentos", 2490, 50, null));
        PRODUCTS.add(new Product("1002", "Azúcar 1kg", "Alimentos", 420, 100, null));
        PRODUCTS.add(new Product("1003", "Aceite 1L", "Alimentos", 850, 30, null));
        PRODUCTS.add(new Product("1004", "Leche 1L", "Lácteos", 380, 80, LocalDate.now().plusMonths(2)));
        PRODUCTS.add(new Product("1005", "Fideos 500g", "Alimentos", 290, 60, null));
    }

    /** Devuelve lista de solo lectura con todos los productos. */
    public static List<Product> allProducts() {
        return Collections.unmodifiableList(PRODUCTS);
    }

    /** Búsqueda por código o nombre (contiene, case-insensitive). */
    public static List<Product> search(String query) {
        if (query == null || query.isBlank()) return allProducts();
        final String q = query.toLowerCase(Locale.ROOT);
        return PRODUCTS.stream()
                .filter(p -> p.getCode().toLowerCase(Locale.ROOT).contains(q)
                          || p.getName().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());
    }

    /** Busca por código exacto (case-insensitive). */
    public static Optional<Product> findByCode(String code) {
        if (code == null) return Optional.empty();
        return PRODUCTS.stream()
                .filter(p -> p.getCode().equalsIgnoreCase(code))
                .findFirst();
    }

    /** Inserta (si ya existe el código, lo reemplaza). */
    public static void upsert(Product product) {
        removeByCode(product.getCode());
        PRODUCTS.add(product);
    }

    /** Elimina por código. */
    public static boolean removeByCode(String code) {
        return PRODUCTS.removeIf(p -> p.getCode().equalsIgnoreCase(code));
    }

    /** Actualiza un producto existente (por código). Devuelve true si lo encontró. */
    public static boolean update(Product product) {
        Optional<Product> opt = findByCode(product.getCode());
        if (opt.isEmpty()) return false;
        Product p = opt.get();
        p.setName(product.getName());
        p.setCategory(product.getCategory());
        p.setPrice(product.getPrice());
        p.setStock(product.getStock());
        p.setExpiry(product.getExpiry());
        return true;
    }

    /** Genera un código nuevo simple (siguiente correlativo de 4 dígitos). */
    public static String nextCode() {
        int max = PRODUCTS.stream()
                .map(Product::getCode)
                .filter(c -> c.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max().orElse(1000);
        return String.valueOf(max + 1);
    }

    /** Productos con stock bajo. */
    public static List<Product> lowStock(int threshold) {
        return PRODUCTS.stream()
                .filter(p -> p.getStock() <= threshold)
                .collect(Collectors.toList());
    }

    /** Productos próximos a vencer. */
    public static List<Product> expiringInDays(int days) {
        LocalDate limit = LocalDate.now().plusDays(days);
        return PRODUCTS.stream()
                .filter(p -> p.getExpiry() != null && !p.getExpiry().isAfter(limit))
                .collect(Collectors.toList());
    }
}

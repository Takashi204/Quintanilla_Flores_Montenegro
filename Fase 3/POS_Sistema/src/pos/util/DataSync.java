package pos.util;

import java.util.*;
import java.util.concurrent.*;

/**
 * 🔄 Clase DataSync
 * Sistema simple de sincronización global entre paneles (como CajeroPanel, InventarioPanel, etc.)
 * Permite que todos los módulos escuchen cambios de tablas específicas (ej: "inventory").
 */
public class DataSync {

    // Mapa de listeners por categoría (por ejemplo: "inventory", "sales", etc.)
    private static final Map<String, List<Runnable>> listeners = new ConcurrentHashMap<>();

    /**
     * Registra un listener que se ejecutará cuando haya un cambio en la categoría indicada.
     * @param category Nombre de la categoría (ej: "inventory")
     * @param listener Código a ejecutar cuando se notifique un cambio
     */
    public static void addListener(String category, Runnable listener) {
        listeners.computeIfAbsent(category, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Elimina un listener registrado (opcional).
     * @param category Categoría asociada
     * @param listener Listener a eliminar
     */
    public static void removeListener(String category, Runnable listener) {
        List<Runnable> list = listeners.get(category);
        if (list != null) list.remove(listener);
    }

    /**
     * Notifica a todos los paneles suscritos que hubo un cambio en la categoría.
     * @param category Categoría afectada
     */
    public static void notifyChange(String category) {
        List<Runnable> list = listeners.get(category);
        if (list != null) {
            for (Runnable r : list) {
                try {
                    r.run();
                } catch (Exception e) {
                    System.err.println("[DataSync] Error al ejecutar listener de " + category + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Limpia todos los listeners (útil al cerrar sesión o reiniciar la app).
     */
    public static void clearAll() {
        listeners.clear();
    }
}

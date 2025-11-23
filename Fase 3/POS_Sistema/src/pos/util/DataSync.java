package pos.util; // Paquete de utilidades del sistema

import java.util.*; // List, Map, etc.
import java.util.concurrent.*; // ConcurrentHashMap y CopyOnWriteArrayList

/**
 * 🔄 Clase DataSync — sistema global de sincronización.
 * Permite que varios paneles (inventario, ventas, admin) reaccionen
 * automáticamente cuando otro módulo cambia datos en BD.
 */
public class DataSync {

    // Mapa donde cada categoría tiene una lista de listeners → ej: "inventory" → listeners
    private static final Map<String, List<Runnable>> listeners = new ConcurrentHashMap<>(); // Thread-safe

    /**
     * Agrega un listener que se ejecuta cuando ocurre un cambio.
     * @param category categoría ej: "inventory", "sales", "products"
     * @param listener código a ejecutar (Runnable)
     */
    public static void addListener(String category, Runnable listener) {
        listeners                          // Mapa global de categorías
            .computeIfAbsent(category, k -> new CopyOnWriteArrayList<>()) // Si no existe, crea lista segura
            .add(listener); // Agregar listener para esta categoría
    }

    /**
     * Quita un listener registrado.
     * @param category categoría asociada al listener
     * @param listener acción a eliminar
     */
    public static void removeListener(String category, Runnable listener) {
        List<Runnable> list = listeners.get(category); // Obtiene lista de la categoría
        if (list != null) list.remove(listener); // Si existe, lo elimina
    }

    /**
     * Ejecuta TODOS los listeners asociados a una categoría.
     * Esto es lo que provoca que los paneles refresquen su información.
     */
    public static void notifyChange(String category) {
        List<Runnable> list = listeners.get(category); // Lista de listeners de la categoría
        if (list != null) {
            for (Runnable r : list) { // Recorre todos los listeners
                try {
                    r.run(); // Ejecuta callback
                } catch (Exception e) {
                    System.err.println("[DataSync] Error al ejecutar listener de "
                            + category + ": " + e.getMessage()); // Evita que un error rompa todo
                }
            }
        }
    }

    /**
     * Borra todas las suscripciones de toda la app.
     * Útil al cerrar sesión o recargar el sistema.
     */
    public static void clearAll() {
        listeners.clear(); // Limpia el mapa entero
    }
}

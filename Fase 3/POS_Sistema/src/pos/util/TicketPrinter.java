package pos.util; // 📦 Paquete de utilidades del POS

import javax.print.*; // 🖨️ API de impresión
import javax.print.attribute.*; // Atributos de impresión
import javax.print.attribute.standard.Copies; // Cantidad de copias
import java.io.*; // Manejo de archivos
import java.text.NumberFormat; // Formato de moneda
import java.time.LocalDateTime; // Fecha/hora actual
import java.time.format.DateTimeFormatter; // Formateo fecha/hora
import java.util.List; // Listas
import java.util.Locale; // Locale CLP

import pos.model.Product; // Modelo Producto
import pos.model.SaleItem; // Modelo Ítem de Venta

/**
 * Clase responsable de generar, guardar e imprimir tickets de venta.
 * Permite impresión real en impresora térmica o PDF.
 */
public class TicketPrinter {

    private static final int WIDTH = 40; // 📏 Ancho del ticket en caracteres
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CL")); // Formato CLP

    /**
     * Genera el contenido del ticket en texto formateado.
     */
    public String generarTicket(List<SaleItem> items, int total, String cajero, int idVenta) {
        StringBuilder sb = new StringBuilder(); // Acumula las líneas del ticket

        // === ENCABEZADO ===
        sb.append(center("FERREMAS - MiCaja POS")).append("\n"); // Centrado
        sb.append(center("RUT: 77.777.777-7")).append("\n"); // RUT fijo
        sb.append(center("Av. Central 1234, Santiago")).append("\n"); // Dirección
        sb.append(repeat("-", WIDTH)).append("\n"); // Línea separadora

        // === DATOS DE VENTA ===
        sb.append("Boleta N°: ").append(idVenta).append("\n"); // ID venta
        sb.append("Fecha: ").append(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
        ).append("\n"); // Fecha/Hora actual
        sb.append("Cajero: ").append(cajero).append("\n"); // Nombre del cajero
        sb.append(repeat("-", WIDTH)).append("\n");

        // === DETALLE ===
        sb.append(String.format("%-20s %6s %12s\n", "Producto", "Cant", "Subtotal")); // Cabecera de columnas
        sb.append(repeat("-", WIDTH)).append("\n");

        for (SaleItem item : items) { // Recorre cada ítem
            Product p = item.getProduct(); // Producto
            String nombre = p.getName().length() > 18 ? p.getName().substring(0, 18) + "." : p.getName(); // Ajusta nombre largo
            int subtotal = p.getPrice() * item.getQuantity(); // Calcula subtotal

            sb.append(String.format(
                    "%-20s %6d %12s\n", // Alineaciones de columnas
                    nombre,
                    item.getQuantity(),
                    currencyFormat.format(subtotal) // Formato CLP
            ));
        }

        sb.append(repeat("-", WIDTH)).append("\n");
        sb.append(String.format("%-26s %12s\n", "TOTAL:", currencyFormat.format(total))); // Total final formateado
        sb.append(repeat("-", WIDTH)).append("\n");

        // === PIE ===
        sb.append(center("¡Gracias por su compra!")).append("\n"); // Mensaje final
        sb.append(center("www.ferremas.cl")).append("\n\n\n"); // Sitio web + espacio extra

        return sb.toString(); // Devuelve TODO el ticket como String
    }

    /**
     * Guarda el ticket como archivo TXT.
     */
    public void guardarArchivo(String ticket, int idVenta) {
        try {
            File dir = new File("tickets"); // Carpeta tickets/
            if (!dir.exists()) dir.mkdirs(); // Crea si no existe

            String nombreArchivo = "tickets/Ticket_" + idVenta + ".txt"; // Nombre archivo
            try (FileWriter writer = new FileWriter(nombreArchivo)) {
                writer.write(ticket); // Escribe contenido del ticket
            }
            System.out.println("✅ Ticket guardado en: " + nombreArchivo); // Log
        } catch (IOException e) {
            System.err.println("❌ Error al guardar ticket: " + e.getMessage()); // Error
        }
    }

    /**
     * Imprime el ticket en la impresora predeterminada.
     * Si no hay impresora física, puedes seleccionar "Microsoft Print to PDF".
     */
    public void imprimirFisico(String ticket) {
        try {
            byte[] bytes = ticket.getBytes("UTF-8"); // Convierte el ticket a bytes
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE; // Tipo de documento auto-detectado
            Doc doc = new SimpleDoc(bytes, flavor, null); // Documento imprimible

            PrintService service = PrintServiceLookup.lookupDefaultPrintService(); // Busca impresora predeterminada
            if (service == null) { // Si no hay impresora
                System.err.println("⚠️ No se encontró una impresora predeterminada.");
                return;
            }

            DocPrintJob job = service.createPrintJob(); // Crea el trabajo de impresión
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet(); // Atributos
            attrs.add(new Copies(1)); // 1 copia
            job.print(doc, attrs); // Enviar a impresión

            System.out.println("🖨️ Ticket enviado a la impresora: " + service.getName());
        } catch (Exception e) {
            System.err.println("❌ Error al imprimir ticket: " + e.getMessage());
        }
    }

    /**
     * Imprime el ticket por consola (modo simulación).
     */
    public void imprimirConsola(String ticket) {
        System.out.println(ticket); // Print normal
    }

    // ====== Helpers ======
    private String repeat(String s, int count) { return s.repeat(count); } // Repetir caracteres
    private String center(String text) { // Centrar un texto
        int padding = Math.max(0, (WIDTH - text.length()) / 2); // Calcula espacios
        return " ".repeat(padding) + text; // Devuelve el texto centrado
    }
}


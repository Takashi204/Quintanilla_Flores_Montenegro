package pos.util;

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.Copies;
import java.io.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import pos.model.Product;
import pos.model.SaleItem;

/**
 * Clase responsable de generar, guardar e imprimir tickets de venta.
 * Permite impresión real en impresora térmica o PDF.
 */
public class TicketPrinter {

    private static final int WIDTH = 40;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));

    /**
     * Genera el contenido del ticket en texto formateado.
     */
    public String generarTicket(List<SaleItem> items, int total, String cajero, int idVenta) {
        StringBuilder sb = new StringBuilder();

        // === ENCABEZADO ===
        sb.append(center("FERREMAS - MiCaja POS")).append("\n");
        sb.append(center("RUT: 77.777.777-7")).append("\n");
        sb.append(center("Av. Central 1234, Santiago")).append("\n");
        sb.append(repeat("-", WIDTH)).append("\n");

        // === DATOS DE VENTA ===
        sb.append("Boleta N°: ").append(idVenta).append("\n");
        sb.append("Fecha: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))).append("\n");
        sb.append("Cajero: ").append(cajero).append("\n");
        sb.append(repeat("-", WIDTH)).append("\n");

        // === DETALLE ===
        sb.append(String.format("%-20s %6s %12s\n", "Producto", "Cant", "Subtotal"));
        sb.append(repeat("-", WIDTH)).append("\n");

        for (SaleItem item : items) {
            Product p = item.getProduct();
            String nombre = p.getName().length() > 18 ? p.getName().substring(0, 18) + "." : p.getName();
            int subtotal = p.getPrice() * item.getQuantity();
            sb.append(String.format("%-20s %6d %12s\n",
                    nombre,
                    item.getQuantity(),
                    currencyFormat.format(subtotal)));
        }

        sb.append(repeat("-", WIDTH)).append("\n");
        sb.append(String.format("%-26s %12s\n", "TOTAL:", currencyFormat.format(total)));
        sb.append(repeat("-", WIDTH)).append("\n");

        // === PIE ===
        sb.append(center("¡Gracias por su compra!")).append("\n");
        sb.append(center("www.ferremas.cl")).append("\n\n\n");

        return sb.toString();
    }

    /**
     * Guarda el ticket como archivo TXT.
     */
    public void guardarArchivo(String ticket, int idVenta) {
        try {
            File dir = new File("tickets");
            if (!dir.exists()) dir.mkdirs();
            String nombreArchivo = "tickets/Ticket_" + idVenta + ".txt";
            try (FileWriter writer = new FileWriter(nombreArchivo)) {
                writer.write(ticket);
            }
            System.out.println("✅ Ticket guardado en: " + nombreArchivo);
        } catch (IOException e) {
            System.err.println("❌ Error al guardar ticket: " + e.getMessage());
        }
    }

    /**
     * Imprime el ticket en la impresora predeterminada.
     * Si no hay impresora física, puedes seleccionar "Microsoft Print to PDF".
     */
    public void imprimirFisico(String ticket) {
        try {
            // Convertir el texto del ticket a bytes
            byte[] bytes = ticket.getBytes("UTF-8");
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(bytes, flavor, null);

            // Buscar impresora predeterminada
            PrintService service = PrintServiceLookup.lookupDefaultPrintService();
            if (service == null) {
                System.err.println("⚠️ No se encontró una impresora predeterminada.");
                return;
            }

            DocPrintJob job = service.createPrintJob();
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(new Copies(1));
            job.print(doc, attrs);

            System.out.println("🖨️ Ticket enviado a la impresora: " + service.getName());
        } catch (Exception e) {
            System.err.println("❌ Error al imprimir ticket: " + e.getMessage());
        }
    }

    /**
     * Imprime el ticket por consola (modo simulación).
     */
    public void imprimirConsola(String ticket) {
        System.out.println(ticket);
    }

    // ====== Helpers ======
    private String repeat(String s, int count) { return s.repeat(count); }
    private String center(String text) {
        int padding = Math.max(0, (WIDTH - text.length()) / 2);
        return " ".repeat(padding) + text;
    }
}


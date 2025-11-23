package pos.util; // Paquete utilidades POS

import java.math.BigDecimal; // Para cálculos seguros
import java.math.RoundingMode; // Para redondeo
import java.text.DecimalFormat; // Formateo CLP
import java.text.DecimalFormatSymbols; // Símbolos de formato
import java.util.Locale; // Locale chileno

public final class Money { // Clase final (no heredable)

    private static final Locale CLP_LOCALE = new Locale("es", "CL"); // Locale de Chile
    private static final DecimalFormatSymbols DFS; // Símbolos de formato
    private static final DecimalFormat CLP_FMT; // Formateador CLP

    static {
        DFS = new DecimalFormatSymbols(CLP_LOCALE); // Usa locale chileno
        DFS.setDecimalSeparator(','); // Configura coma como decimal
        DFS.setGroupingSeparator('.'); // Agrupación con punto
        CLP_FMT = new DecimalFormat("$#,##0", DFS); // Formato: $1.234.567
        CLP_FMT.setParseBigDecimal(true); // Permitir parseo seguro
    }

    private Money(){} // Constructor privado, evita instancias

    /** Formatea entero CLP a “$1.234.567”. */
    public static String format(int amount) {
        return CLP_FMT.format(amount); // Formateo directo
    }

    /** Parsea texto tipo “$1.234” → 1234. Si falla, 0. */
    public static int parseOrZero(String raw) {
        if (raw == null) return 0; // Si viene null → 0
        String clean = raw.trim() // Limpia espacios y formatos
                .replace("$","") // Saca $
                .replace(".", "") // Saca puntos
                .replace(" ", "") // Saca espacios normales
                .replace("\u00A0","") // Saca "espacio no separable"
                .replace(",", ""); // Saca coma decimal (CLP no usa)
        if (clean.isEmpty()) return 0; // Cadena vacía → 0
        try {
            BigDecimal bd = new BigDecimal(clean); // Convierte a BigDecimal
            return bd.setScale(0, RoundingMode.HALF_UP).intValueExact(); // Redondeo correcto
        } catch (Exception e) {
            return 0; // Si no se puede parsear → 0
        }
    }

    public static int safeAdd(int a, int b) {
        long s = (long)a + (long)b; // Suma con long para evitar overflow
        if (s > Integer.MAX_VALUE) return Integer.MAX_VALUE; // Si se pasó arriba → clamp
        if (s < Integer.MIN_VALUE) return Integer.MIN_VALUE; // Si se pasó abajo → clamp
        return (int)s; // Retorna seguro
    }

    public static int safeMul(int a, int b) {
        long m = (long)a * (long)b; // Multiplica usando long
        if (m > Integer.MAX_VALUE) return Integer.MAX_VALUE; // Overflow arriba
        if (m < Integer.MIN_VALUE) return Integer.MIN_VALUE; // Overflow abajo
        return (int)m; // Multiplicación segura
    }

    public static int toInt(BigDecimal bd) {
        if (bd == null) return 0; // Null = 0
        return bd.setScale(0, RoundingMode.HALF_UP).intValue(); // Convierte BD → entero con redondeo
    }
}


package pos.util; // Paquete de utilidades del sistema POS

import java.math.BigDecimal; // Para cálculos exactos
import java.math.RoundingMode; // Para redondeo comercial

/** Utilidad de IVA (Chile) con 19% y resultados enteros (CLP). */
public final class Tax { // Clase final (no se hereda, solo utilidades)

    /** 19% IVA Chile */
    public static final BigDecimal IVA_RATE = new BigDecimal("0.19"); // IVA fijo como BigDecimal

    private Tax(){} // Constructor privado (evita instancias)

    /** IVA desde neto (ej: neto 1000 -> 190). */
    public static int ivaFromNeto(int neto) {
        BigDecimal n = new BigDecimal(neto); // Convierte neto a BigDecimal
        BigDecimal iva = n.multiply(IVA_RATE).setScale(0, RoundingMode.HALF_UP); // Multiplica por 0.19 y redondea
        return Money.toInt(iva); // Vuelve a int usando método seguro
    }

    /** Total desde neto (neto + IVA). */
    public static int totalFromNeto(int neto) {
        return neto + ivaFromNeto(neto); // Suma neto + IVA calculado
    }

    /** Neto desde total (divide por 1.19, redondeo comercial). */
    public static int netoFromTotal(int total) {
        BigDecimal t = new BigDecimal(total); // Total como BigDecimal
        BigDecimal factor = BigDecimal.ONE.add(IVA_RATE); // 1.19 (1 + 0.19)
        return Money.toInt(t.divide(factor, 0, RoundingMode.HALF_UP)); // Divide y redondea a entero
    }

    /** IVA desde total (total - neto). */
    public static int ivaFromTotal(int total) {
        int neto = netoFromTotal(total); // Calcula el neto desde total
        return total - neto; // IVA = total - neto
    }
}

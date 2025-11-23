package pos.model;

import java.time.LocalDateTime;

// Modelo que representa una sesión de caja (apertura/cierre) realizada por un usuario.
public class CashSession {

    private int id;                    // ID único en la tabla cash_sessions (PRIMARY KEY)
    private String user;               // Usuario que abrió la caja
    private LocalDateTime openTime;    // Fecha/hora de apertura
    private LocalDateTime closeTime;   // Fecha/hora de cierre (puede ser null si la caja sigue abierta)
    private int montoInicial;          // Monto inicial con el que se abrió la caja
    private int montoFinal;            // Monto final cuando se cierra la caja
    private boolean closed;            // Estado: false = abierta, true = cerrada

    // === GETTERS & SETTERS ===

    public int getId() {               // Devuelve el ID de la sesión
        return id;
    }
    public void setId(int id) {        // Asigna el ID (lo establece SQLite automáticamente)
        this.id = id;
    }

    public String getUser() {          // Devuelve el usuario que abrió la caja
        return user;
    }
    public void setUser(String user) { // Asigna el usuario
        this.user = user;
    }

    public LocalDateTime getOpenTime() {   // Obtiene el timestamp de apertura
        return openTime;
    }
    public void setOpenTime(LocalDateTime openTime) { // Asigna la fecha/hora de apertura
        this.openTime = openTime;
    }

    public LocalDateTime getCloseTime() {  // Obtiene el timestamp de cierre (si existe)
        return closeTime;
    }
    public void setCloseTime(LocalDateTime closeTime) { // Asigna la fecha/hora de cierre
        this.closeTime = closeTime;
    }

    public int getMontoInicial() {     // Retorna el monto con el que comenzó la caja
        return montoInicial;
    }
    public void setMontoInicial(int montoInicial) { // Asigna el monto inicial
        this.montoInicial = montoInicial;
    }

    public int getMontoFinal() {       // Retorna el monto final al cierre
        return montoFinal;
    }
    public void setMontoFinal(int montoFinal) { // Asigna el monto final
        this.montoFinal = montoFinal;
    }

    public boolean isClosed() {        // Indica si la caja está cerrada (true) o no (false)
        return closed;
    }
    public void setClosed(boolean closed) { // Cambia el estado de la caja
        this.closed = closed;
    }
}

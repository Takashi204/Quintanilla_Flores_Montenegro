package pos.model;

import java.time.LocalDateTime;

public class CashSession {

    private int id;
    private String user;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private int montoInicial;
    private int montoFinal;
    private boolean closed;

    // === GETTERS & SETTERS ===
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }

    public LocalDateTime getOpenTime() {
        return openTime;
    }
    public void setOpenTime(LocalDateTime openTime) {
        this.openTime = openTime;
    }

    public LocalDateTime getCloseTime() {
        return closeTime;
    }
    public void setCloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }

    public int getMontoInicial() {
        return montoInicial;
    }
    public void setMontoInicial(int montoInicial) {
        this.montoInicial = montoInicial;
    }

    public int getMontoFinal() {
        return montoFinal;
    }
    public void setMontoFinal(int montoFinal) {
        this.montoFinal = montoFinal;
    }

    public boolean isClosed() {
        return closed;
    }
    public void setClosed(boolean closed) {
        this.closed = closed;
    }
}

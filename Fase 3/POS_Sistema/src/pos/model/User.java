package pos.model;

import java.time.LocalDate;

public class User {

    private String id;          // ID del usuario
    private String username;    // nombre de usuario
    private String fullName;    // NOMBRE COMPLETO (API lo exige)
    private String role;        // ADMIN / CAJERO
    private boolean active;     // activo / inactivo
    private LocalDate createdAt; // fecha de creación
    private String password;    // contraseña (solo cuando se crea)

    // ==========================================
    //          CONSTRUCTOR PRINCIPAL
    // ==========================================
    public User(String id, String username, String role,
                boolean active, LocalDate createdAt,
                String password, String fullName) {

        this.id = id;
        this.username = username;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.password = password;
        this.fullName = fullName;
    }

    // ==========================================
    //               GETTERS
    // ==========================================
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }     // NECESARIO PARA API
    public String getRole() { return role; }
    public boolean isActive() { return active; }
    public LocalDate getCreatedAt() { return createdAt; }
    public String getPassword() { return password; }

    // ==========================================
    //               SETTERS
    // ==========================================
    public void setId(String id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setFullName(String fullName) { this.fullName = fullName; } // NUEVO
    public void setRole(String role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
    public void setPassword(String password) { this.password = password; }
}
package pos.model; // el modelo User pertenece a este paquete

import java.time.LocalDate; // fecha de creación del usuario

public class User {

    private String id;          // ID único del usuario (formato U0001, U0002...)
    private String username;    // nombre de usuario para iniciar sesión
    private String role;        // rol del sistema: ADMIN o CAJERO
    private boolean active;     // indica si el usuario está activo o deshabilitado
    private LocalDate createdAt; // fecha en que se creó el usuario
    private String password;    // contraseña (sin encriptar, simple)

    // ==========================================
    //           CONSTRUCTOR PRINCIPAL
    // ==========================================
    public User(String id, String username, String role,
                boolean active, LocalDate createdAt, String password) {

        this.id = id;               // asigna ID único
        this.username = username;   // username para login
        this.role = role;           // ADMIN / CAJERO
        this.active = active;       // estado activo/inactivo
        this.createdAt = createdAt; // fecha creación
        this.password = password;   // contraseña
    }

    // ==========================================
    //                 GETTERS
    // ==========================================
    public String getId() { return id; }                   // devuelve ID
    public String getUsername() { return username; }       // devuelve username
    public String getRole() { return role; }               // devuelve rol
    public boolean isActive() { return active; }           // estado activo
    public LocalDate getCreatedAt() { return createdAt; }  // fecha de creación
    public String getPassword() { return password; }       // contraseña actual

    // ==========================================
    //                 SETTERS
    // ==========================================
    public void setId(String id) { this.id = id; }                     // asigna nuevo ID
    public void setUsername(String username) { this.username = username; } // cambia username
    public void setRole(String role) { this.role = role; }             // cambia rol
    public void setActive(boolean active) { this.active = active; }    // activa/desactiva usuario
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; } // cambia fecha
    public void setPassword(String password) { this.password = password; } // cambia contraseña
}


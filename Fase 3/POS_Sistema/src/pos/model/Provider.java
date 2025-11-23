package pos.model;

// Maneja la fecha de creación del proveedor
import java.time.LocalDate;

public class Provider {

    // ==============================
    // Atributos del proveedor
    // ==============================

    private String id;          // Identificador único del proveedor (RUT, código, etc.)
    private String name;        // Nombre del proveedor
    private String phone;       // Teléfono de contacto
    private String email;       // Correo electrónico
    private String address;     // Dirección del proveedor
    private LocalDate createdAt; // Fecha en que se registró en el sistema

    // ==============================
    // Constructor principal
    // ==============================

    public Provider(String id, String name, String phone, String email, String address, LocalDate createdAt) {
        this.id = id;             // asigna ID del proveedor
        this.name = name;         // asigna nombre
        this.phone = phone;       // asigna teléfono
        this.email = email;       // asigna email
        this.address = address;   // asigna dirección
        this.createdAt = createdAt; // asigna fecha de creación
    }

    // ==============================
    // Getters / Setters
    // ==============================

    public String getId() { return id; }         // devuelve ID
    public void setId(String id) { this.id = id; } // permite cambiar ID

    public String getName() { return name; }     // devuelve nombre
    public void setName(String name) { this.name = name; } // cambia nombre

    public String getPhone() { return phone; }   // devuelve teléfono
    public void setPhone(String phone) { this.phone = phone; } // cambia teléfono

    public String getEmail() { return email; }   // devuelve correo
    public void setEmail(String email) { this.email = email; } // cambia correo

    public String getAddress() { return address; } // devuelve dirección
    public void setAddress(String address) { this.address = address; } // cambia dirección

    public LocalDate getCreatedAt() { return createdAt; } // fecha de creación
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; } // cambia fecha

    // ==============================
    // Representación en texto
    // ==============================

    @Override
    public String toString() {
        // Se verá en listas así: "ProveedorName (ID)"
        return name + " (" + id + ")";
    }
}

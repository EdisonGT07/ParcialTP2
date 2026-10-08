package com.example.demo.Modelos.Entity;

import java.time.LocalDate;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Se prefiere Long en lugar de int para manejar valores nulos antes de guardar

    private String nombre;
    private String correo;
    private LocalDate fechaInscripcion;

    @OneToOne
    @JoinColumn(name = "usuario_id")
    @JsonIgnore
    private Usuario usuario;

    // 1. Constructor vacÃ­o (OBLIGATORIO para JPA)
    public Cliente() {
    }

    // 2. Constructor con parÃ¡metros (opcional, Ãºtil para crear objetos rÃ¡pidos)
    public Cliente(String nombre, String correo, LocalDate fechaInscripcion) {
        this.nombre = nombre;
        this.correo = correo;
        this.fechaInscripcion = fechaInscripcion;
    }

    public Cliente(int i, String string, String string2, Date date) {
        //TODO Auto-generated constructor stub
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public LocalDate getFechaInscripcion() {
        return fechaInscripcion;
    }

    public void setFechaInscripcion(LocalDate fechaInscripcion) {
        this.fechaInscripcion = fechaInscripcion;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
package com.example.demo.Modelos.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "detalles")
public class Detalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "encabezado_id")
    private Encabezado encabezado;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private int cantidad;

    private double valor; // precio congelado al momento de la venta

    public Detalle() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Encabezado getEncabezado() { return encabezado; }
    public void setEncabezado(Encabezado encabezado) { this.encabezado = encabezado; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
}

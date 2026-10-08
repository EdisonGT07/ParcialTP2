package com.example.demo.Servicios;

import com.example.demo.Modelos.Entity.*;
import com.example.demo.Modelos.DAO.*;
import com.example.demo.dto.ItemCompra;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VentaService {

    private final EncabezadoRepository encRepo;
    private final DetalleRepository detRepo;
    private final ProductoRepository prodRepo;
    private final ClienteRepository cliRepo;

    public VentaService(EncabezadoRepository encRepo,
                        DetalleRepository detRepo,
                        ProductoRepository prodRepo,
                        ClienteRepository cliRepo) {
        this.encRepo = encRepo;
        this.detRepo = detRepo;
        this.prodRepo = prodRepo;
        this.cliRepo = cliRepo;
    }

    @Transactional
    public Encabezado comprar(Long clienteId, List<ItemCompra> items) {
        Cliente cliente = cliRepo.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La compra debe incluir al menos un producto");
        }

        Map<Long, Integer> cantidadesPorProducto = new TreeMap<>();
        for (ItemCompra item : items) {
            if (item == null || item.getProductoId() == null || item.getCantidad() == null
                    || item.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada producto debe ser mayor a cero");
            }
            try {
                cantidadesPorProducto.merge(item.getProductoId(), item.getCantidad(), Math::addExact);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("La cantidad solicitada es demasiado grande", e);
            }
        }

        Map<Long, Producto> productos = new LinkedHashMap<>();
        for (Long productoId : cantidadesPorProducto.keySet()) {
            Producto producto = prodRepo.findByIdForUpdate(productoId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
            int cantidad = cantidadesPorProducto.get(productoId);
            if (producto.getStock() < cantidad) {
                throw new IllegalArgumentException("Stock insuficiente para: " + producto.getNombre());
            }
            if (!Double.isFinite(producto.getPrecio()) || producto.getPrecio() < 0) {
                throw new IllegalArgumentException("El precio del producto no es válido: " + producto.getNombre());
            }
            productos.put(productoId, producto);
        }

        double total = 0;
        List<Detalle> detalles = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : cantidadesPorProducto.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            int cantidad = entry.getValue();
            Detalle d = new Detalle();
            d.setProducto(producto);
            d.setCantidad(cantidad);
            d.setValor(producto.getPrecio());
            detalles.add(d);
            total += cantidad * producto.getPrecio();
            if (!Double.isFinite(total)) {
                throw new IllegalArgumentException("El valor total de la compra no es válido");
            }
        }

        Encabezado encabezado = new Encabezado();
        encabezado.setCliente(cliente);
        encabezado.setFecha(LocalDate.now());
        encabezado.setTotal(total);
        Encabezado compraGuardada = encRepo.save(encabezado);

        for (Map.Entry<Long, Integer> entry : cantidadesPorProducto.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            producto.setStock(producto.getStock() - entry.getValue());
        }
        prodRepo.saveAll(productos.values());

        for (Detalle detalle : detalles) {
            detalle.setEncabezado(compraGuardada);
        }
        detRepo.saveAll(detalles);

        return compraGuardada;
    }

    public List<Encabezado> listarPorCliente(Cliente cliente) {
        return encRepo.findByCliente(cliente);
    }
}

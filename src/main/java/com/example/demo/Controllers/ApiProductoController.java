package com.example.demo.Controllers;

import com.example.demo.Modelos.DAO.ProductoRepository;
import com.example.demo.Modelos.Entity.Producto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ApiProductoController {

    private final ProductoRepository productoRepository;

    public ApiProductoController(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @GetMapping
    public ResponseEntity<List<Producto>> listar() {
        return ResponseEntity.ok(productoRepository.findAll());
    }
}

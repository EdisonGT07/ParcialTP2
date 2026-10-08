package com.example.demo.Controllers;

import com.example.demo.Modelos.Entity.*;
import com.example.demo.Modelos.DAO.*;
import com.example.demo.Servicios.VentaService;
import com.example.demo.Util.JwtUtil;
import com.example.demo.dto.ItemCompra;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final UsuarioRepository usuarioRepo;
    private final ClienteRepository clienteRepo;
    private final JwtUtil jwtUtil;

    public VentaController(VentaService ventaService,
                           UsuarioRepository usuarioRepo,
                           ClienteRepository clienteRepo,
                           JwtUtil jwtUtil) {
        this.ventaService = ventaService;
        this.usuarioRepo = usuarioRepo;
        this.clienteRepo = clienteRepo;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/comprar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<?> comprar(@RequestHeader("Authorization") String authHeader,
                                     @RequestBody List<ItemCompra> items) {
        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        Usuario usuario = usuarioRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getRole() != Role.CLIENTE || usuario.getEstado() != Estado.ACTIVO || !usuario.isActivo()) {
            throw new IllegalArgumentException("Solo los clientes activos pueden realizar compras");
        }

        Cliente cliente = clienteRepo.findByUsuario_Id(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado para este usuario"));

        Encabezado venta = ventaService.comprar(cliente.getId(), items);
        return ResponseEntity.ok(Map.of(
            "mensaje", "Compra realizada exitosamente",
            "total", venta.getTotal(),
            "ventaId", venta.getId()
        ));
    }
}

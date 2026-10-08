package com.example.demo.Controllers;

import com.example.demo.Modelos.Entity.*;
import com.example.demo.Servicios.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UsuarioService usuarioService;

    public AdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/solicitudes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listarPendientes() {
        return ResponseEntity.ok(usuarioService.listarPendientes());
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @PutMapping("/solicitudes/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> aprobar(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "CLIENTE") String rol) {
        Role role = Role.valueOf(rol);
        usuarioService.aprobar(id, role);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario aprobado con rol " + rol));
    }

    @PutMapping("/solicitudes/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> rechazar(@PathVariable Long id) {
        usuarioService.rechazar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario rechazado"));
    }
}

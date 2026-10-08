package com.example.demo.Controllers;

import com.example.demo.Modelos.Entity.*;
import com.example.demo.Servicios.UsuarioService;
import com.example.demo.Util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    public AuthController(UsuarioService usuarioService, JwtUtil jwtUtil) {
        this.usuarioService = usuarioService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registrar(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        usuarioService.registrar(username, password);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario registrado. Pendiente de aprobacion por el admin."));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Usuario u = usuarioService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Credenciales invalidas"));

        if (!u.isActivo() || u.getEstado() != Estado.ACTIVO) {
            throw new RuntimeException("Cuenta no activada. Espera aprobacion del administrador.");
        }

        if (!usuarioService.checkPassword(password, u.getPassword())) {
            throw new RuntimeException("Credenciales invalidas");
        }

        String token = jwtUtil.generateToken(u.getUsername(), u.getRole().name());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "username", u.getUsername(),
            "role", u.getRole().name()
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleError(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

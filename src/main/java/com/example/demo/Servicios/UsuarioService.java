package com.example.demo.Servicios;

import com.example.demo.Modelos.Entity.*;
import com.example.demo.Modelos.DAO.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private final UsuarioRepository usuarioRepo;
    private final ClienteRepository clienteRepo;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepo,
                          ClienteRepository clienteRepo,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.clienteRepo = clienteRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario registrar(String username, String rawPassword) {
        String correo = username == null ? "" : username.trim();
        if (correo.isEmpty() || !EMAIL_PATTERN.matcher(correo).matches()) {
            throw new IllegalArgumentException("El correo no es válido.");
        }
        if (rawPassword == null || !PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número.");
        }
        if (usuarioRepo.findByUsername(correo).isPresent()) {
            throw new IllegalArgumentException("El correo ya existe en el sistema.");
        }

        Usuario u = new Usuario();
        u.setUsername(correo);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(Role.PENDIENTE);
        u.setEstado(Estado.PENDIENTE);
        u.setActivo(false);
        return usuarioRepo.save(u);
    }

    public List<Usuario> listarPendientes() {
        return usuarioRepo.findAll()
                .stream()
                .filter(u -> u.getEstado() == Estado.PENDIENTE)
                .toList();
    }

    public List<Usuario> listarTodos() {
        return usuarioRepo.findAll();
    }

    public Usuario aprobar(Long id, Role rol) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        u.setEstado(Estado.ACTIVO);
        u.setActivo(true);
        u.setRole(rol);
        usuarioRepo.save(u);

        if (clienteRepo.findAll().stream().noneMatch(c -> c.getUsuario() != null && c.getUsuario().getId().equals(u.getId()))) {
            Cliente c = new Cliente();
            c.setNombre(u.getUsername().split("@")[0]);
            c.setCorreo(u.getUsername());
            c.setFechaInscripcion(LocalDate.now());
            c.setUsuario(u);
            clienteRepo.save(c);
        }
        return u;
    }

    public void rechazar(Long id) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        u.setEstado(Estado.RECHAZADO);
        u.setActivo(false);
        usuarioRepo.save(u);
    }

    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepo.findByUsername(username);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}

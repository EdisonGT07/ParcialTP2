package com.example.demo.Controllers;

import com.example.demo.Modelos.DAO.ClienteRepository;
import com.example.demo.Modelos.DAO.UsuarioRepository;
import com.example.demo.Modelos.Entity.Cliente;
import com.example.demo.Modelos.Entity.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class ClientePerfilController {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;

    public ClientePerfilController(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/cliente/perfil")
    public String perfil(Authentication authentication, Model model) {
        Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cliente cliente = clienteRepository.findAll().stream()
                .filter(c -> c.getUsuario() != null && c.getUsuario().getId().equals(usuario.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente();
                    nuevo.setNombre("");
                    nuevo.setCorreo(usuario.getUsername());
                    nuevo.setFechaInscripcion(LocalDate.now());
                    nuevo.setUsuario(usuario);
                    return nuevo;
                });

        model.addAttribute("cliente", cliente);
        return "cliente/perfil";
    }

    @PostMapping("/cliente/perfil")
    public String guardarPerfil(@ModelAttribute("cliente") Cliente cliente,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cliente existente = clienteRepository.findAll().stream()
                .filter(c -> c.getUsuario() != null && c.getUsuario().getId().equals(usuario.getId()))
                .findFirst()
                .orElse(new Cliente());

        existente.setNombre(cliente.getNombre());
        existente.setCorreo(cliente.getCorreo());
        existente.setFechaInscripcion(cliente.getFechaInscripcion() != null ? cliente.getFechaInscripcion() : LocalDate.now());
        existente.setUsuario(usuario);

        clienteRepository.save(existente);
        redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente.");
        return "redirect:/cliente/perfil";
    }
}

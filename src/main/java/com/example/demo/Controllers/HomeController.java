package com.example.demo.Controllers;

import com.example.demo.Modelos.DAO.ClienteRepository;
import com.example.demo.Modelos.DAO.UsuarioRepository;
import com.example.demo.Modelos.Entity.Cliente;
import com.example.demo.Modelos.Entity.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class HomeController {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;

    public HomeController(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        model.addAttribute("usuario", usuario);
        Optional<Cliente> cliente = clienteRepository.findAll().stream()
                .filter(c -> c.getUsuario() != null && c.getUsuario().getId().equals(usuario.getId()))
                .findFirst();
        model.addAttribute("cliente", cliente.orElse(null));
        return "home";
    }
}

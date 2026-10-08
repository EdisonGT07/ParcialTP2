package com.example.demo.Controllers;

import com.example.demo.Modelos.DAO.UsuarioRepository;
import com.example.demo.Modelos.Entity.Role;
import com.example.demo.Modelos.Entity.Usuario;
import com.example.demo.Servicios.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminViewController {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public AdminViewController(UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/admin/usuarios")
    public String usuariosPendientes(Model model) {
        List<Usuario> usuarios = usuarioRepository.findAll().stream()
                .filter(u -> u.getEstado() != null && u.getEstado().name().equals("PENDIENTE"))
                .toList();
        model.addAttribute("usuarios", usuarios);
        return "admin/usuarios-pendientes";
    }

    @PostMapping("/admin/usuarios/{id}/activar")
    public String activar(@PathVariable Long id,
                          @RequestParam String rol,
                          RedirectAttributes redirectAttributes) {
        try {
            usuarioService.aprobar(id, Role.valueOf(rol));
            redirectAttributes.addFlashAttribute("success", "Cuenta activada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
}

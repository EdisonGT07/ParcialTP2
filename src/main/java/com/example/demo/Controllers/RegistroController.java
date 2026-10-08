package com.example.demo.Controllers;

import com.example.demo.Servicios.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistroController {

    private final UsuarioService usuarioService;

    public RegistroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public String registrar(@RequestParam String username,
                            @RequestParam String password,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        try {
            usuarioService.registrar(username, password);
            redirectAttributes.addFlashAttribute("success", "Registro exitoso. Su cuenta queda pendiente de aprobación por el administrador.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            return "auth/registro";
        }
    }
}

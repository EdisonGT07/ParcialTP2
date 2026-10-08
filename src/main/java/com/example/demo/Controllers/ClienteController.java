package com.example.demo.Controllers;

import com.example.demo.Modelos.Entity.Cliente;
import com.example.demo.Modelos.DAO.ClienteRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cliente")
@PreAuthorize("hasRole('ADMIN')")
public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // 1. READ: Listar todos los clientes existentes
    @GetMapping("/listar")
    public String listar(Model model) {
        model.addAttribute("titulo", "Listado de Clientes");
        model.addAttribute("clientes", clienteRepository.findAll());
        return "cliente/listar";
    }

    // 2. CREATE: Mostrar formulario para un cliente nuevo
    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("titulo", "Nuevo Cliente");
        model.addAttribute("cliente", new Cliente()); // EnvÃ­a un objeto vacÃ­o a la vista
        return "cliente/formulario";
    }

    // 3. CREATE / UPDATE: Recibir datos del formulario y guardar
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("cliente") Cliente cliente,
                          RedirectAttributes redirectAttributes) {
        clienteRepository.save(cliente); // Inserta si es nuevo, actualiza si el ID ya existe
        redirectAttributes.addFlashAttribute("success", "Cliente guardado correctamente.");
        return "redirect:/cliente/listar";
    }

    // 4. UPDATE: Cargar los datos de un cliente existente para modificarlo
    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, Model model) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de cliente no vÃ¡lido: " + id));
        model.addAttribute("titulo", "Editar Cliente");
        model.addAttribute("cliente", cliente);
        return "cliente/formulario";
    }

    // 5. DELETE: Eliminar registro por ID
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        clienteRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Cliente eliminado correctamente.");
        return "redirect:/cliente/listar";
    }
}
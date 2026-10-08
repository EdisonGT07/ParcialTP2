package com.example.demo.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.Modelos.DAO.ProductoRepository;
import com.example.demo.Modelos.Entity.Producto;

@Controller 
@RequestMapping ("/producto")
@PreAuthorize("hasRole('ADMIN')")
public class ProductoController {
    
    private final ProductoRepository productoRepository;

    public ProductoController(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }   
    
    @GetMapping("/listar")
    public String listar(Model model) {
        model.addAttribute("titulo", "Listado de Productos");
        model.addAttribute("productos", productoRepository.findAll());
        return "Producto/listar";
    }
        
    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("titulo", "Nuevo Producto");
        model.addAttribute("producto", new Producto());
        return "Producto/formulario";
    }
    
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("producto") Producto producto,
                          RedirectAttributes redirectAttributes) {
        productoRepository.save(producto);
        redirectAttributes.addFlashAttribute("success", "Producto guardado correctamente.");
        return "redirect:/producto/listar";
    }
    
    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, Model model) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de producto no vÃ¡lido: " + id));
        model.addAttribute("titulo", "Editar Producto");
        model.addAttribute("producto", producto);
        return "Producto/formulario";
    }
    
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        productoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Producto eliminado correctamente.");
        return "redirect:/producto/listar";
    }   
}

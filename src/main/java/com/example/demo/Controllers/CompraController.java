package com.example.demo.Controllers;

import com.example.demo.Modelos.DAO.ClienteRepository;
import com.example.demo.Modelos.DAO.DetalleRepository;
import com.example.demo.Modelos.DAO.EncabezadoRepository;
import com.example.demo.Modelos.DAO.ProductoRepository;
import com.example.demo.Modelos.DAO.UsuarioRepository;
import com.example.demo.Modelos.Entity.Cliente;
import com.example.demo.Modelos.Entity.Detalle;
import com.example.demo.Modelos.Entity.Encabezado;
import com.example.demo.Modelos.Entity.Estado;
import com.example.demo.Modelos.Entity.Producto;
import com.example.demo.Modelos.Entity.Role;
import com.example.demo.Modelos.Entity.Usuario;
import com.example.demo.Servicios.VentaService;
import com.example.demo.dto.CompraForm;
import com.example.demo.dto.ItemCompra;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
public class CompraController {

    private final ProductoRepository productoRepository;
    private final EncabezadoRepository encabezadoRepository;
    private final DetalleRepository detalleRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final VentaService ventaService;

    public CompraController(ProductoRepository productoRepository,
                           EncabezadoRepository encabezadoRepository,
                           DetalleRepository detalleRepository,
                           UsuarioRepository usuarioRepository,
                           ClienteRepository clienteRepository,
                           VentaService ventaService) {
        this.productoRepository = productoRepository;
        this.encabezadoRepository = encabezadoRepository;
        this.detalleRepository = detalleRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.ventaService = ventaService;
    }

    @GetMapping("/comprar")
    public String comprar(Model model, Authentication authentication) {
        Usuario usuario = usuarioActivo(authentication);
        if (usuario == null) {
            return "redirect:/acceso-denegado";
        }

        Cliente cliente = clienteRepository.findByUsuario_Id(usuario.getId()).orElse(null);

        if (cliente == null) {
            model.addAttribute("mensajeCliente", "Debe completar su perfil antes de realizar compras.");
        }

        List<Producto> productos = productoRepository.findAll().stream()
                .filter(producto -> producto.getStock() > 0)
                .sorted((primero, segundo) -> primero.getNombre().compareToIgnoreCase(segundo.getNombre()))
                .toList();
        CompraForm compraForm = new CompraForm();
        for (Producto producto : productos) {
            ItemCompra item = new ItemCompra();
            item.setProductoId(producto.getId());
            item.setCantidad(0);
            compraForm.getItems().add(item);
        }

        model.addAttribute("productos", productos);
        model.addAttribute("categorias", productos.stream()
                .map(Producto::getCategoria)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(categoria -> !categoria.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList());
        model.addAttribute("compraForm", compraForm);
        return "compra/comprar";
    }

    @GetMapping("/comprar/confirmacion/{compraId}")
    public String confirmacion(@PathVariable Long compraId,
                               Authentication authentication,
                               Model model) {
        Usuario usuario = usuarioActivo(authentication);
        if (usuario == null) {
            return "redirect:/acceso-denegado";
        }

        Cliente cliente = clienteRepository.findByUsuario_Id(usuario.getId()).orElse(null);
        if (cliente == null) {
            return "redirect:/cliente/perfil";
        }

        Encabezado compra = encabezadoRepository.findById(compraId)
                .filter(encabezado -> encabezado.getCliente() != null
                        && Objects.equals(encabezado.getCliente().getId(), cliente.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada"));

        List<Detalle> detalles = detalleRepository.findByEncabezado_Id(compra.getId());
        model.addAttribute("compra", compra);
        model.addAttribute("detalles", detalles);
        model.addAttribute("cantidadArticulos", detalles.stream().mapToInt(Detalle::getCantidad).sum());
        return "compra/confirmacion";
    }

    @PostMapping("/comprar/confirmar")
    public String confirmar(@ModelAttribute("compraForm") CompraForm compraForm,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioActivo(authentication);
        if (usuario == null) {
            return "redirect:/acceso-denegado";
        }

        Cliente cliente = clienteRepository.findByUsuario_Id(usuario.getId()).orElse(null);

        if (cliente == null) {
            redirectAttributes.addFlashAttribute("error", "Debe completar su perfil para poder comprar.");
            return "redirect:/cliente/perfil";
        }

        List<ItemCompra> items = new ArrayList<>();
        if (compraForm.getItems() != null) {
            for (ItemCompra item : compraForm.getItems()) {
                if (item == null || item.getProductoId() == null || item.getCantidad() == null || item.getCantidad() < 0) {
                    redirectAttributes.addFlashAttribute("error", "La selección de productos o cantidades no es válida.");
                    return "redirect:/comprar";
                }
                if (item.getCantidad() == 0) {
                    continue;
                }
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar al menos un producto con cantidad mayor a cero.");
            return "redirect:/comprar";
        }

        try {
            var compra = ventaService.comprar(cliente.getId(), items);
            return "redirect:/comprar/confirmacion/" + compra.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/comprar";
        }
    }

    private Usuario usuarioActivo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Usuario usuario = usuarioRepository.findByUsername(authentication.getName()).orElse(null);
        if (usuario == null || usuario.getRole() != Role.CLIENTE
                || usuario.getEstado() != Estado.ACTIVO || !usuario.isActivo()) {
            return null;
        }
        return usuario;
    }
}

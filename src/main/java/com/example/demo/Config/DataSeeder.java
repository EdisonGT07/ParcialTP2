package com.example.demo.Config;

import com.example.demo.Modelos.Entity.*;
import com.example.demo.Modelos.DAO.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seed(UsuarioRepository usuarioRepo,
                                  ClienteRepository clienteRepo,
                                  ProductoRepository productoRepo,
                                  PasswordEncoder passwordEncoder) {
        return args -> {
            if (usuarioRepo.findByUsername("admin@parcial.com").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setUsername("admin@parcial.com");
                admin.setPassword(passwordEncoder.encode("Admin123*"));
                admin.setRole(Role.ADMIN);
                admin.setEstado(Estado.ACTIVO);
                admin.setActivo(true);
                usuarioRepo.save(admin);

                Cliente c = new Cliente();
                c.setNombre("Administrador");
                c.setCorreo("admin@parcial.com");
                c.setFechaInscripcion(LocalDate.now());
                c.setUsuario(admin);
                clienteRepo.save(c);
            }

            if (productoRepo.count() == 0) {
                productoRepo.save(new Producto(null, "Laptop HP", "Laptop HP 15 pulgadas", 1500000.0, 10, "Tecnologia", "HP"));
                productoRepo.save(new Producto(null, "Mouse Logitech", "Mouse inalambrico", 80000.0, 25, "Accesorios", "Logitech"));
                productoRepo.save(new Producto(null, "Teclado Mecanico", "Teclado mecanico RGB", 250000.0, 15, "Accesorios", "Razer"));
                productoRepo.save(new Producto(null, "Monitor Samsung", "Monitor 27 pulgadas 4K", 1200000.0, 8, "Tecnologia", "Samsung"));
                productoRepo.save(new Producto(null, "Audifonos Sony", "Audifonos bluetooth", 350000.0, 20, "Audio", "Sony"));
            }
        };
    }
}

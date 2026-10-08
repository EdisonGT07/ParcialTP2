package com.example.demo.Modelos.DAO;

import com.example.demo.Modelos.Entity.Encabezado;
import com.example.demo.Modelos.Entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EncabezadoRepository extends JpaRepository<Encabezado, Long> {
    List<Encabezado> findByCliente(Cliente cliente);
}

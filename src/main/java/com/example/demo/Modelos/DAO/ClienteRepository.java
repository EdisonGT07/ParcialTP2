package com.example.demo.Modelos.DAO;

import com.example.demo.Modelos.Entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByUsuario_Id(Long usuarioId);
}
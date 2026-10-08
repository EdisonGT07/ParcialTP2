package com.example.demo.Modelos.DAO;

import com.example.demo.Modelos.Entity.Detalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleRepository extends JpaRepository<Detalle, Long> {
    List<Detalle> findByEncabezado_Id(Long encabezadoId);
}

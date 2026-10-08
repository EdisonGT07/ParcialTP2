package com.example.demo.security;

import com.example.demo.Modelos.DAO.UsuarioRepository;
import com.example.demo.Modelos.Entity.Estado;
import com.example.demo.Modelos.Entity.Usuario;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Correo o contraseña incorrectos"));

        boolean disabled = usuario.getEstado() != Estado.ACTIVO || !usuario.isActivo();
        String authority = usuario.getRole() == null ? "ROLE_CLIENTE" : "ROLE_" + usuario.getRole().name();

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(authority)
                .disabled(disabled)
                .build();
    }
}

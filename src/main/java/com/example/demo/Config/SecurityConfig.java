package com.example.demo.Config;

import com.example.demo.Servicios.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler(UsuarioService usuarioService) {
        return (request, response, exception) -> {
            String target = "/login?error";
            if (exception != null && exception.getClass().getSimpleName().contains("Disabled")) {
                target = "/login?pendiente";
            } else {
                String username = request.getParameter("username");
                if (username != null && usuarioService.findByUsername(username.trim()).isEmpty()) {
                    target = "/registro?username=" + URLEncoder.encode(username.trim(), StandardCharsets.UTF_8);
                }
            }
            response.sendRedirect(request.getContextPath() + target);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                  AuthenticationFailureHandler authenticationFailureHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/registro", "/acceso-denegado", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/home", "/procesos", "/procesos/**").authenticated()
                .requestMatchers("/cliente/perfil").hasRole("CLIENTE")
                .requestMatchers("/admin", "/admin/**", "/producto/**", "/productos/**", "/cliente/**",
                        "/clientes/**", "/compras/**", "/detalles/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/comprar/**", "/api/ventas/**").hasRole("CLIENTE")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/home", true)
                .failureHandler(authenticationFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/acceso-denegado"));

        return http.build();
    }
}

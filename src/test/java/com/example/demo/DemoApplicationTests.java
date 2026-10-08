package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:compras-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private ClienteRepository clienteRepository;

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private EncabezadoRepository encabezadoRepository;

	@Autowired
	private DetalleRepository detalleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
	}

	@Test
	void anonymousUsersAreRedirectedFromProcessRoutesToLogin() throws Exception {
		mockMvc.perform(get("/home"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/procesos"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/procesos/ventas"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/producto/listar"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/comprar"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void activeClientCanSeeAvailableProductsAndBuySelectedQuantities() throws Exception {
		String username = "cliente-" + UUID.randomUUID() + "@example.com";
		Usuario usuario = new Usuario();
		usuario.setUsername(username);
		usuario.setPassword(passwordEncoder.encode("Cliente123"));
		usuario.setRole(Role.CLIENTE);
		usuario.setEstado(Estado.ACTIVO);
		usuario.setActivo(true);
		usuarioRepository.save(usuario);

		Cliente cliente = new Cliente();
		cliente.setNombre("Cliente HU-05");
		cliente.setCorreo(username);
		cliente.setFechaInscripcion(LocalDate.now());
		cliente.setUsuario(usuario);
		clienteRepository.save(cliente);

		Producto disponible = productoRepository.save(
				new Producto(null, "Producto HU-05", "Descripción de prueba", 1250.0, 5, "Prueba", "Marca"));
		Producto agotado = productoRepository.save(
				new Producto(null, "Producto agotado HU-05", "Sin stock", 500.0, 0, "Prueba", "Marca"));

		LoginPage loginPage = loginPage();
		MvcResult loginResult = mockMvc.perform(post("/login")
						.session(loginPage.session())
						.param("_csrf", loginPage.csrfToken())
						.param("username", username)
						.param("password", "Cliente123"))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		MvcResult page = mockMvc.perform(get("/comprar").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Producto HU-05")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Descripción de prueba")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Valor unitario:")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("Producto agotado HU-05"))))
				.andReturn();
		String csrfToken = extractCsrfToken(page.getResponse().getContentAsString());
		List<Producto> productosDisponibles = productoRepository.findAll().stream()
				.filter(producto -> producto.getStock() > 0)
				.toList();

		MockHttpServletRequestBuilder overstockedConfirmation = post("/comprar/confirmar")
				.session(session)
				.param("_csrf", csrfToken);
		for (int index = 0; index < productosDisponibles.size(); index++) {
			Producto producto = productosDisponibles.get(index);
			overstockedConfirmation.param("items[" + index + "].productoId", producto.getId().toString());
			overstockedConfirmation.param("items[" + index + "].cantidad",
					producto.getId().equals(disponible.getId()) ? "6" : "0");
		}
		mockMvc.perform(overstockedConfirmation)
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/comprar"));
		org.junit.jupiter.api.Assertions.assertTrue(encabezadoRepository.findByCliente(cliente).isEmpty());
		org.junit.jupiter.api.Assertions.assertEquals(5,
				productoRepository.findById(disponible.getId()).orElseThrow().getStock());

		MockHttpServletRequestBuilder confirmation = post("/comprar/confirmar")
				.session(session)
				.param("_csrf", csrfToken);
		for (int index = 0; index < productosDisponibles.size(); index++) {
			Producto producto = productosDisponibles.get(index);
			confirmation.param("items[" + index + "].productoId", producto.getId().toString());
			confirmation.param("items[" + index + "].cantidad",
					producto.getId().equals(disponible.getId()) ? "2" : "0");
		}

		MvcResult purchaseResult = mockMvc.perform(confirmation)
				.andExpect(status().is3xxRedirection())
				.andReturn();
		List<Encabezado> compras = encabezadoRepository.findByCliente(cliente);
		org.junit.jupiter.api.Assertions.assertEquals(1, compras.size());
		Encabezado compra = compras.get(0);
		org.junit.jupiter.api.Assertions.assertEquals("/comprar/confirmacion/" + compra.getId(),
				purchaseResult.getResponse().getRedirectedUrl());
		org.junit.jupiter.api.Assertions.assertEquals(LocalDate.now(), compra.getFecha());
		org.junit.jupiter.api.Assertions.assertEquals(2500.0, compra.getTotal());
		List<Detalle> detalles = detalleRepository.findByEncabezado_Id(compra.getId());
		org.junit.jupiter.api.Assertions.assertEquals(1, detalles.size());
		org.junit.jupiter.api.Assertions.assertEquals(disponible.getId(), detalles.get(0).getProducto().getId());
		org.junit.jupiter.api.Assertions.assertEquals(2, detalles.get(0).getCantidad());
		org.junit.jupiter.api.Assertions.assertEquals(1250.0, detalles.get(0).getValor());
		mockMvc.perform(get("/comprar/confirmacion/" + compra.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("¡Gracias por tu compra!")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("#" + compra.getId())))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Producto HU-05")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Total pagado")));
		org.junit.jupiter.api.Assertions.assertEquals(3,
				productoRepository.findById(disponible.getId()).orElseThrow().getStock());
		org.junit.jupiter.api.Assertions.assertEquals(0,
				productoRepository.findById(agotado.getId()).orElseThrow().getStock());

		usuario.setActivo(false);
		usuario.setEstado(Estado.PENDIENTE);
		usuarioRepository.save(usuario);
		mockMvc.perform(get("/comprar").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/acceso-denegado"));
	}

	@Test
	void unknownEmailRedirectsToRegistrationAndPrefillsEmail() throws Exception {
		String email = "nuevo@example.com";
		LoginPage loginPage = loginPage();

		mockMvc.perform(post("/login")
						.session(loginPage.session())
						.param("_csrf", loginPage.csrfToken())
						.param("username", email)
						.param("password", "ClaveIncorrecta1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/registro?username=nuevo%40example.com"));

		mockMvc.perform(get("/registro").param("username", email))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"" + email + "\"")));
	}

	@Test
	void incorrectPasswordForExistingEmailStaysOnLogin() throws Exception {
		LoginPage loginPage = loginPage();

		mockMvc.perform(post("/login")
						.session(loginPage.session())
						.param("_csrf", loginPage.csrfToken())
						.param("username", "admin@parcial.com")
						.param("password", "ClaveIncorrecta1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?error"));
	}

	private LoginPage loginPage() throws Exception {
		MvcResult result = mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andReturn();
		String token = extractCsrfToken(result.getResponse().getContentAsString());
		return new LoginPage(token, (MockHttpSession) result.getRequest().getSession(false));
	}

	private String extractCsrfToken(String content) {
		Matcher matcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(content);
		if (!matcher.find()) {
			throw new AssertionError("Page did not render a CSRF token");
		}
		return matcher.group(1);
	}

	private record LoginPage(String csrfToken, MockHttpSession session) {
	}
}

# Documentación técnica — Mercado

## 1. Propósito y alcance

Este proyecto implementa una aplicación web para administrar un catálogo, registrar clientes y gestionar compras e inventario. Tiene dos tipos de interacción:

- **MVC web:** páginas renderizadas en el servidor con Spring MVC y Thymeleaf.
- **API REST:** algunos endpoints JSON para autenticación, administración de usuarios, consulta de productos y compras.

La descripción siguiente refleja el código actual. Las observaciones técnicas indican diferencias entre componentes o decisiones que conviene tener presentes; no implican que ya estén corregidas.

## 2. Tecnologías

| Tecnología | Uso |
|---|---|
| Java 25 | Lenguaje y versión configurada en Maven |
| Spring Boot 4.0.8 | Arranque y configuración de la aplicación |
| Spring MVC | Controladores web y REST |
| Thymeleaf | Renderizado de vistas HTML del servidor |
| Spring Data JPA / Hibernate | Persistencia y mapeo objeto-relacional |
| PostgreSQL | Base de datos relacional para desarrollo y ejecución |
| Spring Security | Autenticación de formularios, autorización por roles y protección CSRF |
| JJWT 0.11.5 | Generación y lectura de tokens JWT para los endpoints de autenticación/API |
| Maven | Dependencias, pruebas y ejecución |

La clase de entrada es `DemoApplication`, ubicada en `src/main/java/com/example/demo/DemoApplication.java`.

## 3. Organización del código

Las carpetas principales dentro de `src/main/java/com/example/demo/` son:

| Carpeta | Responsabilidad |
|---|---|
| `Controllers/` | Controladores MVC que devuelven vistas y controladores REST que devuelven JSON |
| `Modelos/Entity/` | Entidades JPA y enumeraciones del dominio |
| `Modelos/DAO/` | Repositorios Spring Data para consultar y persistir entidades |
| `Servicios/` | Reglas de negocio de registro/aprobación de usuarios y compras |
| `Config/` | Configuración de seguridad y carga inicial de datos |
| `security/` | Adaptador entre usuarios persistidos y la autenticación de Spring Security |
| `Util/` | Clases auxiliares relacionadas con JWT y respuestas ante fallas de autenticación |
| `dto/` | Objetos para recibir los datos de compra de un formulario o petición JSON |

En `src/main/resources/` están la configuración, las vistas Thymeleaf, los recursos estáticos y `import.sql`. Las vistas se encuentran en `templates/`; los estilos y scripts, en `static/`.

## 4. Cómo funciona el MVC

### 4.1 Modelo

El modelo de dominio está formado por las entidades JPA `Usuario`, `Cliente`, `Producto`, `Encabezado` y `Detalle`, además de las enumeraciones `Role` y `Estado`. Los repositorios de `Modelos/DAO/` abstraen el acceso a la base de datos.

Los formularios de compra no se enlazan directamente a una entidad de venta. El controlador recibe un `CompraForm` con una lista de `ItemCompra` (`productoId`, `cantidad`) y delega la operación en `VentaService`.

### 4.2 Vista

Thymeleaf renderiza las páginas de `resources/templates/` con los atributos que cada controlador agrega a `Model`. Por ejemplo, `ProductoController` prepara `productos` y `titulo`, y devuelve la plantilla `Producto/listar`.

La interfaz incluye páginas de acceso y registro, administración de productos/clientes/usuarios, perfil del cliente y tienda/confirmación de compra. Los fragmentos reutilizables están en `templates/fragments/`. El JavaScript de la tienda mejora el filtrado, ordenamiento y resumen del carrito en el navegador; los valores mostrados allí son una ayuda de interfaz, no la autoridad para aceptar una compra.

### 4.3 Controlador y recorrido de una petición

En el flujo web típico:

1. El navegador realiza una petición HTTP a una ruta.
2. Spring Security evalúa autenticación, CSRF y permisos configurados.
3. El controlador MVC (`@Controller`) consulta repositorios o delega reglas a un servicio.
4. El controlador agrega datos al modelo y devuelve el nombre de una vista, o redirige a otra ruta.
5. Thymeleaf procesa la plantilla y genera el HTML.

Los controladores REST (`@RestController`) siguen el mismo recorrido hasta la lógica de aplicación, pero serializan sus respuestas —por lo general— como JSON en vez de seleccionar una plantilla.

### 4.4 Mapa de controladores

| Controlador | Rutas/área | Responsabilidad |
|---|---|---|
| `ViewController` | `/`, `/login`, `/registro`, `/acceso-denegado` | Entrada y páginas públicas de autenticación |
| `RegistroController` | `POST /registro` | Registro web y mensajes de validación |
| `HomeController` | `GET /home` | Inicio del usuario autenticado |
| `AdminViewController` | `/admin/usuarios` | Vista web para aprobar solicitudes |
| `AdminController` | `/api/admin/**` | API para listar, aprobar y rechazar usuarios |
| `ProductoController` | `/producto/**` | CRUD web de productos |
| `ClienteController` | `/cliente/**` | CRUD administrativo de clientes |
| `ClientePerfilController` | `/cliente/perfil` | Consulta y actualización del perfil del cliente autenticado |
| `CompraController` | `/comprar/**` | Catálogo, envío de compra y confirmación web |
| `AuthController` | `/api/auth/**` | Registro y autenticación JSON con generación de JWT |
| `ApiProductoController` | `GET /api/productos` | Consulta de productos en JSON |
| `VentaController` | `POST /api/ventas/comprar` | Compra desde API, delegada al servicio de ventas |

## 5. Arquitectura de persistencia

### 5.1 Entidades y campos

| Entidad | Tabla y campos principales | Observaciones |
|---|---|---|
| `Usuario` | `usuarios`: `id`, `username`, `password`, `role`, `estado`, `activo` | `username` es único y no nulo; `password` es no nula. Los enumerados se guardan como texto. |
| `Cliente` | Nombre de tabla JPA derivado de la entidad: `id`, `nombre`, `correo`, `fechaInscripcion`, `usuario_id` | Su relación con `Usuario` está declarada como `@OneToOne`; `usuario_id` es la clave foránea. |
| `Producto` | Nombre de tabla JPA derivado de la entidad: `id`, `nombre`, `descripcion`, `precio`, `stock`, `categoria`, `marca` | Representa el catálogo y la existencia disponible. |
| `Encabezado` | `encabezados`: `id`, `cliente_id`, `fecha`, `total` | Cabecera de una compra, asociada a un cliente. |
| `Detalle` | `detalles`: `id`, `encabezado_id`, `producto_id`, `cantidad`, `valor` | Línea de compra; `valor` conserva el precio unitario utilizado en esa venta. |

Hibernate aplica su estrategia de nombres para las tablas/columnas no nombradas explícitamente. El esquema concreto se crea o actualiza a partir de las entidades al iniciar la aplicación; no hay un conjunto de migraciones versionadas en el proyecto.

### 5.2 Relaciones

```text
Usuario 1 ─── 0..1 Cliente
Cliente 1 ─── 0..N Encabezado
Encabezado 1 ─── 0..N Detalle
Producto 1 ─── 0..N Detalle
```

- `Cliente.usuario` es el lado propietario de la relación uno a uno; `Usuario.cliente` usa `mappedBy = "usuario"`.
- Una compra (`Encabezado`) pertenece a un cliente.
- Cada detalle pertenece a un encabezado y apunta a un producto.
- `Detalle.valor` guarda el precio unitario al momento de la venta, de modo que el detalle conserva ese importe aunque luego cambie el precio actual del producto.
- No se declara una entidad de pago, envío ni un estado de compra.

### 5.3 Configuración y datos iniciales

Por defecto, la aplicación usa el perfil `postgres`. Se conecta a `jdbc:postgresql://localhost:5432/comprasdb` con el usuario `compras_app`; la URL, el usuario y la contraseña se pueden sobreescribir mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. Spring carga las variables de un archivo `.env` en la raíz del proyecto si existe, y las variables de entorno del sistema tienen prioridad. Copia `.env.example` como `.env` y reemplaza la contraseña de ejemplo; `.env` está excluido de Git. Flyway crea el esquema desde `src/main/resources/db/migration/` y Hibernate valida las entidades con `ddl-auto=validate`.

La migración crea un esquema nuevo y vacío en PostgreSQL; no copia los registros del archivo H2 `data/comprasdb.mv.db`, que se conserva sin cambios. Al iniciar, `DataSeeder` crea el administrador y los productos de ejemplo si todavía no existen.

`DataSeeder` crea datos de ejemplo únicamente si faltan el administrador o productos. Después de migrar, cambiar o retirar las credenciales de administrador de desarrollo antes de desplegar el sistema. Las sesiones web vencen a los 30 minutos.

`DataSeeder` crea si no existen un usuario administrativo y cinco productos de ejemplo; también genera un perfil de cliente asociado al administrador. Las credenciales de administrador que aparecen en el código son de desarrollo y deben cambiarse o eliminarse antes de desplegar el sistema. `import.sql` está vacío salvo por un comentario.

## 6. Lógica de negocio

### 6.1 Registro, aprobación y acceso

La lógica está en `UsuarioService`:

1. Normaliza los espacios externos del correo y verifica su formato.
2. Comprueba que la contraseña tenga al menos ocho caracteres, una minúscula, una mayúscula y un número.
3. Rechaza un correo que ya exista.
4. Guarda la contraseña codificada con BCrypt.
5. Crea la cuenta con rol `PENDIENTE`, estado `PENDIENTE` y `activo = false`.
6. El administrador puede aprobarla con un rol (`CLIENTE` por defecto en la API) o rechazarla. Al aprobar, se marca activa y se crea el registro `Cliente` asociado si aún no existe. Al rechazar, se marca `RECHAZADO` y permanece desactivada.
7. `CustomUserDetailsService` informa a Spring Security que las cuentas no activas o no aprobadas están deshabilitadas.

Los estados posibles son `ACTIVO`, `PENDIENTE` y `RECHAZADO`; los roles son `ADMIN`, `CLIENTE` y `PENDIENTE`.

### 6.2 Catálogo e inventario

Un administrador puede crear, editar, listar y eliminar productos desde `/producto/**`. El producto contiene precio y stock enteros (cantidad) o decimales (precio) representados actualmente como `int` y `double`, respectivamente.

La tienda muestra solo productos con stock mayor que cero. Las cantidades y el total que calcula la interfaz son preliminares: al confirmar, el servidor vuelve a consultar el producto, comprueba precio/stock y calcula el total definitivo.

### 6.3 Perfil

El cliente puede guardar nombre, correo y fecha de inscripción desde `/cliente/perfil`. El perfil se asocia con el usuario autenticado. Si no existe todavía, la página prepara un `Cliente` nuevo y al guardarlo lo vincula con el usuario actual.

Para comprar se exige que el usuario tenga perfil de cliente asociado; si falta, el flujo redirige a completar perfil.

### 6.4 Compra (flujo principal)

La lógica transaccional está centralizada en `VentaService.comprar`:

1. Comprueba que exista el cliente y que la lista de artículos no sea nula ni vacía.
2. Valida que cada artículo, producto y cantidad estén presentes y que la cantidad sea mayor que cero.
3. Consolida artículos duplicados por producto y detecta desbordamiento al sumar cantidades.
4. Carga cada producto mediante una consulta con bloqueo pesimista de escritura (`findByIdForUpdate`).
5. Verifica que exista el producto, que su stock alcance y que su precio sea finito y no negativo.
6. Calcula el importe total y genera los detalles conservando la cantidad y el precio unitario actual.
7. Guarda el encabezado con cliente, fecha del día y total.
8. Descuenta el stock y guarda los detalles vinculados al encabezado.
9. Al estar dentro de `@Transactional`, los cambios de persistencia se confirman juntos o se revierten si la transacción falla.

La compra web finaliza en una pantalla de confirmación que solo muestra la compra si pertenece al cliente autenticado. El detalle de compra persiste el precio unitario histórico, aunque la vista de confirmación también presenta información descriptiva consultada desde el producto.

**Límites de la implementación actual:** el total se maneja con `double`, no se modela moneda ni precisión decimal contable. Tampoco se especifican validaciones declarativas (`@Valid`) ni restricciones de dominio en las entidades para impedir, por ejemplo, stock negativo al editar un producto desde el CRUD.

## 7. Seguridad y perfiles de acceso

`SecurityConfig` configura autenticación basada en formulario, logout y autorización por ruta. Las contraseñas nuevas se codifican con BCrypt. Se mantiene CSRF habilitado para formularios. Las páginas públicas incluyen `/`, `/login`, `/registro`, recursos CSS/JS/imágenes y `/acceso-denegado`.

Resumen de permisos configurados:

| Permiso | Rutas destacadas |
|---|---|
| Público | `/`, `/login`, `/registro`, `/acceso-denegado`, `/css/**`, `/js/**`, `/images/**` |
| Cualquier usuario autenticado | `/home`, `/procesos`, `/procesos/**` y, por regla general, el resto de rutas no declaradas antes |
| Rol `CLIENTE` | `/cliente/perfil`, `/comprar/**`, `/api/ventas/**` |
| Rol `ADMIN` | `/admin/**`, `/producto/**`, `/productos/**`, `/cliente/**` salvo la regla más específica del perfil, `/clientes/**`, `/compras/**`, `/detalles/**` y `/api/admin/**` |

También hay comprobaciones de rol a nivel de método/controlador con `@PreAuthorize`. La autorización efectiva resulta de combinar la configuración por ruta y esas comprobaciones.

### JWT y rutas REST: estado actual

`AuthController` permite solicitar registro e iniciar sesión en JSON; para un login correcto llama a `JwtUtil` y devuelve token, usuario y rol. `JwtAuthFilter` puede extraer un token `Bearer` y crear una autenticación, pero **no se agrega explícitamente a `SecurityFilterChain` en `SecurityConfig`**. Además, `/api/auth/**` no figura en la lista pública y cae en `anyRequest().authenticated()`. Por eso, el flujo API de registro/login no está claramente habilitado como acceso anónimo por la configuración actual y requiere una revisión/integración explícita antes de considerarlo una API JWT funcional.

`VentaController` extrae un token JWT del encabezado HTTP de autorización y usa `JwtUtil`, mientras que la configuración de seguridad también protege `/api/ventas/**` con el rol `CLIENTE`. El comportamiento de extremo a extremo de estos endpoints depende de que el filtro JWT se ejecute dentro de la cadena adecuada y de cómo se resuelva CSRF para solicitudes REST; no debe asumirse que funciona igual que el flujo MVC con sesión.

La clave JWT tiene un valor por defecto en `JwtUtil`. Debe configurarse con un secreto externo seguro en cualquier entorno real; nunca conviene depender del valor de respaldo incluido en el código.

## 8. Rutas funcionales

### Web MVC

| Método | Ruta | Función |
|---|---|---|
| `GET` | `/` | Redirige al login |
| `GET`, `POST` | `/login` | Página y procesamiento de login de Spring Security |
| `GET`, `POST` | `/registro` | Formulario y registro web |
| `GET` | `/home` | Inicio del usuario autenticado |
| `GET` | `/admin/usuarios` | Solicitudes pendientes |
| `POST` | `/admin/usuarios/{id}/activar` | Aprueba usuario y asigna rol |
| `GET` | `/producto/listar` | Catálogo administrativo |
| `GET` | `/producto/nuevo`, `/producto/editar/{id}` | Formularios de producto |
| `POST` | `/producto/guardar`, `/producto/eliminar/{id}` | Alta, edición y eliminación de producto |
| `GET` | `/cliente/listar`, `/cliente/nuevo`, `/cliente/editar/{id}` | Administración de clientes |
| `POST` | `/cliente/guardar`, `/cliente/eliminar/{id}` | Alta, edición y eliminación de cliente |
| `GET`, `POST` | `/cliente/perfil` | Consulta/actualización del perfil propio |
| `GET` | `/comprar` | Catálogo disponible para compra |
| `POST` | `/comprar/confirmar` | Valida y registra el pedido |
| `GET` | `/comprar/confirmacion/{compraId}` | Resumen de una compra propia |
| `POST` | `/logout` | Cierre de sesión |

### REST

| Método | Ruta | Función |
|---|---|---|
| `POST` | `/api/auth/registro` | Registro JSON |
| `POST` | `/api/auth/login` | Autenticación JSON y respuesta JWT |
| `GET` | `/api/productos` | Lista productos |
| `GET` | `/api/admin/solicitudes` | Lista usuarios pendientes |
| `GET` | `/api/admin/usuarios` | Lista usuarios |
| `PUT` | `/api/admin/solicitudes/{id}/aprobar?rol=CLIENTE` | Aprueba usuario |
| `PUT` | `/api/admin/solicitudes/{id}/rechazar` | Rechaza usuario |
| `POST` | `/api/ventas/comprar` | Registra compra y devuelve total e identificador |

Los permisos de estas rutas REST también están sujetos a las reglas globales de Spring Security descritas anteriormente.

## 9. Ejecución y pruebas

### Preparar PostgreSQL

Instala PostgreSQL (incluyendo pgAdmin) e inicia el servidor local. En pgAdmin, abre el Query Tool conectado a la base `postgres` y crea el usuario:

```sql
CREATE ROLE compras_app WITH LOGIN PASSWORD 'elige-una-clave';
```

En pgAdmin, conéctate otra vez a `postgres` como un usuario administrador y ejecuta:

```sql
CREATE DATABASE comprasdb OWNER compras_app;
```

Copia `.env.example` como `.env` en la raíz del proyecto y pon en `DB_PASSWORD` la misma clave elegida para `compras_app`. También puedes cambiar `DB_URL` si PostgreSQL está en otro host o puerto. No publiques ni compartas `.env`.

### Iniciar la aplicación

Desde PowerShell, en la raíz del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

La misma configuración funciona con el botón **Spring Boot Run** de VS Code. Abre `http://localhost:8080`; si el servidor establece la variable `PORT`, la aplicación escucha en ese puerto en vez de `8080`. La base y el usuario deben existir antes de iniciar. Flyway crea las tablas y `DataSeeder` añade los datos de ejemplo al arrancar por primera vez. Los registros existentes del archivo H2 `data/comprasdb.mv.db` no se copian automáticamente.

Para ejecutar las pruebas:

```powershell
.\mvnw.cmd test
```

La prueba de integración configura una base H2 en memoria. Entre otras cosas, verifica acceso web, login, catálogo de productos disponibles, rechazo de una compra con stock insuficiente, creación de encabezado/detalle, total, reducción de inventario y acceso a confirmación.

## 10. Observaciones y mejoras recomendadas

1. **Persistencia monetaria:** sustituir `double` por `BigDecimal` en precio, total y precio histórico de venta; especificar precisión/escala en las columnas.
2. **API JWT:** permitir explícitamente el acceso anónimo donde corresponda, agregar el filtro JWT a la cadena de seguridad y definir una política CSRF apropiada para clientes API. Añadir pruebas HTTP que demuestren el flujo completo token → autorización → compra.
3. **Secretos y administrador inicial:** retirar credenciales codificadas del código y obtener secreto JWT/credenciales de configuración segura por entorno.
4. **Restricciones de integridad:** declarar restricciones explícitas para cantidades positivas, precio no negativo, stock no negativo, relaciones obligatorias y unicidad del usuario asociado a cliente.
5. **Búsqueda de perfil:** `ClientePerfilController` y `HomeController` recorren `findAll()` aunque existe `ClienteRepository.findByUsuario_Id`; usar ese método evita cargar todos los clientes.
6. **Errores de API:** unificar el mapeo de excepciones a códigos HTTP y respuestas JSON; actualmente hay fallas reportadas mediante `RuntimeException` o `IllegalArgumentException` y no todos los controladores definen el mismo tratamiento.
7. **Pruebas adicionales:** cubrir registro (correo/clave duplicada e inválida), aprobación/rechazo, aislamiento de compras entre clientes, API JWT, concurrencia de inventario y errores de persistencia.
8. **Migraciones:** el esquema ya se gestiona con migraciones Flyway versionadas; agrega una nueva migración para cada cambio futuro de estructura y conserva Hibernate en `validate`.

Estas recomendaciones describen áreas de mejora observadas al documentar el código; no cambian el alcance ni el comportamiento actual de la aplicación.

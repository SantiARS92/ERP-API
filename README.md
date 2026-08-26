# PYMES Backend

API REST para la gestión operativa de pymes y pequeños negocios. El proyecto está desarrollado con Spring Boot y PostgreSQL, e incluye módulos para autenticación, clientes, servicios, citas, ventas, gastos, inventario, profesionales, configuración del negocio e informes.

El backend está planteado como una API transaccional y orientada a negocio. Mantiene históricos de ventas y gastos, sincroniza stock con operaciones de inventario, permite trabajar con productos simples y con variantes, y genera informes y exportaciones PDF para distintas áreas de la aplicación.

## Descripción general

La aplicación cubre la operativa principal de un negocio que necesita gestionar clientes, agenda, catálogo de servicios, ventas, gastos e inventario desde una única API. Aunque parte de un contexto muy cercano a centros de estética o peluquería, la estructura actual también permite adaptarse a otros negocios con necesidades similares, especialmente gracias al soporte de inventario con variantes.

El sistema está preparado para trabajar con distintos perfiles de acceso y separa la lógica de negocio de la exposición web y de la persistencia, siguiendo una estructura por capas.

## Funcionalidades principales

- Autenticación JWT stateless con roles `ADMIN`, `PROFESSIONAL` y `GUEST`.
- Gestión de categorías, servicios, clientes y profesionales.
- Gestión de citas con generación automática de venta asociada cuando una cita pasa a estado confirmado.
- Ventas manuales de servicios, productos simples y variantes de producto.
- Gestión de gastos por categorías `SUPPLIES`, `SERVICES` y `OTHER`.
- Sincronización entre ventas de producto e inventario.
- Sincronización entre gastos de suministros e inventario.
- Inventario con productos simples y productos con variantes.
- Configuración global del negocio.
- Informes mensuales, trimestrales y anuales.
- Exportación PDF de ventas, gastos, inventario e informes.
- Tests de integración para validar flujos principales de negocio.

## Tecnologías utilizadas

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL
- H2 para entorno de test
- JJWT
- OpenPDF
- Lombok
- Maven Wrapper

## Arquitectura del proyecto

El proyecto sigue una organización por capas bajo el paquete raíz `es.pedro.pymes_backend`:

- `domain`: entidades JPA, enums y conceptos del dominio.
- `application`: servicios de aplicación, reglas de negocio, sincronizaciones e implementación de casos de uso.
- `infrastructure.repository`: repositorios JPA y consultas específicas.
- `infrastructure.security`: configuración de seguridad, JWT y handlers asociados.
- `infrastructure.web.controller`: controladores REST.
- `infrastructure.web.dto`: contratos de entrada y salida de la API.
- `infrastructure.web.exception`: excepciones y handler global.

Esta estructura permite mantener separada la lógica de negocio del acceso a datos y de la capa HTTP, facilitando la evolución del proyecto y el mantenimiento del código.

## Módulos implementados

### Auth / Security

Gestiona la autenticación y autorización del sistema. Permite login de administrador, login de profesional y emisión de token de invitado. La seguridad está basada en JWT y en control de acceso por roles.

### Categories

Gestiona las categorías utilizadas por servicios e inventario.

### Services

Gestiona el catálogo de servicios del negocio, incluyendo nombre, descripción, duración, precio, estado activo y categoría.

### Clients

Gestiona los clientes del sistema, incluyendo datos de contacto y soporte para información fiscal básica cuando el cliente admite facturación.

### Appointments

Gestiona las citas del negocio con cliente, servicio, profesional, fecha, hora, precio, estado y notas internas. Cuando una cita pasa a `CONFIRMED`, el backend puede generar automáticamente una venta asociada de tipo servicio, evitando duplicados para la misma cita.

### Sales

Gestiona ventas manuales y ventas generadas desde citas. Soporta venta de servicios, productos simples y variantes de producto. Las ventas manuales de producto actualizan stock; las ventas de cita continúan funcionando como ventas de servicio.

### Expenses

Gestiona gastos por categorías `SUPPLIES`, `SERVICES` y `OTHER`. Los gastos de suministros pueden vincularse a inventario para reponer stock de un producto simple o de una variante concreta.

### Inventory

Gestiona productos del inventario con soporte para dos modelos:
- producto simple
- producto con variantes

También incluye control de stock, stock mínimo, SKU, precio de compra, estado calculado y borrado lógico.

### Settings

Gestiona la configuración global del negocio, incluyendo datos generales, horarios y cierres especiales.

### Professionals

Gestiona los profesionales del negocio. Los profesionales activos con contraseña configurada pueden autenticarse con rol `PROFESSIONAL`.

### Reports

Genera informes financieros y operativos a nivel mensual, trimestral y anual, además de rankings y documentos PDF asociados.

## Productos simples y productos con variantes

El inventario soporta dos tipos de producto.

### Producto simple

Es el modelo clásico de inventario. Un producto simple tiene un único stock operativo, un stock mínimo, un SKU opcional y un precio de compra.

Este modelo sigue siendo válido para negocios con catálogo sencillo o sin necesidad de separar atributos por talla, color, medida o formato.

### Producto con variantes

Un producto con variantes mantiene un producto base y desplaza el stock real a sus variantes activas. Cada variante puede tener su propio nombre visible, SKU, stock, stock mínimo, precio de compra y atributos descriptivos.

Este enfoque permite trabajar con casos como:
- ropa por talla y color
- calzado por número y color
- ferretería por medida o formato
- otros catálogos donde un mismo producto se divide en combinaciones concretas

### Reglas principales de variantes

- Si un producto no tiene variantes, el stock vive en el propio producto.
- Si un producto tiene variantes, el stock real vive en las variantes activas.
- El listado principal de inventario devuelve stock agregado para productos con variantes.
- El estado de stock de un producto con variantes se calcula a partir del conjunto de variantes activas.
- Las ventas de un producto con variantes exigen seleccionar una variante concreta.
- Los gastos de suministros pueden reponer una variante concreta.
- Los nombres históricos de ventas y gastos conservan un formato legible, por ejemplo `Camiseta Básica - Roja / M`.
- Los informes de producto siguen trabajando a nivel de producto base.

## Seguridad y autenticación

La API utiliza JWT stateless con Spring Security.

El sistema trabaja con tres roles principales:

- `ADMIN`: acceso completo a los módulos administrativos, informes y ajustes.
- `PROFESSIONAL`: acceso operativo a módulos funcionales según las reglas definidas.
- `GUEST`: acceso limitado de solo lectura a recursos concretos.

Los endpoints de autenticación permiten:
- login de administrador o profesional
- generación de token de invitado
- consulta del usuario autenticado actual

## Configuración local

El proyecto utiliza PostgreSQL como base de datos principal y dispone de configuración separada para entorno de test.

El perfil por defecto está preparado para usar la configuración del entorno principal definido en propiedades. La aplicación utiliza variables de entorno o properties externas para los valores sensibles y de conexión.

Entre las configuraciones relevantes del proyecto se encuentran:
- puerto HTTP de la aplicación
- conexión a base de datos PostgreSQL
- orígenes CORS permitidos
- configuración del administrador
- secreto y expiración del JWT
- activación opcional de seed de datos demo

Para arrancar rápido en local puedes copiar `.env.local.example` a `.env.local` y ajustar los valores que necesites. Las variables más útiles son:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PORT`
- `CORS_ALLOWED_ORIGINS`
- `APP_AUTH_ADMIN_USERNAME`
- `APP_AUTH_ADMIN_PASSWORD`
- `APP_AUTH_JWT_SECRET`
- `APP_AUTH_JWT_EXPIRATION_MS`
- `APP_AUTH_JWT_REFRESH_EXPIRATION_MS`
- `APP_SEED_DEMO_DATA`

Para entorno de test se utiliza H2 en memoria con compatibilidad orientada a PostgreSQL.

### Scripts manuales de base de datos

La carpeta `src/main/resources/db/manual/` contiene scripts SQL para cambios de esquema que `spring.jpa.hibernate.ddl-auto=update` no gestiona automáticamente (secuencias nativas, índices, alteraciones puntuales de columnas, etc.). Estos scripts **no se ejecutan solos** al arrancar la aplicación: hay que aplicarlos manualmente contra cada base de datos (local, staging, producción) después de hacer `git pull` si el cambio los incluye.

Ejemplo de ejecución con `psql`:

```bash
psql -h <host> -p <puerto> -U <usuario> -d <base_de_datos> -f src/main/resources/db/manual/<archivo>.sql
```

Todos los scripts de esta carpeta están escritos para ser idempotentes (usan `if not exists` o equivalente), por lo que pueden ejecutarse varias veces sin causar errores.

## Ejecución del proyecto

El proyecto puede ejecutarse con Maven Wrapper desde la raíz del repositorio.

En local, la aplicación arranca sobre el puerto configurado en propiedades y expone la API bajo el prefijo `/api`.

## Endpoints principales

La API está organizada por módulos y expone rutas base como:

- `/api/auth`
- `/api/categories`
- `/api/services`
- `/api/clients`
- `/api/appointments`
- `/api/sales`
- `/api/expenses`
- `/api/inventory/products`
- `/api/professionals`
- `/api/settings`
- `/api/reports`

El proyecto está orientado a contratos REST basados en DTOs, evitando exponer directamente entidades JPA al exterior.

## Reglas de negocio importantes

- Las ventas generadas desde citas confirmadas son ventas de servicio.
- No se puede borrar directamente una venta generada desde una cita.
- Las ventas manuales de producto simple descuentan stock del producto base.
- Las ventas manuales de variante descuentan stock de la variante seleccionada.
- Al actualizar una venta de producto, se revierte primero el impacto anterior en stock y después se aplica el nuevo.
- Los gastos `SUPPLIES` vinculados a inventario aumentan stock.
- Los gastos `SUPPLIES` de variante aumentan stock de la variante seleccionada.
- Los gastos `SERVICES` y `OTHER` no modifican inventario.
- Al actualizar o borrar un gasto sincronizado con inventario, se revierte correctamente el impacto anterior.
- Los gastos automáticos generados al registrar stock inicial no vuelven a tocar stock.
- Los productos se borran lógicamente mediante `active=false`.
- Al desactivar un producto con variantes, sus variantes también quedan inactivas.
- El historial conserva nombres legibles aunque cambien después los datos actuales del producto, servicio o variante.

## Estado actual del proyecto

El backend se encuentra en un estado funcional para la operativa principal del negocio. Actualmente incluye:

- autenticación y autorización
- gestión de datos maestros
- agenda
- ventas
- gastos
- inventario con soporte para variantes
- integraciones de stock entre módulos
- informes financieros y operativos
- exportaciones PDF
- tests de integración sobre flujos principales

Además, el proyecto mantiene compatibilidad con los flujos de productos simples ya existentes y amplía el comportamiento del inventario para soportar variantes sin romper la operativa anterior.

## Tests

El proyecto incluye tests de integración con perfil de test y base de datos en memoria.

La suite cubre, entre otros, estos bloques:

- seguridad y roles
- smoke tests de API
- informes
- inventario simple y con variantes
- sincronización entre gastos e inventario
- sincronización entre ventas e inventario
- validaciones y regresiones de los flujos con variantes

## Nota final

Este README describe el estado actual del backend y resume su estructura, módulos y reglas principales de negocio. Su objetivo es servir como guía general del proyecto y facilitar la comprensión de la API y de su funcionamiento interno.

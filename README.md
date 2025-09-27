  # Distribuidora Limitada — App Android 

Demuestra el desarrollo de una aplicación móvil Android conectada a una base de datos en la nube (Firebase Realtime Database) y autenticación SSO (correo y Google Sign‑In)
## Resumen del aplicación  

La aplicación Distribuidora Limitada optimiza la gestión de pedidos y logística. Considera dos perfiles:

Cliente: navega productos, arma carrito y obtiene el costo de despacho automático según reglas de negocio y su ubicación GPS.

Transportista: registra temperaturas para asegurar la cadena de frío (alerta por umbral) y reporta trazabilidad.

Backend en la nube: Firebase Authentication (correo y Google) + Firebase Realtime Database. Arquitectura: MVVM con Kotlin, Coroutines, ViewBinding.

## Objetivos del Proyecto

Implementar una app Android con SSO y base de datos en la nube.

Diferenciar roles y flujos de navegación.

Calcular despacho según reglas de negocio y distancia (Haversine).

Gestionar el proyecto en GitHub con historias de usuario, issues, tableros y reportes de avance.

### Git Clon: https://github.com/LcioDiaz/DISTRIBUIDORA-LIMITADA.git

## Asignación de Tareas 

## HU-01: Registro y Autenticación de Usuarios

### Epic: Autenticación

Descripción: Implementar el flujo completo para que un nuevo usuario pueda crear una cuenta y acceder a la aplicación.

ID Tarea	Tarea	Responsable	Prioridad	Estado

1	Diseñar la interfaz de usuario (layout XML) para la pantalla de registro (activity_register.xml).	

2	Crear RegisterActivity.kt y conectar la UI con ViewBinding.

3	Implementar validación de campos en RegisterActivity (nombre, email, contraseña).	

4	Desarrollar register en AuthViewModel para crear usuarios en Firebase Auth.	

5	Integrar Google Sign‑In en AuthActivity y obtener idToken.	

6	Implementar loginWithGoogle en AuthViewModel.	

7	Guardar (nombre, email, rol) en /users/$uid (Realtime Database).	

## HU-02: Selección de Rol


### Epic: Autenticación


Descripción: Crear la pantalla inicial donde el usuario elige su perfil antes de la autenticación.

ID Tarea	Tarea	Responsable	Prioridad	Estado

1	Diseñar la UI para selección de rol (activity_login.xml).

2	Crear LoginActivity.kt y configurar listeners ("Soy Cliente" / "Soy Transportista").	

3	Implementar navegación a AuthActivity, pasando el rol en el Intent.

4	En AuthActivity, leer y persistir el rol para registro/redirección post‑login.

### HU-03 y HU-04: Catálogo de Productos y Carrito (Cliente)


### Epic: Compras Cliente

Descripción: Permitir al cliente ver productos y gestionar las cantidades de su pedido.

ID Tarea	Tarea	Responsable	Prioridad	Estado

1	Diseñar item_product.xml (nombre, precio, etiqueta "Cadena de Frío").

2	Crear ProductAdapter (RecyclerView) para gestionar lista e interacciones (+/‑).	

3	Implementar RecyclerView en HomeActivity y poblar con productos.	

1	Implementar updateTotals en HomeActivity (subtotal y total en tiempo real).	

2	Crear modelo Product.kt.	


### HU-05: Cálculo Automático de Despacho (Cliente)


### Epic: Compras Cliente

Descripción: Calcular el costo de envío según ubicación y reglas de negocio.

ID Tarea	Tarea	Responsable	Prioridad	Estado

1	Solicitar permisos de ubicación (ACCESS_FINE_LOCATION) en HomeActivity.

2	Integrar FusedLocationProviderClient para obtener ubicación actual.	

3	Crear Haversine.kt para distancia entre coordenadas.	

4	Implementar ReglasEnvio.kt (lógica de tarifas/radio).

5	Integrar costo de despacho en updateTotals de HomeActivity.	

6	Guardar última ubicación del cliente en Realtime Database.	

### HU-06 y HU-07: Control y Alerta de Temperatura (Transportista)


### Epic: Logística Transportista

Descripción: Registrar temperaturas y alertar sobre desviaciones.

ID Tarea	Tarea	Responsable	Prioridad	Estado

1	Diseñar activity_control_temperatura.xml.

2	Crear ControlTemperaturaActivity.kt y validar entrada numérica.

3	Guardar {valor, timestamp} en /registros_temperatura.	

4	Desarrollar checkAlarma contra umbral ‑18 °C.

5 Mostrar AlertDialog al superar el umbral.

6	Añadir navegación desde TransportistaHomeActivity a ControlTemperaturaActivity.

## Historias de Usuario

### HU-01: Registro y Autenticación de Usuarios

Como un nuevo usuario (cliente o transportista),

Quiero poder registrarme en la aplicación usando mi correo y contraseña, o a través de mi cuenta de Google (SSO),

Para acceder a las funcionalidades correspondientes a mi rol.


### HU-02: Selección de Rol

Como usuario que abre la aplicación por primera vez,

Quiero poder elegir si soy "Cliente" o "Transportista",

Para que la aplicación me dirija al flujo de registro o inicio de sesión correcto.


### HU-03: Visualización de Productos (Cliente)

Como Cliente,

Quiero ver una lista de productos con su nombre, precio y si requieren cadena de frío,

Para poder armar mi pedido.


### HU-04: Carrito de Compras (Cliente)

Como Cliente,

Quiero poder agregar o quitar unidades de cada producto y ver el subtotal actualizado en tiempo real,

Para controlar el monto de mi compra.


### HU-05: Cálculo Automático de Despacho (Cliente)

Como Cliente,

Quiero que la aplicación use mi ubicación para calcular automáticamente el costo de despacho basado en reglas de negocio (monto y distancia),

Para conocer el costo total de mi pedido antes de finalizarlo.


### HU-06: Control de Temperatura (Transportista)

Como Transportista,

Quiero una pantalla para registrar manualmente la temperatura de la carga,

Para asegurar que los productos se mantengan dentro de la cadena de frío.


### HU-07: Alerta de Temperatura (Transportista)

Como Transportista,

Quiero recibir una alerta visual si la temperatura registrada supera el umbral permitido (-18 °C),

Para tomar acciones correctivas de inmediato.

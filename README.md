# SuperApp: Ecosistema digital integral en una sola plataforma

> Proyecto del curso **Ingeniería de Software I** · Universidad Nacional de Colombia
> Estado actual: **Entrega 1 – Especificación de Requisitos y Diseño Arquitectónico**

## Descripción

SuperApp es una plataforma que reúne en **una sola cuenta** actividades que hoy exigen aplicaciones distintas: comprar y vender productos, solicitar viajes, enviar paquetes, hacer pedidos y llevar el control del dinero.

Hoy cada plataforma tiene su propia cuenta, historial, pagos y notificaciones. El usuario cambia constantemente de aplicación y no tiene una vista única de lo que compró, vendió, pagó o ganó. Esto afecta especialmente a quienes compran y venden, y a los conductores y mensajeros independientes, que reparten su trabajo entre varias aplicaciones, WhatsApp y cuadernos de notas.

**Idea central:** los servicios no solo conviven en la misma aplicación, sino que **comparten datos**. Una operación ejecutada en un módulo genera automáticamente información consistente en los demás. Por ejemplo, una compra crea el pedido, registra el movimiento de dinero, suma el gasto al resumen financiero, habilita la solicitud de envío y notifica al comprador y al vendedor.

## Alcance del MVP

Dentro del alcance de esta versión:

- **Cuenta y acceso:** una cuenta por persona, con roles (Cliente, Vendedor, Conductor/Mensajero, Administrador).
- **Marketplace:** publicación, edición y eliminación de productos; búsqueda y filtros; carrito y checkout.
- **Pedidos:** creación y seguimiento con estados y transiciones definidos.
- **Wallet:** billetera interna que registra cada movimiento económico (los pagos son simulados).
- **Finanzas:** panel con ingresos, gastos y saldo; registro manual de gastos operativos (por ejemplo, combustible).
- **Envíos y transporte:** solicitudes con asignación simulada de conductores y mensajeros, y seguimiento de estado.
- **Historial unificado:** un único historial cronológico de todas las operaciones del usuario.
- **Notificaciones** dentro de la aplicación y **panel de administración**.

Fuera del alcance de esta versión: pasarelas de pago reales, mapas y GPS reales, aplicaciones móviles nativas, facturación electrónica (DIAN), reseñas y chat entre usuarios, y soporte multimoneda (se usa solo COP).

## Validación con usuarios

El proyecto se ajustó con una entrevista a un conductor independiente que combina viajes de pasajeros y entregas de paquetes. Sus aportes dieron lugar a: una tarjeta de servicio simple y legible para usar en ruta, interruptores para activar o desactivar tipos de servicio, detalles obligatorios del paquete (tamaño, peso, fragilidad), registro manual de gastos y alertas sonoras para nuevas solicitudes. El detalle completo está en la sección 1.4 del documento de la entrega.

## Documentación

La especificación completa (requisitos funcionales y no funcionales, historias de usuario con criterios Gherkin, decisiones de arquitectura, diagramas UML, matriz de trazabilidad, riesgos y supuestos) está en el documento de la **Entrega 1**, disponible en la carpeta `docs/` de este repositorio.


## Equipo

| Integrante | Correo | Rol |
|---|---|---|
| Diego Andrés Rodríguez Cuarán | dirodriguezcu@unal.edu.co | QA / Documentación |
| Santiago Andrés Durán González | sadurango@unal.edu.co
| Jesús David Cáceres Fonseca | jcaceresf@unal.edu.co
| Juan Sebastián Basabe Hernández | jbasabe@unal.edu.co
| Marita Paola Urteaga Cueva | murteaga@unal.edu.co 

## Información académica

- **Curso:** Ingeniería de Software I
- **Docente:** Magda Lucia Mejia Torres
- **Repositorio:** https://github.com/D13G0-G0/SuperApp.git

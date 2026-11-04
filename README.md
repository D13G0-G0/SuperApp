# SuperApp MVC (Listings)

Estructura por capas, como en el tablero:

```
controller  ->  recibe HTTP, delega, responde
dto         ->  datos que entran/salen por la red     }  modelos
entity      ->  tabla de la base de datos             }
repository  ->  acceso a datos (JPA)
service     ->  lógica de negocio
```

Flujo de una petición: `Controller -> Service -> Repository -> BD` y de vuelta
(`Entity -> DTO` se convierte en el Service).

## Requisitos
- Java 21
- Maven 3.9+

## Ejecutar
```bash
cd superapp-mvc
mvn spring-boot:run
```
La app queda en http://localhost:8080 (BD H2 en memoria, no instalas nada).

## Probar
```bash
# Crear
curl -i -X POST localhost:8080/api/listings \
  -H "Content-Type: application/json" \
  -d '{"title":"Bicicleta","description":"Poco uso","price":150000}'

# Listar disponibles
curl localhost:8080/api/listings

# Ver una
curl localhost:8080/api/listings/1

# Borrar
curl -i -X DELETE localhost:8080/api/listings/1

# Validación (debe dar 400): precio negativo y título vacío
curl -i -X POST localhost:8080/api/listings \
  -H "Content-Type: application/json" -d '{"title":"","price":-5}'
```

Consola de la BD: http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:mem:superapp_db`, usuario `sa`, sin contraseña).

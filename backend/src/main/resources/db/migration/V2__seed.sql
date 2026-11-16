-- Datos de prueba: una cuenta por rol y 6 publicaciones del vendedor.
-- Todas las cuentas usan el dominio reservado superapp.test (no son correos reales).
-- La contraseña llega en la migración V3 (login con BCrypt).

INSERT INTO users (full_name, email, shipping_address_line, shipping_city, shipping_state, shipping_postal_code, shipping_country) VALUES
    ('Admin SuperApp',     'admin@superapp.test',     NULL,                 NULL,     NULL,     NULL,     NULL),
    ('Carla Cliente',      'cliente@superapp.test',   'Calle 45 # 30-12',   'Bogotá', 'Bogotá', '111321', 'Colombia'),
    ('Valentín Vendedor',  'vendedor@superapp.test',  'Carrera 7 # 72-41',  'Bogotá', 'Bogotá', '110231', 'Colombia'),
    ('Camilo Conductor',   'conductor@superapp.test', NULL,                 NULL,     NULL,     NULL,     NULL);

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE email = 'admin@superapp.test';
INSERT INTO user_roles (user_id, role)
SELECT id, 'CUSTOMER' FROM users WHERE email IN ('cliente@superapp.test', 'vendedor@superapp.test');
INSERT INTO user_roles (user_id, role)
SELECT id, 'SELLER' FROM users WHERE email = 'vendedor@superapp.test';
INSERT INTO user_roles (user_id, role)
SELECT id, 'DRIVER' FROM users WHERE email = 'conductor@superapp.test';

INSERT INTO listings (title, description, price, category, image_url, status, seller_id, created_at)
SELECT l.title, l.description, l.price, l.category, l.image_url, 'PUBLISHED', u.id, CURRENT_TIMESTAMP
FROM users u
CROSS JOIN (VALUES
    ('Bicicleta urbana', 'Bicicleta rin 26 en buen estado, ideal para la ciudad.', 450000.00, 'Deportes', 'https://picsum.photos/seed/superapp-bici/600/400'),
    ('Audífonos inalámbricos', 'Audífonos Bluetooth con estuche de carga, poco uso.', 120000.00, 'Tecnología', 'https://picsum.photos/seed/superapp-audifonos/600/400'),
    ('Libro de Cálculo (Stewart)', 'Cálculo de una variable, 8.ª edición, sin rayones.', 85000.00, 'Libros', 'https://picsum.photos/seed/superapp-libro/600/400'),
    ('Chaqueta impermeable', 'Talla M, color negro, perfecta para la lluvia.', 95000.00, 'Ropa', 'https://picsum.photos/seed/superapp-chaqueta/600/400'),
    ('Lámpara de escritorio LED', 'Tres niveles de luz y brazo flexible.', 60000.00, 'Hogar', 'https://picsum.photos/seed/superapp-lampara/600/400'),
    ('Teclado mecánico', 'Teclado mecánico en español con luz, switches azules.', 180000.00, 'Tecnología', 'https://picsum.photos/seed/superapp-teclado/600/400')
) AS l(title, description, price, category, image_url)
WHERE u.email = 'vendedor@superapp.test';

-- Borrado lógico: una publicación con pedidos no se elimina, queda en estado DELETED.
ALTER TABLE listings DROP CONSTRAINT ck_listings_status;
ALTER TABLE listings ADD CONSTRAINT ck_listings_status
    CHECK (status IN ('PUBLISHED', 'SOLD', 'SUSPENDED', 'DELETED'));

-- Las categorías de la semilla usan las mismas de la app (Comida, Mercado, Tecnología, Hogar, Moda, Otros).
UPDATE listings SET category = 'Moda' WHERE category = 'Ropa';
UPDATE listings SET category = 'Otros' WHERE category IN ('Deportes', 'Libros');

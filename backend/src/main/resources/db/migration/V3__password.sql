-- Contraseñas: solo se guarda el hash BCrypt (costo 10), nunca la contraseña.
ALTER TABLE users ADD COLUMN password_hash VARCHAR(100);

-- Cuentas de prueba de V2: la contraseña de todas es Demo1234.
UPDATE users
SET password_hash = '$2a$10$KMybQCxcrvI6wqYJ4.XcIORxyavfWRHyvyarx2CAXr0ufrIm76ufe'
WHERE email IN ('admin@superapp.test', 'cliente@superapp.test', 'vendedor@superapp.test', 'conductor@superapp.test');

ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;

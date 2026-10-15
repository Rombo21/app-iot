-- =======================================================
-- SCRIPT SQL PARA BASE DE DATOS MYSQL EN AWS (pnkcl_iot)
-- =======================================================

CREATE DATABASE IF NOT EXISTS pnkcl_iot CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pnkcl_iot;

CREATE TABLE IF NOT EXISTS Usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    usuario VARCHAR(150) NOT NULL UNIQUE,
    clave VARCHAR(255) NOT NULL,
    bloqueado TINYINT(1) DEFAULT 0,
    codigo_recuperacion VARCHAR(10) NULL,
    fecha_expiracion_codigo DATETIME NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Datos iniciales de prueba
INSERT INTO Usuarios (nombre, apellido, usuario, clave, bloqueado) VALUES
('Jorge Luis', 'Cortés Gallardo', 'jorge.cortes@correo.cl', 'Password123!', 0),
('Ana María', 'Pérez Soto', 'ana.perez@correo.cl', 'SecurePass1#', 0),
('Carlos Andrés', 'González Muñoz', 'carlos.gonzalez@correo.cl', 'Admin2025*', 0),
('Valentina Paz', 'Silva Castro', 'valentina.silva@correo.cl', 'ClaveTest8$', 0),
('Roberto Ignacio', 'López Herrera', 'roberto.lopez@correo.cl', 'PassIot2025@', 0)
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre);

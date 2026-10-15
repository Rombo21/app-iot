<?php
// Configuración centralizada de base de datos MySQL en AWS
$host = 'localhost';
$user = 'iot_user';
$pass = 'IotPassword2026!';
$db   = 'evaluacion_iot';

$conn = mysqli_connect($host, $user, $pass, $db);

if (!$conn) {
    http_response_code(500);
    echo json_encode([
        'estado'  => '0',
        'success' => false,
        'mensaje' => 'Error de conexión a la base de datos MySQL: ' . mysqli_connect_error(),
        'error'   => 'Error de conexión a la base de datos MySQL'
    ]);
    exit();
}

mysqli_set_charset($conn, 'utf8mb4');

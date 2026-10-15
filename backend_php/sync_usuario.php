<?php
header('Content-Type: application/json; charset=utf-8');

$host = 'localhost';
$user = 'root';
$pass = 'clave';
$db   = 'pnkcl_iot';

$conn = mysqli_connect($host, $user, $pass, $db);
if (!$conn) {
    echo json_encode(array('success' => false, 'error' => 'No se pudo conectar a MySQL'));
    exit();
}

mysqli_set_charset($conn, 'utf8');

$action    = isset($_POST['sync_action']) ? trim($_POST['sync_action']) : '';
$nombres   = isset($_POST['nombres']) ? mysqli_real_escape_string($conn, trim($_POST['nombres'])) : '';
$apellidos = isset($_POST['apellidos']) ? mysqli_real_escape_string($conn, trim($_POST['apellidos'])) : '';
$email     = isset($_POST['email']) ? mysqli_real_escape_string($conn, trim($_POST['email'])) : '';
$clave     = isset($_POST['clave']) ? mysqli_real_escape_string($conn, trim($_POST['clave'])) : '';

$success = false;

switch ($action) {
    case 'PENDING_INSERT':
        $sql = "INSERT INTO Usuarios (nombre, apellido, usuario, clave, bloqueado) 
                VALUES ('$nombres', '$apellidos', '$email', '$clave', 0)
                ON DUPLICATE KEY UPDATE nombre='$nombres', apellido='$apellidos', clave='$clave'";
        $success = mysqli_query($conn, $sql);
        break;

    case 'PENDING_UPDATE':
        $sql = "UPDATE Usuarios SET nombre='$nombres', apellido='$apellidos', clave='$clave' WHERE usuario='$email'";
        $success = mysqli_query($conn, $sql);
        break;

    case 'PENDING_DELETE':
        $sql = "DELETE FROM Usuarios WHERE usuario='$email'";
        $success = mysqli_query($conn, $sql);
        break;

    default:
        $success = false;
        break;
}

echo json_encode(array(
    'success' => $success,
    'action' => $action,
    'email' => $email,
    'error' => $success ? null : mysqli_error($conn)
));

mysqli_close($conn);
?>

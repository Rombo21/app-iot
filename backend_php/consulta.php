<?php
header('Content-Type: application/json; charset=utf-8');

$host = 'localhost';
$user = 'root';
$pass = 'clave';
$db   = 'pnkcl_iot';

$conn = mysqli_connect($host, $user, $pass, $db);

if (!$conn) {
    echo json_encode(array());
    exit();
}

mysqli_set_charset($conn, 'utf8');

$sql = "SELECT id, nombre, apellido, usuario FROM Usuarios WHERE bloqueado = 0";
$result = mysqli_query($conn, $sql);

$usuarios = array();
if ($result) {
    while ($row = mysqli_fetch_assoc($result)) {
        $usuarios[] = $row;
    }
}

echo json_encode($usuarios);
mysqli_close($conn);
?>

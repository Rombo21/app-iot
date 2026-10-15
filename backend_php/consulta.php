<?php
header('Content-Type: application/json; charset=utf-8');

require_once __DIR__ . '/db.php';


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

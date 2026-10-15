<?php
header('Content-Type: application/json; charset=utf-8');

require_once __DIR__ . '/db.php';


$usu  = isset($_GET['usu']) ? mysqli_real_escape_string($conn, trim($_GET['usu'])) : '';
$clave = isset($_GET['pass']) ? mysqli_real_escape_string($conn, trim($_GET['pass'])) : '';

if (empty($usu) || empty($clave)) {
    echo json_encode(array('estado' => '0', 'mensaje' => 'Parámetros incompletos'));
    exit();
}

$sql = "SELECT id, nombre, apellido, usuario, bloqueado FROM Usuarios WHERE (usuario='$usu' OR CONCAT(nombre, ' ', apellido)='$usu') AND clave='$clave'";
$result = mysqli_query($conn, $sql);

if ($result && mysqli_num_rows($result) > 0) {
    $row = mysqli_fetch_assoc($result);
    // Verificar si la cuenta está bloqueada temporalmente por recuperación (Requerimiento N° 1)
    if ((int)$row['bloqueado'] === 1) {
        echo json_encode(array(
            'estado' => 'bloqueado',
            'mensaje' => 'Cuenta bloqueada temporalmente por recuperación de contraseña'
        ));
    } else {
        echo json_encode(array(
            'estado' => '1',
            'id' => $row['id'],
            'nombre' => $row['nombre'],
            'apellido' => $row['apellido'],
            'usuario' => $row['usuario']
        ));
    }
} else {
    echo json_encode(array('estado' => '0', 'mensaje' => 'Usuario o clave incorrectos'));
}

mysqli_close($conn);
?>

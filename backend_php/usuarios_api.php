<?php
header('Content-Type: application/json; charset=utf-8');

// ============================================================
// API REST de Usuarios - App Movil IoT
// Acciones soportadas (parametro 'action' via GET o POST):
//   list_users   -> Lista todos los usuarios
//   register     -> Registra un usuario nuevo (POST)
//   update       -> Actualiza nombre, apellido o email (POST)
//   delete       -> Elimina un usuario (POST id)
//   check_email  -> Verifica si un email ya esta registrado
// ============================================================

require_once __DIR__ . '/db.php';


$action = isset($_REQUEST['action']) ? trim($_REQUEST['action']) : '';

switch ($action) {

    case 'list_users':
        $sql    = "SELECT id, nombre, apellido, usuario, bloqueado FROM Usuarios ORDER BY nombre ASC";
        $result = mysqli_query($conn, $sql);
        $list   = [];
        if ($result) {
            while ($row = mysqli_fetch_assoc($result)) {
                $list[] = $row;
            }
        }
        echo json_encode(['success' => true, 'usuarios' => $list]);
        break;

    case 'register':
        $nombres   = mysqli_real_escape_string($conn, trim($_POST['nombres']   ?? ''));
        $apellidos = mysqli_real_escape_string($conn, trim($_POST['apellidos'] ?? ''));
        $email     = mysqli_real_escape_string($conn, trim($_POST['email']     ?? ''));
        $clave     = mysqli_real_escape_string($conn, trim($_POST['clave']     ?? ''));

        if (!$nombres || !$apellidos || !$email || !$clave) {
            echo json_encode(['success' => false, 'error' => 'Faltan campos obligatorios']);
            break;
        }

        $check = mysqli_query($conn, "SELECT id FROM Usuarios WHERE usuario='$email'");
        if (mysqli_num_rows($check) > 0) {
            echo json_encode(['success' => false, 'error' => 'duplicate_email',
                              'mensaje' => 'Ya existe un usuario registrado con ese correo electronico.']);
            break;
        }

        $sql = "INSERT INTO Usuarios (nombre, apellido, usuario, clave, bloqueado)
                VALUES ('$nombres', '$apellidos', '$email', '$clave', 0)";
        if (mysqli_query($conn, $sql)) {
            echo json_encode(['success' => true, 'id' => mysqli_insert_id($conn),
                              'mensaje' => 'Usuario registrado correctamente']);
        } else {
            echo json_encode(['success' => false, 'error' => mysqli_error($conn)]);
        }
        break;

    case 'update':
        $id        = (int)($_POST['id']        ?? 0);
        $nombres   = mysqli_real_escape_string($conn, trim($_POST['nombres']   ?? ''));
        $apellidos = mysqli_real_escape_string($conn, trim($_POST['apellidos'] ?? ''));
        $email     = mysqli_real_escape_string($conn, trim($_POST['email']     ?? ''));

        if (!$id || !$nombres || !$apellidos || !$email) {
            echo json_encode(['success' => false, 'error' => 'Faltan campos obligatorios']);
            break;
        }

        $check = mysqli_query($conn, "SELECT id FROM Usuarios WHERE usuario='$email' AND id != $id");
        if (mysqli_num_rows($check) > 0) {
            echo json_encode(['success' => false, 'error' => 'duplicate_email',
                              'mensaje' => 'Ese correo ya esta asignado a otro usuario.']);
            break;
        }

        $sql = "UPDATE Usuarios SET nombre='$nombres', apellido='$apellidos', usuario='$email' WHERE id=$id";
        if (mysqli_query($conn, $sql)) {
            echo json_encode(['success' => true, 'mensaje' => 'Usuario actualizado correctamente',
                              'affected' => mysqli_affected_rows($conn)]);
        } else {
            echo json_encode(['success' => false, 'error' => mysqli_error($conn)]);
        }
        break;

    case 'delete':
        $id = (int)($_POST['id'] ?? 0);
        if (!$id) {
            echo json_encode(['success' => false, 'error' => 'ID no proporcionado']);
            break;
        }

        $sql = "DELETE FROM Usuarios WHERE id=$id";
        if (mysqli_query($conn, $sql)) {
            echo json_encode(['success' => true, 'mensaje' => 'Usuario eliminado correctamente',
                              'affected' => mysqli_affected_rows($conn)]);
        } else {
            echo json_encode(['success' => false, 'error' => mysqli_error($conn)]);
        }
        break;

    case 'check_email':
        $email  = mysqli_real_escape_string($conn, trim($_REQUEST['email'] ?? ''));
        $result = mysqli_query($conn, "SELECT id FROM Usuarios WHERE usuario='$email'");
        $exists = ($result && mysqli_num_rows($result) > 0);
        echo json_encode(['success' => true, 'exists' => $exists]);
        break;

    default:
        echo json_encode(['success' => false, 'error' => "Accion no reconocida. Disponibles: list_users, register, update, delete, check_email"]);
        break;
}

mysqli_close($conn);
?>

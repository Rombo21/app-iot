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
        $sql    = "SELECT id, nombre, apellido, usuario, clave, bloqueado FROM Usuarios ORDER BY nombre ASC";
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
        $email     = strtolower(trim($_POST['email'] ?? ''));
        $emailEsc  = mysqli_real_escape_string($conn, $email);
        $clave     = mysqli_real_escape_string($conn, trim($_POST['clave']     ?? ''));

        if (!$nombres || !$apellidos || !$email || !$clave) {
            echo json_encode(['success' => false, 'error' => 'Faltan campos obligatorios']);
            break;
        }

        // Verificar duplicado ignorando mayusculas/minusculas
        $check = mysqli_query($conn, "SELECT id FROM Usuarios WHERE LOWER(usuario)='$emailEsc'");
        if (mysqli_num_rows($check) > 0) {
            echo json_encode(['success' => false, 'error' => 'duplicate_email',
                              'mensaje' => 'Ya existe un usuario registrado con ese correo electronico.']);
            break;
        }

        $sql = "INSERT INTO Usuarios (nombre, apellido, usuario, clave, bloqueado)
                VALUES ('$nombres', '$apellidos', '$emailEsc', '$clave', 0)";
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
        $id    = (int)($_POST['id'] ?? 0);
        $email = strtolower(trim($_POST['email'] ?? ''));

        if (!$id && !$email) {
            echo json_encode(['success' => false, 'error' => 'ID o email requerido']);
            break;
        }

        // Intentar borrar por ID primero; si no afecta filas, usar email
        $affected = 0;
        if ($id) {
            mysqli_query($conn, "DELETE FROM Usuarios WHERE id=$id");
            $affected = mysqli_affected_rows($conn);
        }
        if ($affected == 0 && $email) {
            $emailEsc = mysqli_real_escape_string($conn, $email);
            mysqli_query($conn, "DELETE FROM Usuarios WHERE LOWER(usuario)='$emailEsc'");
            $affected = mysqli_affected_rows($conn);
        }

        if ($affected > 0) {
            echo json_encode(['success' => true, 'mensaje' => 'Usuario eliminado correctamente',
                              'affected' => $affected]);
        } else {
            // Si no existia en MySQL, igual se considera exito para no bloquear el flujo
            echo json_encode(['success' => true, 'mensaje' => 'Usuario no encontrado en MySQL (ya eliminado)',
                              'affected' => 0]);
        }
        break;

    case 'check_email':
        $email    = strtolower(trim($_REQUEST['email'] ?? ''));
        $emailEsc = mysqli_real_escape_string($conn, $email);
        $result   = mysqli_query($conn, "SELECT id FROM Usuarios WHERE LOWER(usuario)='$emailEsc'");
        $exists   = ($result && mysqli_num_rows($result) > 0);
        echo json_encode(['success' => true, 'exists' => $exists]);
        break;

    case 'reset_password':
        $email    = strtolower(trim($_POST['email'] ?? ''));
        $emailEsc = mysqli_real_escape_string($conn, $email);
        $clave    = mysqli_real_escape_string($conn, trim($_POST['clave'] ?? ''));

        if (!$email || !$clave) {
            echo json_encode(['success' => false, 'error' => 'Email y clave obligatorios']);
            break;
        }

        $sql = "UPDATE Usuarios 
                SET clave='$clave', bloqueado=0, codigo_recuperacion=NULL, fecha_expiracion_codigo=NULL 
                WHERE LOWER(usuario)='$emailEsc'";
        if (mysqli_query($conn, $sql)) {
            echo json_encode(['success' => true, 'mensaje' => 'Contraseña actualizada y cuenta reactivada']);
        } else {
            echo json_encode(['success' => false, 'error' => mysqli_error($conn)]);
        }
        break;

    default:
        echo json_encode(['success' => false, 'error' => "Accion no reconocida. Disponibles: list_users, register, update, delete, check_email, reset_password"]);
        break;
}

mysqli_close($conn);
?>

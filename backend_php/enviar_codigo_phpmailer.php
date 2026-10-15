<?php
header('Content-Type: application/json; charset=utf-8');

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

require 'PHPMailer/src/Exception.php';
require 'PHPMailer/src/PHPMailer.php';
require 'PHPMailer/src/SMTP.php';

$host = 'localhost';
$user = 'root';
$pass = 'clave';
$db   = 'pnkcl_iot';

$conn = mysqli_connect($host, $user, $pass, $db);
if (!$conn) {
    echo json_encode(array('success' => false, 'error' => 'Error de conexión a la base de datos'));
    exit();
}

$email = isset($_POST['email']) ? mysqli_real_escape_string($conn, trim($_POST['email'])) : '';

if (empty($email)) {
    echo json_encode(array('success' => false, 'error' => 'Email no proporcionado'));
    exit();
}

// 1. Generar código aleatorio de 6 dígitos
$codigo = (string)random_int(100000, 999999);

// 2. Bloquear cuenta y guardar código con vigencia de 1 minuto
$expira = date('Y-m-d H:i:s', time() + 60);
$updateSql = "UPDATE Usuarios SET bloqueado = 1, codigo_recuperacion = '$codigo', fecha_expiracion_codigo = '$expira' WHERE usuario = '$email'";
mysqli_query($conn, $updateSql);

// 3. Enviar correo mediante PHPMailer
$mail = new PHPMailer(true);

try {
    $mail->isSMTP();
    $mail->Host       = 'smtp.gmail.com';
    $mail->SMTPAuth   = true;
    $mail->Username   = 'TU_CORREO@gmail.com';          // Reemplazar con su correo Gmail
    $mail->Password   = 'TU_CONTRASENA_DE_APLICACION'; // Contraseña de aplicación de 16 caracteres de Google
    $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
    $mail->Port       = 587;

    $mail->setFrom('TU_CORREO@gmail.com', 'App Movil IoT');
    $mail->addAddress($email);

    $mail->isHTML(true);
    $mail->CharSet = 'UTF-8';
    $mail->Subject = 'Código de verificación - Recuperación de contraseña';

    $mail->Body = '
        <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 30px auto; padding: 25px; border: 1px solid #e0e0e0; border-radius: 10px; text-align: center; background-color: #ffffff;">
            <h2 style="color: #003366;">Código de Verificación</h2>
            <p style="color: #555555; font-size: 15px;">Hemos recibido una solicitud para restablecer tu contraseña en la Aplicación Móvil IoT.</p>
            <p style="color: #333333; font-size: 16px; font-weight: bold;">Tu código temporal de verificación es:</p>
            <div style="margin: 20px 0; padding: 18px; background-color: #f1f5f9; border-radius: 8px; font-size: 34px; font-weight: bold; letter-spacing: 8px; color: #d32f2f;">
                ' . $codigo . '
            </div>
            <p style="color: #777777; font-size: 13px;">⚠️ Este código tiene una vigencia estricta de <strong>1 minuto</strong> y es de un solo uso.</p>
            <p style="color: #999999; font-size: 12px; margin-top: 20px;">Tu cuenta ha sido bloqueada temporalmente hasta que completes la verificación o restablezcas tu clave.</p>
        </div>
    ';

    $mail->send();
    echo json_encode(array('success' => true, 'mensaje' => 'Código enviado exitosamente', 'codigo_debug' => $codigo));
} catch (Exception $e) {
    echo json_encode(array('success' => false, 'error' => $mail->ErrorInfo, 'codigo_debug' => $codigo));
}

mysqli_close($conn);
?>

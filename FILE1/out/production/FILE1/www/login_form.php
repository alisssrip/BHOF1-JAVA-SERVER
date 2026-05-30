<?php
session_start();

$login_result = '';

if (!function_exists('curl_init')) {
    $login_result = 'ERROR FATAL: La extensión php-curl no está instalada en Naoto.';
} else {
    $clientIp = $_SERVER['REMOTE_ADDR'] ?? '';

    if (empty($clientIp)) {
        $login_result = 'No se pudo determinar tu IP pública.';
    } else {
        $url = "https://yoko.makii.net/api/OutbreakJava/sessions/by-ip";

        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPGET, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array(
            'X-Forwarded-For: ' . $clientIp
        ));
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 5);

        $response = curl_exec($ch);

        if (curl_errno($ch)) {
            $login_result = 'ERROR DE cURL: ' . curl_error($ch);
            curl_close($ch);
        } else {
            $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
            curl_close($ch);

            if ($httpCode == 200) {
                // Camino feliz: una sola session, redirigimos al juego
                $resData = json_decode($response, true);
                $sessid  = $resData["sessionId"] ?? '';

                if (!empty($sessid)) {
                    header("Location: startsession.php?sessid=" . urlencode($sessid));
                    exit();
                } else {
                    $login_result = 'La respuesta de Yoko no contiene sessionId.';
                }

            } else if ($httpCode == 404) {
                $login_result = 'No hay sesión activa para tu IP ('.htmlspecialchars($clientIp).').<br>'
                . 'Iniciá sesión en el launcher primero.';

            } else if ($httpCode == 409) {
                $resData = json_decode($response, true);
                $userids = $resData["userids"] ?? [];
                $list = !empty($userids)
                ? '<br>Cuentas detectadas: ' . htmlspecialchars(implode(', ', $userids))
                : '';
                $login_result = 'Se detectaron múltiples sesiones activas desde tu IP.<br>'
                . 'El soporte para varios usuarios en la misma red todavía no está implementado.' . $list;

            } else {
                $login_result = 'Error de Yoko (HTTP '.$httpCode.'). Intentá de nuevo.';
            }
        }
    }
}

include('header.php');
echo "<br><br><center><h2 style='color:red; background-color:white; padding:10px;'>$login_result</h2></center><br><br>";
include('footer.php');

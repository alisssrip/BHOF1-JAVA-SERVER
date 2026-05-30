<?php
/*
    OBSRV - Fan Made Biohazard Outbreak(tm) Server
    Copyright (C) 2013-2024  obsrv.org
              (C) 2026  alissrip — https://makii.net

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

session_start();

$login_result = '';

if (!function_exists('curl_init')) {
    $login_result = 'FATAL ERROR: the php-curl extension is not installed.';
} else {
    $clientIp = $_SERVER['REMOTE_ADDR'] ?? '';

    if (empty($clientIp)) {
        $login_result = 'Could not determine your public IP.';
    } else {
        $url = "https://yoko.makii.net/api/OutbreakJava/sessions/by-ip";

        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPGET, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array(
            'X-Forwarded-For: ' . $clientIp
        ));
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 2);
        curl_setopt($ch, CURLOPT_TIMEOUT, 5);

        $response = curl_exec($ch);

        if (curl_errno($ch)) {
            $login_result = 'cURL ERROR: ' . curl_error($ch);
            curl_close($ch);
        } else {
            $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
            curl_close($ch);

            if ($httpCode == 200) {
                $resData = json_decode($response, true);
                $sessid  = $resData["sessionId"] ?? '';

                if (!empty($sessid)) {
                    header("Location: startsession.php?sessid=" . urlencode($sessid));
                    exit();
                } else {
                    $login_result = 'The backend response did not contain a sessionId.';
                }

            } else if ($httpCode == 404) {
                $login_result = 'No active session for your IP ('.htmlspecialchars($clientIp).').<br>'
                . 'Log in from the launcher first.';

            } else if ($httpCode == 409) {
                $resData = json_decode($response, true);
                $userids = $resData["userids"] ?? [];
                $list = !empty($userids)
                ? '<br>Detected accounts: ' . htmlspecialchars(implode(', ', $userids))
                : '';
                $login_result = 'Multiple active sessions detected from your IP.<br>'
                . 'Multi-user support on the same network is not implemented yet.' . $list;

            } else {
                $login_result = 'Backend error (HTTP '.$httpCode.'). Try again.';
            }
        }
    }
}

include('header.php');
echo "<br><br><center><h2 style='color:red; background-color:white; padding:10px;'>$login_result</h2></center><br><br>";
include('footer.php');

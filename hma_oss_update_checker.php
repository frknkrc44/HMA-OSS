<?php
    $cacheFileName = 'hma_oss_update_request_cache.json';
    $changelogFileName = 'hma_oss_changelog.md';

    if (file_exists($cacheFileName) && (time() - filemtime($cacheFileName)) <= 60000) {
        $cachedFile = file_get_contents($cacheFileName);

        if (strlen($cachedFile) > 0) {
            http_response_code(200);
            header('Content-Type: application/json');

            echo $cachedFile;
            return;
        }
    }

    try {
        $ch = curl_init('https://api.github.com/repos/frknkrc44/HMA-OSS/releases/latest');
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HEADER, 0);
        curl_setopt($ch, CURLOPT_USERAGENT, 'curl/8.21.0');
        $data = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if (!$data || $httpCode >= 400) {
            throw new \Exception('Asset not found');
        }

        $json = json_decode($data, true);

        $name = $json['name'];
        $download = '';
        foreach ($json['assets'] as $assets) {
            if (str_ends_with($assets['name'], '-release.zip')) {
                $download = $assets['browser_download_url'];
            }
        }

        if (strlen($download) < 1) {
            throw new \Exception('Asset not found');
        }

        $changelog = $json['body'];
        file_put_contents($changelogFileName, $changelog);

        // version code calculation
        $versionCodeBase = (int) substr($name, 4);
        $gitCommitCountBeforeOss = 432;
        $foss = 0x6f7373;
        $versionCode = $versionCodeBase + $gitCommitCountBeforeOss + $foss;

        $final_json = json_encode([
            'version' => $name,
            'versionCode' => $versionCode,
            'zipUrl' => $download,
            'changelog' => 'https://' . $_SERVER['SERVER_NAME'] . '/' . $changelogFileName,
        ]);

        file_put_contents($cacheFileName, $final_json);

        http_response_code(200);
        header('Content-Type: application/json');

        echo $final_json;
    } catch (\Throwable $th) {
        http_response_code(500); 
        header('Content-Type: application/json');

        echo json_encode([
            'error' => $th->getMessage(),
        ]);
    }
?>

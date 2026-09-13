package es.laucher.cobblemon;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Handles the basic files needed by the launcher without embedding binary assets. */
final class LauncherService {
    static final String MINECRAFT_VERSION = "1.21.1";
    static final String FABRIC_INSTALLER_URL =
            "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.3/fabric-installer-1.0.3.jar";
    static final String FABRIC_API_URL =
            "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.116.15+1.21.1/fabric-api-0.116.15+1.21.1.jar";

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    void prepare(Path gameDir, Progress progress) throws IOException, InterruptedException {
        Objects.requireNonNull(gameDir);
        Files.createDirectories(gameDir);
        Files.createDirectories(gameDir.resolve("mods"));
        Files.createDirectories(gameDir.resolve("launcher"));

        progress.update("Comprobando Java 21…");
        if (!javaVersionIs21OrNewer()) {
            throw new IOException("LaucherCobblemon necesita Java 21 o superior.");
        }

        progress.update("Descargando instalador de Fabric…");
        Path installer = gameDir.resolve("launcher/fabric-installer.jar");
        downloadIfMissing(FABRIC_INSTALLER_URL, installer);

        progress.update("Descargando Fabric API…");
        downloadIfMissing(FABRIC_API_URL,
                gameDir.resolve("mods/fabric-api-0.116.15+1.21.1.jar"));

        progress.update("Preparación base completada.");
    }

    private boolean javaVersionIs21OrNewer() {
        return Runtime.version().feature() >= 21;
    }

    private void downloadIfMissing(String url, Path target) throws IOException, InterruptedException {
        if (Files.exists(target) && Files.size(target) > 0) return;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("No se pudo descargar " + url + " (HTTP " + response.statusCode() + ")");
        }
        Path temp = target.resolveSibling(target.getFileName() + ".part");
        try (InputStream input = response.body()) {
            Files.copy(input, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @FunctionalInterface
    interface Progress {
        void update(String message);
    }
}

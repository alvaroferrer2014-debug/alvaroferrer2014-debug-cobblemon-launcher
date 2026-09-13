package es.laucher.cobblemon;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Downloads the base files for a Fabric 1.21.1 Cobblemon profile. */
final class LauncherService {
    static final String MINECRAFT_VERSION = "1.21.1";
    static final String FABRIC_LOADER_VERSION = "0.19.4";
    static final String COBBLEMON_VERSION = "1.7.3";
    static final String FABRIC_API_VERSION = "0.116.15+1.21.1";

    private static final String FABRIC_INSTALLER_URL =
            "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.3/fabric-installer-1.0.3.jar";
    private static final String FABRIC_API_URL =
            "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.116.15+1.21.1/fabric-api-0.116.15+1.21.1.jar";
    private static final String COBBLEMON_URL =
            "https://cdn.modrinth.com/data/MdwFAVRL/versions/kF7CvxTo/cobblemon-fabric-1.7.3+1.21.1.jar";

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    void prepare(Path gameDir, Progress progress) throws IOException, InterruptedException {
        Objects.requireNonNull(gameDir);
        Files.createDirectories(gameDir);
        Files.createDirectories(gameDir.resolve("mods"));
        Files.createDirectories(gameDir.resolve("launcher"));

        progress.update("Comprobando Java 21…");
        if (Runtime.version().feature() < 21) {
            throw new IOException("LaucherCobblemon necesita Java 21 o superior.");
        }

        progress.update("Descargando instalador de Fabric…");
        downloadIfMissing(FABRIC_INSTALLER_URL, gameDir.resolve("launcher/fabric-installer.jar"));

        progress.update("Descargando Fabric API " + FABRIC_API_VERSION + "…");
        downloadIfMissing(FABRIC_API_URL,
                gameDir.resolve("mods/fabric-api-" + FABRIC_API_VERSION + ".jar"));

        progress.update("Descargando Cobblemon " + COBBLEMON_VERSION + "…");
        downloadIfMissing(COBBLEMON_URL,
                gameDir.resolve("mods/cobblemon-fabric-" + COBBLEMON_VERSION + "+1.21.1.jar"));

        progress.update("Base de Cobblemon preparada.");
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
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @FunctionalInterface
    interface Progress {
        void update(String message);
    }
}

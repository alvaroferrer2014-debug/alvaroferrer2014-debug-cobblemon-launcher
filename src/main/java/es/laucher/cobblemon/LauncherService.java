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

/** Downloads and installs the base files for a Fabric 1.21.1 Cobblemon profile. */
final class LauncherService {
    static final String MINECRAFT_VERSION = "1.21.1";
    static final String FABRIC_LOADER_VERSION = "0.19.4";
    static final String COBBLEMON_VERSION = "1.7.3";
    static final String FABRIC_API_VERSION = "0.116.15+1.21.1";
    private static final String FABRIC_INSTALLER_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.3/fabric-installer-1.0.3.jar";
    private static final String FABRIC_API_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.116.15+1.21.1/fabric-api-0.116.15+1.21.1.jar";
    private static final String COBBLEMON_URL = "https://cdn.modrinth.com/data/MdwFAVRL/versions/kF7CvxTo/cobblemon-fabric-1.7.3+1.21.1.jar";
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    void prepare(Path gameDir, Progress progress) throws IOException, InterruptedException {
        Objects.requireNonNull(gameDir);
        Files.createDirectories(gameDir);
        Files.createDirectories(gameDir.resolve("mods"));
        Files.createDirectories(gameDir.resolve("launcher"));
        requireJava21();
        Path installer = gameDir.resolve("launcher/fabric-installer.jar");
        progress.update("Descargando instalador de Fabric…");
        downloadIfMissing(FABRIC_INSTALLER_URL, installer);
        progress.update("Instalando Fabric Loader " + FABRIC_LOADER_VERSION + "…");
        installFabric(installer, gameDir, "client");
        progress.update("Descargando Fabric API…");
        downloadIfMissing(FABRIC_API_URL, gameDir.resolve("mods/fabric-api-" + FABRIC_API_VERSION + ".jar"));
        progress.update("Descargando Cobblemon " + COBBLEMON_VERSION + "…");
        downloadIfMissing(COBBLEMON_URL, gameDir.resolve("mods/cobblemon-fabric-" + COBBLEMON_VERSION + "+1.21.1.jar"));
        progress.update("Base de Cobblemon preparada.");
    }

    void prepareServer(Path serverDir, Progress progress) throws IOException, InterruptedException {
        Objects.requireNonNull(serverDir);
        requireJava21();
        Files.createDirectories(serverDir.resolve("mods"));
        Files.createDirectories(serverDir.resolve("launcher"));
        Path installer = serverDir.resolve("launcher/fabric-installer.jar");
        progress.update("Descargando instalador de Fabric para servidor…");
        downloadIfMissing(FABRIC_INSTALLER_URL, installer);
        progress.update("Instalando Fabric Server " + FABRIC_LOADER_VERSION + "…");
        installFabric(installer, serverDir, "server");
        progress.update("Descargando Fabric API para servidor…");
        downloadIfMissing(FABRIC_API_URL, serverDir.resolve("mods/fabric-api-" + FABRIC_API_VERSION + ".jar"));
        progress.update("Descargando Cobblemon para servidor…");
        downloadIfMissing(COBBLEMON_URL, serverDir.resolve("mods/cobblemon-fabric-" + COBBLEMON_VERSION + "+1.21.1.jar"));
    }

    void launch(Path gameDir, int ramGb, Progress progress) throws IOException, InterruptedException {
        if (ramGb < 2 || ramGb > 16) throw new IOException("La RAM debe estar entre 2 y 16 GB.");
        Path installer = gameDir.resolve("launcher/fabric-installer.jar");
        if (!Files.exists(installer)) throw new IOException("Primero pulsa «Preparar Fabric + Cobblemon».");
        installFabric(installer, gameDir, "client");
        Path launcher = findMinecraftLauncher();
        if (launcher == null) throw new IOException("No se encontró el launcher oficial de Minecraft.");
        progress.update("Abriendo Minecraft Launcher…");
        new ProcessBuilder(launcher.toString()).start();
    }

    private void installFabric(Path installer, Path dir, String mode) throws IOException, InterruptedException {
        Path marker = mode.equals("client")
                ? dir.resolve("versions/fabric-loader-" + FABRIC_LOADER_VERSION + "-" + MINECRAFT_VERSION + "/fabric-loader-" + FABRIC_LOADER_VERSION + "-" + MINECRAFT_VERSION + ".json")
                : dir.resolve("fabric-server-launch.jar");
        if (Files.exists(marker)) return;
        Process process = new ProcessBuilder(javaExecutable(), "-jar", installer.toString(), mode, "-dir", dir.toString(), "-mcversion", MINECRAFT_VERSION, "-loader", FABRIC_LOADER_VERSION).redirectErrorStream(true).start();
        int exit = process.waitFor();
        if (exit != 0) throw new IOException("El instalador de Fabric terminó con código " + exit + ".");
    }

    private void requireJava21() throws IOException {
        if (Runtime.version().feature() < 21) throw new IOException("LaucherCobblemon necesita Java 21 o superior.");
    }

    private Path findMinecraftLauncher() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            Path local = Path.of(System.getenv().getOrDefault("LOCALAPPDATA", ""), "Programs", "Minecraft Launcher", "MinecraftLauncher.exe");
            if (Files.isRegularFile(local)) return local;
            Path store = Path.of(System.getenv().getOrDefault("ProgramFiles", "C:\\Program Files"), "Minecraft Launcher", "MinecraftLauncher.exe");
            if (Files.isRegularFile(store)) return store;
        } else if (os.contains("mac")) {
            Path mac = Path.of("/Applications/Minecraft.app/Contents/MacOS/launcher");
            if (Files.isRegularFile(mac)) return mac;
        } else {
            Path linux = Path.of("/usr/bin/minecraft-launcher");
            if (Files.isRegularFile(linux)) return linux;
        }
        return null;
    }

    private String javaExecutable() {
        String javaHome = System.getProperty("java.home");
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        return Path.of(javaHome, "bin", windows ? "java.exe" : "java").toString();
    }

    private void downloadIfMissing(String url, Path target) throws IOException, InterruptedException {
        if (Files.exists(target) && Files.size(target) > 0) return;
        Files.createDirectories(target.getParent());
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) throw new IOException("No se pudo descargar " + url + " (HTTP " + response.statusCode() + ")");
        Path temp = target.resolveSibling(target.getFileName() + ".part");
        try (InputStream input = response.body()) { Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING); }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @FunctionalInterface
    interface Progress { void update(String message); }
}

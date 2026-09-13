package es.laucher.cobblemon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

/** Prepares and controls a local Fabric + Cobblemon dedicated server. */
final class ServerService {
    private final LauncherService launcherService = new LauncherService();
    private Process process;
    private Writer serverInput;

    synchronized void prepare(Path serverDir, LauncherService.Progress progress) throws IOException, InterruptedException {
        Objects.requireNonNull(serverDir);
        Files.createDirectories(serverDir);
        Files.createDirectories(serverDir.resolve("mods"));
        Files.createDirectories(serverDir.resolve("launcher"));
        launcherService.prepareServer(serverDir, progress);
        Path eula = serverDir.resolve("eula.txt");
        if (!Files.exists(eula)) {
            Files.writeString(eula, "# Change eula=false to eula=true after reading Minecraft's EULA.\neula=false\n", StandardCharsets.UTF_8);
        }
        progress.update("Servidor preparado. Revisa eula.txt antes de iniciarlo.");
    }

    synchronized void start(Path serverDir, int ramGb, Consumer<String> output) throws IOException {
        Objects.requireNonNull(serverDir);
        if (process != null && process.isAlive()) throw new IOException("El servidor ya está ejecutándose.");
        Path eula = serverDir.resolve("eula.txt");
        if (!Files.exists(eula) || !Files.readString(eula, StandardCharsets.UTF_8).contains("eula=true")) {
            throw new IOException("Acepta la EULA en eula.txt antes de iniciar el servidor.");
        }
        Path serverJar = serverDir.resolve("fabric-server-launch.jar");
        if (!Files.exists(serverJar)) throw new IOException("Primero pulsa «Preparar servidor».");
        if (ramGb < 2 || ramGb > 16) throw new IOException("La RAM debe estar entre 2 y 16 GB.");

        process = new ProcessBuilder(javaExecutable(), "-Xms" + ramGb + "G", "-Xmx" + ramGb + "G", "-jar", serverJar.toString(), "nogui")
                .directory(serverDir.toFile()).redirectErrorStream(true).start();
        serverInput = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
        Thread reader = new Thread(() -> readOutput(output), "LaucherCobblemon-server-console");
        reader.setDaemon(true);
        reader.start();
    }

    synchronized void stop() throws IOException {
        if (process == null || !process.isAlive()) return;
        if (serverInput != null) {
            serverInput.write("stop\n");
            serverInput.flush();
        }
    }

    synchronized boolean isRunning() {
        return process != null && process.isAlive();
    }

    private void readOutput(Consumer<String> output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) output.accept(line);
        } catch (IOException ignored) {
            // The process may close its output stream during shutdown.
        }
    }

    private String javaExecutable() {
        String javaHome = System.getProperty("java.home");
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        return Path.of(javaHome, "bin", windows ? "java.exe" : "java").toString();
    }
}

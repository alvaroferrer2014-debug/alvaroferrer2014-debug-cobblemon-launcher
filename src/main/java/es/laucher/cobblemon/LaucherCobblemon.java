package es.laucher.cobblemon;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/** Punto de entrada de LaucherCobblemon. */
public final class LaucherCobblemon {
    private static final String MINECRAFT_VERSION = LauncherService.MINECRAFT_VERSION;
    private static final LauncherService SERVICE = new LauncherService();
    private static final ServerService SERVER = new ServerService();

    private LaucherCobblemon() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LaucherCobblemon::createWindow);
    }

    private static void createWindow() {
        JFrame frame = new JFrame("LaucherCobblemon");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(760, 600);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel title = new JLabel("LaucherCobblemon");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        JLabel version = new JLabel("Cobblemon " + LauncherService.COBBLEMON_VERSION + " • Fabric " + MINECRAFT_VERSION);
        version.setFont(version.getFont().deriveFont(16f));
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title); header.add(Box.createVerticalStrut(5)); header.add(version);

        JLabel folderLabel = new JLabel("Carpeta del juego: no seleccionada");
        final Path[] selectedFolder = new Path[1];
        JButton folderButton = new JButton("Elegir carpeta");
        folderButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                selectedFolder[0] = chooser.getSelectedFile().toPath();
                folderLabel.setText("Carpeta: " + selectedFolder[0]);
            }
        });

        JSpinner ram = new JSpinner(new SpinnerNumberModel(4, 2, 16, 1));
        JPanel settings = new JPanel(new GridLayout(2, 2, 10, 10));
        settings.setBorder(BorderFactory.createTitledBorder("Configuración"));
        settings.add(new JLabel("RAM (GB):")); settings.add(ram);
        settings.add(folderLabel); settings.add(folderButton);

        JLabel status = new JLabel("Listo.");
        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true); progressBar.setString("Listo");

        JTextArea console = new JTextArea();
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane consoleScroll = new JScrollPane(console);
        consoleScroll.setBorder(BorderFactory.createTitledBorder("Consola del servidor"));

        JButton install = new JButton("Preparar Fabric + Cobblemon");
        install.addActionListener(e -> runTask(frame, install, status, progressBar, "Preparando…", () -> {
            requireFolder(selectedFolder[0]);
            SERVICE.prepare(selectedFolder[0], message -> SwingUtilities.invokeLater(() -> status.setText(message)));
        }, "Preparado", "Error de preparación"));

        JButton play = new JButton("JUGAR");
        play.setFont(play.getFont().deriveFont(Font.BOLD, 18f));
        play.addActionListener(e -> runTask(frame, play, status, progressBar, "Iniciando Minecraft…", () -> {
            requireFolder(selectedFolder[0]);
            SERVICE.launch(selectedFolder[0], (Integer) ram.getValue(), status::setText);
        }, "Minecraft iniciado", "Error al iniciar Minecraft"));

        JButton prepareServer = new JButton("Preparar servidor");
        prepareServer.addActionListener(e -> runTask(frame, prepareServer, status, progressBar, "Preparando servidor…", () -> {
            requireFolder(selectedFolder[0]);
            SERVER.prepare(selectedFolder[0].resolve("server"), status::setText);
        }, "Servidor preparado", "Error del servidor"));

        JButton startServer = new JButton("Iniciar servidor");
        startServer.addActionListener(e -> {
            try { requireFolder(selectedFolder[0]); } catch (Exception ex) { showError(frame, ex); return; }
            startServer.setEnabled(false);
            status.setText("Iniciando servidor…");
            Thread thread = new Thread(() -> {
                try {
                    SERVER.start(selectedFolder[0].resolve("server"), (Integer) ram.getValue(), line -> SwingUtilities.invokeLater(() -> {
                        console.append(line + "\n");
                        console.setCaretPosition(console.getDocument().getLength());
                    }));
                    SwingUtilities.invokeLater(() -> { status.setText("Servidor ejecutándose."); startServer.setEnabled(true); });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> { startServer.setEnabled(true); showError(frame, ex); });
                }
            }, "LaucherCobblemon-server-start");
            thread.start();
        });

        JButton stopServer = new JButton("Detener servidor");
        stopServer.addActionListener(e -> {
            try { SERVER.stop(); status.setText("Solicitud de apagado enviada."); }
            catch (Exception ex) { showError(frame, ex); }
        });

        JPanel actions = new JPanel(new GridLayout(2, 3, 8, 8));
        actions.add(install); actions.add(play); actions.add(prepareServer);
        actions.add(startServer); actions.add(stopServer); actions.add(new JLabel());

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(settings, BorderLayout.NORTH);
        center.add(consoleScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(progressBar, BorderLayout.SOUTH);

        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private static void runTask(JFrame frame, JButton button, JLabel status, JProgressBar progress, String start, Task task, String success, String errorTitle) {
        button.setEnabled(false); progress.setIndeterminate(true); status.setText(start);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { task.run(); return null; }
            @Override protected void done() {
                button.setEnabled(true); progress.setIndeterminate(false);
                try { get(); progress.setString(success); status.setText(success); }
                catch (Exception ex) { Throwable cause = ex.getCause() != null ? ex.getCause() : ex; progress.setString("Error"); status.setText(cause.getMessage()); showError(frame, cause); }
            }
        }.execute();
    }

    private static void requireFolder(Path folder) throws Exception {
        if (folder == null) throw new Exception("Elige primero la carpeta del juego.");
    }

    private static void showError(JFrame frame, Throwable error) {
        JOptionPane.showMessageDialog(frame, error.getMessage(), "LaucherCobblemon", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface private interface Task { void run() throws Exception; }
}

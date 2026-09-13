package es.laucher.cobblemon;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/** Punto de entrada de LaucherCobblemon. */
public final class LaucherCobblemon {
    private static final String MINECRAFT_VERSION = LauncherService.MINECRAFT_VERSION;
    private static final LauncherService SERVICE = new LauncherService();

    private LaucherCobblemon() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LaucherCobblemon::createWindow);
    }

    private static void createWindow() {
        JFrame frame = new JFrame("LaucherCobblemon");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(680, 460);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("LaucherCobblemon");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        JLabel version = new JLabel("Cobblemon " + LauncherService.COBBLEMON_VERSION + " • Fabric " + MINECRAFT_VERSION);
        version.setFont(version.getFont().deriveFont(16f));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(version);

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
        settings.add(new JLabel("RAM (GB):"));
        settings.add(ram);
        settings.add(folderLabel);
        settings.add(folderButton);

        JLabel status = new JLabel("Listo.");
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setString("Listo");

        JButton install = new JButton("Preparar Fabric + Cobblemon");
        install.addActionListener(e -> {
            if (selectedFolder[0] == null) {
                JOptionPane.showMessageDialog(frame, "Elige primero la carpeta del juego.", "LaucherCobblemon", JOptionPane.WARNING_MESSAGE);
                return;
            }
            install.setEnabled(false);
            progressBar.setIndeterminate(true);
            status.setText("Preparando…");
            new SwingWorker<Void, String>() {
                @Override protected Void doInBackground() throws Exception {
                    SERVICE.prepare(selectedFolder[0], message -> SwingUtilities.invokeLater(() -> {
                        status.setText(message);
                        progressBar.setString(message);
                    }));
                    return null;
                }
                @Override protected void done() {
                    install.setEnabled(true);
                    progressBar.setIndeterminate(false);
                    try {
                        get();
                        progressBar.setString("Preparado");
                        status.setText("Fabric API y Cobblemon están en la carpeta mods.");
                        JOptionPane.showMessageDialog(frame,
                                "Preparación completada.\n\n" +
                                "Cobblemon " + LauncherService.COBBLEMON_VERSION + " para Fabric 1.21.1 está instalado.",
                                "LaucherCobblemon", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        progressBar.setString("Error");
                        status.setText("Error: " + cause.getMessage());
                        JOptionPane.showMessageDialog(frame, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        JButton play = new JButton("JUGAR");
        play.setFont(play.getFont().deriveFont(Font.BOLD, 18f));
        play.addActionListener(e -> JOptionPane.showMessageDialog(
                frame,
                "El siguiente paso es añadir el arranque autenticado de Minecraft y Fabric.",
                "LaucherCobblemon",
                JOptionPane.INFORMATION_MESSAGE));

        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        JPanel actions = new JPanel(new BorderLayout(10, 10));
        actions.add(install, BorderLayout.CENTER);
        actions.add(play, BorderLayout.EAST);
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(progressBar, BorderLayout.SOUTH);

        root.add(header, BorderLayout.NORTH);
        root.add(settings, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
    }
}

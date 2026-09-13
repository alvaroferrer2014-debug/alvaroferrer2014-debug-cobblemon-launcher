package es.laucher.cobblemon;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/**
 * Punto de entrada de LaucherCobblemon.
 * Base inicial del launcher para Minecraft Fabric 1.21.1.
 */
public final class LaucherCobblemon {
    private static final String MINECRAFT_VERSION = "1.21.1";

    private LaucherCobblemon() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LaucherCobblemon::createWindow);
    }

    private static void createWindow() {
        JFrame frame = new JFrame("LaucherCobblemon");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(620, 420);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("LaucherCobblemon");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));

        JLabel version = new JLabel("Cobblemon • Fabric " + MINECRAFT_VERSION);
        version.setFont(version.getFont().deriveFont(16f));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(version);

        JLabel folderLabel = new JLabel("Carpeta del juego: no seleccionada");
        JButton folderButton = new JButton("Elegir carpeta");
        folderButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                Path path = chooser.getSelectedFile().toPath();
                folderLabel.setText("Carpeta del juego: " + path);
            }
        });

        JSpinner ram = new JSpinner(new SpinnerNumberModel(4, 2, 16, 1));
        JPanel settings = new JPanel(new GridLayout(2, 2, 10, 10));
        settings.setBorder(BorderFactory.createTitledBorder("Configuración"));
        settings.add(new JLabel("RAM (GB):"));
        settings.add(ram);
        settings.add(folderLabel);
        settings.add(folderButton);

        JButton install = new JButton("Preparar Fabric + Cobblemon");
        install.addActionListener(e -> JOptionPane.showMessageDialog(
                frame,
                "La preparación automática se añadirá en la siguiente fase.\n" +
                        "Versión objetivo: Fabric " + MINECRAFT_VERSION,
                "LaucherCobblemon",
                JOptionPane.INFORMATION_MESSAGE));

        JButton play = new JButton("JUGAR");
        play.setFont(play.getFont().deriveFont(Font.BOLD, 18f));
        play.addActionListener(e -> JOptionPane.showMessageDialog(
                frame,
                "El sistema de lanzamiento de Minecraft se añadirá después de preparar Fabric y Cobblemon.",
                "LaucherCobblemon",
                JOptionPane.INFORMATION_MESSAGE));

        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.add(install, BorderLayout.CENTER);
        bottom.add(play, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(settings, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
    }
}

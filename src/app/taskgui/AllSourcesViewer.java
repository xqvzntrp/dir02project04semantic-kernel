package app.taskgui;

import app.sourceapp.AllSourcesView;
import app.sourceapp.AllSourcesViews;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public final class AllSourcesViewer {

    private AllSourcesViewer() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AllSourcesViewer::createAndShow);
    }

    private static void createAndShow() {
        AllSourcesView view = loadView();

        JFrame frame = new JFrame("All Sources Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);

        JLabel sourceLabel = new JLabel("Source: " + view.sourcePath());

        JTextArea textArea = new JTextArea(view.content());
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        frame.setLayout(new BorderLayout());
        frame.add(sourceLabel, BorderLayout.NORTH);
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private static AllSourcesView loadView() {
        try {
            return AllSourcesViews.load();
        } catch (RuntimeException e) {
            return new AllSourcesView(
                java.nio.file.Path.of("all-sources.txt"),
                "Unable to load all-sources view" + System.lineSeparator()
                    + System.lineSeparator()
                    + e.getMessage()
            );
        }
    }
}

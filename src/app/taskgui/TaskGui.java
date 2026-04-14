package app.taskgui;

import app.taskapp.TaskApplications;
import app.taskapp.TaskLoadResult;
import app.taskapp.TaskView;
import app.taskapp.TaskViews;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TaskGui {

    private final Path historyPath = Path.of("samples/eventchain/task-history.verified");

    private JFrame frame;
    private JTextField input;
    private DefaultListModel<String> listModel;
    private JList<String> actionList;
    private JTextArea stateArea;
    private JTextArea historyArea;

    private List<String> currentActions = List.of();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TaskGui().createAndShow());
    }

    private void createAndShow() {
        frame = new JFrame("Task Command Palette");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        input = new JTextField();
        listModel = new DefaultListModel<>();
        actionList = new JList<>(listModel);

        stateArea = new JTextArea();
        stateArea.setEditable(false);

        historyArea = new JTextArea();
        historyArea.setEditable(false);

        frame.setLayout(new BorderLayout());
        frame.add(input, BorderLayout.NORTH);

        JSplitPane centerSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(actionList),
            new JScrollPane(stateArea)
        );
        frame.add(centerSplit, BorderLayout.CENTER);
        frame.add(new JScrollPane(historyArea), BorderLayout.SOUTH);

        wireInputFiltering();
        wireEnterToApply();

        frame.setVisible(true);
        refresh();
    }

    private void refresh() {
        TaskLoadResult result = TaskViews.load(historyPath);

        if (result instanceof TaskLoadResult.EmptyHistory empty) {
            currentActions = List.of();
            listModel.clear();
            stateArea.setText("No verified task history.");
            historyArea.setText(
                "verified=" + empty.verifiedHistory() + "\n"
                    + "decoded=" + empty.decodedEvents()
            );
            return;
        }

        TaskView view = ((TaskLoadResult.Loaded) result).view();

        currentActions = view.snapshot().actions().stream()
            .map(action -> action.eventName())
            .collect(Collectors.toList());

        filter(input.getText());

        stateArea.setText(
            "Task ID: " + view.snapshot().state().id() + "\n"
                + "Status: " + view.snapshot().state().status() + "\n\n"
                + "Next Moves:\n" + view.snapshot().nextMoves()
        );

        historyArea.setText(
            "verified=" + view.verifiedHistory() + "\n\n"
                + "decoded=" + view.decodedEvents()
        );
    }

    private void wireInputFiltering() {
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filter(input.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filter(input.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter(input.getText());
            }
        });
    }

    private void filter(String text) {
        listModel.clear();

        String lower = text.toLowerCase();
        currentActions.stream()
            .filter(action -> action.toLowerCase().contains(lower))
            .forEach(listModel::addElement);

        if (!listModel.isEmpty()) {
            actionList.setSelectedIndex(0);
        }
    }

    private void wireEnterToApply() {
        input.addActionListener(e -> applySelected());
        actionList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    applySelected();
                }
            }
        });
    }

    private void applySelected() {
        String selected = actionList.getSelectedValue();
        if (selected == null) {
            return;
        }

        try {
            TaskApplications.apply(historyPath, selected);
            input.setText("");
            refresh();
        } catch (Exception e) {
            stateArea.setText("Error: " + e.getMessage());
        }
    }
}

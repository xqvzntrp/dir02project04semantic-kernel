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
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TaskGui {

    private final Path historyPath = Path.of("semantic-kernel", "samples", "eventchain", "task-history.verified");

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
        input.setToolTipText("Type to filter available actions");
        listModel = new DefaultListModel<>();
        actionList = new JList<>(listModel);

        stateArea = new JTextArea();
        stateArea.setEditable(false);

        historyArea = new JTextArea();
        historyArea.setEditable(false);

        JLabel actionsLabel = new JLabel("Actions");
        JLabel stateLabel = new JLabel("State");
        JLabel historyLabel = new JLabel("History");

        frame.setLayout(new BorderLayout());
        frame.add(input, BorderLayout.NORTH);

        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.add(actionsLabel, BorderLayout.NORTH);
        actionsPanel.add(new JScrollPane(actionList), BorderLayout.CENTER);

        JPanel statePanel = new JPanel(new BorderLayout());
        statePanel.add(stateLabel, BorderLayout.NORTH);
        statePanel.add(new JScrollPane(stateArea), BorderLayout.CENTER);

        JSplitPane centerSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            actionsPanel,
            statePanel
        );
        centerSplit.setResizeWeight(0.4);

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(historyLabel, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(historyArea), BorderLayout.CENTER);

        JSplitPane verticalSplit = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            centerSplit,
            historyPanel
        );
        verticalSplit.setResizeWeight(0.7);

        frame.add(verticalSplit, BorderLayout.CENTER);

        wireInputFiltering();
        wireEnterToApply();

        frame.setVisible(true);
        refresh();
    }

    private void refresh() {
        final TaskLoadResult result;
        try {
            result = TaskViews.load(historyPath);
        } catch (RuntimeException e) {
            currentActions = List.of();
            listModel.clear();
            listModel.addElement("Unable to load actions.");
            stateArea.setText("Error: " + e.getMessage());
            historyArea.setText("historyPath=" + historyPath);
            return;
        }

        if (result instanceof TaskLoadResult.EmptyHistory empty) {
            currentActions = List.of();
            listModel.clear();
            listModel.addElement("No actions available.");
            stateArea.setText(
                "No verified task history.\n\n"
                    + "Type \"create <taskId>\" to create a new task."
            );
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

        if (currentActions.isEmpty()) {
            listModel.addElement("No actions available.");
            return;
        }

        String lower = text.toLowerCase();
        List<String> filtered = currentActions.stream()
            .filter(action -> action.toLowerCase().contains(lower))
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            listModel.addElement("No matching actions.");
        } else {
            filtered.forEach(listModel::addElement);
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
        String text = input.getText().trim();
        if (text.startsWith("create ")) {
            String taskId = text.substring("create ".length()).trim();
            try {
                TaskApplications.create(historyPath, taskId);
                input.setText("");
                refresh();
            } catch (Exception e) {
                refresh();
                stateArea.append("\n\nError: " + e.getMessage());
            }
            return;
        }

        String selected = actionList.getSelectedValue();
        if (selected == null) {
            return;
        }

        try {
            TaskApplications.apply(historyPath, selected);
            input.setText("");
            refresh();
        } catch (Exception e) {
            refresh();
            stateArea.append("\n\nError: " + e.getMessage());
        }
    }
}

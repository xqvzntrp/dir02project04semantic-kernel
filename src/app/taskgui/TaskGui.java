package app.taskgui;

import app.taskapp.TaskApplications;
import app.taskapp.TaskLoadResult;
import app.taskapp.TaskView;
import app.taskapp.TaskViews;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import semantic.kernel.ActionDescriptor;
import task.domain.TaskActionAdapter;

public final class TaskGui {

    private final Path historyPath = Path.of("semantic-kernel", "samples", "eventchain", "task-history.verified");

    private JFrame frame;
    private JTextField input;
    private DefaultListModel<String> listModel;
    private JList<String> actionList;
    private JLabel statusLabel;
    private final TaskActionAdapter actionAdapter = new TaskActionAdapter();

    private List<ActionDescriptor> currentDescriptors = List.of();
    private List<ActionDescriptor> visibleDescriptors = List.of();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TaskGui().createAndShow());
    }

    private void createAndShow() {
        frame = new JFrame("Task Action Filter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 260);

        input = new JTextField();
        input.setToolTipText("Type to filter actions. Press Enter to apply the selected action.");
        listModel = new DefaultListModel<>();
        actionList = new JList<>(listModel);
        statusLabel = new JLabel("Loading actions...");

        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.add(new JLabel("Available Options"), BorderLayout.NORTH);
        actionsPanel.add(new JScrollPane(actionList), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(0, 8));
        inputPanel.add(new JLabel("Input"), BorderLayout.NORTH);
        inputPanel.add(input, BorderLayout.CENTER);
        inputPanel.add(
            new JLabel("Type to filter. Use \"create <taskId>\" or press Enter on a selection."),
            BorderLayout.SOUTH
        );

        JPanel gridPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        gridPanel.add(actionsPanel);
        gridPanel.add(inputPanel);

        frame.setLayout(new BorderLayout());
        frame.add(gridPanel, BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);

        wireInputFiltering();
        wireEnterToApply();

        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        refresh();
    }

    private void refresh() {
        final TaskLoadResult result;
        try {
            result = TaskViews.load(historyPath);
        } catch (RuntimeException e) {
            currentDescriptors = List.of();
            visibleDescriptors = List.of();
            listModel.clear();
            listModel.addElement("Unable to load actions.");
            actionList.clearSelection();
            statusLabel.setText("Error loading actions from " + historyPath + ": " + e.getMessage());
            actionList.setToolTipText(null);
            return;
        }

        if (result instanceof TaskLoadResult.EmptyHistory empty) {
            currentDescriptors = List.of();
            visibleDescriptors = List.of();
            listModel.clear();
            listModel.addElement("No actions available.");
            actionList.clearSelection();
            statusLabel.setText(
                "No verified task history. Type \"create <taskId>\" to create one. verified="
                    + empty.verifiedHistory().size()
                    + ", decoded="
                    + empty.decodedEvents().size()
            );
            actionList.setToolTipText(null);
            return;
        }

        TaskView view = ((TaskLoadResult.Loaded) result).view();
        currentDescriptors = actionAdapter.describe(view.snapshot().actions());

        filter(input.getText());
        statusLabel.setText(
            "Task ID: " + view.snapshot().state().id()
                + " | Status: " + view.snapshot().state().status()
                + " | Actions: "
                + currentDescriptors.stream().map(ActionDescriptor::name).collect(Collectors.toList())
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

        if (currentDescriptors.isEmpty()) {
            visibleDescriptors = List.of();
            listModel.addElement("No actions available.");
            actionList.clearSelection();
            actionList.setToolTipText(null);
            return;
        }

        String lower = text.toLowerCase();
        List<ActionDescriptor> filtered = currentDescriptors.stream()
            .filter(action -> matchesFilter(action, lower))
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            visibleDescriptors = List.of();
            listModel.addElement("No matching actions.");
            actionList.clearSelection();
            actionList.setToolTipText(null);
            return;
        }

        visibleDescriptors = filtered;
        filtered.stream()
            .map(this::displayText)
            .forEach(listModel::addElement);
        actionList.setSelectedIndex(0);
        updateSelectionHelp();
    }

    private void wireEnterToApply() {
        input.addActionListener(e -> applySelected());
        actionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectionHelp();
            }
        });
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
                showError(e);
            }
            return;
        }

        int index = actionList.getSelectedIndex();
        if (index < 0 || index >= visibleDescriptors.size()) {
            return;
        }
        ActionDescriptor selected = visibleDescriptors.get(index);

        try {
            TaskApplications.apply(historyPath, selected.name());
            input.setText("");
            refresh();
        } catch (Exception e) {
            refresh();
            showError(e);
        }
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(
            frame,
            e.getMessage(),
            "Task Action Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private boolean matchesFilter(ActionDescriptor action, String text) {
        return action.name().toLowerCase().contains(text)
            || action.label().toLowerCase().contains(text)
            || action.help().toLowerCase().contains(text);
    }

    private String displayText(ActionDescriptor action) {
        return action.label() + " (" + action.name() + ")";
    }

    private void updateSelectionHelp() {
        int index = actionList.getSelectedIndex();
        if (index < 0 || index >= visibleDescriptors.size()) {
            actionList.setToolTipText(null);
            return;
        }
        ActionDescriptor descriptor = visibleDescriptors.get(index);
        actionList.setToolTipText(descriptor.help());
        statusLabel.setText(descriptor.label() + " | " + descriptor.help());
    }
}

package app.approvalgui;

import app.approvalapp.ApprovalApplications;
import app.approvalapp.ApprovalExplorations;
import app.approvalapp.ApprovalLoadResult;
import app.approvalapp.ApprovalSimulationTrace;
import app.approvalapp.ApprovalTimeline;
import app.approvalapp.ApprovalTimelineEntry;
import app.approvalapp.ApprovalView;
import app.approvalapp.ApprovalViews;
import app.approvalcli.ApprovalHistoryFile;
import approval.domain.ApprovalActionAdapter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
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

public final class ApprovalGui {

    private static final Path DEFAULT_HISTORY_PATH = Path.of(
        "semantic-kernel",
        "samples",
        "eventchain",
        "approval-history.verified"
    );

    private Path historyPath;

    private JFrame frame;
    private JTextField input;
    private DefaultListModel<String> timelineModel;
    private JList<String> timelineList;
    private DefaultListModel<String> listModel;
    private JList<String> actionList;
    private JLabel statusLabel;
    private JLabel currentStateLabel;
    private JLabel simulatedPathLabel;
    private JLabel previewStateLabel;
    private DefaultListModel<String> simulatedPathModel;
    private JList<String> simulatedPathList;
    private DefaultListModel<String> previewModel;
    private JList<String> previewList;
    private JButton openHistoryButton;
    private JButton saveSimulationButton;
    private JButton addStepButton;
    private JButton undoStepButton;
    private JButton clearSimulationButton;
    private final ApprovalActionAdapter actionAdapter = new ApprovalActionAdapter();

    private ApprovalView currentView;
    private ApprovalTimeline currentTimeline;
    private List<ActionDescriptor> currentDescriptors = List.of();
    private List<ActionDescriptor> visibleDescriptors = List.of();
    private final List<String> simulatedActionNames = new ArrayList<>();
    private int selectedTimelineIndex = -1;

    public static void main(String[] args) {
        Path history = args.length > 0 ? Path.of(args[0]) : DEFAULT_HISTORY_PATH;
        SwingUtilities.invokeLater(() -> new ApprovalGui(history).createAndShow());
    }

    public ApprovalGui(Path historyPath) {
        this.historyPath = historyPath;
    }

    private void createAndShow() {
        frame = new JFrame("Approval Semantic Explorer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 420);

        input = new JTextField();
        input.setToolTipText("Type to filter actions. Press Enter to apply the selected action.");
        timelineModel = new DefaultListModel<>();
        timelineList = new JList<>(timelineModel);
        listModel = new DefaultListModel<>();
        actionList = new JList<>(listModel);
        simulatedPathModel = new DefaultListModel<>();
        simulatedPathList = new JList<>(simulatedPathModel);
        previewModel = new DefaultListModel<>();
        previewList = new JList<>(previewModel);
        statusLabel = new JLabel("Loading explorer...");
        currentStateLabel = new JLabel("Current state");
        simulatedPathLabel = new JLabel("Simulated path");
        previewStateLabel = new JLabel("Preview state");
        openHistoryButton = new JButton("Open History File");
        saveSimulationButton = new JButton("Save Simulated History As...");
        addStepButton = new JButton("Add Step");
        undoStepButton = new JButton("Undo Last");
        clearSimulationButton = new JButton("Clear Simulation");

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topBar.add(openHistoryButton);
        topBar.add(saveSimulationButton);

        JPanel timelinePanel = new JPanel(new BorderLayout());
        timelinePanel.add(new JLabel("Timeline"), BorderLayout.NORTH);
        timelinePanel.add(new JScrollPane(timelineList), BorderLayout.CENTER);
        timelinePanel.setPreferredSize(new Dimension(320, 0));

        JPanel currentPanel = new JPanel(new BorderLayout(0, 8));
        currentPanel.add(currentStateLabel, BorderLayout.NORTH);
        currentPanel.add(new JScrollPane(actionList), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(0, 8));
        inputPanel.add(new JLabel("Input"), BorderLayout.NORTH);
        inputPanel.add(input, BorderLayout.CENTER);
        inputPanel.add(
            new JLabel("Type to filter. Use \"submit <approvalId>\" or press Enter on a selection at the live tip."),
            BorderLayout.SOUTH
        );

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.add(currentPanel, BorderLayout.CENTER);
        centerPanel.add(inputPanel, BorderLayout.SOUTH);

        JPanel simulationControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        simulationControls.add(addStepButton);
        simulationControls.add(undoStepButton);
        simulationControls.add(clearSimulationButton);

        JPanel simulationPanel = new JPanel(new BorderLayout(0, 8));
        simulationPanel.add(simulatedPathLabel, BorderLayout.NORTH);
        simulationPanel.add(new JScrollPane(simulatedPathList), BorderLayout.CENTER);
        simulationPanel.add(simulationControls, BorderLayout.SOUTH);
        simulationPanel.setPreferredSize(new Dimension(280, 0));

        JPanel previewPanel = new JPanel(new BorderLayout(0, 8));
        previewPanel.add(previewStateLabel, BorderLayout.NORTH);
        previewPanel.add(new JScrollPane(previewList), BorderLayout.CENTER);
        previewPanel.setPreferredSize(new Dimension(320, 0));

        frame.setLayout(new BorderLayout());
        frame.add(topBar, BorderLayout.NORTH);
        frame.add(timelinePanel, BorderLayout.WEST);
        frame.add(centerPanel, BorderLayout.CENTER);
        JPanel eastPanel = new JPanel(new BorderLayout(12, 0));
        eastPanel.add(simulationPanel, BorderLayout.WEST);
        eastPanel.add(previewPanel, BorderLayout.CENTER);
        frame.add(eastPanel, BorderLayout.EAST);
        frame.add(statusLabel, BorderLayout.SOUTH);

        wireInputFiltering();
        wireEnterToApply();
        wireTimelineSelection();
        wireSimulationButtons();
        wireOpenHistoryButton();

        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        updateWindowTitle();
        refresh();
    }

    private void refresh() {
        final ApprovalLoadResult result;
        try {
            result = ApprovalViews.load(historyPath);
        } catch (RuntimeException e) {
            currentView = null;
            currentTimeline = null;
            selectedTimelineIndex = -1;
            simulatedActionNames.clear();
            timelineModel.clear();
            currentDescriptors = List.of();
            visibleDescriptors = List.of();
            listModel.clear();
            simulatedPathModel.clear();
            previewModel.clear();
            listModel.addElement("Unable to load actions.");
            simulatedPathModel.addElement("Simulation unavailable.");
            previewModel.addElement("Preview unavailable.");
            actionList.clearSelection();
            timelineList.clearSelection();
            currentStateLabel.setText("Current state unavailable");
            simulatedPathLabel.setText("Simulated path unavailable");
            previewStateLabel.setText("Preview unavailable");
            statusLabel.setText("Error loading actions from " + historyPath + ": " + e.getMessage());
            actionList.setToolTipText(null);
            updateSimulationButtons();
            return;
        }

        if (result instanceof ApprovalLoadResult.EmptyHistory empty) {
            currentView = null;
            currentTimeline = null;
            selectedTimelineIndex = -1;
            simulatedActionNames.clear();
            timelineModel.clear();
            currentDescriptors = List.of();
            visibleDescriptors = List.of();
            listModel.clear();
            simulatedPathModel.clear();
            previewModel.clear();
            listModel.addElement("No actions available.");
            simulatedPathModel.addElement("Submit an approval to start exploring.");
            previewModel.addElement("Submit an approval to start exploring.");
            actionList.clearSelection();
            timelineList.clearSelection();
            currentStateLabel.setText("No approval state yet");
            simulatedPathLabel.setText("Simulated path");
            previewStateLabel.setText("Preview unavailable");
            statusLabel.setText(
                "No verified approval history. Type \"submit <approvalId>\" to create one. verified="
                    + empty.verifiedHistory().size()
                    + ", decoded="
                    + empty.decodedEvents().size()
            );
            actionList.setToolTipText(null);
            updateSimulationButtons();
            return;
        }

        currentView = ((ApprovalLoadResult.Loaded) result).view();
        currentTimeline = ApprovalExplorations.timeline(currentView);
        populateTimeline();
        if (selectedTimelineIndex < 0 || selectedTimelineIndex >= currentTimeline.entries().size()) {
            selectedTimelineIndex = currentTimeline.entries().size() - 1;
        }
        timelineList.setSelectedIndex(selectedTimelineIndex);
        updateCurrentSelection();
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
            updatePreview();
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
            updatePreview();
            return;
        }

        visibleDescriptors = filtered;
        filtered.stream().map(this::displayText).forEach(listModel::addElement);
        actionList.setSelectedIndex(0);
        updateSelectionHelp();
        updatePreview();
    }

    private void wireEnterToApply() {
        input.addActionListener(this::handleEnter);
        actionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectionHelp();
                updatePreview();
            }
        });
        actionList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    handleEnter(null);
                }
            }
        });
    }

    private void wireTimelineSelection() {
        timelineList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = timelineList.getSelectedIndex();
                if (index >= 0) {
                    if (index != selectedTimelineIndex) {
                        selectedTimelineIndex = index;
                        simulatedActionNames.clear();
                    }
                    updateCurrentSelection();
                }
            }
        });
    }

    private void wireSimulationButtons() {
        saveSimulationButton.addActionListener(e -> saveSimulationAs());
        addStepButton.addActionListener(e -> addSimulationStep());
        undoStepButton.addActionListener(e -> {
            if (!simulatedActionNames.isEmpty()) {
                simulatedActionNames.remove(simulatedActionNames.size() - 1);
                updateCurrentSelection();
            }
        });
        clearSimulationButton.addActionListener(e -> {
            if (!simulatedActionNames.isEmpty()) {
                simulatedActionNames.clear();
                updateCurrentSelection();
            }
        });
    }

    private void wireOpenHistoryButton() {
        openHistoryButton.addActionListener(e -> chooseFile());
    }

    private void handleEnter(ActionEvent event) {
        String text = input.getText().trim();
        if (text.startsWith("submit ")) {
            applySelected();
            return;
        }
        if (isAtLiveTip() && simulatedActionNames.isEmpty()) {
            applySelected();
            return;
        }
        addSimulationStep();
    }

    private void applySelected() {
        String text = input.getText().trim();
        if (text.startsWith("submit ")) {
            String approvalId = text.substring("submit ".length()).trim();
            try {
                ApprovalApplications.submit(historyPath, approvalId);
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
        if (!isAtLiveTip() || !simulatedActionNames.isEmpty()) {
            showError(new IllegalStateException("Appending to history is only available at the live tip with no simulated steps."));
            return;
        }
        ActionDescriptor selected = visibleDescriptors.get(index);

        try {
            ApprovalApplications.apply(historyPath, selected.name());
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
            "Approval Action Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void addSimulationStep() {
        if (currentView == null || currentTimeline == null) {
            return;
        }
        int index = actionList.getSelectedIndex();
        if (index < 0 || index >= visibleDescriptors.size()) {
            return;
        }
        simulatedActionNames.add(visibleDescriptors.get(index).name());
        updateCurrentSelection();
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(historyPath.toFile());
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            setHistoryPath(chooser.getSelectedFile().toPath());
        }
    }

    private void setHistoryPath(Path newPath) {
        historyPath = newPath;
        simulatedActionNames.clear();
        updateWindowTitle();
        refresh();
    }

    private void saveSimulationAs() {
        if (simulatedActionNames.isEmpty()) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(suggestForkPath().toFile());
        int result = chooser.showSaveDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path target = chooser.getSelectedFile().toPath();
        try {
            ApprovalSimulationTrace trace = currentSimulationTrace();
            new ApprovalHistoryFile().writeAll(target, trace.fullEvents(), currentLineageMetadata(trace));
            int openResult = JOptionPane.showConfirmDialog(
                frame,
                "Saved simulated history to " + target + ". Open it now?",
                "Simulation Saved",
                JOptionPane.YES_NO_OPTION
            );
            if (openResult == JOptionPane.YES_OPTION) {
                setHistoryPath(target);
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private ApprovalHistoryFile.LineageMetadata currentLineageMetadata(ApprovalSimulationTrace trace) {
        String sourceName = historyPath.getFileName() != null ? historyPath.getFileName().toString() : historyPath.toString();
        return new ApprovalHistoryFile.LineageMetadata(
            sourceName,
            trace.baseEvents().size(),
            Instant.now()
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
    }

    private void populateTimeline() {
        timelineModel.clear();
        for (ApprovalTimelineEntry entry : currentTimeline.entries()) {
            timelineModel.addElement(timelineText(entry));
        }
    }

    private void updateCurrentSelection() {
        if (currentTimeline == null || selectedTimelineIndex < 0 || selectedTimelineIndex >= currentTimeline.entries().size()) {
            currentDescriptors = List.of();
            visibleDescriptors = List.of();
            currentStateLabel.setText("Current state unavailable");
            simulatedPathLabel.setText("Simulated path unavailable");
            filter(input.getText());
            updateSimulationButtons();
            return;
        }

        ApprovalTimelineEntry entry = currentTimeline.at(selectedTimelineIndex);
        ApprovalSimulationTrace trace = currentSimulationTrace();
        currentDescriptors = actionAdapter.describe(trace.finalSnapshot().actions());
        currentStateLabel.setText(
            "At step " + (entry.eventIndex() + 1)
                + ": " + trace.finalSnapshot().state().id()
                + " | Status: " + trace.finalSnapshot().state().status()
                + " | Actions: "
                + currentDescriptors.stream().map(ActionDescriptor::name).collect(Collectors.toList())
        );
        updateSimulationPath(trace);
        statusLabel.setText(statusText());
        filter(input.getText());
        updateSimulationButtons();
    }

    private void updatePreview() {
        previewModel.clear();
        if (currentView == null || currentTimeline == null || visibleDescriptors.isEmpty()) {
            previewStateLabel.setText("Preview unavailable");
            previewList.setToolTipText(null);
            return;
        }

        int actionIndex = actionList.getSelectedIndex();
        if (actionIndex < 0 || actionIndex >= visibleDescriptors.size()) {
            previewStateLabel.setText("Preview unavailable");
            previewList.setToolTipText(null);
            return;
        }

        ActionDescriptor selected = visibleDescriptors.get(actionIndex);
        try {
            List<String> previewActions = new ArrayList<>(simulatedActionNames);
            previewActions.add(selected.name());
            ApprovalSimulationTrace preview = ApprovalExplorations.simulationTrace(currentView, selectedTimelineIndex, previewActions);
            List<ActionDescriptor> previewDescriptors = actionAdapter.describe(preview.finalSnapshot().actions());
            previewStateLabel.setText(
                "Preview after " + selected.label()
                    + ": " + preview.finalSnapshot().state().id()
                    + " | Status: " + preview.finalSnapshot().state().status()
            );
            if (previewDescriptors.isEmpty()) {
                previewModel.addElement("No actions available.");
            } else {
                previewDescriptors.stream().map(this::displayText).forEach(previewModel::addElement);
            }
            previewList.setToolTipText(selected.help());
        } catch (RuntimeException e) {
            previewStateLabel.setText("Preview failed: " + e.getMessage());
            previewModel.addElement("Preview unavailable.");
            previewList.setToolTipText(null);
        }
    }

    private boolean isAtLiveTip() {
        return currentTimeline != null && selectedTimelineIndex == currentTimeline.entries().size() - 1;
    }

    private ApprovalSimulationTrace currentSimulationTrace() {
        if (currentView == null || currentTimeline == null || selectedTimelineIndex < 0) {
            throw new IllegalStateException("No current simulation trace is available");
        }
        return ApprovalExplorations.simulationTrace(currentView, selectedTimelineIndex, simulatedActionNames);
    }

    private void updateSimulationPath(ApprovalSimulationTrace trace) {
        simulatedPathModel.clear();
        simulatedPathLabel.setText("Simulated path (" + trace.steps().size() + " step" + (trace.steps().size() == 1 ? "" : "s") + ")");
        if (trace.steps().isEmpty()) {
            simulatedPathModel.addElement("No simulated steps yet.");
            return;
        }
        trace.steps().forEach(step -> simulatedPathModel.addElement(
            (step.stepIndex() + 1)
                + ". "
                + step.descriptor().label()
                + " -> "
                + step.snapshot().state().status()
        ));
    }

    private String statusText() {
        if (!simulatedActionNames.isEmpty()) {
            return "Simulation path active. Enter or Add Step extends the in-memory path only.";
        }
        if (isAtLiveTip()) {
            return "Live tip selected. Press Enter to append the selected action.";
        }
        return "Past step selected. Enter or Add Step extends an in-memory simulation without writing history.";
    }

    private void updateSimulationButtons() {
        boolean canSimulate = currentView != null && currentTimeline != null && !visibleDescriptors.isEmpty();
        saveSimulationButton.setEnabled(!simulatedActionNames.isEmpty());
        addStepButton.setEnabled(canSimulate);
        undoStepButton.setEnabled(!simulatedActionNames.isEmpty());
        clearSimulationButton.setEnabled(!simulatedActionNames.isEmpty());
        openHistoryButton.setEnabled(true);
    }

    private String timelineText(ApprovalTimelineEntry entry) {
        return (entry.eventIndex() + 1)
            + ". "
            + entry.event().getClass().getSimpleName()
            + " -> "
            + entry.snapshot().state().status();
    }

    private void updateWindowTitle() {
        if (frame != null) {
            frame.setTitle("Approval Semantic Explorer - " + historyPath);
        }
    }

    private Path suggestForkPath() {
        String fileName = historyPath.getFileName() == null ? "approval-history.verified" : historyPath.getFileName().toString();
        String suggestedName = fileName.endsWith(".verified")
            ? fileName.substring(0, fileName.length() - ".verified".length()) + "-fork.verified"
            : fileName + "-fork.verified";
        return historyPath.resolveSibling(suggestedName);
    }
}

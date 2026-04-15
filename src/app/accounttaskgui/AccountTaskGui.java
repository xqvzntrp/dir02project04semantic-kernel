package app.accounttaskgui;

import accounttask.domain.AccountTaskAction;
import accounttask.domain.AccountTaskActionAdapter;
import accounttask.domain.AccountTaskDomainKernel;
import accounttask.domain.AccountTaskEvent;
import accounttask.domain.AccountTaskRuleState;
import accounttask.domain.AccountTaskState;
import accounttask.domain.NoteRecordedForAccountTask;
import accounttask.domain.WorkItemOpenedForAccount;
import app.accounttaskcli.AccountTaskHistoryFile;
import app.accounttaskcli.ContentStore;
import app.accounttaskcli.FileContentStore;
import integration.eventchain.AccountTaskEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import semantic.kernel.ActionDescriptor;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;

public final class AccountTaskGui {

    private static final Path DEFAULT_HISTORY_PATH = Path.of(
        "semantic-kernel",
        "samples",
        "eventchain",
        "accounttask",
        "accounttask-session.verified"
    );

    private final AccountTaskHistoryFile historyFile = new AccountTaskHistoryFile();
    private final AccountTaskEventChainDecoder decoder = new AccountTaskEventChainDecoder();
    private final AccountTaskActionAdapter actionAdapter = new AccountTaskActionAdapter();
    private final ContentStore contentStore = new FileContentStore(Path.of("semantic-kernel", "content"));
    private final Path historyPath;

    private JFrame frame;
    private JTextArea historyArea;
    private JTextArea infoArea;
    private JTextField inputField;
    private DefaultListModel<String> optionsModel;
    private JList<String> optionsList;
    private JLabel statusLabel;
    private String statusText = "Loading account-task explorer...";

    private List<AccountTaskEvent> decodedEvents = List.of();
    private SemanticSnapshot<AccountTaskState, NextMove<AccountTaskRuleState>, AccountTaskAction> snapshot;
    private List<OptionEntry> visibleOptions = List.of();

    public static void main(String[] args) {
        Path history = args.length > 0 ? Path.of(args[0]) : DEFAULT_HISTORY_PATH;
        SwingUtilities.invokeLater(() -> new AccountTaskGui(history).createAndShow());
    }

    public AccountTaskGui(Path historyPath) {
        this.historyPath = historyPath;
    }

    private void createAndShow() {
        frame = new JFrame("AccountTask Console");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 720);

        historyArea = createReadOnlyArea();
        infoArea = createReadOnlyArea();
        inputField = new JTextField();
        inputField.setToolTipText("Type here only. Use up/down to select options and Enter to execute.");
        optionsModel = new DefaultListModel<>();
        optionsList = new JList<>(optionsModel);
        optionsList.setFocusable(false);
        optionsList.setRequestFocusEnabled(false);
        statusLabel = new JLabel("  " + statusText);

        JPanel quadrants = new JPanel(new GridLayout(2, 2, 12, 12));
        quadrants.add(panel("CLI / History", new JScrollPane(historyArea)));
        quadrants.add(panel("Input", inputField));
        quadrants.add(panel("Info", new JScrollPane(infoArea)));
        quadrants.add(panel("Filtered Options", new JScrollPane(optionsList)));

        frame.setLayout(new BorderLayout(0, 8));
        frame.add(quadrants, BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);

        wireInput();
        refresh();

        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private JPanel panel(String title, java.awt.Component component) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(400, 280));
        return panel;
    }

    private JTextArea createReadOnlyArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        return area;
    }

    private void wireInput() {
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterOptions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterOptions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterOptions();
            }
        });

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    moveSelection(-1);
                    e.consume();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    moveSelection(1);
                    e.consume();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeSelectedOption();
                    e.consume();
                }
            }
        });
    }

    private void refresh() {
        try {
            List<VerifiedFieldEvent> verified = historyFile.load(historyPath);
            decodedEvents = decoder.decode(verified);
            snapshot = decodedEvents.isEmpty() ? null : AccountTaskDomainKernel.create().analyze(decodedEvents);
            statusText = decodedEvents.isEmpty()
                ? "No account-task history yet. Type \"open <accountId> <taskId>\" to begin."
                : "Loaded " + decodedEvents.size() + " event(s). Use input + arrows + Enter.";
            renderHistory(verified);
            filterOptions();
            renderInfo();
        } catch (Exception e) {
            decodedEvents = List.of();
            snapshot = null;
            statusText = "Error loading " + historyPath + ": " + e.getMessage();
            historyArea.setText("Unable to read history.\n\n" + e.getMessage());
            optionsModel.clear();
            infoArea.setText("State unavailable.");
        }
        statusLabel.setText("  " + statusText);
        inputField.requestFocusInWindow();
    }

    private void renderHistory(List<VerifiedFieldEvent> verified) {
        StringBuilder builder = new StringBuilder();
        builder.append("History file: ").append(historyPath).append(System.lineSeparator()).append(System.lineSeparator());
        if (verified.isEmpty()) {
            builder.append("No verified events yet.");
        } else {
            builder.append("Verified events:").append(System.lineSeparator());
            for (VerifiedFieldEvent event : verified) {
                builder.append("  ")
                    .append(event.sequence())
                    .append(". ")
                    .append(event.eventType())
                    .append(" ")
                    .append(event.fields())
                    .append(System.lineSeparator());
            }
        }
        historyArea.setText(builder.toString());
        historyArea.setCaretPosition(0);
    }

    private void renderInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("Status: ").append(statusText).append(System.lineSeparator()).append(System.lineSeparator());
        if (snapshot == null) {
            builder.append("State: <none>").append(System.lineSeparator());
            builder.append("Guidance: type ").append("\"open <accountId> <taskId>\"").append(" to create the first composite event.");
        } else {
            builder.append("State: ").append(snapshot.state()).append(System.lineSeparator());
            builder.append("Next moves: ").append(snapshot.nextMoves()).append(System.lineSeparator());
            builder.append("Actions: ").append(snapshot.actions()).append(System.lineSeparator()).append(System.lineSeparator());
            builder.append("Notes:").append(System.lineSeparator());
            if (snapshot.state().noteHashes().isEmpty()) {
                builder.append("  <none>").append(System.lineSeparator()).append(System.lineSeparator());
            } else {
                for (String hash : snapshot.state().noteHashes()) {
                    builder.append("  ")
                        .append(renderNote(hash))
                        .append(System.lineSeparator());
                }
                builder.append(System.lineSeparator());
            }
            OptionEntry selected = selectedOption();
            if (selected != null) {
                builder.append("Selected option: ").append(selected.display()).append(System.lineSeparator());
                builder.append("Help: ").append(selected.help()).append(System.lineSeparator());
            } else {
                builder.append("Selected option: <none>").append(System.lineSeparator());
            }
        }
        infoArea.setText(builder.toString());
        infoArea.setCaretPosition(0);
    }

    private void filterOptions() {
        String text = inputField.getText().trim().toLowerCase();
        List<OptionEntry> candidates = availableOptions();
        List<OptionEntry> filtered = new ArrayList<>();
        for (OptionEntry option : candidates) {
            if (text.isBlank() || option.matches(text)) {
                filtered.add(option);
            }
        }

        visibleOptions = List.copyOf(filtered);
        optionsModel.clear();
        if (visibleOptions.isEmpty()) {
            optionsModel.addElement("No matching options.");
            optionsList.clearSelection();
        } else {
            for (OptionEntry option : visibleOptions) {
                optionsModel.addElement(option.display());
            }
            optionsList.setSelectedIndex(0);
        }
        renderInfo();
    }

    private List<OptionEntry> availableOptions() {
        if (snapshot == null) {
            return List.of(new OptionEntry(
                "open",
                "Open Work Item",
                "Creates the first composite event. Type: open <accountId> <taskId>",
                true
            ));
        }

        List<OptionEntry> options = new ArrayList<>();
        options.add(new OptionEntry(
            "record-note",
            "Record Note",
            "Adds a non-semantic note. Type: record-note <text>",
            true
        ));
        for (ActionDescriptor descriptor : actionAdapter.describe(snapshot.actions())) {
            options.add(new OptionEntry(descriptor.name(), descriptor.label(), descriptor.help(), false));
        }
        return List.copyOf(options);
    }

    private void moveSelection(int delta) {
        if (visibleOptions.isEmpty()) {
            return;
        }
        int selected = optionsList.getSelectedIndex();
        if (selected < 0) {
            optionsList.setSelectedIndex(0);
        } else {
            int next = Math.max(0, Math.min(visibleOptions.size() - 1, selected + delta));
            optionsList.setSelectedIndex(next);
        }
        renderInfo();
    }

    private void executeSelectedOption() {
        OptionEntry selected = selectedOption();
        if (selected == null) {
            return;
        }

        try {
            if (selected.requiresArguments()) {
                executeCommandWithArguments(selected.name());
            } else {
                historyFile.append(historyPath, actionAdapter.toEvent(snapshot.state(), snapshot.actions(), selected.name()));
            }
            inputField.setText("");
            refresh();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), "AccountTask Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executeCommandWithArguments(String commandName) throws IOException {
        if ("open".equals(commandName)) {
            executeOpenCommand();
            return;
        }
        if ("record-note".equals(commandName)) {
            executeRecordNoteCommand();
            return;
        }
        throw new IllegalArgumentException("Unsupported command: " + commandName);
    }

    private void executeOpenCommand() throws IOException {
        String[] parts = inputField.getText().trim().split("\\s+");
        if (parts.length != 3 || !"open".equals(parts[0])) {
            throw new IllegalArgumentException("Use: open <accountId> <taskId>");
        }
        historyFile.append(historyPath, new WorkItemOpenedForAccount(parts[1], parts[2]));
    }

    private void executeRecordNoteCommand() throws IOException {
        if (snapshot == null) {
            throw new IllegalStateException("Open a work item before recording notes.");
        }
        String text = inputField.getText().trim();
        if (!text.startsWith("record-note ")) {
            throw new IllegalArgumentException("Use: record-note <text>");
        }
        String noteText = text.substring("record-note ".length()).trim();
        if (noteText.isBlank()) {
            throw new IllegalArgumentException("Note text must not be blank");
        }
        String hash = contentStore.put(noteText);
        historyFile.append(historyPath, new NoteRecordedForAccountTask(snapshot.state().accountId(), snapshot.state().taskId(), hash));
    }

    private String renderNote(String hash) {
        try {
            return hash + " -> " + contentStore.get(hash).orElse("[content unavailable]");
        } catch (IOException e) {
            return hash + " -> [content unavailable]";
        }
    }

    private OptionEntry selectedOption() {
        int index = optionsList.getSelectedIndex();
        if (index < 0 || index >= visibleOptions.size()) {
            return null;
        }
        return visibleOptions.get(index);
    }

    private record OptionEntry(
        String name,
        String label,
        String help,
        boolean requiresArguments
    ) {
        String display() {
            if (requiresArguments) {
                if ("record-note".equals(name)) {
                    return name + " <text>";
                }
                return name + " <accountId> <taskId>";
            }
            return label + " (" + name + ")";
        }

        boolean matches(String query) {
            if (requiresArguments && query.startsWith(name.toLowerCase())) {
                return true;
            }
            return name.toLowerCase().contains(query)
                || label.toLowerCase().contains(query)
                || help.toLowerCase().contains(query);
        }
    }
}

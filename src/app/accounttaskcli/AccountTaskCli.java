package app.accounttaskcli;

import accounttask.domain.AccountTaskAction;
import accounttask.domain.AccountTaskActionAdapter;
import accounttask.domain.AccountTaskDomainKernel;
import accounttask.domain.AccountTaskEvent;
import accounttask.domain.AccountTaskRuleState;
import accounttask.domain.AccountTaskState;
import accounttask.domain.NoteRecordedForAccountTask;
import accounttask.domain.WorkItemOpenedForAccount;
import app.accounttaskgui.AccountTaskGui;
import integration.eventchain.AccountTaskEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;

public final class AccountTaskCli {

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

    public static void main(String[] args) throws Exception {
        new AccountTaskCli().run(args);
    }

    private void run(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        if ("--gui".equals(args[0])) {
            Path historyPath = args.length >= 2 ? Path.of(args[1]) : DEFAULT_HISTORY_PATH;
            AccountTaskGui.main(new String[] {historyPath.toString()});
            return;
        }

        String command = args[0];
        Path historyPath = historyPathFor(args, command);

        switch (command) {
            case "open" -> open(args, historyPath);
            case "show" -> show(historyPath);
            case "next" -> next(historyPath);
            case "history" -> history(historyPath);
            case "apply" -> apply(args, historyPath);
            case "record-note" -> recordNote(args, historyPath);
            case "notes" -> notes(historyPath);
            default -> printUsage();
        }
    }

    private void open(String[] args, Path historyPath) throws IOException {
        if (args.length < 3) {
            printUsage();
            return;
        }

        if (!loadDecoded(historyPath).isEmpty()) {
            throw new IllegalStateException("Account-task history already exists in this file.");
        }

        AccountTaskEvent event = new WorkItemOpenedForAccount(args[1], args[2]);
        historyFile.append(historyPath, event);
        printSnapshot(historyPath, "appended=" + event);
    }

    private void show(Path historyPath) throws IOException {
        var snapshot = requireSnapshot(historyPath);
        System.out.println("state=" + snapshot.state());
        System.out.println("nextMoves=" + snapshot.nextMoves());
    }

    private void next(Path historyPath) throws IOException {
        var snapshot = requireSnapshot(historyPath);
        System.out.println("actions=" + snapshot.actions());
    }

    private void history(Path historyPath) throws IOException {
        List<VerifiedFieldEvent> verified = historyFile.load(historyPath);
        List<AccountTaskEvent> decoded = decoder.decode(verified);
        System.out.println("verified=" + verified);
        System.out.println("decoded=" + decoded);
    }

    private void recordNote(String[] args, Path historyPath) throws IOException {
        if (args.length < 2) {
            printUsage();
            return;
        }

        var snapshot = requireSnapshot(historyPath);
        String noteText = args[1];
        String hash = contentStore.put(noteText);
        AccountTaskEvent event = new NoteRecordedForAccountTask(snapshot.state().accountId(), snapshot.state().taskId(), hash);
        historyFile.append(historyPath, event);
        printSnapshot(historyPath, "appended=" + event);
    }

    private void notes(Path historyPath) throws IOException {
        var snapshot = requireSnapshot(historyPath);
        if (snapshot.state().noteHashes().isEmpty()) {
            System.out.println("notes=[]");
            return;
        }
        for (int i = 0; i < snapshot.state().noteHashes().size(); i++) {
            String hash = snapshot.state().noteHashes().get(i);
            String content = contentStore.get(hash).orElse("[content unavailable]");
            System.out.println((i + 1) + ". " + hash + " -> " + content);
        }
    }

    private void apply(String[] args, Path historyPath) throws IOException {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String actionName = args[1];
        var snapshot = requireSnapshot(historyPath);
        AccountTaskEvent event = actionAdapter.toEvent(snapshot.state(), snapshot.actions(), actionName);
        historyFile.append(historyPath, event);
        printSnapshot(historyPath, "appended=" + event);
    }

    private void printSnapshot(Path historyPath, String prefix) throws IOException {
        var snapshot = requireSnapshot(historyPath);
        System.out.println(prefix);
        System.out.println("state=" + snapshot.state());
        System.out.println("nextMoves=" + snapshot.nextMoves());
        System.out.println("actions=" + snapshot.actions());
    }

    private semantic.snapshot.SemanticSnapshot<AccountTaskState, NextMove<AccountTaskRuleState>, AccountTaskAction>
    requireSnapshot(Path historyPath) throws IOException {
        List<AccountTaskEvent> decoded = loadDecoded(historyPath);
        if (decoded.isEmpty()) {
            throw new IllegalStateException("No verified account-task history.");
        }
        return kernel().analyze(decoded);
    }

    private List<AccountTaskEvent> loadDecoded(Path historyPath) throws IOException {
        List<VerifiedFieldEvent> verified = historyFile.load(historyPath);
        return decoder.decode(verified);
    }

    private SemanticKernel<AccountTaskState, AccountTaskEvent, NextMove<AccountTaskRuleState>, AccountTaskAction>
    kernel() {
        return AccountTaskDomainKernel.create();
    }

    private Path historyPathFor(String[] args, String command) {
        return switch (command) {
            case "open" -> args.length >= 4 ? Path.of(args[3]) : DEFAULT_HISTORY_PATH;
            case "apply" -> args.length >= 3 ? Path.of(args[2]) : DEFAULT_HISTORY_PATH;
            case "record-note" -> args.length >= 3 ? Path.of(args[2]) : DEFAULT_HISTORY_PATH;
            case "show", "next", "history" -> args.length >= 2 ? Path.of(args[1]) : DEFAULT_HISTORY_PATH;
            case "notes" -> args.length >= 2 ? Path.of(args[1]) : DEFAULT_HISTORY_PATH;
            default -> DEFAULT_HISTORY_PATH;
        };
    }

    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  --gui [history-file]");
        System.out.println("  open <account-id> <task-id> [history-file]");
        System.out.println("  show [history-file]");
        System.out.println("  next [history-file]");
        System.out.println("  history [history-file]");
        System.out.println("  apply <action> [history-file]");
        System.out.println("  record-note <text> [history-file]");
        System.out.println("  notes [history-file]");
    }
}

package document.domain;

import java.util.List;
import semantic.kernel.ActionAdapter;
import semantic.kernel.ActionDescriptor;
import semantic.kernel.InputField;
import semantic.rules.NextMove;

public final class DocumentActionAdapter
    implements ActionAdapter<DocumentState, NextMove<DocumentRuleState>, DocumentAction, DocumentEvent> {

    @Override
    public List<DocumentAction> fromMoves(List<NextMove<DocumentRuleState>> moves) {
        return moves.stream()
            .map(move -> new DocumentAction(move.eventName(), move.resultingState()))
            .toList();
    }

    @Override
    public List<ActionDescriptor> describe(List<DocumentAction> actions) {
        return actions.stream()
            .map(this::describe)
            .toList();
    }

    @Override
    public DocumentEvent toEvent(DocumentState state, List<DocumentAction> actions, Object request) {
        if (!(request instanceof DocumentCommand command)) {
            throw new IllegalArgumentException("request must be a DocumentCommand");
        }

        actions.stream()
            .filter(action -> action.eventName().equals(command.actionName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("unsupported document action: " + command.actionName()));

        String documentId = required(command.documentId(), "documentId");
        return switch (command.actionName()) {
            case "add_section" -> new DocumentSectionAdded(
                documentId,
                required(command.sectionId(), "sectionId"),
                required(command.sectionName(), "sectionName"),
                required(command.position(), "position"),
                required(command.textRef(), "textRef")
            );
            case "edit_text" -> new DocumentTextEdited(
                documentId,
                required(command.sectionId(), "sectionId"),
                required(command.textRef(), "textRef")
            );
            case "set_collapsed" -> new DocumentSectionCollapsed(
                documentId,
                required(command.sectionId(), "sectionId"),
                required(command.collapsed(), "collapsed")
            );
            case "reorder_section" -> new DocumentSectionReordered(
                documentId,
                required(command.sectionId(), "sectionId"),
                required(command.targetIndex(), "targetIndex")
            );
            default -> throw new IllegalArgumentException("unsupported document action: " + command.actionName());
        };
    }

    private ActionDescriptor describe(DocumentAction action) {
        return switch (action.eventName()) {
            case "add_section" -> new ActionDescriptor(
                "add_section",
                "Add Section",
                List.of(
                    new InputField("sectionId", "string", true),
                    new InputField("sectionName", "string", true),
                    new InputField("position", "int", true),
                    new InputField("textRef", "string", true)
                ),
                "Adds a named section to the document."
            );
            case "edit_text" -> new ActionDescriptor(
                "edit_text",
                "Edit Section Text",
                List.of(
                    new InputField("sectionId", "string", true),
                    new InputField("textRef", "string", true)
                ),
                "Replaces the text reference for a section."
            );
            case "set_collapsed" -> new ActionDescriptor(
                "set_collapsed",
                "Set Section Collapsed",
                List.of(
                    new InputField("sectionId", "string", true),
                    new InputField("collapsed", "boolean", true)
                ),
                "Changes whether a section is collapsed."
            );
            case "reorder_section" -> new ActionDescriptor(
                "reorder_section",
                "Reorder Section",
                List.of(
                    new InputField("sectionId", "string", true),
                    new InputField("targetIndex", "int", true)
                ),
                "Moves a section to a new index."
            );
            default -> throw new IllegalArgumentException("unsupported document action: " + action.eventName());
        };
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static int required(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private static boolean required(Boolean value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }
}

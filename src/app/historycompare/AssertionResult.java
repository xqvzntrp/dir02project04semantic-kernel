package app.historycompare;

public record AssertionResult(
    boolean pass,
    String message
) {
    public AssertionResult {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}

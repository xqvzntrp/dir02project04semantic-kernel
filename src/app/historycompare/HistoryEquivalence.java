package app.historycompare;

public enum HistoryEquivalence {
    EXACT,
    SEMANTICALLY_EQUAL,
    STATE_EQUAL_ONLY,
    ACTIONS_EQUAL_ONLY,
    DIFFERENT
}

package app.historycompare;

public enum HistoryEquivalence {
    EXACT(true, true),
    STATE_AND_ACTIONS_EQUAL(true, true),
    STATE_EQUAL_ONLY(true, false),
    ACTIONS_EQUAL_ONLY(false, true),
    NONE(false, false);

    private final boolean stateEqual;
    private final boolean actionsEqual;

    HistoryEquivalence(boolean stateEqual, boolean actionsEqual) {
        this.stateEqual = stateEqual;
        this.actionsEqual = actionsEqual;
    }

    public boolean preservesState() {
        return stateEqual;
    }

    public boolean preservesActions() {
        return actionsEqual;
    }

    public boolean isSemanticConvergence() {
        return this == EXACT || this == STATE_AND_ACTIONS_EQUAL;
    }

    public boolean isOperationallyEquivalent() {
        return preservesActions();
    }
}

package app.laws;

import app.historycompare.AssertionResult;

public interface Law {
    String name();
    AssertionResult run() throws Exception;

    default boolean expectedNegative() {
        return false;
    }
}

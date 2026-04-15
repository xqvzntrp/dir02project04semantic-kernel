package app.laws;

import java.util.List;

public final class LawRunner {
    private LawRunner() {
    }

    public static void runAll(List<Law> laws) {
        int failures = 0;

        for (Law law : laws) {
            System.out.println("Running: " + law.name());
            try {
                var result = law.run();
                if (result.pass()) {
                    if (law.expectedNegative()) {
                        System.out.println("PASS (expected negative): " + result.message());
                    } else {
                        System.out.println("PASS: " + result.message());
                    }
                } else {
                    System.out.println("FAIL: " + result.message());
                    failures++;
                }
            } catch (Exception e) {
                String message = e.getMessage();
                System.out.println("ERROR: " + (message == null || message.isBlank() ? e.getClass().getSimpleName() : message));
                failures++;
            }
            System.out.println();
        }

        if (failures == 0) {
            System.out.println("All laws passed");
            System.exit(0);
        }
        System.exit(1);
    }
}

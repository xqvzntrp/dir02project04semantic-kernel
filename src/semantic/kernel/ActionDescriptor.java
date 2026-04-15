package semantic.kernel;

import java.util.List;

public record ActionDescriptor(
    String name,
    String label,
    List<InputField> inputs,
    String help
) {
}

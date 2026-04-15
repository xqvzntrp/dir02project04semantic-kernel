package semantic.kernel;

public record InputField(
    String name,
    String type,
    boolean required
) {
}

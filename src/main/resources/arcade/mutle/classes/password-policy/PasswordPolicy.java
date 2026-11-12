/** Validates passwords against a minimum length and two character requirements. */
public final class PasswordPolicy {
    private final int minimumLength;

    public PasswordPolicy(int minimumLength) {
        if (minimumLength < 4 || minimumLength > 30) {
            throw new IllegalArgumentException("minimum length must be between 4 and 30");
        }
        this.minimumLength = minimumLength;
    }

    public boolean accepts(String password) {
        if (password == null || password.length() < minimumLength) {
            return false;
        }
        boolean hasDigit = false;
        boolean hasUppercase = false;
        for (int index = 0; index < password.length(); index++) {
            char character = password.charAt(index);
            hasDigit |= Character.isDigit(character);
            hasUppercase |= Character.isUpperCase(character);
        }
        return hasDigit && hasUppercase;
    }

    public int minimumLength() {
        return minimumLength;
    }
}

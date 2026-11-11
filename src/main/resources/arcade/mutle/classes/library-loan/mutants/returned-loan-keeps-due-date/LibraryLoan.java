/** Tracks one library book loan, its due period, and bounded renewals. */
public final class LibraryLoan {
    private final String title;
    private final int loanDays;
    private final int maxRenewals;
    private boolean checkedOut;
    private int dueDay;
    private int renewalsUsed;
    public LibraryLoan(String title, int loanDays, int maxRenewals) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (loanDays <= 0 || maxRenewals < 0) {
            throw new IllegalArgumentException("loan settings are invalid");
        }
        this.title = title;
        this.loanDays = loanDays;
        this.maxRenewals = maxRenewals;
    }
    public boolean checkout() {
        if (checkedOut) {
            return false;
        }
        checkedOut = true;
        dueDay = loanDays;
        renewalsUsed = 0;
        return true;
    }
    public boolean renew() {
        if (!checkedOut || renewalsUsed >= maxRenewals) {
            return false;
        }
        dueDay += loanDays;
        renewalsUsed++;
        return true;
    }
    public boolean returnBook() {
        if (!checkedOut) {
            return false;
        }
        checkedOut = false;
        ;
        renewalsUsed = 0;
        return true;
    }
    public boolean isCheckedOut() {
        return checkedOut;
    }
    public int daysRemaining() {
        return dueDay;
    }
    public int renewalsRemaining() {
        return maxRenewals - renewalsUsed;
    }
    public String title() {
        return title;
    }
}

import java.io.Serializable;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

public class User implements Serializable {
    private static final long serialVersionUID = 4L;

    public String email, password, username;
    public boolean isAdmin;
    public Map<String, LocalDate> issuedBooks = new HashMap<>();
    public List<String> borrowingHistory = new ArrayList<>();
    public List<Message> inbox = new ArrayList<>();
    public double finePaid = 0.0;
    public int maxBorrowLimit = 3;

    public User(String email, String password) { this(email, password, false); }

    public User(String email, String password, boolean isAdmin) {
        this.email = email;
        this.password = hashPassword(password);
        this.isAdmin = isAdmin;
        this.username = email.split("@")[0];
    }

    public User(String email, String password, String username, boolean isAdmin) {
        this.email = email;
        this.password = hashPassword(password);
        this.username = (username != null && !username.isBlank()) ? username : email.split("@")[0];
        this.isAdmin = isAdmin;
    }

    public static String hashPassword(String plain) {
        if (plain == null) return "";
        if (plain.matches("[0-9a-f]{64}")) return plain;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plain.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return plain; }
    }

    public boolean checkPassword(String plain) {
        return password != null && password.equals(hashPassword(plain));
    }

    public String getDisplayName() {
        return (username != null && !username.isBlank()) ? username : email;
    }

    public void issueBook(String isbn, String title) {
        issuedBooks.put(isbn, LocalDate.now().plusDays(LibraryConfig.LOAN_DAYS));
    }

    public void returnBook(String isbn, String title) {
        issuedBooks.remove(isbn);
        borrowingHistory.add(isbn + "|" + title + "|" + LocalDate.now());
        if (borrowingHistory.size() > 50) borrowingHistory.remove(0);
    }

    public double calculateFine() {
        LocalDate today = LocalDate.now();
        return issuedBooks.values().stream()
            .filter(today::isAfter)
            .mapToLong(d -> today.toEpochDay() - d.toEpochDay())
            .sum() * LibraryConfig.FINE_PER_DAY;
    }

    public void payFine(double amount) { finePaid += amount; }

    public int unreadCount() {
        if (inbox == null) inbox = new ArrayList<>();
        return (int) inbox.stream().filter(m -> !m.isRead()).count();
    }

    public boolean canBorrowMore() {
        return issuedBooks.size() < maxBorrowLimit;
    }
}

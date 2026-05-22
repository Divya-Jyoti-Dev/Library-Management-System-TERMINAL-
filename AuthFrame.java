import java.util.HashMap;
import java.util.Scanner;

/**
 * AuthFrame — replaces TerminalAuth.java.
 * Handles first-run admin setup, login, and registration in the terminal.
 */
public class AuthFrame {

    private final HashMap<String, User> userDb;
    private final Scanner sc;

    public AuthFrame(HashMap<String, User> userDb, Scanner sc) {
        this.userDb = userDb;
        this.sc = sc;
    }

    /**
     * Returns the logged-in User, or null if the user chose to exit.
     */
    public User authenticate() {
        if (userDb.isEmpty()) {
            return adminSetup();
        }
        return loginMenu();
    }

    // ── First-run admin setup ─────────────────────────────────────────────────
    private User adminSetup() {
        UITheme.printPlainHeader("CREATE ADMIN ACCOUNT");
        UITheme.info("No users found. Please create the administrator account.");
        System.out.println();

        while (true) {
            UITheme.prompt("Username");
            String username = sc.nextLine().trim();
            if (username.length() < 3) { UITheme.error("Username must be at least 3 characters."); continue; }

            UITheme.prompt("Email");
            String email = sc.nextLine().trim();
            if (!email.contains("@")) { UITheme.error("Enter a valid email."); continue; }

            UITheme.prompt("Password (min 6 chars)");
            String pass = sc.nextLine().trim();
            if (pass.length() < 6) { UITheme.error("Password must be at least 6 characters."); continue; }

            UITheme.prompt("Confirm Password");
            String confirm = sc.nextLine().trim();
            if (!pass.equals(confirm)) { UITheme.error("Passwords do not match."); continue; }

            User admin = new User(email, pass, username, true);
            userDb.put(email, admin);
            UITheme.success("Admin account created! Welcome, " + username + ".");
            System.out.println();
            return admin;
        }
    }

    // ── Login / Register menu ─────────────────────────────────────────────────
    private User loginMenu() {
        while (true) {
            UITheme.printPlainHeader("LIBRARY MANAGEMENT SYSTEM");
            UITheme.menuItem(1, "Sign In");
            UITheme.menuItem(2, "Register");
            UITheme.menuItem(0, "Exit");
            System.out.println();
            UITheme.prompt("Choice");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    User u = doLogin();
                    if (u != null) return u;
                }
                case "2" -> doRegister();
                case "0" -> {
                    UITheme.info("Goodbye!");
                    System.exit(0);
                }
                default  -> UITheme.error("Invalid choice.");
            }
            System.out.println();
        }
    }

    private User doLogin() {
        System.out.println();
        UITheme.printPlainHeader("SIGN IN");
        UITheme.prompt("Email");
        String email = sc.nextLine().trim();
        UITheme.prompt("Password");
        String pass = sc.nextLine().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            UITheme.error("Please fill in all fields.");
            return null;
        }
        if (pass.length() < 6) {
            UITheme.error("Password must be at least 6 characters.");
            return null;
        }

        User u = userDb.get(email);
        if (u != null && (u.checkPassword(pass) || u.password.equals(pass))) {
            // Migrate plain-text password to hashed on first login
            if (!u.password.matches("[0-9a-f]{64}")) {
                u.password = User.hashPassword(pass);
            }
            UITheme.success("Welcome back, " + u.getDisplayName() + "!");
            System.out.println();
            return u;
        }

        UITheme.error("Invalid email or password.");
        return null;
    }

    private void doRegister() {
        System.out.println();
        UITheme.printPlainHeader("REGISTER");

        UITheme.prompt("Username (min 3 chars)");
        String username = sc.nextLine().trim();
        if (username.length() < 3) { UITheme.error("Username must be at least 3 characters."); return; }

        boolean takenName = userDb.values().stream().anyMatch(u -> username.equalsIgnoreCase(u.username));
        if (takenName) { UITheme.error("Username already taken."); return; }

        UITheme.prompt("Email");
        String email = sc.nextLine().trim();
        if (!email.contains("@")) { UITheme.error("Enter a valid email."); return; }
        if (userDb.containsKey(email)) { UITheme.error("Email already registered."); return; }

        UITheme.prompt("Password (min 6 chars)");
        String pass = sc.nextLine().trim();
        if (pass.length() < 6) { UITheme.error("Password must be at least 6 characters."); return; }

        UITheme.prompt("Confirm Password");
        String confirm = sc.nextLine().trim();
        if (!pass.equals(confirm)) { UITheme.error("Passwords do not match."); return; }

        User newUser = new User(email, pass, username, false);
        newUser.maxBorrowLimit = LibraryConfig.DEFAULT_BORROW_LIMIT;
        userDb.put(email, newUser);
        UITheme.success("Registered successfully! Please sign in.");
    }
}

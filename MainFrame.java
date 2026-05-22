import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MainFrame — replaces Terminal.java.
 * Drives the entire terminal UI after login.
 */
public class MainFrame {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private final Scanner sc = new Scanner(System.in);

    private HashMap<String, User> userDb;
    private Library library;
    private Runnable saveAll;
    private User currentUser;

    public void start(HashMap<String, User> userDb, Library library, Runnable saveAll) {
        this.userDb  = userDb;
        this.library = library;
        this.saveAll = saveAll;

        while (true) {
            AuthFrame auth = new AuthFrame(userDb, sc);
            currentUser = auth.authenticate();
            if (currentUser == null) break;

            sendAutoRemindersOnStartup();
            showReservationNotices();
            mainLoop();
        }
    }

    // ── Main menu loop ────────────────────────────────────────────────────────
    private void mainLoop() {
        while (true) {
            printMainMenu();
            UITheme.prompt("Choice");
            String choice = sc.nextLine().trim().toLowerCase();

            boolean logout = false;
            if (currentUser.isAdmin) {
                logout = handleAdminChoice(choice);
            } else {
                logout = handleMemberChoice(choice);
            }
            if (logout) break;
        }
    }

    private void printMainMenu() {
        System.out.println();
        String role = currentUser.isAdmin ? "ADMINISTRATOR" : "MEMBER";
        UITheme.printHeader("LIBRARY MANAGEMENT SYSTEM  [" + role + ": " + currentUser.getDisplayName() + "]");
        printDashboard();
        UITheme.printThinLine();
        System.out.println();

        if (currentUser.isAdmin) {
            UITheme.menuItem(1, "Book Catalog");
            UITheme.menuItem(2, "Categories");
            UITheme.menuItem(3, "Members");
            UITheme.menuItem(4, "Reminders");
            UITheme.menuItem(5, "Reports");
            UITheme.menuItem(6, "Settings");
        } else {
            UITheme.menuItem(1, "Book Catalog");
            UITheme.menuItem(2, "My Account");
            UITheme.menuItem(3, "Messages" + unreadBadge());
        }
        System.out.println();
        UITheme.menuItem("s", "Save");
        UITheme.menuItem("0", "Sign Out");
        System.out.println();
    }

    private boolean handleAdminChoice(String choice) {
        return switch (choice) {
            case "1" -> { catalogMenu(); yield false; }
            case "2" -> { categoriesMenu(); yield false; }
            case "3" -> { membersMenu(); yield false; }
            case "4" -> { remindersMenu(); yield false; }
            case "5" -> { reportsMenu(); yield false; }
            case "6" -> { settingsMenu(); yield false; }
            case "s" -> { saveAll.run(); UITheme.success("Saved."); yield false; }
            case "0" -> { confirmAndSave(); yield true; }
            default  -> { UITheme.error("Invalid choice."); yield false; }
        };
    }

    private boolean handleMemberChoice(String choice) {
        return switch (choice) {
            case "1" -> { catalogMenu(); yield false; }
            case "2" -> { myAccountMenu(); yield false; }
            case "3" -> { messagesMenu(); yield false; }
            case "s" -> { saveAll.run(); UITheme.success("Saved."); yield false; }
            case "0" -> { confirmAndSave(); yield true; }
            default  -> { UITheme.error("Invalid choice."); yield false; }
        };
    }

    private void confirmAndSave() {
        UITheme.promptInline("Sign out? (y/n): ");
        if (sc.nextLine().trim().equalsIgnoreCase("y")) {
            saveAll.run();
            UITheme.success("Saved. Goodbye, " + currentUser.getDisplayName() + "!");
        }
    }

    // ── Dashboard summary ─────────────────────────────────────────────────────
    private void printDashboard() {
        int totalBooks = library.getTotalBooks();
        String borrowed;
        String fineStr;

        if (currentUser.isAdmin) {
            int total = userDb.values().stream().filter(u -> !u.isAdmin).mapToInt(u -> u.issuedBooks.size()).sum();
            double totalFine = userDb.values().stream().filter(u -> !u.isAdmin).mapToDouble(User::calculateFine).sum();
            borrowed = String.valueOf(total);
            fineStr  = totalFine > 0 ? String.format("৳ %.0f", totalFine) : "No dues";
        } else {
            borrowed = String.valueOf(currentUser.issuedBooks.size());
            double fine = currentUser.calculateFine();
            fineStr = fine > 0 ? String.format("৳ %.0f", fine) : "No dues";
        }

        System.out.printf("  %s%-18s%s  %s%-18s%s  %s%-18s%s%n",
            UITheme.BOLD, "Collection", UITheme.RESET,
            UITheme.BOLD, "Borrowed", UITheme.RESET,
            UITheme.BOLD, "Fine", UITheme.RESET);
        System.out.printf("  %-18s  %-18s  %-18s%n",
            UITheme.CYAN + totalBooks + UITheme.RESET,
            UITheme.CYAN + borrowed + UITheme.RESET,
            (currentUser.calculateFine() > 0 ? UITheme.RED : UITheme.GREEN) + fineStr + UITheme.RESET);
    }

    private String unreadBadge() {
        int n = currentUser.unreadCount();
        return n > 0 ? UITheme.RED + " (" + n + " unread)" + UITheme.RESET : "";
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CATALOG
    // ──────────────────────────────────────────────────────────────────────────
    private void catalogMenu() {
        while (true) {
            System.out.println();
            UITheme.printPlainHeader("BOOK CATALOG");
            listBooks(new ArrayList<>(library.getAllBooks()));
            System.out.println();

            if (currentUser.isAdmin) {
                UITheme.menuItem(1, "Add Book");
                UITheme.menuItem(2, "Edit Book (by ISBN)");
                UITheme.menuItem(3, "Delete Book (by ISBN)");
                UITheme.menuItem(4, "View Book Details");
                UITheme.menuItem(5, "Search Books");
            } else {
                UITheme.menuItem(1, "Borrow Book");
                UITheme.menuItem(2, "Return Book");
                UITheme.menuItem(3, "Renew Book");
                UITheme.menuItem(4, "View Book Details");
                UITheme.menuItem(5, "Search Books");
            }
            UITheme.menuItem(0, "Back");
            System.out.println();
            UITheme.prompt("Choice");
            String choice = sc.nextLine().trim();

            if (currentUser.isAdmin) {
                switch (choice) {
                    case "1" -> addBook();
                    case "2" -> editBook();
                    case "3" -> deleteBook();
                    case "4" -> viewBookDetail();
                    case "5" -> searchBooks();
                    case "0" -> { return; }
                    default  -> UITheme.error("Invalid choice.");
                }
            } else {
                switch (choice) {
                    case "1" -> borrowBook();
                    case "2" -> returnBook();
                    case "3" -> renewBook();
                    case "4" -> viewBookDetail();
                    case "5" -> searchBooks();
                    case "0" -> { return; }
                    default  -> UITheme.error("Invalid choice.");
                }
            }
        }
    }

    private void listBooks(List<Book> books) {
        books.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
        System.out.printf("  %-4s %-32s %-20s %-12s %-8s %s%n",
            "#", "Title", "Author", "ISBN", "Copies", "Status");
        UITheme.printThinLine();
        int i = 1;
        for (Book b : books) {
            System.out.printf("  %-4d %-32s %-20s %-12s %-8s %s%n",
                i++,
                shorten(b.getTitle(), 31),
                shorten(b.getAuthor(), 19),
                b.getIsbn(),
                b.getAvailableCopies() + "/" + b.getTotalCopies(),
                UITheme.statusTag(b.getStatusTag()));
        }
        if (books.isEmpty()) UITheme.info("No books found.");
    }

    private void addBook() {
        System.out.println();
        UITheme.printPlainHeader("ADD BOOK");
        UITheme.prompt("Title");        String title  = sc.nextLine().trim();
        UITheme.prompt("Author");       String author = sc.nextLine().trim();
        UITheme.prompt("ISBN");         String isbn   = sc.nextLine().trim();
        UITheme.prompt("Copies");
        int copies;
        try { copies = Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { UITheme.error("Copies must be a number."); return; }
        UITheme.prompt("Category (e.g. Fiction, Science, Academic)");
        String cat = sc.nextLine().trim();
        if (cat.isEmpty()) cat = "General";
        UITheme.prompt("Publisher (optional)");
        String publisher = sc.nextLine().trim();
        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
            UITheme.error("Title, Author, and ISBN are required.");
            return;
        }
        library.addBook(new Book(title, author, isbn, copies, cat, publisher));
        saveAll.run();
        UITheme.success("Book added: " + title);
    }

    private void editBook() {
        System.out.println();
        UITheme.printPlainHeader("EDIT BOOK");
        UITheme.prompt("Enter ISBN of book to edit");
        String isbn = sc.nextLine().trim();
        Book b = library.getBook(isbn);
        if (b == null) { UITheme.error("Book not found."); return; }

        System.out.println();
        UITheme.info("Leave blank to keep current value.");
        UITheme.promptInline("Title [" + b.getTitle() + "]: ");
        String title  = sc.nextLine().trim(); if (title.isEmpty())  title  = b.getTitle();
        UITheme.promptInline("Author [" + b.getAuthor() + "]: ");
        String author = sc.nextLine().trim(); if (author.isEmpty()) author = b.getAuthor();
        UITheme.promptInline("ISBN [" + b.getIsbn() + "]: ");
        String newIsbn = sc.nextLine().trim(); if (newIsbn.isEmpty()) newIsbn = b.getIsbn();
        UITheme.promptInline("Copies [" + b.getTotalCopies() + "]: ");
        String copStr  = sc.nextLine().trim();
        int copies;
        try { copies = copStr.isEmpty() ? b.getTotalCopies() : Integer.parseInt(copStr); }
        catch (NumberFormatException e) { UITheme.error("Copies must be a number."); return; }
        UITheme.promptInline("Category [" + b.getCategory() + "]: ");
        String cat = sc.nextLine().trim(); if (cat.isEmpty()) cat = b.getCategory();
        UITheme.promptInline("Publisher [" + b.getPublisher() + "]: ");
        String publisher = sc.nextLine().trim(); if (publisher.isEmpty()) publisher = b.getPublisher();

        library.updateBook(isbn, title, author, newIsbn, copies, cat, null, publisher);
        saveAll.run();
        UITheme.success("Book updated.");
    }

    private void deleteBook() {
        System.out.println();
        UITheme.printPlainHeader("DELETE BOOK");
        UITheme.prompt("Enter ISBN to delete");
        String isbn = sc.nextLine().trim();
        Book b = library.getBook(isbn);
        if (b == null) { UITheme.error("Book not found."); return; }

        UITheme.promptInline("Delete \"" + b.getTitle() + "\"? (y/n): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) { UITheme.info("Cancelled."); return; }

        library.removeBook(isbn);
        saveAll.run();
        UITheme.success("Book deleted.");
    }

    private void viewBookDetail() {
        System.out.println();
        UITheme.prompt("Enter ISBN");
        String isbn = sc.nextLine().trim();
        Book b = library.getBook(isbn);
        if (b == null) { UITheme.error("Book not found."); return; }

        System.out.println();
        UITheme.printPlainHeader("BOOK DETAILS");
        UITheme.print(UITheme.BOLD + "Title:     " + UITheme.RESET + b.getTitle());
        UITheme.print(UITheme.BOLD + "Author:    " + UITheme.RESET + b.getAuthor());
        UITheme.print(UITheme.BOLD + "ISBN:      " + UITheme.RESET + b.getIsbn());
        UITheme.print(UITheme.BOLD + "Publisher: " + UITheme.RESET + (b.getPublisher().isEmpty() ? "—" : b.getPublisher()));
        UITheme.print(UITheme.BOLD + "Category:  " + UITheme.RESET + b.getCategory());
        UITheme.print(UITheme.BOLD + "Copies:    " + UITheme.RESET + b.getAvailableCopies() + " / " + b.getTotalCopies());
        UITheme.print(UITheme.BOLD + "Status:    " + UITheme.RESET + UITheme.statusTag(b.getStatusTag()));
        UITheme.print(UITheme.BOLD + "Loan:      " + UITheme.RESET + LibraryConfig.LOAN_DAYS + " days");
        List<String> queue = b.getReservedBy();
        UITheme.print(UITheme.BOLD + "Queue:    " + UITheme.RESET + (queue.isEmpty() ? "None" : queue.size() + " waiting"));
        if (currentUser.isAdmin && !queue.isEmpty()) {
            UITheme.print("  Queue members: " + String.join(", ", queue));
        }
        System.out.println();
        UITheme.promptInline("Press Enter to continue...");
        sc.nextLine();
    }

    private void searchBooks() {
        System.out.println();
        UITheme.prompt("Search (title / author / ISBN)");
        String q = sc.nextLine().trim();
        List<Book> results = library.searchByTitle(q);
        if (results.isEmpty()) results = library.searchByAuthor(q);
        Book exact = library.getBook(q);
        if (exact != null && !results.contains(exact)) results.add(0, exact);
        System.out.println();
        if (results.isEmpty()) { UITheme.warn("No books found for: " + q); }
        else { listBooks(results); }
        System.out.println();
        UITheme.promptInline("Press Enter to continue...");
        sc.nextLine();
    }

    // ── Borrow / Return / Renew ───────────────────────────────────────────────
    private void borrowBook() {
        System.out.println();
        UITheme.prompt("Enter ISBN to borrow");
        String isbn = sc.nextLine().trim();
        Book b = library.getBook(isbn);
        if (b == null) { UITheme.error("Book not found."); return; }

        if (!currentUser.canBorrowMore()) {
            UITheme.error("Borrow limit reached (" + currentUser.maxBorrowLimit + "). Return a book first.");
            return;
        }
        if (currentUser.issuedBooks.containsKey(isbn)) {
            UITheme.error("You already have this book.");
            return;
        }

        List<String> queue = b.getReservedBy();
        boolean userIsFirst = !queue.isEmpty() && queue.get(0).equals(currentUser.email);
        boolean inQueue     = queue.contains(currentUser.email);

        if (inQueue && !userIsFirst) {
            UITheme.warn("You are #" + (queue.indexOf(currentUser.email) + 1) + " in queue.");
            return;
        }
        if (!userIsFirst && !queue.isEmpty()) {
            UITheme.warn("\"" + b.getTitle() + "\" is reserved by others.");
            offerReservation(b, queue.size() + 1);
            return;
        }
        if (library.issueBook(isbn)) {
            library.cancelReservation(isbn, currentUser.email);
            currentUser.issueBook(isbn, b.getTitle());
            saveAll.run();
            System.out.println();
            UITheme.printPlainHeader("BORROWED SUCCESSFULLY");
            UITheme.print("Book:  " + b.getTitle());
            UITheme.print("Due:   " + LocalDate.now().plusDays(LibraryConfig.LOAN_DAYS).format(FMT));
            UITheme.print("Stock: " + b.getAvailableCopies() + " / " + b.getTotalCopies());
        } else {
            UITheme.warn("\"" + b.getTitle() + "\" is out of stock.");
            offerReservation(b, queue.size() + 1);
        }
    }

    private void offerReservation(Book b, int pos) {
        UITheme.promptInline("Reserve it? Queue position #" + pos + " (y/n): ");
        if (sc.nextLine().trim().equalsIgnoreCase("y")) {
            library.reserveBook(b.getIsbn(), currentUser.email);
            if (currentUser.inbox == null) currentUser.inbox = new ArrayList<>();
            currentUser.inbox.add(new Message("SYSTEM", currentUser.email,
                "Reservation Confirmed: \"" + b.getTitle() + "\"",
                "You have been added to the waitlist for \"" + b.getTitle() + "\".\nQueue position: #" + pos,
                Message.Type.AUTO_DUE_REMINDER));
            saveAll.run();
            UITheme.success("Reserved! You are #" + pos + " in queue.");
        }
    }

    private void returnBook() {
        System.out.println();
        if (currentUser.issuedBooks.isEmpty()) {
            UITheme.error("You have no borrowed books.");
            return;
        }
        UITheme.printPlainHeader("RETURN BOOK");
        UITheme.info("Your borrowed books:");
        for (Map.Entry<String, LocalDate> e : currentUser.issuedBooks.entrySet()) {
            Book bk = library.getBook(e.getKey());
            UITheme.print("  ISBN: " + e.getKey() + "  \"" + (bk != null ? bk.getTitle() : e.getKey()) + "\"  Due: " + e.getValue().format(FMT));
        }
        System.out.println();
        UITheme.prompt("Enter ISBN to return");
        String isbn = sc.nextLine().trim();

        if (!currentUser.issuedBooks.containsKey(isbn)) {
            UITheme.error("You haven't borrowed that book.");
            return;
        }
        Book b = library.getBook(isbn);
        if (b == null) { UITheme.error("Book not found."); return; }

        library.returnBook(isbn);
        currentUser.returnBook(isbn, b.getTitle());

        // Notify first in reservation queue
        String nextEmail = library.notifyFirstReservation(isbn);
        if (nextEmail != null) {
            User waiting = userDb.get(nextEmail);
            if (waiting != null) {
                if (waiting.inbox == null) waiting.inbox = new ArrayList<>();
                waiting.inbox.add(new Message("SYSTEM", nextEmail,
                    "Book Available: \"" + b.getTitle() + "\"",
                    "A copy of \"" + b.getTitle() + "\" is now available. You are #1 in queue.",
                    Message.Type.AUTO_DUE_REMINDER));
            }
        }
        saveAll.run();
        System.out.println();
        UITheme.printPlainHeader("RETURNED SUCCESSFULLY");
        UITheme.print("Book:  " + b.getTitle());
        UITheme.print("Date:  " + LocalDate.now().format(FMT));
        UITheme.print("Stock: " + b.getAvailableCopies() + " / " + b.getTotalCopies());
        if (nextEmail != null) UITheme.info("Notified " + nextEmail + " that the book is available.");
    }

    private void renewBook() {
        System.out.println();
        if (currentUser.issuedBooks.isEmpty()) {
            UITheme.error("You have no borrowed books to renew.");
            return;
        }
        UITheme.printPlainHeader("RENEW BOOK");
        UITheme.info("Your borrowed books:");
        for (Map.Entry<String, LocalDate> e : currentUser.issuedBooks.entrySet()) {
            Book bk = library.getBook(e.getKey());
            UITheme.print("  ISBN: " + e.getKey() + "  \"" + (bk != null ? bk.getTitle() : e.getKey()) + "\"  Due: " + e.getValue().format(FMT));
        }
        System.out.println();
        UITheme.prompt("Enter ISBN to renew");
        String isbn = sc.nextLine().trim();
        if (!currentUser.issuedBooks.containsKey(isbn)) {
            UITheme.error("You haven't borrowed that book.");
            return;
        }
        Book b = library.getBook(isbn);
        if (b == null) { UITheme.error("Book not found."); return; }
        if (!b.getReservedBy().isEmpty()) {
            UITheme.error("Cannot renew — others are waiting for \"" + b.getTitle() + "\".");
            return;
        }
        LocalDate newDue = LocalDate.now().plusDays(LibraryConfig.LOAN_DAYS);
        currentUser.issuedBooks.put(isbn, newDue);
        saveAll.run();
        UITheme.success("Renewed! New due date: " + newDue.format(FMT));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CATEGORIES
    // ──────────────────────────────────────────────────────────────────────────
    private void categoriesMenu() {
        System.out.println();
        UITheme.printPlainHeader("BOOK CATEGORIES");

        Map<String, List<Book>> catMap = new LinkedHashMap<>();
        for (Book b : library.getAllBooks())
            catMap.computeIfAbsent(b.getCategory(), k -> new ArrayList<>()).add(b);

        System.out.printf("  %s%-20s %-12s %-12s %-12s%s%n",
            UITheme.BOLD, "Category", "Total Books", "Available", "Borrowed", UITheme.RESET);
        UITheme.printThinLine();
        for (Map.Entry<String, List<Book>> e : catMap.entrySet()) {
            int avail  = e.getValue().stream().mapToInt(Book::getAvailableCopies).sum();
            int total  = e.getValue().stream().mapToInt(Book::getTotalCopies).sum();
            System.out.printf("  %-20s %-12d %-12d %-12d%n",
                shorten(e.getKey(), 19), e.getValue().size(), avail, total - avail);
        }

        System.out.println();
        UITheme.prompt("Enter category name to browse its books (or Enter to go back)");
        String cat = sc.nextLine().trim();
        if (!cat.isEmpty() && catMap.containsKey(cat)) {
            System.out.println();
            UITheme.printPlainHeader("CATEGORY: " + cat);
            listBooks(catMap.get(cat));
            System.out.println();
            UITheme.promptInline("Press Enter to continue...");
            sc.nextLine();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // MEMBERS (admin)
    // ──────────────────────────────────────────────────────────────────────────
    private void membersMenu() {
        while (true) {
            System.out.println();
            UITheme.printPlainHeader("LIBRARY MEMBERS");
            System.out.printf("  %s%-18s %-28s %-10s %-8s %-10s%s%n",
                UITheme.BOLD, "Username", "Email", "Borrowed", "Limit", "Fine (৳)", UITheme.RESET);
            UITheme.printThinLine();
            for (User u : userDb.values()) {
                if (u.isAdmin) continue;
                System.out.printf("  %-18s %-28s %-10d %-8d %-10.0f%n",
                    shorten(u.getDisplayName(), 17), shorten(u.email, 27),
                    u.issuedBooks.size(), u.maxBorrowLimit, u.calculateFine());
            }
            System.out.println();
            UITheme.menuItem(1, "View Member Details");
            UITheme.menuItem(0, "Back");
            System.out.println();
            UITheme.prompt("Choice");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> viewMemberDetails();
                case "0" -> { return; }
                default  -> UITheme.error("Invalid choice.");
            }
        }
    }

    private void viewMemberDetails() {
        UITheme.prompt("Enter member email");
        String email = sc.nextLine().trim();
        User u = userDb.get(email);
        if (u == null || u.isAdmin) { UITheme.error("Member not found."); return; }

        System.out.println();
        UITheme.printPlainHeader("MEMBER: " + u.getDisplayName());
        LocalDate today = LocalDate.now();

        System.out.printf("  %s%-28s %-14s %-10s %-12s%s%n",
            UITheme.BOLD, "Title", "ISBN", "Due Date", "Status", UITheme.RESET);
        UITheme.printThinLine();
        for (Map.Entry<String, LocalDate> e : u.issuedBooks.entrySet()) {
            Book b = library.getBook(e.getKey());
            long days = e.getValue().toEpochDay() - today.toEpochDay();
            System.out.printf("  %-28s %-14s %-10s %s%n",
                shorten(b != null ? b.getTitle() : e.getKey(), 27),
                e.getKey(), e.getValue().format(FMT),
                UITheme.dueStatus(days));
        }
        if (u.issuedBooks.isEmpty()) UITheme.info("No books currently borrowed.");

        double fine = u.calculateFine();
        System.out.println();
        if (fine > 0)
            UITheme.warn(String.format("Outstanding fine: ৳ %.0f", fine));
        else
            UITheme.success("No outstanding fine.");

        System.out.println();
        UITheme.promptInline("Press Enter to continue...");
        sc.nextLine();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // REMINDERS (admin)
    // ──────────────────────────────────────────────────────────────────────────
    private void remindersMenu() {
        while (true) {
            System.out.println();
            UITheme.printPlainHeader("REMINDERS & MESSAGES");
            LocalDate today = LocalDate.now();

            System.out.printf("  %-24s %-28s %-12s %-10s %-12s%n",
                "User", "Book Title", "ISBN", "Due Date", "Status");
            UITheme.printThinLine();

            boolean any = false;
            for (User u : userDb.values()) {
                if (u.isAdmin || u.issuedBooks == null || u.issuedBooks.isEmpty()) continue;
                for (Map.Entry<String, LocalDate> e : u.issuedBooks.entrySet()) {
                    Book b = library.getBook(e.getKey());
                    long days = e.getValue().toEpochDay() - today.toEpochDay();
                    System.out.printf("  %-24s %-28s %-12s %-10s %s%n",
                        shorten(u.email, 23),
                        shorten(b != null ? b.getTitle() : e.getKey(), 27),
                        e.getKey(), e.getValue().format(FMT),
                        UITheme.dueStatus(days));
                    any = true;
                }
            }
            if (!any) UITheme.info("No books currently borrowed.");

            System.out.println();
            UITheme.menuItem(1, "Auto-Remind (near due / overdue)");
            UITheme.menuItem(2, "Send Manual Reminder");
            UITheme.menuItem(0, "Back");
            System.out.println();
            UITheme.prompt("Choice");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> sendAutoReminders();
                case "2" -> sendManualReminder();
                case "0" -> { return; }
                default  -> UITheme.error("Invalid choice.");
            }
        }
    }

    private void sendAutoReminders() {
        LocalDate today = LocalDate.now();
        int sent = 0;
        for (User u : userDb.values()) {
            if (u.isAdmin || u.issuedBooks == null) continue;
            if (u.inbox == null) u.inbox = new ArrayList<>();
            for (Map.Entry<String, LocalDate> e : u.issuedBooks.entrySet()) {
                long days = e.getValue().toEpochDay() - today.toEpochDay();
                if (days > 3) continue;
                Book b = library.getBook(e.getKey());
                String title = b != null ? b.getTitle() : e.getKey();
                u.inbox.add(new Message("SYSTEM", u.email,
                    ReminderHelper.subject(title, days),
                    ReminderHelper.body(u.email, title, e.getValue(), days, "Regards,\nLibraryOS Admin"),
                    Message.Type.AUTO_DUE_REMINDER));
                sent++;
            }
        }
        saveAll.run();
        if (sent > 0)
            UITheme.success(sent + " auto-reminder(s) sent.");
        else
            UITheme.info("No near-due or overdue books found.");
    }

    private void sendManualReminder() {
        System.out.println();
        UITheme.printPlainHeader("SEND MANUAL REMINDER");
        UITheme.prompt("Recipient email");
        String toEmail = sc.nextLine().trim();
        User target = userDb.get(toEmail);
        if (target == null || target.isAdmin) { UITheme.error("Member not found."); return; }

        // Pre-filled subject — press Enter to accept, or type to replace
        String defaultSubject = "Library Reminder: Please Return / Renew Your Book";
        UITheme.promptInline("Subject [" + defaultSubject + "]: ");
        String subject = sc.nextLine().trim();
        if (subject.isEmpty()) subject = defaultSubject;

        // Pre-filled body — press Enter to accept, or type a custom message
        String defaultBody = "Dear " + target.getDisplayName() + ",\n\n"
            + "This is a reminder from the Library Management System.\n"
            + "Please return or renew your borrowed book(s) before the due date to avoid fines.\n\n"
            + "If you have any questions, feel free to contact the library.\n\n"
            + "Regards,\nLibrary Administration";
        System.out.println();
        UITheme.info("Default message (press Enter to use, or type your own):");
        for (String line : defaultBody.split("\\n")) System.out.println("    " + line);
        System.out.println();
        UITheme.prompt("Custom message body (or Enter to use default)");
        String body = sc.nextLine().trim();
        if (body.isEmpty()) body = defaultBody.replace("\\n", "\n");

        if (subject.isEmpty() || body.isEmpty()) { UITheme.error("Subject and message cannot be empty."); return; }

        if (target.inbox == null) target.inbox = new ArrayList<>();
        target.inbox.add(new Message(currentUser.email, toEmail, subject, body, Message.Type.MANUAL_REMINDER));
        saveAll.run();
        UITheme.success("Message sent to " + toEmail);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // REPORTS (admin)
    // ──────────────────────────────────────────────────────────────────────────
    private void reportsMenu() {
        while (true) {
            System.out.println();
            UITheme.printPlainHeader("REPORTS");
            UITheme.menuItem(1, "Overdue Books");
            UITheme.menuItem(2, "Fine Summary");
            UITheme.menuItem(3, "Most Borrowed");
            UITheme.menuItem(4, "Member Activity");
            UITheme.menuItem(0, "Back");
            System.out.println();
            UITheme.prompt("Choice");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> reportOverdue();
                case "2" -> reportFines();
                case "3" -> reportPopular();
                case "4" -> reportMemberActivity();
                case "0" -> { return; }
                default  -> UITheme.error("Invalid choice.");
            }
        }
    }

    private void reportOverdue() {
        System.out.println();
        UITheme.printPlainHeader("OVERDUE BOOKS");
        LocalDate today = LocalDate.now();
        int count = 0;
        System.out.printf("  %s%-24s %-28s %-12s %-12s %-10s%s%n",
            UITheme.BOLD, "Member Email", "Book Title", "Due Date", "Days Overdue", "Fine (৳)", UITheme.RESET);
        UITheme.printThinLine();
        for (User u : userDb.values()) {
            if (u.isAdmin || u.issuedBooks == null) continue;
            for (Map.Entry<String, LocalDate> e : u.issuedBooks.entrySet()) {
                long days = today.toEpochDay() - e.getValue().toEpochDay();
                if (days <= 0) continue;
                Book b = library.getBook(e.getKey());
                System.out.printf("  %-24s %-28s %-12s %-12d %-10.0f%n",
                    shorten(u.email, 23),
                    shorten(b != null ? b.getTitle() : e.getKey(), 27),
                    e.getValue().format(FMT), days, days * LibraryConfig.FINE_PER_DAY);
                count++;
            }
        }
        if (count == 0) UITheme.success("No overdue books.");
        else System.out.println();
        UITheme.warn("Total overdue: " + count + " book(s)");
        System.out.println();
        UITheme.promptInline("Press Enter to continue...");
        sc.nextLine();
    }

    private void reportFines() {
        System.out.println();
        UITheme.printPlainHeader("FINE SUMMARY");
        System.out.printf("  %s%-18s %-28s %-18s %-14s%s%n",
            UITheme.BOLD, "Member", "Email", "Outstanding Fine (৳)", "Total Paid (৳)", UITheme.RESET);
        UITheme.printThinLine();
        double totalOut = 0;
        for (User u : userDb.values()) {
            if (u.isAdmin) continue;
            double fine = u.calculateFine();
            totalOut += fine;
            if (fine > 0 || u.finePaid > 0) {
                System.out.printf("  %-18s %-28s %-18.0f %-14.0f%n",
                    shorten(u.getDisplayName(), 17), shorten(u.email, 27), fine, u.finePaid);
            }
        }
        System.out.println();
        UITheme.warn(String.format("Total outstanding: ৳ %.0f", totalOut));
        System.out.println();
        UITheme.promptInline("Press Enter to continue...");
        sc.nextLine();
    }

    private void reportPopular() {
        System.out.println();
        UITheme.printPlainHeader("MOST BORROWED BOOKS");
        Map<String, Long> counts = new HashMap<>();
        for (User u : userDb.values()) {
            if (u.borrowingHistory == null) continue;
            for (String entry : u.borrowingHistory) {
                String isbn = entry.split("\\|")[0];
                counts.merge(isbn, 1L, Long::sum);
            }
        }
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Map.Entry.<String, Long>comparingByValue().reversed());

        System.out.printf("  %s%-4s %-28s %-20s %-12s %-12s%s%n",
            UITheme.BOLD, "#", "Title", "Author", "ISBN", "Times Borrowed", UITheme.RESET);
        UITheme.printThinLine();
        int rank = 1;
        for (Map.Entry<String, Long> e : sorted) {
            Book b = library.getBook(e.getKey());
            System.out.printf("  %-4d %-28s %-20s %-12s %-12d%n",
                rank++,
                shorten(b != null ? b.getTitle()  : e.getKey(), 27),
                shorten(b != null ? b.getAuthor() : "—", 19),
                e.getKey(), e.getValue());
        }
        if (sorted.isEmpty()) UITheme.info("No borrowing history yet.");
        System.out.println();
        UITheme.promptInline("Press Enter to continue...");
        sc.nextLine();
    }

    private void reportMemberActivity() {
        System.out.println();
        UITheme.printPlainHeader("MEMBER ACTIVITY");
        System.out.printf("  %s%-18s %-28s %-10s %-10s %-8s %-10s%s%n",
            UITheme.BOLD, "Member", "Email", "Current", "Total", "Limit", "Fine (৳)", UITheme.RESET);
        UITheme.printThinLine();
        for (User u : userDb.values()) {
            if (u.isAdmin) continue;
            int hist = u.borrowingHistory != null ? u.borrowingHistory.size() : 0;
            System.out.printf("  %-18s %-28s %-10d %-10d %-8d %-10.0f%n",
                shorten(u.getDisplayName(), 17), shorten(u.email, 27),
                u.issuedBooks.size(), hist, u.maxBorrowLimit, u.calculateFine());
        }
        System.out.println();
        UITheme.promptInline("Press Enter to continue...");
        sc.nextLine();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // SETTINGS (admin)
    // ──────────────────────────────────────────────────────────────────────────
    private void settingsMenu() {
        while (true) {
            System.out.println();
            UITheme.printPlainHeader("SETTINGS");
            UITheme.print("Current Global Settings:");
            UITheme.print("  Loan Period:    " + LibraryConfig.LOAN_DAYS + " days");
            UITheme.print("  Fine Per Day:   ৳ " + (int) LibraryConfig.FINE_PER_DAY);
            UITheme.print("  Borrow Limit:   " + LibraryConfig.DEFAULT_BORROW_LIMIT + " books");
            System.out.println();
            UITheme.menuItem(1, "Edit Global Settings");
            UITheme.menuItem(2, "Set Member Borrow Limit");
            UITheme.menuItem(3, "Mark Member Fine as Paid");
            UITheme.menuItem(0, "Back");
            System.out.println();
            UITheme.prompt("Choice");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> editGlobalSettings();
                case "2" -> setMemberBorrowLimit();
                case "3" -> markFinePaid();
                case "0" -> { return; }
                default  -> UITheme.error("Invalid choice.");
            }
        }
    }

    private void editGlobalSettings() {
        System.out.println();
        UITheme.printPlainHeader("EDIT GLOBAL SETTINGS");
        UITheme.info("Leave blank to keep current value.");
        try {
            UITheme.promptInline("Loan Period (days) [" + LibraryConfig.LOAN_DAYS + "]: ");
            String ld = sc.nextLine().trim();
            UITheme.promptInline("Fine Per Day (৳) [" + (int)LibraryConfig.FINE_PER_DAY + "]: ");
            String fp = sc.nextLine().trim();
            UITheme.promptInline("Default Borrow Limit [" + LibraryConfig.DEFAULT_BORROW_LIMIT + "]: ");
            String bl = sc.nextLine().trim();

            if (!ld.isEmpty()) LibraryConfig.LOAN_DAYS            = Integer.parseInt(ld);
            if (!fp.isEmpty()) LibraryConfig.FINE_PER_DAY         = Integer.parseInt(fp);
            if (!bl.isEmpty()) LibraryConfig.DEFAULT_BORROW_LIMIT = Integer.parseInt(bl);

            LibraryConfig.save();
            saveAll.run();
            UITheme.success("Settings saved.");
        } catch (NumberFormatException e) {
            UITheme.error("Please enter valid numbers.");
        }
    }

    private void setMemberBorrowLimit() {
        UITheme.prompt("Member email");
        String email = sc.nextLine().trim();
        User u = userDb.get(email);
        if (u == null || u.isAdmin) { UITheme.error("Member not found."); return; }
        UITheme.promptInline("New borrow limit [current: " + u.maxBorrowLimit + "]: ");
        String input = sc.nextLine().trim();
        try {
            int lim = Integer.parseInt(input);
            if (lim < 1) { UITheme.error("Limit must be at least 1."); return; }
            u.maxBorrowLimit = lim;
            saveAll.run();
            UITheme.success("Borrow limit updated for " + u.getDisplayName());
        } catch (NumberFormatException e) {
            UITheme.error("Enter a valid number.");
        }
    }

    private void markFinePaid() {
        UITheme.prompt("Member email");
        String email = sc.nextLine().trim();
        User u = userDb.get(email);
        if (u == null || u.isAdmin) { UITheme.error("Member not found."); return; }
        double fine = u.calculateFine();
        if (fine <= 0) { UITheme.info(u.getDisplayName() + " has no outstanding fine."); return; }
        UITheme.promptInline(String.format("Mark ৳%.0f as paid for %s? (y/n): ", fine, u.getDisplayName()));
        if (sc.nextLine().trim().equalsIgnoreCase("y")) {
            u.payFine(fine);
            saveAll.run();
            UITheme.success(String.format("৳%.0f marked as paid.", fine));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // MY ACCOUNT (member)
    // ──────────────────────────────────────────────────────────────────────────
    private void myAccountMenu() {
        System.out.println();
        UITheme.printPlainHeader("MY ACCOUNT — " + currentUser.getDisplayName());
        LocalDate today = LocalDate.now();

        // Borrowed
        UITheme.print(UITheme.BOLD + "Currently Borrowed:" + UITheme.RESET);
        System.out.printf("  %s%-28s %-12s %-12s %s%s%n",
            UITheme.BOLD, "Title", "ISBN", "Due Date", "Status", UITheme.RESET);
        UITheme.printThinLine();
        for (Map.Entry<String, LocalDate> e : currentUser.issuedBooks.entrySet()) {
            Book b = library.getBook(e.getKey());
            long days = e.getValue().toEpochDay() - today.toEpochDay();
            System.out.printf("  %-28s %-12s %-12s %s%n",
                shorten(b != null ? b.getTitle() : e.getKey(), 27),
                e.getKey(), e.getValue().format(FMT),
                UITheme.dueStatus(days));
        }
        if (currentUser.issuedBooks.isEmpty()) UITheme.info("No books currently borrowed.");

        double fine = currentUser.calculateFine();
        System.out.println();
        if (fine > 0) UITheme.warn(String.format("Outstanding fine: ৳ %.0f", fine));
        else          UITheme.success("No outstanding fine.");

        // Reservations
        System.out.println();
        UITheme.print(UITheme.BOLD + "My Reservations:" + UITheme.RESET);
        boolean hasRes = false;
        for (Book b : library.getAllBooks()) {
            int pos = b.getReservedBy().indexOf(currentUser.email);
            if (pos >= 0) {
                System.out.printf("  %-28s %-12s Queue #%d%n",
                    shorten(b.getTitle(), 27), b.getIsbn(), pos + 1);
                hasRes = true;
            }
        }
        if (!hasRes) UITheme.info("No reservations.");

        // Borrowing history
        System.out.println();
        UITheme.print(UITheme.BOLD + "Borrowing History (last 10):" + UITheme.RESET);
        List<String> hist = currentUser.borrowingHistory;
        if (hist == null || hist.isEmpty()) {
            UITheme.info("No borrowing history.");
        } else {
            int start = Math.max(0, hist.size() - 10);
            for (int i = hist.size() - 1; i >= start; i--) {
                String[] parts = hist.get(i).split("\\|");
                if (parts.length == 3)
                    System.out.printf("  %-28s %-12s Returned: %s%n",
                        shorten(parts[1], 27), parts[0], parts[2]);
            }
        }

        System.out.println();
        UITheme.promptInline("Press Enter to continue...");
        sc.nextLine();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // MESSAGES (member)
    // ──────────────────────────────────────────────────────────────────────────
    private void messagesMenu() {
        while (true) {
            System.out.println();
            UITheme.printPlainHeader("MY MESSAGES" + unreadBadge());

            if (currentUser.inbox == null) currentUser.inbox = new ArrayList<>();
            List<Message> msgs = currentUser.inbox;

            if (msgs.isEmpty()) {
                UITheme.info("No messages.");
            } else {
                System.out.printf("  %s%-4s %-3s %-36s %-20s %-24s%s%n",
                    UITheme.BOLD, "#", "✉", "Subject", "From", "Date", UITheme.RESET);
                UITheme.printThinLine();
                for (int i = msgs.size() - 1; i >= 0; i--) {
                    Message m = msgs.get(i);
                    String dot = !m.isRead() ? UITheme.RED + "●" + UITheme.RESET : " ";
                    System.out.printf("  %-4d %s  %-36s %-20s %-24s%n",
                        msgs.size() - i, dot,
                        shorten(m.getSubject(), 35),
                        shorten(m.getFromEmail(), 19),
                        m.getFormattedTime());
                }
            }

            System.out.println();
            UITheme.menuItem(1, "Read a Message (enter number)");
            UITheme.menuItem(0, "Back");
            System.out.println();
            UITheme.prompt("Choice");
            String choice = sc.nextLine().trim();
            if (choice.equals("0")) return;
            if (choice.equals("1")) {
                if (msgs.isEmpty()) { UITheme.info("No messages."); continue; }
                UITheme.prompt("Message number");
                try {
                    int num = Integer.parseInt(sc.nextLine().trim());
                    int idx = msgs.size() - num;
                    if (idx < 0 || idx >= msgs.size()) { UITheme.error("Invalid number."); continue; }
                    Message m = msgs.get(idx);
                    m.markRead();
                    System.out.println();
                    UITheme.printPlainHeader("MESSAGE");
                    UITheme.print("From:    " + m.getFromEmail());
                    UITheme.print("Date:    " + m.getFormattedTime());
                    UITheme.print("Subject: " + m.getSubject());
                    System.out.println();
                    for (String line : m.getBody().split("\n"))
                        UITheme.print(line);
                    System.out.println();
                    UITheme.promptInline("Press Enter to continue...");
                    sc.nextLine();
                } catch (NumberFormatException e) {
                    UITheme.error("Enter a valid number.");
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Startup helpers
    // ──────────────────────────────────────────────────────────────────────────
    private void sendAutoRemindersOnStartup() {
        if (!currentUser.isAdmin) return;
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        for (User u : userDb.values()) {
            if (u.isAdmin || u.issuedBooks == null) continue;
            if (u.inbox == null) u.inbox = new ArrayList<>();
            for (Map.Entry<String, LocalDate> e : u.issuedBooks.entrySet()) {
                long daysLeft = e.getValue().toEpochDay() - today.toEpochDay();
                if (daysLeft > 3) continue;
                String isbn = e.getKey();
                boolean alreadySent = u.inbox.stream().anyMatch(m ->
                    m.getType() == Message.Type.AUTO_DUE_REMINDER &&
                    m.getSubject().contains(isbn) &&
                    m.getFormattedTime().startsWith(todayStr));
                if (alreadySent) continue;
                Book b = library.getBook(isbn);
                String title = b != null ? b.getTitle() : isbn;
                u.inbox.add(new Message("SYSTEM", u.email,
                    ReminderHelper.subject(title, daysLeft),
                    ReminderHelper.body(u.email, title, e.getValue(), daysLeft, "Regards,\nLibrary Management System"),
                    Message.Type.AUTO_DUE_REMINDER));
            }
        }
        saveAll.run();
    }

    private void showReservationNotices() {
        if (currentUser.isAdmin || currentUser.inbox == null) return;
        long available = currentUser.inbox.stream()
            .filter(m -> !m.isRead() && m.getSubject().startsWith("Book Available:"))
            .count();
        if (available > 0) {
            System.out.println();
            UITheme.warn(available + " book(s) you reserved are now available!");
            UITheme.info("Go to Catalog -> Borrow or check Messages.");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────────────────────────────────
    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}

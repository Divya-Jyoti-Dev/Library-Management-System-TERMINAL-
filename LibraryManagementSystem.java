import java.io.*;
import java.util.HashMap;

public class LibraryManagementSystem {
    private static final String LIBRARY_FILE = "library.dat";
    private static final String USERS_FILE   = "users.dat";

    static HashMap<String, User> userDb = new HashMap<>();
    static Library library = new Library();

    public static void main(String[] args) {
        LibraryConfig.load();
        loadUsers();
        library.loadFromFile(LIBRARY_FILE);
        if (library.getTotalBooks() == 0) seedBooks();

        MainFrame terminal = new MainFrame();
        terminal.start(userDb, library, LibraryManagementSystem::saveAll);
    }

    private static void seedBooks() {
        try (BufferedReader br = new BufferedReader(new FileReader("books.csv"))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                String[] parts = line.split(",", 6);
                if (parts.length < 5) continue;
                String title     = parts[0].trim();
                String author    = parts[1].trim();
                String isbn      = parts[2].trim();
                int    copies    = Integer.parseInt(parts[3].trim());
                String category  = parts[4].trim();
                String publisher = parts.length >= 6 ? parts[5].trim() : "";
                library.addBook(new Book(title, author, isbn, copies, category, publisher));
            }
            System.out.println("Books loaded from books.csv");
        } catch (FileNotFoundException e) {
            System.err.println("books.csv not found — no seed books loaded.");
        } catch (Exception e) {
            System.err.println("Error reading books.csv: " + e.getMessage());
        }
    }

    public static void saveAll() {
        library.saveToFile(LIBRARY_FILE);
        saveUsers();
        LibraryConfig.save();
    }

    @SuppressWarnings("unchecked")
    private static void loadUsers() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(USERS_FILE))) {
            userDb = (HashMap<String, User>) in.readObject();
        } catch (FileNotFoundException ignored) {
            userDb = new HashMap<>();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading users: " + e.getMessage());
            userDb = new HashMap<>();
        }
    }

    private static void saveUsers() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            out.writeObject(userDb);
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }
}

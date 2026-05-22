import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Library {
    private HashMap<String, Book> books = new HashMap<>();

    public void addBook(Book book)               { books.put(book.getIsbn(), book); }
    public Book getBook(String isbn)             { return books.get(isbn); }
    public boolean removeBook(String isbn)       { return books.remove(isbn) != null; }
    public Collection<Book> getAllBooks()        { return books.values(); }
    public int getTotalBooks()                   { return books.size(); }

    public void updateBook(String oldIsbn, String title, String author, String isbn, int copies, String cat) {
        updateBook(oldIsbn, title, author, isbn, copies, cat, null, null);
    }

    public void updateBook(String oldIsbn, String title, String author, String isbn, int copies, String cat, String url) {
        updateBook(oldIsbn, title, author, isbn, copies, cat, url, null);
    }

    public void updateBook(String oldIsbn, String title, String author, String isbn, int copies, String cat, String url, String publisher) {
        Book b = books.get(oldIsbn);
        if (b == null) return;
        if (!oldIsbn.equals(isbn)) { books.remove(oldIsbn); b.setIsbn(isbn); books.put(isbn, b); }
        b.setTitle(title); b.setAuthor(author); b.setTotalCopies(copies); b.setCategory(cat);
        if (publisher != null) b.setPublisher(publisher);
    }

    public boolean issueBook(String isbn) {
        Book b = books.get(isbn); return b != null && b.issueBook();
    }

    public boolean returnBook(String isbn) {
        Book b = books.get(isbn); if (b == null) return false; b.returnBook(); return true;
    }

    // FIX: Allow reservation regardless of available copies — the copy count check
    // is handled in MainFrame logic. Only block duplicate reservations.
    public boolean reserveBook(String isbn, String email) {
        Book b = books.get(isbn);
        if (b == null || b.getReservedBy().contains(email)) return false;
        b.getReservedBy().add(email);
        return true;
    }

    public boolean cancelReservation(String isbn, String email) {
        Book b = books.get(isbn); return b != null && b.getReservedBy().remove(email);
    }

    // Returns email of first in queue WITHOUT removing them (they must borrow to be removed)
    public String notifyFirstReservation(String isbn) {
        Book b = books.get(isbn);
        return (b == null || b.getReservedBy().isEmpty()) ? null : b.getReservedBy().get(0);
    }

    public List<Book> searchByTitle(String q) {
        return books.values().stream()
            .filter(b -> b.getTitle().toLowerCase().contains(q.toLowerCase()))
            .collect(Collectors.toList());
    }

    public List<Book> searchByAuthor(String q) {
        return books.values().stream()
            .filter(b -> b.getAuthor().toLowerCase().contains(q.toLowerCase()))
            .collect(Collectors.toList());
    }

    public int getTotalIssuedCopies() {
        return books.values().stream().mapToInt(b -> b.getTotalCopies() - b.getAvailableCopies()).sum();
    }

    public String getMostPopularCategory() {
        return books.values().stream()
            .collect(Collectors.groupingBy(Book::getCategory, Collectors.counting()))
            .entrySet().stream().max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse("N/A");
    }

    public void saveToFile(String file) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(books);
        } catch (IOException e) { System.out.println("Error saving library: " + e.getMessage()); }
    }

    @SuppressWarnings("unchecked")
    public void loadFromFile(String file) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            books = (HashMap<String, Book>) in.readObject();
        } catch (FileNotFoundException ignored) {
        } catch (IOException | ClassNotFoundException e) { System.out.println("Error loading library: " + e.getMessage()); }
    }
}

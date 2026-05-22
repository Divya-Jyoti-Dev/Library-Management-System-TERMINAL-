import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title, author, isbn, category, publisher;
    private int totalCopies, availableCopies;
    private List<String> reservedBy = new ArrayList<>();

    public Book(String title, String author, String isbn, int copies) {
        this(title, author, isbn, copies, "General", "");
    }

    public Book(String title, String author, String isbn, int copies, String category) {
        this(title, author, isbn, copies, category, "");
    }

    public Book(String title, String author, String isbn, int copies, String category, String publisher) {
        this.title = title; this.author = author; this.isbn = isbn;
        this.totalCopies = this.availableCopies = copies;
        this.category = category;
        this.publisher = (publisher == null) ? "" : publisher;
    }

    // Getters
    public String getTitle()             { return title; }
    public String getAuthor()            { return author; }
    public String getIsbn()              { return isbn; }
    public String getCategory()          { return category; }
    public String getPublisher()         { return publisher == null ? "" : publisher; }
    public int getAvailableCopies()      { return availableCopies; }
    public int getTotalCopies()          { return totalCopies; }
    public List<String> getReservedBy()  { if (reservedBy == null) reservedBy = new ArrayList<>(); return reservedBy; }

    // Setters
    public void setTitle(String v)     { title = v; }
    public void setAuthor(String v)    { author = v; }
    public void setIsbn(String v)      { isbn = v; }
    public void setCategory(String v)  { category = v; }
    public void setPublisher(String v) { publisher = (v == null) ? "" : v; }

    public void setTotalCopies(int n) {
        availableCopies += (n - totalCopies);
        totalCopies = n;
        if (availableCopies < 0) availableCopies = 0;
    }

    public boolean issueBook() {
        if (availableCopies <= 0) return false;
        availableCopies--; return true;
    }

    public void returnBook() { if (availableCopies < totalCopies) availableCopies++; }

    public String getStatusTag() {
        if (availableCopies == 0) return "OUT OF STOCK";
        if (availableCopies <= 2) return "LOW STOCK";
        return "AVAILABLE";
    }

    @Override
    public String toString() {
        return "Title: " + title + ", Author: " + author +
               ", ISBN: " + isbn + ", Publisher: " + getPublisher() +
               ", Available: " + availableCopies + "/" + totalCopies;
    }
}

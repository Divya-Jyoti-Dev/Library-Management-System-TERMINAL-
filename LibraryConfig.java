import java.io.*;

/**
 * Global configuration — loan period, fine rate, borrow limit.
 * Admin can change these at runtime from the Settings panel.
 * Values are persisted to config.dat.
 */
public class LibraryConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String CONFIG_FILE = "config.dat";

    public static int    LOAN_DAYS      = 14;   // default 14-day loan
    public static double FINE_PER_DAY   = 5.0;  // BDT per overdue day
    public static int    DEFAULT_BORROW_LIMIT = 3; // books per member

    public static void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(CONFIG_FILE))) {
            int[]    ints  = { LOAN_DAYS, DEFAULT_BORROW_LIMIT };
            double[] dbls  = { FINE_PER_DAY };
            out.writeObject(ints);
            out.writeObject(dbls);
        } catch (IOException e) { System.err.println("Config save error: " + e.getMessage()); }
    }

    public static void load() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(CONFIG_FILE))) {
            int[]    ints = (int[])    in.readObject();
            double[] dbls = (double[]) in.readObject();
            LOAN_DAYS             = ints[0];
            DEFAULT_BORROW_LIMIT  = ints[1];
            FINE_PER_DAY          = dbls[0];
        } catch (FileNotFoundException ignored) {
        } catch (Exception e) { System.err.println("Config load error: " + e.getMessage()); }
    }
}

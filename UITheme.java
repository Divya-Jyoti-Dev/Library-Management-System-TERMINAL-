/**
 * UITheme — helper methods for clean terminal output.
 * No colors, no box borders. Plain dashes only.
 */
public class UITheme {

    public static final String RESET  = "";
    public static final String BOLD   = "";
    public static final String DIM    = "";
    public static final String GREEN  = "";
    public static final String RED    = "";
    public static final String YELLOW = "";
    public static final String CYAN   = "";
    public static final String BLUE   = "";
    public static final String WHITE  = "";

    private static final int WIDTH = 92;

    /** Main dashboard header — plain dashes, no +---+ box */
    public static void printHeader(String title) {
        printThinLine();
        int pad = (WIDTH - title.length() - 2) / 2;
        String spaces = " ".repeat(Math.max(pad, 0));
        System.out.println(" " + spaces + title + spaces);
        printThinLine();
    }

    /** Section header for auth screens and sub-menus */
    public static void printPlainHeader(String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("-".repeat(WIDTH));
    }

    /** Plain dashes line */
    public static void printThinLine() {
        System.out.println("-".repeat(WIDTH));
    }

    /** Kept for source compatibility — same as printThinLine */
    public static void printBoxLine() {
        printThinLine();
    }

    public static void success(String msg) {
        System.out.println("  [OK] " + msg);
    }

    public static void error(String msg) {
        System.out.println("  [ERROR] " + msg);
    }

    public static void warn(String msg) {
        System.out.println("  [!] " + msg);
    }

    public static void info(String msg) {
        System.out.println("  [i] " + msg);
    }

    public static void print(String msg) {
        System.out.println("  " + msg);
    }

    public static void menuItem(int num, String label) {
        System.out.printf("  [%d] %s%n", num, label);
    }

    public static void menuItem(String key, String label) {
        System.out.printf("  [%s] %s%n", key, label);
    }

    public static void prompt(String msg) {
        System.out.print("  > " + msg + ": ");
    }

    public static void promptInline(String msg) {
        System.out.print("  > " + msg);
    }

    public static String statusTag(String tag) {
        return tag;
    }

    public static String dueStatus(long days) {
        if (days < 0)  return "OVERDUE (" + Math.abs(days) + "d)";
        if (days == 0) return "DUE TODAY";
        if (days <= 3) return "DUE SOON";
        return "On time";
    }

    public static void tableRow(String... cols) {
        System.out.println("  " + String.join("  |  ", cols));
    }

    public static String padRight(String s, int n) {
        if (s == null) s = "";
        return String.format("%-" + n + "s", s.length() > n ? s.substring(0, n - 1) + "..." : s);
    }

    private static String stripAnsi(String s) {
        return s == null ? "" : s.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}

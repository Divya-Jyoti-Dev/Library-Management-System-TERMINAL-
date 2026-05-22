import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReminderHelper {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public static String subject(String title, long days) {
        if (days < 0)  return "⚠ Overdue Book: \"" + title + "\"";
        if (days == 0) return "📅 Book Due Today: \"" + title + "\"";
        return "🔔 Return Reminder: \"" + title + "\"";
    }

    public static String body(String email, String title, LocalDate due, long days, String sign) {
        String greeting = "Dear " + email + ",\n\n";
        if (days < 0)
            return greeting + "Your borrowed book \"" + title + "\" was due on " + due.format(FMT) +
                   " and is now " + Math.abs(days) + " day(s) overdue.\n\nA fine of ৳" +
                   String.format("%.0f", Math.abs(days) * 5.0) + " has accumulated. Please return it soon.\n\n" + sign;
        if (days == 0)
            return greeting + "Your borrowed book \"" + title + "\" is due for return TODAY (" +
                   due.format(FMT) + ").\n\nPlease return it to avoid any fines.\n\n" + sign;
        return greeting + "Friendly reminder: \"" + title + "\" is due in " + days + " day(s) on " +
               due.format(FMT) + ".\n\nPlease return it on time to avoid a fine.\n\n" + sign;
    }
}

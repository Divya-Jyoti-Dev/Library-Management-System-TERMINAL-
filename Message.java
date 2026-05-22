import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { MANUAL_REMINDER, AUTO_DUE_REMINDER }

    private final String fromEmail, toEmail, subject, body;
    private final LocalDateTime sentAt = LocalDateTime.now();
    private final Type type;
    private boolean read;

    public Message(String from, String to, String subject, String body, Type type) {
        this.fromEmail = from; this.toEmail = to;
        this.subject = subject; this.body = body; this.type = type;
    }

    public String getFromEmail()  { return fromEmail; }
    public String getToEmail()    { return toEmail; }
    public String getSubject()    { return subject; }
    public String getBody()       { return body; }
    public Type getType()         { return type; }
    public boolean isRead()       { return read; }
    public void markRead()        { read = true; }
    public LocalDateTime getSentAt() { return sentAt; }

    public String getFormattedTime() {
        return sentAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a"));
    }
}

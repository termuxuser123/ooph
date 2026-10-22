package service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import dao.WaitlistDAO;
import dao.impl.WaitlistDAOImpl;
import model.User;
import model.WaitlistEntry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class WaitlistManager {
    private final WaitlistDAO waitlistDAO = new WaitlistDAOImpl();

    // add user to the waitlist for a book (default title)
    public boolean addToWaitlist(String isbn, User user) {
        return addToWaitlist(isbn, null, user);
    }

    // add user to the waitlist with title, without duplicate-check loop
    public boolean addToWaitlist(String isbn, String title, User user) {
        try {
            waitlistDAO.add(isbn, title, user);
            System.out.println("[WAITLIST] " + user.getName() + " added to waitlist for ISBN: "
                    + isbn + " (Position: " + waitlistDAO.size(isbn) + ")");
            return true;
        } catch (SQLException e) {
            System.out.println("[WAITLIST ERROR] Could not update waitlist: " + e.getMessage());
            return false;
        }
    }

    // notify waiting users, one per free copy (copies already reserved for NOTIFIED users don't count)
    public List<User> notifyNextuser(String isbn) {
        List<User> notified = new ArrayList<>();
        try {
            int free = waitlistDAO.availableCopies(isbn) - waitlistDAO.notifiedCount(isbn);
            if (free <= 0) return notified;
            String title = waitlistDAO.getBookTitle(isbn);
            while (free-- > 0) {
                User nextUser = waitlistDAO.popNext(isbn);
                if (nextUser == null) break;
                NotificationManager.send(nextUser.getId(), "A copy of '" + title + "' (ISBN: " + isbn + ") is now locked and reserved for you. Please issue it from the Borrow tab.");
                notified.add(nextUser);
            }
        } catch (SQLException e) {
            System.out.println("[WAITLIST ERROR] Could not read waitlist: " + e.getMessage());
        }
        return notified;
    }

    // called when an admin adds a book or copy
    public List<User> notifyBookArrived(String isbn) {
        return notifyNextuser(isbn);
    }

    // called when a user issues the book: a NOTIFIED entry becomes FULFILLED
    public void markFulfilled(String isbn, int userId) {
        try {
            waitlistDAO.markFulfilled(isbn, userId);
        } catch (SQLException e) {
            System.out.println("[WAITLIST ERROR] Could not mark entry fulfilled: " + e.getMessage());
        }
    }

    // cancel / leave waitlist
    public void leaveWaitlist(String isbn, int userId) {
        try {
            waitlistDAO.leaveWaitlist(isbn, userId);
            System.out.println("[WAITLIST] User " + userId + " left waitlist for ISBN: " + isbn);
            notifyNextuser(isbn);   // a reserved copy may have just been freed up
        } catch (SQLException e) {
            System.out.println("[WAITLIST ERROR] Could not leave waitlist: " + e.getMessage());
        }
    }

    // get waitlist for user
    public List<WaitlistEntry> getMyWaitlist(int userId) {
        try {
            return waitlistDAO.getMyWaitlist(userId);
        } catch (SQLException e) {
            System.out.println("[WAITLIST ERROR] Could not get user waitlist: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public int getWaitlistSize(String isbn) {
        try {
            return waitlistDAO.size(isbn);
        } catch (SQLException e) {
            System.out.println("[WAITLIST ERROR] " + e.getMessage());
            return 0;
        }
    }

    public List<WaitlistEntry> getRequestedBooks() {
        try {
            return waitlistDAO.getRequestedBooks();
        } catch (SQLException e) {
            System.out.println("[WAITLIST ERROR] Could not get requested books: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean isUserEligibleToIssue(String isbn, int userId) {
        try {
            return waitlistDAO.isUserEligibleToIssue(isbn, userId);
        } catch (SQLException e) {
            System.out.println("[WAITLIST ERROR] Could not verify reservation lock: " + e.getMessage());
            return true;
        }
    }

    private static String normalize(String s) {
        return (s == null) ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    // placeholder "ISBN" for a requested book that isn't in the library yet
    public static String requestKey(String title, String author) {
        String norm = normalize(title) + "|" + normalize(author);
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(norm.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("REQ");
            for (int i = 0; i < 5; i++) sb.append(String.format("%02x", h[i]));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // called when an admin adds a NEW book: move matching requests to the real ISBN, then notify
    public List<User> notifyBookArrived(String isbn, String title, String author) {
        try {
            // Match both exact title+author key and title-based requests
            waitlistDAO.reassignIsbn(requestKey(title, author), isbn);
            waitlistDAO.reassignByTitle(title, isbn);
        } catch (SQLException e) {
            System.out.println("[WAITLIST ERROR] Could not match book requests: " + e.getMessage());
        }
        return notifyNextuser(isbn);
    }
}
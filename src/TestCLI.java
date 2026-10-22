import java.sql.SQLException;
import dao.UserDAO;
import dao.impl.UserDAOImpl;
import model.*;
import service.Library;

// backend console test runner
public class TestCLI {
    public static void main(String[] args) throws SQLException {
        Library library = new Library();
        UserDAO userDAO = new UserDAOImpl();

        // unique values so the test can be re-run
        long stamp = System.currentTimeMillis();
        String isbn = "TEST-" + stamp;
        String refIsbn = "REF-" + stamp;
        String emailA = "a" + stamp + "@test.com";
        String emailB = "b" + stamp + "@test.com";

        // register two students, then load them back so they carry the DB id
        userDAO.insert(new Student("Test A", emailA, 0, "9876543210", "pass123"));
        userDAO.insert(new Student("Test B", emailB, 0, "9876543211", "pass123"));
        User a = userDAO.authenticate(emailA, "pass123");
        User b = userDAO.authenticate(emailB, "pass123");
        System.out.println("Users created: ids " + a.getId() + " and " + b.getId());

        // catalog: a textbook with ONE copy, and a reference book
        TextBook book = new TextBook(isbn, "Test Textbook", "Some Author", 1, 1, "x");
        ReferenceBook ref = new ReferenceBook(refIsbn, "Test Reference", "Some Author", 1, 1, "x");
        library.addItem(book);
        library.addItem(ref);

        System.out.println("\n--- 1. A borrows the textbook (expect success) ---");
        library.issue(a, book);

        System.out.println("\n--- 2. B borrows the same book (expect out of stock + waitlist) ---");
        library.issue(b, book);

        System.out.println("\n--- 3. A borrows the reference book (expect refusal) ---");
        library.issue(a, ref);

        System.out.println("\n--- 4. A borrows the same book again (expect 'already has') ---");
        library.issue(a, book);

        System.out.println("\n--- 5. A returns the textbook (expect success + B notified & locked) ---");
        library.returnItem(a, book);

        // Register user C who was not on the waitlist
        String emailC = "c" + stamp + "@test.com";
        userDAO.insert(new Student("Test C", emailC, 0, "9876543212", "pass123"));
        User c = userDAO.authenticate(emailC, "pass123");

        System.out.println("\n--- 5b. C tries to borrow the returned book (expect REFUSED / LOCKED for B) ---");
        library.issue(c, book);

        System.out.println("\n--- 5c. B (who holds the reservation lock) borrows the book (expect SUCCESS) ---");
        library.issue(b, book);

        System.out.println("\n--- 6. A returns it again (expect 'no active loan') ---");
        library.returnItem(a, book);

        System.out.println("\n" + library.generateSummary());
        System.out.println("Search 'Test': " + library.search("Test").size() + " item(s)");
    }
}

package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import model.User;
import service.Library;
import service.WaitlistManager;

public class BookSearchPanel extends JPanel {
    private JTextField searchtxt;
    private JButton searchbtn;
    private JButton showbtn, waitlistbtn, requestbtn;
    private DefaultTableModel tablem;
    private JTable bookt;
    private JScrollPane sp;
    private JLabel statuslbl;
    private Library lib;
    private User u;
    private Runnable onWaitlistChanged = () -> {};

    // load books into the table rows
    private void poptable(java.util.List<model.LibraryItem> items) {
        tablem.setRowCount(0);
        for (model.LibraryItem item : items) {
            tablem.addRow(new Object[] { item.getIsbn(), item.getTitle(), item.getAuthor(),
                    item.getAvailableCopies(), item.getTotalCopies(), item.getShelfLocation() });
        }
        statuslbl.setText("Found: " + items.size() + " books.");
    }

    // search books by title or isbn
    public void dosearchex() {
        String query = searchtxt.getText().trim();
        java.util.List<model.LibraryItem> results = lib.search(query);
        poptable(results);
    }

    public void reload() {
        dosearchex();
    }

    public BookSearchPanel() {
        this(null, new Library());
    }

    public BookSearchPanel(Library lib) {
        this(null, lib);
    }

    public BookSearchPanel(User u, Library lib, Runnable onWaitlistChanged) {
        this(u, lib);
        if (onWaitlistChanged != null) this.onWaitlistChanged = onWaitlistChanged;
    }

    public BookSearchPanel(User u, Library lib) {
        this.u = u;
        this.lib = lib;
        String[] cols = { "ISBN", "Title", "Author", "Available", "Total", "Shelf Location" };

        tablem = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        bookt = new JTable(tablem);
        sp = new JScrollPane(bookt);

        searchtxt = new JTextField(12);
        searchbtn = new JButton("Search");
        showbtn = new JButton("Show All");
        waitlistbtn = new JButton("Join Waitlist");
        waitlistbtn.setEnabled(false);
        requestbtn = new JButton("Request a Book");
        statuslbl = new JLabel("Loading books...");

        if (this.u == null) {
            // Standalone test user fallback
            this.u = new model.Student("Test Student", "student@test.com", 1, "9876543210", "pass123");
        }

        boolean isadmin = "ADMIN".equalsIgnoreCase(this.u.getRole());
        waitlistbtn.setVisible(!isadmin);
        requestbtn.setVisible(!isadmin);

        // search on button click or enter key
        searchbtn.addActionListener(e -> dosearchex());
        searchtxt.addActionListener(e -> dosearchex());
        showbtn.addActionListener(e -> {
            searchtxt.setText("");
            dosearchex();
        });

        // Enable Join Waitlist only when selected book is out of stock (available == 0)
        bookt.getSelectionModel().addListSelectionListener(e -> {
            int r = bookt.getSelectedRow();
            if (r != -1) {
                int avail = Integer.parseInt(tablem.getValueAt(r, 3).toString());
                waitlistbtn.setEnabled(avail == 0);
            } else {
                waitlistbtn.setEnabled(false);
            }
        });

        // Join Waitlist click
        waitlistbtn.addActionListener(e -> {
            int r = bookt.getSelectedRow();
            if (r == -1) return;
            String isbn = (String) tablem.getValueAt(r, 0);
            String title = (String) tablem.getValueAt(r, 1);
            int avail = Integer.parseInt(tablem.getValueAt(r, 3).toString());

            if (avail == 0) {
                if (new WaitlistManager().addToWaitlist(isbn, title, u)) {
                    JOptionPane.showMessageDialog(this, "Joined waitlist for: '" + title + "' (ISBN: " + isbn + ")");
                    onWaitlistChanged.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not join the waitlist. Please try again.",
                            "Waitlist", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "This book has copies available! You can borrow it in the 'Borrow / Return' tab.");
            }
        });

        // Request a Book dialog for books not in library yet
        requestbtn.addActionListener(e -> {
        JTextField titleF = new JTextField();
        JTextField authorF = new JTextField();
        Object[] fields = { "Book Title:", titleF, "Author:", authorF };
        int res = JOptionPane.showConfirmDialog(this, fields, "Request a Book", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String reqTitle = titleF.getText().trim();
            String reqAuthor = authorF.getText().trim();
            if (!reqTitle.isEmpty() && !reqAuthor.isEmpty()) {
                String key = WaitlistManager.requestKey(reqTitle, reqAuthor);
                boolean inCatalog = false;
                for (model.LibraryItem it : lib.search(reqTitle)) {
                    if (WaitlistManager.requestKey(it.getTitle(), it.getAuthor()).equals(key)) { inCatalog = true; break; }
                }
                if (inCatalog) {
                    JOptionPane.showMessageDialog(this, "This book is already in the library. Search for it and borrow it or join its waitlist.");
                } else if (new WaitlistManager().addToWaitlist(key, reqTitle, u)) {
                    JOptionPane.showMessageDialog(this, "Book '" + reqTitle + "' requested and added to your waitlist!");
                    onWaitlistChanged.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not submit the request. Please try again.",
                            "Request a Book", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Both Title and Author are required.");
            }
        }
    });

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // top search bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(searchtxt);
        top.add(searchbtn);
        top.add(showbtn);
        top.add(waitlistbtn);
        top.add(requestbtn);

        add(top, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(statuslbl, BorderLayout.SOUTH);

        // load all books on start
        dosearchex();
    }

    // test frame
    public static void main(String[] args) {
        JFrame f = new JFrame("Books");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new BookSearchPanel());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}

package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import model.WaitlistEntry;
import service.Library;
import service.WaitlistManager;

public class RequestedBooksPanel extends JPanel {
    private Library lib;
    private DefaultTableModel tableModel;
    private JTable table;
    private JButton refreshBtn;
    private JButton addToCatalogBtn;
    private JLabel statusLbl;
    private WaitlistManager waitlistManager = new WaitlistManager();

    public RequestedBooksPanel(Library lib) {
        this.lib = lib;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = { "Requested Title", "Requesters", "Earliest Request Date" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(tableModel);
        JScrollPane sp = new JScrollPane(table);

        refreshBtn = new JButton("Refresh");
        addToCatalogBtn = new JButton("Add Selected to Catalog");
        statusLbl = new JLabel("Book requests from users");

        refreshBtn.addActionListener(e -> reload());

        addToCatalogBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r == -1) {
                JOptionPane.showMessageDialog(this, "Please select a requested book first.");
                return;
            }
            String title = (String) tableModel.getValueAt(r, 0);

            JTextField isbnF = new JTextField();
            JTextField titleF = new JTextField(title);
            JTextField authorF = new JTextField();
            JComboBox<String> typeBox = new JComboBox<>(new String[] { "TEXT BOOK", "REFERENCE BOOK", "JOURNAL" });
            JTextField copiesF = new JTextField("1");
            JTextField shelfF = new JTextField("A1");

            JPanel pan = new JPanel(new GridLayout(6, 2, 6, 6));
            pan.add(new JLabel("ISBN:"));
            pan.add(isbnF);
            pan.add(new JLabel("Title:"));
            pan.add(titleF);
            pan.add(new JLabel("Author:"));
            pan.add(authorF);
            pan.add(new JLabel("Type:"));
            pan.add(typeBox);
            pan.add(new JLabel("Copies:"));
            pan.add(copiesF);
            pan.add(new JLabel("Shelf:"));
            pan.add(shelfF);

            int opt = JOptionPane.showConfirmDialog(this, pan, "Add Requested Book to Catalog", JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.OK_OPTION) {
                String isbn = isbnF.getText().trim();
                String name = titleF.getText().trim();
                String author = authorF.getText().trim();
                String kind = (String) typeBox.getSelectedItem();
                String shelf = shelfF.getText().trim();

                if (isbn.isEmpty() || name.isEmpty() || author.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "ISBN, Title, and Author are required.");
                    return;
                }

                int count = 1;
                try {
                    count = Integer.parseInt(copiesF.getText().trim());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number of copies.");
                    return;
                }

                model.LibraryItem item;
                if ("REFERENCE BOOK".equalsIgnoreCase(kind)) {
                    item = new model.ReferenceBook(isbn, name, author, count, count, shelf);
                } else if ("JOURNAL".equalsIgnoreCase(kind)) {
                    item = new model.Journal(isbn, name, author, count, count, shelf);
                } else {
                    item = new model.TextBook(isbn, name, author, count, count, shelf);
                }

                lib.addItem(item);
                JOptionPane.showMessageDialog(this, "Book added! Waiting users will be automatically reserved.");
                reload();
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(refreshBtn);
        top.add(addToCatalogBtn);

        add(top, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(statusLbl, BorderLayout.SOUTH);

        reload();
    }

    public void reload() {
        tableModel.setRowCount(0);
        List<WaitlistEntry> list = waitlistManager.getRequestedBooks();
        for (WaitlistEntry e : list) {
            tableModel.addRow(new Object[] {
                e.getTitle(),
                e.getPosition() + " user(s)",
                e.getRequestDate() != null ? e.getRequestDate().toString() : "-"
            });
        }
        statusLbl.setText("Total pending book requests: " + list.size());
    }
}

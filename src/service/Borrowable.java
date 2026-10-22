package service;

import model.LibraryItem;
import model.User;

public interface Borrowable {
    void issue(User user, LibraryItem item);

    void issue(User user, LibraryItem item, int customDays);

    void returnItem(User user, LibraryItem item);

}
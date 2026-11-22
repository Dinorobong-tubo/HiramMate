package com.dinurobongtubo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;

public class StaffBorrowersReturnsController {

    @FXML private Label staffNameLabel;
    @FXML private Label staffIdLabel;
    @FXML private Label activeCountLabel;
    @FXML private Label overdueCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;

    @FXML private TableView<BorrowRecordRow> borrowersTable;

    @FXML private TableColumn<BorrowRecordRow, String> recordIdColumn;
    @FXML private TableColumn<BorrowRecordRow, String> borrowerColumn;
    @FXML private TableColumn<BorrowRecordRow, String> studentIdColumn;
    @FXML private TableColumn<BorrowRecordRow, String> itemColumn;
    @FXML private TableColumn<BorrowRecordRow, String> quantityColumn;
    @FXML private TableColumn<BorrowRecordRow, String> borrowDateColumn;
    @FXML private TableColumn<BorrowRecordRow, String> dueDateColumn;
    @FXML private TableColumn<BorrowRecordRow, String> statusColumn;

    private final ObservableList<BorrowRecordRow> borrowRecords =
            FXCollections.observableArrayList();

    private FilteredList<BorrowRecordRow> filteredRecords;

    @FXML
    public void initialize() {

        staffNameLabel.setText(
                StaffSession.fullName == null ? "Staff" : StaffSession.fullName
        );

        staffIdLabel.setText(
                StaffSession.studentId == null ? "" : StaffSession.studentId
        );

        recordIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("recordId")
        );

        borrowerColumn.setCellValueFactory(
                new PropertyValueFactory<>("borrower")
        );

        studentIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("studentId")
        );

        itemColumn.setCellValueFactory(
                new PropertyValueFactory<>("itemName")
        );

        quantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantity")
        );

        borrowDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("borrowDate")
        );

        dueDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("dueDate")
        );

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );

        filterComboBox.setItems(FXCollections.observableArrayList(
                "All",
                "Borrowed",
                "Overdue",
                "Returned"
        ));

        filterComboBox.setValue("All");

        filteredRecords = new FilteredList<>(borrowRecords, p -> true);
        borrowersTable.setItems(filteredRecords);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> applyFilters()
        );

        filterComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> applyFilters()
        );

        loadBorrowRecords();
    }

    // LOAD BORROW RECORDS

    @FXML
    private void loadBorrowRecords() {

        borrowRecords.clear();

        String sql =
                "SELECT br.record_id, u.full_name, u.student_id, " +
                "i.item_name, br.quantity, br.borrow_date, " +
                "br.due_date, br.return_date " +
                "FROM borrow_records br " +
                "JOIN users u ON br.user_id = u.user_id " +
                "JOIN items i ON br.item_id = i.item_id " +
                "ORDER BY br.record_id DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            LocalDate today = LocalDate.now();

            while (result.next()) {

                int recordId = result.getInt("record_id");
                String borrower = result.getString("full_name");
                String studentId = result.getString("student_id");
                String itemName = result.getString("item_name");
                int quantity = result.getInt("quantity");

                Date borrowDateValue = result.getDate("borrow_date");
                Date dueDateValue = result.getDate("due_date");
                Date returnDateValue = result.getDate("return_date");

                String borrowDate = borrowDateValue == null
                        ? "-"
                        : borrowDateValue.toString();

                String dueDate = dueDateValue == null
                        ? "-"
                        : dueDateValue.toString();

                String status;

                if (returnDateValue != null) {
                    status = "Returned";
                } else if (dueDateValue != null &&
                        dueDateValue.toLocalDate().isBefore(today)) {
                    status = "Overdue";
                } else {
                    status = "Borrowed";
                }

                BorrowRecordRow row = new BorrowRecordRow(
                        String.valueOf(recordId),
                        borrower,
                        studentId,
                        itemName,
                        String.valueOf(quantity),
                        borrowDate,
                        dueDate,
                        status
                );

                borrowRecords.add(row);
            }

            updateSummary();
            applyFilters();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database Error",
                    "Unable to load borrow records.");
        }
    }

    // UPDATE SUMMARY CARDS

    private void updateSummary() {

        int activeCount = 0;
        int overdueCount = 0;

        for (BorrowRecordRow record : borrowRecords) {

            if (record.getStatus().equals("Borrowed")) {
                activeCount++;
            }

            if (record.getStatus().equals("Overdue")) {
                activeCount++;
                overdueCount++;
            }
        }

        activeCountLabel.setText(String.valueOf(activeCount));
        overdueCountLabel.setText(String.valueOf(overdueCount));
    }

    // SEARCH AND FILTER

    private void applyFilters() {

        if (filteredRecords == null) {
            return;
        }

        String search = searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase().trim();

        String selectedFilter = filterComboBox.getValue();

        filteredRecords.setPredicate(record -> {

            boolean matchesSearch =
                    record.getBorrower().toLowerCase().contains(search)
                    || record.getStudentId().toLowerCase().contains(search)
                    || record.getItemName().toLowerCase().contains(search)
                    || record.getRecordId().contains(search);

            boolean matchesFilter =
                    selectedFilter == null
                    || selectedFilter.equals("All")
                    || record.getStatus().equals(selectedFilter);

            return matchesSearch && matchesFilter;
        });
    }

    // PROCESS RETURN

    @FXML
    private void processReturn() {

        BorrowRecordRow selectedRecord =
                borrowersTable.getSelectionModel().getSelectedItem();

        if (selectedRecord == null) {
            showWarning("No Record Selected",
                    "Please select a borrow record first.");
            return;
        }

        if (selectedRecord.getStatus().equals("Returned")) {
            showWarning("Already Returned",
                    "This item has already been returned.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Process Return");
        confirmation.setHeaderText("Confirm Item Return");
        confirmation.setContentText(
                "Borrower: " + selectedRecord.getBorrower()
                + "\nItem: " + selectedRecord.getItemName()
                + "\nQuantity: " + selectedRecord.getQuantity()
                + "\n\nContinue processing this return?"
        );

        Optional<ButtonType> answer = confirmation.showAndWait();

        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }

        TextInputDialog conditionDialog =
                new TextInputDialog("Good");

        conditionDialog.setTitle("Item Condition");
        conditionDialog.setHeaderText("Assess the returned item");
        conditionDialog.setContentText(
                "Enter Good or Damaged:"
        );

        Optional<String> conditionAnswer =
                conditionDialog.showAndWait();

        if (conditionAnswer.isEmpty()) {
            return;
        }

        String condition = conditionAnswer.get().trim();

        if (!condition.equalsIgnoreCase("Good")
                && !condition.equalsIgnoreCase("Damaged")) {

            showWarning("Invalid Condition",
                    "Please enter either Good or Damaged.");
            return;
        }

        completeReturn(
                Integer.parseInt(selectedRecord.getRecordId()),
                condition.equalsIgnoreCase("Damaged")
        );
    }

    // SAVE RETURN TO DATABASE

    private void completeReturn(int recordId, boolean damaged) {

        String selectSql =
                "SELECT user_id, item_id, quantity, due_date, return_date " +
                "FROM borrow_records WHERE record_id = ? FOR UPDATE";

        String returnSql =
                "UPDATE borrow_records " +
                "SET return_date = ?, status = 'Returned' " +
                "WHERE record_id = ? AND return_date IS NULL";

        String stockSql =
                "UPDATE items SET available_quantity = " +
                "available_quantity + ? WHERE item_id = ?";

        String maintenanceSql =
                "UPDATE items SET item_condition = 'Maintenance' " +
                "WHERE item_id = ?";

        String liabilitySql =
                "INSERT INTO liabilities " +
                "(user_id, item_id, record_id, reason, amount, status) " +
                "VALUES (?, ?, ?, ?, ?, 'Pending')";

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                int userId;
                int itemId;
                int quantity;
                LocalDate dueDate;
                LocalDate today = LocalDate.now();

                try (PreparedStatement statement =
                             connection.prepareStatement(selectSql)) {

                    statement.setInt(1, recordId);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new SQLException("Borrow record not found.");
                        }

                        if (result.getDate("return_date") != null) {
                            throw new SQLException(
                                    "This item has already been returned."
                            );
                        }

                        userId = result.getInt("user_id");
                        itemId = result.getInt("item_id");
                        quantity = result.getInt("quantity");

                        Date dueDateValue = result.getDate("due_date");

                        dueDate = dueDateValue == null
                                ? null
                                : dueDateValue.toLocalDate();
                    }
                }

                // Mark the record as returned.

                try (PreparedStatement statement =
                             connection.prepareStatement(returnSql)) {

                    statement.setDate(1, Date.valueOf(today));
                    statement.setInt(2, recordId);

                    int updated = statement.executeUpdate();

                    if (updated == 0) {
                        throw new SQLException(
                                "The return could not be processed."
                        );
                    }
                }

                // Restore the returned quantity to available stock.

                try (PreparedStatement statement =
                             connection.prepareStatement(stockSql)) {

                    statement.setInt(1, quantity);
                    statement.setInt(2, itemId);
                    statement.executeUpdate();
                }

                // Damaged items are marked for maintenance.

                if (damaged) {

                    try (PreparedStatement statement =
                                 connection.prepareStatement(maintenanceSql)) {

                        statement.setInt(1, itemId);
                        statement.executeUpdate();
                    }
                }

                // Create a damage liability.

                if (damaged) {

                    insertLiability(
                            connection,
                            liabilitySql,
                            userId,
                            itemId,
                            recordId,
                            "Damaged Equipment",
                            350.00
                    );

                } else if (dueDate != null && today.isAfter(dueDate)) {

                    // Calculate overdue charge: ₱50 per day.

                    long overdueDays =
                            ChronoUnit.DAYS.between(dueDate, today);

                    double amount = overdueDays * 50.00;

                    insertLiability(
                            connection,
                            liabilitySql,
                            userId,
                            itemId,
                            recordId,
                            "Overdue Fine (" + overdueDays + " days)",
                            amount
                    );
                }

                connection.commit();

                showInformation("Return Successful",
                        "The item return has been recorded successfully.");

                loadBorrowRecords();

            } catch (Exception e) {

                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception e) {

            e.printStackTrace();

            showError("Return Failed",
                    e.getMessage() == null
                            ? "Unable to process the return."
                            : e.getMessage());
        }
    }

    // INSERT LIABILITY

    private void insertLiability(
            Connection connection,
            String sql,
            int userId,
            int itemId,
            int recordId,
            String reason,
            double amount
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, itemId);
            statement.setInt(3, recordId);
            statement.setString(4, reason);
            statement.setDouble(5, amount);

            statement.executeUpdate();
        }
    }

    // NAVIGATION

    @FXML
    private void showDashboard() {
        navigateTo("staff-dashboard");
    }

    @FXML
    private void showItems() {
        navigateTo("staff-items");
    }

    @FXML
    private void showRequests() {
        navigateTo("staff-requests");
    }

    @FXML
    private void showLiabilities() {
        navigateTo("staff-liabilities");
    }

    private void navigateTo(String page) {

        try {
            App.setRoot(page);

        } catch (Exception e) {

            e.printStackTrace();

            showError("Navigation Error",
                    "Unable to open " + page + ".");
        }
    }

    // LOGOUT

    @FXML
    private void logout() {
        navigateTo("primary");
    }

    // ALERTS

    private void showInformation(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // TABLE ROW MODEL

    public static class BorrowRecordRow {

        private final String recordId;
        private final String borrower;
        private final String studentId;
        private final String itemName;
        private final String quantity;
        private final String borrowDate;
        private final String dueDate;
        private final String status;

        public BorrowRecordRow(
                String recordId,
                String borrower,
                String studentId,
                String itemName,
                String quantity,
                String borrowDate,
                String dueDate,
                String status
        ) {

            this.recordId = recordId;
            this.borrower = borrower;
            this.studentId = studentId;
            this.itemName = itemName;
            this.quantity = quantity;
            this.borrowDate = borrowDate;
            this.dueDate = dueDate;
            this.status = status;
        }

        public String getRecordId() {
            return recordId;
        }

        public String getBorrower() {
            return borrower;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getItemName() {
            return itemName;
        }

        public String getQuantity() {
            return quantity;
        }

        public String getBorrowDate() {
            return borrowDate;
        }

        public String getDueDate() {
            return dueDate;
        }

        public String getStatus() {
            return status;
        }
    }
}

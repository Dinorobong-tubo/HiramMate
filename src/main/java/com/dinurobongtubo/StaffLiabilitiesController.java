package com.dinurobongtubo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class StaffLiabilitiesController {

    @FXML private Label staffNameLabel;
    @FXML private Label staffIdLabel;

    @FXML private Label pendingCountLabel;
    @FXML private Label outstandingAmountLabel;
    @FXML private Label settledCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;

    @FXML private TableView<LiabilityRow> liabilitiesTable;

    @FXML private TableColumn<LiabilityRow, String> liabilityIdColumn;
    @FXML private TableColumn<LiabilityRow, String> studentColumn;
    @FXML private TableColumn<LiabilityRow, String> studentIdColumn;
    @FXML private TableColumn<LiabilityRow, String> itemColumn;
    @FXML private TableColumn<LiabilityRow, String> reasonColumn;
    @FXML private TableColumn<LiabilityRow, String> amountColumn;
    @FXML private TableColumn<LiabilityRow, String> statusColumn;

    private final ObservableList<LiabilityRow> liabilities =
            FXCollections.observableArrayList();

    private FilteredList<LiabilityRow> filteredLiabilities;

    @FXML
    public void initialize() {

        staffNameLabel.setText(
                StaffSession.fullName == null
                        ? "Staff"
                        : StaffSession.fullName
        );

        staffIdLabel.setText(
                StaffSession.studentId == null
                        ? ""
                        : StaffSession.studentId
        );

        liabilityIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("liabilityId")
        );

        studentColumn.setCellValueFactory(
                new PropertyValueFactory<>("studentName")
        );

        studentIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("studentId")
        );

        itemColumn.setCellValueFactory(
                new PropertyValueFactory<>("itemName")
        );

        reasonColumn.setCellValueFactory(
                new PropertyValueFactory<>("reason")
        );

        amountColumn.setCellValueFactory(
                new PropertyValueFactory<>("amount")
        );

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );

        filterComboBox.setItems(
                FXCollections.observableArrayList(
                        "All",
                        "Pending",
                        "Settled"
                )
        );

        filterComboBox.setValue("All");

        filteredLiabilities =
                new FilteredList<>(liabilities, item -> true);

        liabilitiesTable.setItems(filteredLiabilities);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> applyFilters()
        );

        filterComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> applyFilters()
        );

        loadLiabilities();
    }

    // LOAD LIABILITIES FROM DATABASE
    @FXML
    private void loadLiabilities() {

        liabilities.clear();

        String sql =
                "SELECT l.liability_id, " +
                "u.full_name, u.student_id, " +
                "i.item_name, l.reason, l.amount, " +
                "l.created_at, l.status " +
                "FROM liabilities l " +
                "JOIN users u ON l.user_id = u.user_id " +
                "LEFT JOIN items i ON l.item_id = i.item_id " +
                "ORDER BY l.liability_id DESC";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                String itemName = result.getString("item_name");

                if (itemName == null) {
                    itemName = "N/A";
                }

                BigDecimal amount = result.getBigDecimal("amount");

                if (amount == null) {
                    amount = BigDecimal.ZERO;
                }

                String formattedAmount =
                        "₱" + amount.toPlainString();

                String dateCreated =
                        result.getString("created_at");

                if (dateCreated == null) {
                    dateCreated = "-";
                } else if (dateCreated.length() >= 10) {
                    dateCreated = dateCreated.substring(0, 10);
                }

                liabilities.add(
                        new LiabilityRow(
                                String.valueOf(
                                        result.getInt("liability_id")),
                                result.getString("full_name"),
                                result.getString("student_id"),
                                itemName,
                                result.getString("reason"),
                                formattedAmount,
                                dateCreated,
                                result.getString("status")
                        )
                );
            }

            updateSummary();
            applyFilters();

        } catch (SQLException e) {

            e.printStackTrace();

            showError(
                    "Database Error",
                    "Unable to load student liabilities."
            );
        }
    }

    // UPDATE SUMMARY CARDS
    private void updateSummary() {

        int pending = 0;
        int settled = 0;

        BigDecimal outstanding = BigDecimal.ZERO;

        for (LiabilityRow liability : liabilities) {

            if ("Pending".equalsIgnoreCase(liability.getStatus())) {

                pending++;

                try {
                    String amountText =
                            liability.getAmount().replace("₱", "").trim();

                    outstanding = outstanding.add(
                            new BigDecimal(amountText)
                    );

                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

            } else if ("Settled".equalsIgnoreCase(
                    liability.getStatus())) {

                settled++;
            }
        }

        pendingCountLabel.setText(String.valueOf(pending));

        outstandingAmountLabel.setText(
                "₱" + outstanding.toPlainString()
        );

        settledCountLabel.setText(String.valueOf(settled));
    }

    // SEARCH AND FILTER
    private void applyFilters() {

        if (filteredLiabilities == null) {
            return;
        }

        String search = searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase().trim();

        String selectedFilter = filterComboBox.getValue();

        filteredLiabilities.setPredicate(liability -> {

            boolean matchesSearch =
                    liability.getLiabilityId().toLowerCase().contains(search)
                    || liability.getStudentName().toLowerCase().contains(search)
                    || liability.getStudentId().toLowerCase().contains(search)
                    || liability.getItemName().toLowerCase().contains(search)
                    || liability.getReason().toLowerCase().contains(search);

            boolean matchesStatus =
                    selectedFilter == null
                    || selectedFilter.equals("All")
                    || liability.getStatus().equalsIgnoreCase(selectedFilter);

            return matchesSearch && matchesStatus;
        });
    }

    // SETTLE LIABILITY
    @FXML
    private void settleLiability() {

        LiabilityRow selected =
                liabilitiesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning(
                    "No Liability Selected",
                    "Please select a liability first."
            );
            return;
        }

        if (!selected.getStatus().equalsIgnoreCase("Pending")) {
            showWarning(
                    "Already Settled",
                    "This liability has already been settled."
            );
            return;
        }

        Alert confirmation =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Settle Liability");
        confirmation.setHeaderText("Confirm Payment Settlement");

        confirmation.setContentText(
                "Student: " + selected.getStudentName()
                + "\nStudent ID: " + selected.getStudentId()
                + "\nReason: " + selected.getReason()
                + "\nAmount: " + selected.getAmount()
                + "\n\nMark this liability as settled?"
        );

        Optional<ButtonType> answer = confirmation.showAndWait();

        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }

        String sql =
                "UPDATE liabilities " +
                "SET status = 'Settled', " +
                "settled_at = CURRENT_TIMESTAMP " +
                "WHERE liability_id = ? AND status = 'Pending'";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    Integer.parseInt(selected.getLiabilityId())
            );

            int updated = statement.executeUpdate();

            if (updated > 0) {
                showInformation(
                        "Payment Settled",
                        "The liability has been marked as settled."
                );
            } else {
                showWarning(
                        "Unable to Settle",
                        "This liability may already have been updated."
                );
            }

            loadLiabilities();

        } catch (SQLException e) {

            e.printStackTrace();

            showError(
                    "Database Error",
                    "Unable to settle this liability."
            );
        }
    }

    // CALCULATE AND ASSESS A NEW LIABILITY
    @FXML
    private void openCalculateLiability() {

        // Ask for the student's ID.
        javafx.scene.control.TextInputDialog studentDialog =
                new javafx.scene.control.TextInputDialog();

        studentDialog.setTitle("Assess Liability");
        studentDialog.setHeaderText("Create a Student Liability");
        studentDialog.setContentText("Enter the student's ID:");

        Optional<String> studentAnswer = studentDialog.showAndWait();

        if (studentAnswer.isEmpty()) {
            return;
        }

        String studentId = studentAnswer.get().trim();

        if (studentId.isEmpty()) {
            showWarning("Missing Student ID", "Please enter a student ID.");
            return;
        }

        // Find the student in the database.
        int userId;

        String studentSql =
                "SELECT user_id, full_name FROM users WHERE student_id = ?";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(studentSql)) {

            statement.setString(1, studentId);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    showWarning(
                            "Student Not Found",
                            "No student account was found with that ID."
                    );
                    return;
                }

                userId = result.getInt("user_id");
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showError(
                    "Database Error",
                    "Unable to find the student."
            );
            return;
        }

        // Select the reason for the liability.
        ChoiceDialog<String> reasonDialog =
                new ChoiceDialog<>(
                        "Overdue Fine",
                        "Overdue Fine",
                        "Damaged Equipment",
                        "Lost Accessory/Part"
                );

        reasonDialog.setTitle("Liability Reason");
        reasonDialog.setHeaderText("Select the liability type");
        reasonDialog.setContentText("Reason:");

        Optional<String> reasonAnswer = reasonDialog.showAndWait();

        if (reasonAnswer.isEmpty()) {
            return;
        }

        String reason = reasonAnswer.get();

        // Ask for the amount.
        javafx.scene.control.TextInputDialog amountDialog =
                new javafx.scene.control.TextInputDialog();

        amountDialog.setTitle("Liability Amount");
        amountDialog.setHeaderText("Enter the assessed amount");
        amountDialog.setContentText("Amount in pesos:");

        Optional<String> amountAnswer = amountDialog.showAndWait();

        if (amountAnswer.isEmpty()) {
            return;
        }

        BigDecimal amount;

        try {
            amount = new BigDecimal(amountAnswer.get().trim());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showWarning(
                        "Invalid Amount",
                        "The amount must be greater than zero."
                );
                return;
            }

        } catch (NumberFormatException e) {

            showWarning(
                    "Invalid Amount",
                    "Please enter a valid number, such as 150 or 350.00."
            );
            return;
        }

        // Save the new liability.
        String insertSql =
                "INSERT INTO liabilities " +
                "(user_id, reason, amount, status) " +
                "VALUES (?, ?, ?, 'Pending')";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(insertSql)) {

            statement.setInt(1, userId);
            statement.setString(2, reason);
            statement.setBigDecimal(3, amount);

            statement.executeUpdate();

            showInformation(
                    "Liability Created",
                    "The liability has been added successfully."
            );

            loadLiabilities();

        } catch (SQLException e) {

            e.printStackTrace();

            showError(
                    "Database Error",
                    "Unable to create the liability record."
            );
        }
    }

    // SIDEBAR NAVIGATION
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
    private void showBorrowersReturns() {
        navigateTo("staff-borrowers-returns");
    }

    private void navigateTo(String page) {

        try {
            App.setRoot(page);

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Navigation Error",
                    "Unable to open " + page + "."
            );
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
    public static class LiabilityRow {

        private final String liabilityId;
        private final String studentName;
        private final String studentId;
        private final String itemName;
        private final String reason;
        private final String amount;
        private final String dateCreated;
        private final String status;

        public LiabilityRow(
                String liabilityId,
                String studentName,
                String studentId,
                String itemName,
                String reason,
                String amount,
                String dateCreated,
                String status) {

            this.liabilityId = liabilityId;
            this.studentName = studentName;
            this.studentId = studentId;
            this.itemName = itemName;
            this.reason = reason;
            this.amount = amount;
            this.dateCreated = dateCreated;
            this.status = status;
        }

        public String getLiabilityId() {
            return liabilityId;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getItemName() {
            return itemName;
        }

        public String getReason() {
            return reason;
        }

        public String getAmount() {
            return amount;
        }

        public String getDateCreated() {
            return dateCreated;
        }

        public String getStatus() {
            return status;
        }
    }
}

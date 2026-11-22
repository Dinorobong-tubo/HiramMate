
package com.dinurobongtubo;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class LiabilitiesController {

    @FXML
    private Label profileInitials;

    @FXML
    private Label profileName;

    @FXML
    private Label profileDetails;

    @FXML
    private Label totalOutstandingLabel;

    @FXML
    private Label pendingChargesLabel;

    @FXML
    private Label settledLabel;

    @FXML
    private Label noticeDescription;

    @FXML
    private Label recordsFooter;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<LiabilityRow> liabilitiesTable;

    @FXML
    private TableColumn<LiabilityRow, String> referenceColumn;

    @FXML
    private TableColumn<LiabilityRow, String> itemColumn;

    @FXML
    private TableColumn<LiabilityRow, String> reasonColumn;

    @FXML
    private TableColumn<LiabilityRow, String> dateColumn;

    @FXML
    private TableColumn<LiabilityRow, String> amountColumn;

    @FXML
    private TableColumn<LiabilityRow, String> statusColumn;

    private final ObservableList<LiabilityRow> liabilityList =
        FXCollections.observableArrayList();

    private FilteredList<LiabilityRow> filteredLiabilities;

    private final DateTimeFormatter dateFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy");

    // ==========================================
    // INITIALIZE
    // ==========================================

    @FXML
    public void initialize() {

        loadProfile();
        setupStatusFilter();
        setupTableColumns();
        setupSearchAndFilter();

        loadLiabilities();
    }

    // ==========================================
    // PROFILE INFORMATION
    // ==========================================

    private void loadProfile() {

        String name = StudentSession.fullName;

        if (name == null || name.isBlank()) {
            name = "Student";
        }

        profileName.setText(name);

        String[] nameParts = name.trim().split("\\s+");
        String initials = "";

        for (String part : nameParts) {
            if (!part.isEmpty()) {
                initials += part.charAt(0);
            }
        }

        if (initials.length() > 2) {
            initials = initials.substring(0, 2);
        }

        profileInitials.setText(initials.toUpperCase());

        String studentId = StudentSession.studentId;

        profileDetails.setText(
            studentId != null ? studentId : "No Student ID"
        );
    }

    // ==========================================
    // STATUS FILTER
    // ==========================================

    private void setupStatusFilter() {

        statusFilter.getItems().clear();

        statusFilter.getItems().addAll(
            "All statuses",
            "Pending",
            "Outstanding",
            "Settled"
        );

        statusFilter.setValue("All statuses");
    }

    // ==========================================
    // TABLE COLUMNS
    // ==========================================

    private void setupTableColumns() {

        referenceColumn.setCellValueFactory(
            new PropertyValueFactory<>("reference")
        );

        itemColumn.setCellValueFactory(
            new PropertyValueFactory<>("item")
        );

        reasonColumn.setCellValueFactory(
            new PropertyValueFactory<>("reason")
        );

        dateColumn.setCellValueFactory(
            new PropertyValueFactory<>("date")
        );

        amountColumn.setCellValueFactory(
            new PropertyValueFactory<>("amount")
        );

        statusColumn.setCellValueFactory(
            new PropertyValueFactory<>("status")
        );
    }

    // ==========================================
    // SEARCH AND FILTER
    // ==========================================

    private void setupSearchAndFilter() {

        filteredLiabilities =
            new FilteredList<>(liabilityList, row -> true);

        liabilitiesTable.setItems(filteredLiabilities);

        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> applyFilters()
        );

        statusFilter.valueProperty().addListener(
            (observable, oldValue, newValue) -> applyFilters()
        );
    }

    private void applyFilters() {

        if (filteredLiabilities == null) {
            return;
        }

        String searchText = searchField.getText();

        if (searchText == null) {
            searchText = "";
        }

        final String search = searchText.toLowerCase().trim();

        String selectedStatus = statusFilter.getValue();

        filteredLiabilities.setPredicate(row -> {

            // STATUS FILTER

            boolean matchesStatus =
                selectedStatus == null ||
                selectedStatus.equals("All statuses") ||
                row.getStatus().equalsIgnoreCase(selectedStatus);

            // SEARCH FILTER

            boolean matchesSearch =
                search.isEmpty() ||
                row.getReference().toLowerCase().contains(search) ||
                row.getItem().toLowerCase().contains(search);

            return matchesStatus && matchesSearch;
        });

        updateRecordsFooter();
    }

    // ==========================================
    // LOAD LIABILITY RECORDS FROM MYSQL
    // ==========================================

    private void loadLiabilities() {

        liabilityList.clear();

        if (StudentSession.studentId == null ||
            StudentSession.studentId.isBlank()) {

            showAlert(
                Alert.AlertType.WARNING,
                "No Student Session",
                "Unable to identify the currently logged-in student."
            );

            return;
        }

        String sql =
            "SELECT l.liability_id, l.reason, l.amount, " +
            "l.status, l.created_at, " +
            "COALESCE(i.item_name, 'General Liability') AS item_name " +
            "FROM liabilities l " +
            "INNER JOIN users u ON l.user_id = u.user_id " +
            "LEFT JOIN items i ON l.item_id = i.item_id " +
            "WHERE u.student_id = ? " +
            "ORDER BY l.created_at DESC";

        try (Connection connection =
                 DatabaseConnection.getConnection();

             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, StudentSession.studentId);

            try (ResultSet result = statement.executeQuery()) {

                BigDecimal totalOutstanding = BigDecimal.ZERO;
                BigDecimal pendingCharges = BigDecimal.ZERO;
                BigDecimal settledAmount = BigDecimal.ZERO;

                while (result.next()) {

                    int liabilityId =
                        result.getInt("liability_id");

                    String reason =
                        result.getString("reason");

                    String item =
                        result.getString("item_name");

                    BigDecimal amount =
                        result.getBigDecimal("amount");

                    if (amount == null) {
                        amount = BigDecimal.ZERO;
                    }

                    String status =
                        result.getString("status");

                    Timestamp timestamp =
                        result.getTimestamp("created_at");

                    String formattedDate = "Not available";

                    if (timestamp != null) {

                        LocalDateTime dateTime =
                            timestamp.toLocalDateTime();

                        formattedDate =
                            dateTime.format(dateFormatter);
                    }

                    String reference =
                        String.format("LIAB-%04d", liabilityId);

                    String formattedAmount =
                        formatCurrency(amount);

                    LiabilityRow row = new LiabilityRow(
                        reference,
                        item,
                        reason,
                        formattedDate,
                        formattedAmount,
                        status
                    );

                    liabilityList.add(row);

                    // CALCULATE SUMMARY AMOUNTS

                    if (status != null) {

                        if (status.equalsIgnoreCase("Pending") ||
                            status.equalsIgnoreCase("Outstanding")) {

                            totalOutstanding =
                                totalOutstanding.add(amount);
                        }

                        if (status.equalsIgnoreCase("Pending")) {

                            pendingCharges =
                                pendingCharges.add(amount);
                        }

                        if (status.equalsIgnoreCase("Settled")) {

                            settledAmount =
                                settledAmount.add(amount);
                        }
                    }
                }

                // UPDATE SUMMARY CARDS

                totalOutstandingLabel.setText(
                    formatCurrency(totalOutstanding)
                );

                pendingChargesLabel.setText(
                    formatCurrency(pendingCharges)
                );

                settledLabel.setText(
                    formatCurrency(settledAmount)
                );

                // UPDATE PAYMENT NOTICE

                noticeDescription.setText(
                    "You currently have an outstanding balance of " +
                    formatCurrency(totalOutstanding) +
                    ". Please coordinate with the property office " +
                    "for verification and settlement."
                );

                applyFilters();
            }

        } catch (SQLException e) {

            showAlert(
                Alert.AlertType.ERROR,
                "Database Error",
                "Unable to load liability records.\n\n" +
                e.getMessage()
            );
        }
    }

    // ==========================================
    // RECORDS FOOTER
    // ==========================================

    private void updateRecordsFooter() {

        int count = filteredLiabilities.size();

        recordsFooter.setText(
            "Showing " + count + " record(s)"
        );
    }

    // ==========================================
    // FORMAT CURRENCY
    // ==========================================

    private String formatCurrency(BigDecimal amount) {

        return String.format(
            Locale.US,
            "₱%,.2f",
            amount
        );
    }

    // ==========================================
    // ALERT
    // ==========================================

    private void showAlert(
        Alert.AlertType type,
        String title,
        String message
    ) {

        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // ==========================================
    // DOWNLOAD STATEMENT PLACEHOLDER
    // ==========================================

    @FXML
    private void showComingSoon() {

        showAlert(
            Alert.AlertType.INFORMATION,
            "HiramMate",
            "The statement download feature will be implemented " +
            "in a future update."
        );
    }

    // ==========================================
    // PAGE NAVIGATION
    // ==========================================

    @FXML
    private void showDashboard() throws IOException {
        App.setRoot("student-dashboard");
    }

    @FXML
    private void showBrowseItems() throws IOException {
        App.setRoot("browse-items");
    }

    @FXML
    private void showMyRequests() throws IOException {
        App.setRoot("my-requests");
    }

    @FXML
    private void showBorrowedReturns() throws IOException {
        App.setRoot("borrowed-returns");
    }

    // ==========================================
    // LIABILITY ROW MODEL
    // ==========================================

    public static class LiabilityRow {

        private final javafx.beans.property.SimpleStringProperty reference;
        private final javafx.beans.property.SimpleStringProperty item;
        private final javafx.beans.property.SimpleStringProperty reason;
        private final javafx.beans.property.SimpleStringProperty date;
        private final javafx.beans.property.SimpleStringProperty amount;
        private final javafx.beans.property.SimpleStringProperty status;

        public LiabilityRow(
            String reference,
            String item,
            String reason,
            String date,
            String amount,
            String status
        ) {

            this.reference =
                new javafx.beans.property.SimpleStringProperty(reference);

            this.item =
                new javafx.beans.property.SimpleStringProperty(item);

            this.reason =
                new javafx.beans.property.SimpleStringProperty(reason);

            this.date =
                new javafx.beans.property.SimpleStringProperty(date);

            this.amount =
                new javafx.beans.property.SimpleStringProperty(amount);

            this.status =
                new javafx.beans.property.SimpleStringProperty(status);
        }

        public String getReference() {
            return reference.get();
        }

        public String getItem() {
            return item.get();
        }

        public String getReason() {
            return reason.get();
        }

        public String getDate() {
            return date.get();
        }

        public String getAmount() {
            return amount.get();
        }

        public String getStatus() {
            return status.get();
        }
    }
}

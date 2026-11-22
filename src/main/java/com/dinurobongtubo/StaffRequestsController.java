package com.dinurobongtubo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Date;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class StaffRequestsController {

    // STAFF PROFILE
    @FXML private Label staffNameLabel;
    @FXML private Label staffIdLabel;

    // SEARCH AND FILTER
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;

    // REQUEST TABLE
    @FXML private TableView<RequestRow> requestsTable;

    @FXML private TableColumn<RequestRow, String> requestIdColumn;
    @FXML private TableColumn<RequestRow, String> borrowerColumn;
    @FXML private TableColumn<RequestRow, String> itemColumn;
    @FXML private TableColumn<RequestRow, Integer> quantityColumn;
    @FXML private TableColumn<RequestRow, String> requestDateColumn;
    @FXML private TableColumn<RequestRow, String> dueDateColumn;
    @FXML private TableColumn<RequestRow, String> statusColumn;

    // REQUEST DETAILS
    @FXML private Label selectedRequestLabel;
    @FXML private Label detailBorrowerLabel;
    @FXML private Label detailStudentIdLabel;
    @FXML private Label detailDepartmentLabel;
    @FXML private Label detailItemLabel;
    @FXML private Label detailCategoryLabel;
    @FXML private Label detailQuantityLabel;
    @FXML private Label detailPurposeLabel;
    @FXML private Label detailRequestDateLabel;
    @FXML private Label detailPickupDateLabel;
    @FXML private Label detailDueDateLabel;
    @FXML private Label detailStatusLabel;

    // ACTION BUTTONS
    @FXML private Button approveButton;
    @FXML private Button rejectButton;

    @FXML private Label requestCountLabel;

    private final ObservableList<RequestRow> requestList =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        if (StaffSession.fullName != null) {
            staffNameLabel.setText(StaffSession.fullName);
        }

        if (StaffSession.studentId != null) {
            staffIdLabel.setText(StaffSession.studentId);
        }

        setupTable();
        setupFilters();

        requestsTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, selected) -> {
                    showRequestDetails(selected);
                });

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> loadRequests()
        );

        loadRequests();
        clearDetails();
    }

    private void setupTable() {

        requestIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("requestId"));

        borrowerColumn.setCellValueFactory(
                new PropertyValueFactory<>("borrower"));

        itemColumn.setCellValueFactory(
                new PropertyValueFactory<>("item"));

        quantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantity"));

        requestDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("requestDate"));

        dueDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("dueDate"));

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status"));

        requestsTable.setItems(requestList);
    }

    private void setupFilters() {

        statusFilter.setItems(
                FXCollections.observableArrayList(
                        "All",
                        "Pending",
                        "Approved",
                        "Reserved",
                        "Rejected",
                        "Cancelled",
                        "Overdue"
                )
        );

        statusFilter.setValue("All");

        statusFilter.setOnAction(event -> loadRequests());
    }

    @FXML
    private void loadRequests() {

        requestList.clear();

        String search = searchField.getText() == null
                ? ""
                : searchField.getText().trim();

        String selectedStatus = statusFilter.getValue();

        if (selectedStatus == null) {
            selectedStatus = "All";
        }

        String sql =
                "SELECT br.request_id, br.quantity, br.purpose, "
              + "br.request_date, br.pickup_date, br.due_date, br.status, "
              + "u.full_name, u.student_id, u.department, u.block, "
              + "i.item_name, i.item_code, i.category "
              + "FROM borrow_requests br "
              + "JOIN users u ON br.user_id = u.user_id "
              + "JOIN items i ON br.item_id = i.item_id "
              + "WHERE (? = '' OR CAST(br.request_id AS CHAR) LIKE ? "
              + "OR u.full_name LIKE ? "
              + "OR u.student_id LIKE ? "
              + "OR i.item_name LIKE ?) "
              + "AND (? = 'All' OR br.status = ?) "
              + "ORDER BY CASE WHEN br.status = 'Pending' "
              + "THEN 0 ELSE 1 END, br.request_date DESC";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            String pattern = "%" + search + "%";

            statement.setString(1, search);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            statement.setString(5, pattern);
            statement.setString(6, selectedStatus);
            statement.setString(7, selectedStatus);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    int requestId = result.getInt("request_id");

                    String borrower =
                            result.getString("full_name")
                          + " (" + result.getString("student_id") + ")";

                    String requestDate = formatTimestamp(
                            result.getTimestamp("request_date")
                    );

                    String pickupDate = formatDate(
                            result.getDate("pickup_date")
                    );

                    String dueDate = formatDate(
                            result.getDate("due_date")
                    );

                    requestList.add(new RequestRow(
                            requestId,
                            borrower,
                            result.getString("item_name"),
                            result.getInt("quantity"),
                            requestDate,
                            pickupDate,
                            dueDate,
                            result.getString("status"),
                            result.getString("full_name"),
                            result.getString("student_id"),
                            result.getString("department"),
                            result.getString("block"),
                            result.getString("item_code"),
                            result.getString("category"),
                            result.getString("purpose")
                    ));
                }
            }

            requestCountLabel.setText(
                    requestList.size() + " request(s) found"
            );

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Unable to load borrow requests.");
        }
    }

    private String formatDate(Date date) {

        return date == null ? "Not set" : date.toString();
    }

    private String formatTimestamp(Timestamp timestamp) {

        if (timestamp == null) {
            return "Not available";
        }

        return timestamp.toLocalDateTime()
                .toString()
                .replace("T", " ");
    }

    private void showRequestDetails(RequestRow row) {

        if (row == null) {
            clearDetails();
            return;
        }

        selectedRequestLabel.setText(
                "Request #" + row.getRequestId()
        );

        detailBorrowerLabel.setText(row.getFullName());
        detailStudentIdLabel.setText(row.getStudentId());

        detailDepartmentLabel.setText(
                row.getDepartment() + " / " + row.getBlock()
        );

        detailItemLabel.setText(
                row.getItem() + " (" + row.getItemCode() + ")"
        );

        detailCategoryLabel.setText(row.getCategory());

        detailQuantityLabel.setText(
                String.valueOf(row.getQuantity())
        );

        detailPurposeLabel.setText(
                row.getPurpose() == null || row.getPurpose().isBlank()
                ? "No purpose provided"
                : row.getPurpose()
        );

        detailRequestDateLabel.setText(row.getRequestDate());
        detailPickupDateLabel.setText(row.getPickupDate());
        detailDueDateLabel.setText(row.getDueDate());
        detailStatusLabel.setText(row.getStatus());

        boolean isPending = "Pending".equalsIgnoreCase(row.getStatus());

        approveButton.setDisable(!isPending);
        rejectButton.setDisable(!isPending);
    }

    private void clearDetails() {

        selectedRequestLabel.setText("No request selected");
        detailBorrowerLabel.setText("-");
        detailStudentIdLabel.setText("-");
        detailDepartmentLabel.setText("-");
        detailItemLabel.setText("-");
        detailCategoryLabel.setText("-");
        detailQuantityLabel.setText("-");
        detailPurposeLabel.setText("-");
        detailRequestDateLabel.setText("-");
        detailPickupDateLabel.setText("-");
        detailDueDateLabel.setText("-");
        detailStatusLabel.setText("-");

        approveButton.setDisable(true);
        rejectButton.setDisable(true);
    }

    @FXML
    private void approveRequest() {

        RequestRow selected = requestsTable.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            showAlert("Please select a request first.");
            return;
        }

        if (!"Pending".equalsIgnoreCase(selected.getStatus())) {
            showAlert("Only pending requests can be approved.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Approve Borrow Request");
        confirmation.setHeaderText(
                "Approve Request #" + selected.getRequestId() + "?"
        );

        confirmation.setContentText(
                "Borrower: " + selected.getFullName()
              + "\nItem: " + selected.getItem()
              + "\nQuantity: " + selected.getQuantity()
              + "\n\nThis will change the request status to Approved."
        );

        Optional<ButtonType> answer = confirmation.showAndWait();

        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }

        updateRequestStatus(selected, "Approved");
    }

    @FXML
    private void rejectRequest() {

        RequestRow selected = requestsTable.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            showAlert("Please select a request first.");
            return;
        }

        if (!"Pending".equalsIgnoreCase(selected.getStatus())) {
            showAlert("Only pending requests can be rejected.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Reject Borrow Request");
        confirmation.setHeaderText(
                "Reject Request #" + selected.getRequestId() + "?"
        );

        confirmation.setContentText(
                "Borrower: " + selected.getFullName()
              + "\nItem: " + selected.getItem()
              + "\n\nAre you sure you want to reject this request?"
        );

        Optional<ButtonType> answer = confirmation.showAndWait();

        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }

        updateRequestStatus(selected, "Rejected");
    }

    private void updateRequestStatus(
            RequestRow selected, String newStatus) {

        String sql =
                "UPDATE borrow_requests "
              + "SET status = ? "
              + "WHERE request_id = ? AND status = 'Pending'";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, newStatus);
            statement.setInt(2, selected.getRequestId());

            int updatedRows = statement.executeUpdate();

            if (updatedRows > 0) {

                showAlert(
                        "Request #" + selected.getRequestId()
                      + " has been " + newStatus.toLowerCase() + "."
                );

                loadRequests();
                clearDetails();

            } else {

                showAlert(
                        "This request is no longer pending. "
                      + "Please refresh the list."
                );

                loadRequests();
                clearDetails();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Unable to update the request.");
        }
    }

    @FXML
    private void refreshRequests() {

        loadRequests();
        clearDetails();
    }

    @FXML
    private void showDashboard() {
        openPage("staff-dashboard");
    }

    @FXML
    private void showItems() {
        openPage("staff-items");
    }

    @FXML
    private void showRequests() {
        loadRequests();
    }

    @FXML
    private void showBorrowersReturns() {
        openPage("staff-borrowers-returns");
    }

    @FXML
    private void showLiabilities() {
        openPage("staff-liabilities");
    }

    @FXML
    private void logout() {

        try {
            StaffSession.fullName = null;
            StaffSession.studentId = null;

            App.setRoot("secondary");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Unable to return to login page.");
        }
    }

    private void openPage(String page) {

        try {
            App.setRoot(page);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("This Staff page has not been created yet.");
        }
    }

    private void showAlert(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("HiramMate Staff Portal");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class RequestRow {

        private final int requestId;
        private final String borrower;
        private final String item;
        private final int quantity;
        private final String requestDate;
        private final String pickupDate;
        private final String dueDate;
        private final String status;

        private final String fullName;
        private final String studentId;
        private final String department;
        private final String block;
        private final String itemCode;
        private final String category;
        private final String purpose;

        public RequestRow(
                int requestId,
                String borrower,
                String item,
                int quantity,
                String requestDate,
                String pickupDate,
                String dueDate,
                String status,
                String fullName,
                String studentId,
                String department,
                String block,
                String itemCode,
                String category,
                String purpose) {

            this.requestId = requestId;
            this.borrower = borrower;
            this.item = item;
            this.quantity = quantity;
            this.requestDate = requestDate;
            this.pickupDate = pickupDate;
            this.dueDate = dueDate;
            this.status = status;
            this.fullName = fullName;
            this.studentId = studentId;
            this.department = department;
            this.block = block;
            this.itemCode = itemCode;
            this.category = category;
            this.purpose = purpose;
        }

        public int getRequestId() { return requestId; }
        public String getBorrower() { return borrower; }
        public String getItem() { return item; }
        public int getQuantity() { return quantity; }
        public String getRequestDate() { return requestDate; }
        public String getPickupDate() { return pickupDate; }
        public String getDueDate() { return dueDate; }
        public String getStatus() { return status; }

        public String getFullName() { return fullName; }
        public String getStudentId() { return studentId; }
        public String getDepartment() { return department; }
        public String getBlock() { return block; }
        public String getItemCode() { return itemCode; }
        public String getCategory() { return category; }
        public String getPurpose() { return purpose; }
    }
}

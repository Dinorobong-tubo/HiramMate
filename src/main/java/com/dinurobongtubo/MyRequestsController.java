package com.dinurobongtubo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Callback;

public class MyRequestsController {

    @FXML private Label profileInitials;
    @FXML private Label profileName;
    @FXML private Label profileDetails;

    @FXML private TableView<RequestRow> requestsTable;

    @FXML private TableColumn<RequestRow, String> requestIdColumn;
    @FXML private TableColumn<RequestRow, String> itemColumn;
    @FXML private TableColumn<RequestRow, String> purposeColumn;
    @FXML private TableColumn<RequestRow, String> pickupDateColumn;
    @FXML private TableColumn<RequestRow, String> dueDateColumn;
    @FXML private TableColumn<RequestRow, String> statusColumn;
    @FXML private TableColumn<RequestRow, Void> actionColumn;

    @FXML private TextField searchField;

    @FXML private Label tableFooterLabel;
    @FXML private Label tableCountLabel;

    private final ObservableList<RequestRow> allRequests =
            FXCollections.observableArrayList();

    private String selectedStatus = "All";

    // INITIALIZE PAGE
    @FXML
    public void initialize() {

        displayStudentProfile();

        setupTableColumns();

        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> displayRequests()
        );

        loadRequests();
    }

    // DISPLAY STUDENT PROFILE
    private void displayStudentProfile() {

        if (StudentSession.fullName != null) {

            String fullName = StudentSession.fullName;

            profileName.setText(fullName);

            String[] nameParts = fullName.trim().split("\\s+");

            String initials = "";

            for (String part : nameParts) {
                if (!part.isEmpty()) {
                    initials += part.charAt(0);
                }
            }

            profileInitials.setText(
                initials.length() > 2
                    ? initials.substring(0, 2).toUpperCase()
                    : initials.toUpperCase()
            );
        }

        if (StudentSession.studentId != null) {
            profileDetails.setText(StudentSession.studentId);
        }
    }

    // SET UP TABLE COLUMNS
    private void setupTableColumns() {

        requestIdColumn.setCellValueFactory(
            cell -> cell.getValue().requestIdProperty()
        );

        itemColumn.setCellValueFactory(
            cell -> cell.getValue().itemNameProperty()
        );

        purposeColumn.setCellValueFactory(
            cell -> cell.getValue().purposeProperty()
        );

        pickupDateColumn.setCellValueFactory(
            cell -> cell.getValue().pickupDateProperty()
        );

        dueDateColumn.setCellValueFactory(
            cell -> cell.getValue().dueDateProperty()
        );

        statusColumn.setCellValueFactory(
            cell -> cell.getValue().statusProperty()
        );

        setupActionColumn();
    }

    // LOAD REQUESTS FROM MYSQL
    private void loadRequests() {

        allRequests.clear();

        if (StudentSession.studentId == null) {
            showAlert(
                Alert.AlertType.ERROR,
                "Session Error",
                "Student information is missing. Please log in again."
            );
            return;
        }

        String sql =
            "SELECT br.request_id, i.item_name, br.purpose, " +
            "br.pickup_date, br.due_date, br.status " +
            "FROM borrow_requests br " +
            "INNER JOIN users u ON br.user_id = u.user_id " +
            "INNER JOIN items i ON br.item_id = i.item_id " +
            "WHERE u.student_id = ? " +
            "ORDER BY br.request_date DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, StudentSession.studentId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    String pickupDate = result.getString("pickup_date");
                    String dueDate = result.getString("due_date");

                    RequestRow request = new RequestRow(
                        String.valueOf(result.getInt("request_id")),
                        result.getString("item_name"),
                        result.getString("purpose"),
                        pickupDate == null ? "Not set" : pickupDate,
                        dueDate == null ? "Not set" : dueDate,
                        result.getString("status")
                    );

                    allRequests.add(request);
                }
            }

            displayRequests();

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                Alert.AlertType.ERROR,
                "Database Error",
                "Unable to load your requests.\n\n" + e.getMessage()
            );
        }
    }

    // DISPLAY AND FILTER REQUESTS
    private void displayRequests() {

        ObservableList<RequestRow> filteredRequests =
                FXCollections.observableArrayList();

        String searchText = searchField.getText();

        if (searchText == null) {
            searchText = "";
        }

        searchText = searchText.toLowerCase().trim();

        for (RequestRow request : allRequests) {

            String status = request.getStatus();

            // STATUS FILTER
            boolean matchesStatus;

            if (selectedStatus.equals("All")) {
                matchesStatus = true;

            } else if (selectedStatus.equals("Closed")) {
                matchesStatus =
                    status.equalsIgnoreCase("Returned")
                    || status.equalsIgnoreCase("Cancelled");

            } else {
                matchesStatus =
                    status.equalsIgnoreCase(selectedStatus);
            }

            if (!matchesStatus) {
                continue;
            }

            // SEARCH FILTER
            boolean matchesSearch =
                request.getRequestId().toLowerCase().contains(searchText)
                || request.getItemName().toLowerCase().contains(searchText);

            if (matchesSearch) {
                filteredRequests.add(request);
            }
        }

        requestsTable.setItems(filteredRequests);

        // UPDATE FOOTER
        int displayed = filteredRequests.size();
        int total = allRequests.size();

        tableFooterLabel.setText(
            "Showing " + displayed + " requests"
        );

        if (displayed == 0) {
            tableCountLabel.setText("0 - 0 of 0");
        } else {
            tableCountLabel.setText(
                "1 - " + displayed + " of " + total
            );
        }
    }

    // ACTION COLUMN
    private void setupActionColumn() {

        Callback<TableColumn<RequestRow, Void>, TableCell<RequestRow, Void>>
            cellFactory = column -> new TableCell<RequestRow, Void>() {

                private final Button cancelButton =
                        new Button("Cancel");

                {
                    cancelButton.setStyle(
                        "-fx-background-color: #FCE8E8;" +
                        "-fx-text-fill: #B42318;" +
                        "-fx-cursor: hand;"
                    );

                    cancelButton.setOnAction(event -> {

                        RequestRow request =
                            getTableView().getItems().get(getIndex());

                        cancelRequest(request);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {

                    super.updateItem(item, empty);

                    if (empty) {
                        setGraphic(null);
                        return;
                    }

                    RequestRow request =
                        getTableView().getItems().get(getIndex());

                    if (request.getStatus().equalsIgnoreCase("Pending")) {
                        setGraphic(cancelButton);
                    } else {
                        setGraphic(null);
                    }
                }
            };

        actionColumn.setCellFactory(cellFactory);
    }

    // CANCEL PENDING REQUEST
    private void cancelRequest(RequestRow request) {

        String sql =
            "UPDATE borrow_requests " +
            "SET status = 'Cancelled' " +
            "WHERE request_id = ? " +
            "AND status = 'Pending' " +
            "AND user_id = (" +
            "SELECT user_id FROM users WHERE student_id = ?" +
            ")";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                1,
                Integer.parseInt(request.getRequestId())
            );

            statement.setString(2, StudentSession.studentId);

            int updated = statement.executeUpdate();

            if (updated > 0) {

                showAlert(
                    Alert.AlertType.INFORMATION,
                    "Request Cancelled",
                    "Your borrowing request has been cancelled."
                );

                loadRequests();

            } else {

                showAlert(
                    Alert.AlertType.WARNING,
                    "Unable to Cancel",
                    "This request may no longer be pending."
                );

                loadRequests();
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                Alert.AlertType.ERROR,
                "Database Error",
                "Unable to cancel the request.\n\n" + e.getMessage()
            );
        }
    }

    // STATUS FILTER BUTTONS
    @FXML
    private void showAllRequests() {
        selectedStatus = "All";
        displayRequests();
    }

    @FXML
    private void showPendingRequests() {
        selectedStatus = "Pending";
        displayRequests();
    }

    @FXML
    private void showReservedRequests() {
        selectedStatus = "Reserved";
        displayRequests();
    }

    @FXML
    private void showApprovedRequests() {
        selectedStatus = "Approved";
        displayRequests();
    }

    @FXML
    private void showClosedRequests() {
        selectedStatus = "Closed";
        displayRequests();
    }

    // SIDEBAR NAVIGATION
    @FXML
    private void showDashboard() throws IOException {
        App.setRoot("student-dashboard");
    }

    @FXML
    private void showBrowseItems() throws IOException {
        App.setRoot("browse-items");
    }

    @FXML
    private void showBorrowedReturns() throws IOException {
        App.setRoot("borrowed-returns");
    }

    @FXML
    private void showLiabilities() throws IOException {
        App.setRoot("liabilities");
    }

    // ALERT MESSAGE
    private void showAlert(
            Alert.AlertType type,
            String title,
            String message) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // REQUEST ROW MODEL
    public static class RequestRow {

        private final javafx.beans.property.SimpleStringProperty requestId;
        private final javafx.beans.property.SimpleStringProperty itemName;
        private final javafx.beans.property.SimpleStringProperty purpose;
        private final javafx.beans.property.SimpleStringProperty pickupDate;
        private final javafx.beans.property.SimpleStringProperty dueDate;
        private final javafx.beans.property.SimpleStringProperty status;

        public RequestRow(
                String requestId,
                String itemName,
                String purpose,
                String pickupDate,
                String dueDate,
                String status) {

            this.requestId =
                new javafx.beans.property.SimpleStringProperty(requestId);

            this.itemName =
                new javafx.beans.property.SimpleStringProperty(itemName);

            this.purpose =
                new javafx.beans.property.SimpleStringProperty(
                    purpose == null ? "" : purpose
                );

            this.pickupDate =
                new javafx.beans.property.SimpleStringProperty(pickupDate);

            this.dueDate =
                new javafx.beans.property.SimpleStringProperty(dueDate);

            this.status =
                new javafx.beans.property.SimpleStringProperty(status);
        }

        public String getRequestId() {
            return requestId.get();
        }

        public String getItemName() {
            return itemName.get();
        }

        public String getStatus() {
            return status.get();
        }

        public javafx.beans.property.SimpleStringProperty requestIdProperty() {
            return requestId;
        }

        public javafx.beans.property.SimpleStringProperty itemNameProperty() {
            return itemName;
        }

        public javafx.beans.property.SimpleStringProperty purposeProperty() {
            return purpose;
        }

        public javafx.beans.property.SimpleStringProperty pickupDateProperty() {
            return pickupDate;
        }

        public javafx.beans.property.SimpleStringProperty dueDateProperty() {
            return dueDate;
        }

        public javafx.beans.property.SimpleStringProperty statusProperty() {
            return status;
        }
    }
}

package com.dinurobongtubo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class StaffDashboardController {

    // STAFF PROFILE
    @FXML private Label staffNameLabel;
    @FXML private Label staffIdLabel;

    // SUMMARY LABELS
    @FXML private Label availableLabel;
    @FXML private Label borrowedLabel;
    @FXML private Label overdueLabel;
    @FXML private Label reservedLabel;
    @FXML private Label pendingLabel;

    // SEARCH
    @FXML private TextField searchField;

    // REQUEST TABLE
    @FXML private TableView<RequestRow> requestsTable;
    @FXML private TableColumn<RequestRow, String> borrowerColumn;
    @FXML private TableColumn<RequestRow, String> itemColumn;
    @FXML private TableColumn<RequestRow, String> dueDateColumn;
    @FXML private TableColumn<RequestRow, String> statusColumn;
    @FXML private TableColumn<RequestRow, Void> actionColumn;

    // ACTIVE BORROWER CARDS
    @FXML private HBox activeBorrowersContainer;
    @FXML private Label borrowerEmptyLabel;

    // DATA
    private final ObservableList<RequestRow> requestList =
            FXCollections.observableArrayList();

    private final ObservableList<BorrowerRow> borrowerList =
            FXCollections.observableArrayList();

    private FilteredList<RequestRow> filteredRequests;
    private FilteredList<BorrowerRow> filteredBorrowers;

    // INITIALIZE
    @FXML
    public void initialize() {

        if (StaffSession.fullName != null) {
            staffNameLabel.setText(StaffSession.fullName);
        }

        if (StaffSession.studentId != null) {
            staffIdLabel.setText(StaffSession.studentId);
        }

        setupRequestTable();
        setupSearch();

        loadDashboard();
    }

    // SET UP REQUEST TABLE
    private void setupRequestTable() {

        borrowerColumn.setCellValueFactory(
                new PropertyValueFactory<>("borrower"));

        itemColumn.setCellValueFactory(
                new PropertyValueFactory<>("item"));

        dueDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("dueDate"));

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status"));

        statusColumn.setCellFactory(column ->
                new TableCell<RequestRow, String>() {

            @Override
            protected void updateItem(String status, boolean empty) {

                super.updateItem(status, empty);

                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(status);

                switch (status) {
                    case "Pending":
                        badge.getStyleClass().add("status-pending");
                        break;

                    case "Approved":
                        badge.getStyleClass().add("status-approved");
                        break;

                    case "Reserved":
                        badge.getStyleClass().add("status-reserved");
                        break;

                    case "Overdue":
                        badge.getStyleClass().add("status-overdue");
                        break;

                    default:
                        badge.getStyleClass().add("status-other");
                        break;
                }

                setText(null);
                setGraphic(badge);
            }
        });

        actionColumn.setCellFactory(column ->
                new TableCell<RequestRow, Void>() {

            private final Button acceptButton = new Button("Accept");
            private final Button rejectButton = new Button("Reject");
            private final Button monitorButton =
                    new Button("Monitor Borrower");

            private final HBox buttons = new HBox(7);

            {
                acceptButton.getStyleClass().add("approve-button");
                rejectButton.getStyleClass().add("reject-button");
                monitorButton.getStyleClass().add("outline-button");

                acceptButton.setOnAction(event -> {
                    RequestRow row = getTableView()
                            .getItems().get(getIndex());

                    updateRequestStatus(row.getRequestId(), "Approved");
                });

                rejectButton.setOnAction(event -> {
                    RequestRow row = getTableView()
                            .getItems().get(getIndex());

                    updateRequestStatus(row.getRequestId(), "Rejected");
                });

                monitorButton.setOnAction(event ->
                        showBorrowersReturns());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {

                super.updateItem(item, empty);

                buttons.getChildren().clear();

                if (empty || getIndex() < 0
                        || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                RequestRow row = getTableView()
                        .getItems().get(getIndex());

                if ("Pending".equals(row.getStatus())) {
                    buttons.getChildren().addAll(
                            acceptButton, rejectButton);
                } else {
                    buttons.getChildren().add(monitorButton);
                }

                setGraphic(buttons);
            }
        });

        filteredRequests = new FilteredList<>(requestList, row -> true);

        requestsTable.setItems(filteredRequests);

        // FIX: Make table columns fill the entire table width
        requestsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );
    }

    // SEARCH
    private void setupSearch() {

        filteredBorrowers =
                new FilteredList<>(borrowerList, row -> true);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> {

            String search = newValue == null
                    ? ""
                    : newValue.toLowerCase().trim();

            filteredRequests.setPredicate(row -> {

                if (search.isEmpty()) {
                    return true;
                }

                return row.getBorrower().toLowerCase().contains(search)
                        || row.getItem().toLowerCase().contains(search)
                        || row.getStudentId().toLowerCase().contains(search);
            });

            filteredBorrowers.setPredicate(row -> {

                if (search.isEmpty()) {
                    return true;
                }

                return row.getBorrowerName().toLowerCase().contains(search)
                        || row.getStudentId().toLowerCase().contains(search)
                        || row.getItem().toLowerCase().contains(search);
            });

            displayBorrowerCards();
        });
    }

    // LOAD DASHBOARD
    @FXML
    private void loadDashboard() {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            // AVAILABLE ITEMS
            String availableSql =
                    "SELECT COALESCE(SUM(available_quantity), 0) AS total "
                  + "FROM items";

            try (PreparedStatement statement =
                         connection.prepareStatement(availableSql);
                 ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    availableLabel.setText(
                            String.valueOf(result.getInt("total")));
                }
            }

            // BORROWED
            String borrowedSql =
                    "SELECT COUNT(*) AS total "
                  + "FROM borrow_records "
                  + "WHERE status = 'Borrowed' "
                  + "AND return_date IS NULL";

            borrowedLabel.setText(
                    String.valueOf(getCount(connection, borrowedSql)));

            // OVERDUE
            String overdueSql =
                    "SELECT COUNT(*) AS total "
                  + "FROM borrow_records "
                  + "WHERE status = 'Borrowed' "
                  + "AND return_date IS NULL "
                  + "AND due_date < CURDATE()";

            overdueLabel.setText(
                    String.valueOf(getCount(connection, overdueSql)));

            // RESERVED
            String reservedSql =
                    "SELECT COUNT(*) AS total "
                  + "FROM borrow_requests "
                  + "WHERE status = 'Reserved'";

            reservedLabel.setText(
                    String.valueOf(getCount(connection, reservedSql)));

            // PENDING
            String pendingSql =
                    "SELECT COUNT(*) AS total "
                  + "FROM borrow_requests "
                  + "WHERE status = 'Pending'";

            pendingLabel.setText(
                    String.valueOf(getCount(connection, pendingSql)));

            loadRequests(connection);
            loadActiveBorrowers(connection);

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    "Unable to load Staff Dashboard data. "
                  + "Please check your database connection."
            );
        }
    }

    // GET DATABASE COUNT
    private int getCount(Connection connection, String sql)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            if (result.next()) {
                return result.getInt("total");
            }
        }

        return 0;
    }

    // LOAD REQUESTS
    private void loadRequests(Connection connection)
            throws SQLException {

        requestList.clear();

        String sql =
                "SELECT br.request_id, "
              + "u.full_name, u.student_id, u.department, u.block, "
              + "i.item_name, br.due_date, br.status, "
              + "CASE "
              + "WHEN rec.record_id IS NOT NULL "
              + "AND rec.return_date IS NULL "
              + "AND rec.due_date < CURDATE() "
              + "THEN 'Overdue' "
              + "ELSE br.status "
              + "END AS display_status "
              + "FROM borrow_requests br "
              + "JOIN users u ON br.user_id = u.user_id "
              + "JOIN items i ON br.item_id = i.item_id "
              + "LEFT JOIN borrow_records rec "
              + "ON br.request_id = rec.request_id "
              + "WHERE br.status NOT IN "
              + "('Rejected', 'Cancelled', 'Returned') "
              + "ORDER BY br.request_date DESC";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                String dueDate = result.getDate("due_date") == null
                        ? "Not set"
                        : result.getDate("due_date").toString();

                requestList.add(new RequestRow(
                        String.valueOf(result.getInt("request_id")),
                        result.getString("full_name"),
                        result.getString("student_id"),
                        result.getString("department"),
                        result.getString("block"),
                        result.getString("item_name"),
                        dueDate,
                        result.getString("display_status")
                ));
            }
        }
    }

    // LOAD ACTIVE BORROWERS
    private void loadActiveBorrowers(Connection connection)
            throws SQLException {

        borrowerList.clear();

        String sql =
                "SELECT br.record_id, u.full_name, u.student_id, "
              + "u.department, u.block, i.item_name, "
              + "br.due_date, "
              + "DATEDIFF(br.due_date, CURDATE()) AS days_left "
              + "FROM borrow_records br "
              + "JOIN users u ON br.user_id = u.user_id "
              + "JOIN items i ON br.item_id = i.item_id "
              + "WHERE br.status = 'Borrowed' "
              + "AND br.return_date IS NULL "
              + "ORDER BY br.due_date ASC";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                int daysLeft = result.getInt("days_left");

                String status = daysLeft < 0
                        ? "Overdue • " + Math.abs(daysLeft) + "d late"
                        : "Active • " + daysLeft + "d left";

                String dueDate = result.getDate("due_date") == null
                        ? "Not set"
                        : result.getDate("due_date").toString();

                borrowerList.add(new BorrowerRow(
                        result.getInt("record_id"),
                        result.getString("full_name"),
                        result.getString("student_id"),
                        result.getString("department"),
                        result.getString("block"),
                        result.getString("item_name"),
                        dueDate,
                        status
                ));
            }
        }

        displayBorrowerCards();
    }

    // DISPLAY BORROWER CARDS
    private void displayBorrowerCards() {

        activeBorrowersContainer.getChildren().clear();

        if (filteredBorrowers == null || filteredBorrowers.isEmpty()) {

            borrowerEmptyLabel.setVisible(true);
            borrowerEmptyLabel.setManaged(true);

            return;
        }

        borrowerEmptyLabel.setVisible(false);
        borrowerEmptyLabel.setManaged(false);

        for (BorrowerRow row : filteredBorrowers) {

            VBox card = new VBox(12);
            card.getStyleClass().add("borrower-card");

            boolean isOverdue =
                    row.getStatus().startsWith("Overdue");

            if (isOverdue) {
                card.getStyleClass().add("borrower-card-overdue");
            }

            HBox topRow = new HBox(8);
            topRow.setAlignment(
                    javafx.geometry.Pos.CENTER_LEFT);

            VBox borrowerInfo = new VBox(4);
            borrowerInfo.setMaxWidth(Double.MAX_VALUE);

            HBox.setHgrow(
                    borrowerInfo,
                    javafx.scene.layout.Priority.ALWAYS
            );

            Label nameLabel =
                    new Label(row.getBorrowerName());

            nameLabel.getStyleClass().add("borrower-card-name");

            Label detailsLabel = new Label(
                    row.getDepartment() + " • "
                    + row.getBlock() + " • "
                    + row.getStudentId());

            detailsLabel.getStyleClass().add(
                    "borrower-card-details");

            detailsLabel.setWrapText(true);

            borrowerInfo.getChildren().addAll(
                    nameLabel, detailsLabel);

            Label statusLabel = new Label(row.getStatus());

            statusLabel.getStyleClass().add(
                    "borrower-card-status");

            statusLabel.getStyleClass().add(
                    isOverdue
                            ? "borrower-status-overdue"
                            : "borrower-status-active");

            topRow.getChildren().addAll(
                    borrowerInfo, statusLabel);

            Label itemLabel =
                    new Label("Holding: " + row.getItem());

            itemLabel.getStyleClass().add(
                    "borrower-card-item");

            itemLabel.setWrapText(true);

            Label recordLabel = new Label(
                    "Record #" + row.getRecordId()
                    + " • Due: " + row.getDueDate());

            recordLabel.getStyleClass().add(
                    "borrower-card-details");

            Button inspectButton =
                    new Button("Inspect Profile");

            inspectButton.getStyleClass().add(
                    "inspect-button");

            inspectButton.setOnAction(event ->
                    inspectBorrower(row));

            Button returnButton = new Button("Return");

            returnButton.getStyleClass().add(
                    "return-button");

            returnButton.setOnAction(event ->
                    showBorrowersReturns());

            HBox buttonRow = new HBox(8);

            buttonRow.getChildren().addAll(
                    inspectButton, returnButton);

            card.getChildren().addAll(
                    topRow,
                    itemLabel,
                    recordLabel,
                    buttonRow
            );

            activeBorrowersContainer.getChildren().add(card);
        }
    }

    // INSPECT BORROWER
    private void inspectBorrower(BorrowerRow row) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Borrower Information");
        alert.setHeaderText(row.getBorrowerName());

        alert.setContentText(
                "Student ID: " + row.getStudentId()
              + "\nDepartment: " + row.getDepartment()
              + "\nBlock: " + row.getBlock()
              + "\nBorrowed Item: " + row.getItem()
              + "\nRecord ID: " + row.getRecordId()
              + "\nDue Date: " + row.getDueDate()
              + "\nStatus: " + row.getStatus()
        );

        alert.showAndWait();
    }

    // APPROVE OR REJECT REQUEST
    private void updateRequestStatus(
            String requestId,
            String newStatus) {

        String sql =
                "UPDATE borrow_requests "
              + "SET status = ? "
              + "WHERE request_id = ? "
              + "AND status = 'Pending'";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, newStatus);
            statement.setInt(2, Integer.parseInt(requestId));

            int updatedRows = statement.executeUpdate();

            if (updatedRows > 0) {

                showAlert(
                        "Request #" + requestId
                      + " has been "
                      + newStatus.toLowerCase() + "."
                );

                loadDashboard();

            } else {

                showAlert(
                        "This request is no longer pending. "
                      + "Please refresh the dashboard."
                );

                loadDashboard();
            }

        } catch (SQLException e) {

            e.printStackTrace();
            showAlert("Unable to update this request.");
        }
    }

    // SIDEBAR NAVIGATION
    @FXML
    private void showDashboard() {
        loadDashboard();
    }

    @FXML
    private void showItems() {
        openPage("staff-items");
    }

    @FXML
    private void showRequests() {
        openPage("staff-requests");
    }

    @FXML
    private void showBorrowersReturns() {
        openPage("staff-borrowers-returns");
    }

    @FXML
    private void showLiabilities() {
        openPage("staff-liabilities");
    }

    // LOG OUT
    @FXML
    private void logout() {

        try {
            App.setRoot("secondary");

        } catch (Exception e) {

            e.printStackTrace();
            showAlert("Unable to return to the login page.");
        }
    }

    // OPEN OTHER PAGES
    private void openPage(String page) {

        try {
            App.setRoot(page);

        } catch (Exception e) {

            e.printStackTrace();

            showAlert(
                    "Unable to open " + page + ".fxml\n"
                  + e.getMessage()
            );
        }
    }

    // ALERT
    private void showAlert(String message) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("HiramMate Staff Portal");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // REQUEST ROW
    public static class RequestRow {

        private final String requestId;
        private final String fullName;
        private final String studentId;
        private final String department;
        private final String block;
        private final String item;
        private final String dueDate;
        private final String status;

        public RequestRow(
                String requestId,
                String fullName,
                String studentId,
                String department,
                String block,
                String item,
                String dueDate,
                String status) {

            this.requestId = requestId;
            this.fullName = fullName;
            this.studentId = studentId;
            this.department = department;
            this.block = block;
            this.item = item;
            this.dueDate = dueDate;
            this.status = status;
        }

        public String getRequestId() {
            return requestId;
        }

        public String getBorrower() {
            return fullName + "\n"
                    + department + " • "
                    + block + " • "
                    + studentId;
        }

        public String getFullName() {
            return fullName;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getDepartment() {
            return department;
        }

        public String getBlock() {
            return block;
        }

        public String getItem() {
            return item;
        }

        public String getDueDate() {
            return dueDate;
        }

        public String getStatus() {
            return status;
        }
    }

    // BORROWER ROW
    public static class BorrowerRow {

        private final int recordId;
        private final String borrowerName;
        private final String studentId;
        private final String department;
        private final String block;
        private final String item;
        private final String dueDate;
        private final String status;

        public BorrowerRow(
                int recordId,
                String borrowerName,
                String studentId,
                String department,
                String block,
                String item,
                String dueDate,
                String status) {

            this.recordId = recordId;
            this.borrowerName = borrowerName;
            this.studentId = studentId;
            this.department = department;
            this.block = block;
            this.item = item;
            this.dueDate = dueDate;
            this.status = status;
        }

        public int getRecordId() {
            return recordId;
        }

        public String getBorrowerName() {
            return borrowerName;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getDepartment() {
            return department;
        }

        public String getBlock() {
            return block;
        }

        public String getItem() {
            return item;
        }

        public String getDueDate() {
            return dueDate;
        }

        public String getStatus() {
            return status;
        }
    }
}

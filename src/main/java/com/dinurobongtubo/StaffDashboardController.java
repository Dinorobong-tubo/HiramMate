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
import javafx.scene.control.ButtonType;
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

    // BORROW REQUEST TABLE
    @FXML private TableView<RequestRow> requestsTable;
    @FXML private TableColumn<RequestRow, String> borrowerColumn;
    @FXML private TableColumn<RequestRow, String> itemColumn;
    @FXML private TableColumn<RequestRow, String> dueDateColumn;
    @FXML private TableColumn<RequestRow, String> statusColumn;
    @FXML private TableColumn<RequestRow, Void> actionColumn;

    // EXTENSION REQUEST TABLE
    @FXML private TableView<ExtensionRow> extensionRequestsTable;
    @FXML private TableColumn<ExtensionRow, String> extensionStudentColumn;
    @FXML private TableColumn<ExtensionRow, String> extensionItemColumn;
    @FXML private TableColumn<ExtensionRow, String> extensionDaysColumn;
    @FXML private TableColumn<ExtensionRow, String> extensionDateColumn;
    @FXML private TableColumn<ExtensionRow, String> extensionStatusColumn;
    @FXML private TableColumn<ExtensionRow, Void> extensionActionColumn;

    // ACTIVE BORROWER CARDS
    @FXML private HBox activeBorrowersContainer;
    @FXML private Label borrowerEmptyLabel;

    // DATA
    private final ObservableList<RequestRow> requestList =
            FXCollections.observableArrayList();

    private final ObservableList<BorrowerRow> borrowerList =
            FXCollections.observableArrayList();

    private final ObservableList<ExtensionRow> extensionList =
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
        setupExtensionTable();
        setupSearch();

        loadDashboard();
    }

    // SET UP BORROW REQUEST TABLE
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

        requestsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );
    }

    // SET UP EXTENSION REQUEST TABLE
    private void setupExtensionTable() {

        extensionStudentColumn.setCellValueFactory(
                new PropertyValueFactory<>("student"));

        extensionItemColumn.setCellValueFactory(
                new PropertyValueFactory<>("item"));

        extensionDaysColumn.setCellValueFactory(
                new PropertyValueFactory<>("requestedDays"));

        extensionDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("requestDate"));

        extensionStatusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status"));

        extensionActionColumn.setCellFactory(column ->
                new TableCell<ExtensionRow, Void>() {

            private final Button approveButton = new Button("Approve");
            private final Button rejectButton = new Button("Reject");
            private final HBox buttons = new HBox(7);

            {
                approveButton.getStyleClass().add("approve-button");
                rejectButton.getStyleClass().add("reject-button");

                approveButton.setOnAction(event -> {
                    ExtensionRow row = getTableRow().getItem();

                    if (row != null) {
                        processExtensionRequest(row, "Approved");
                    }
                });

                rejectButton.setOnAction(event -> {
                    ExtensionRow row = getTableRow().getItem();

                    if (row != null) {
                        processExtensionRequest(row, "Rejected");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {

                super.updateItem(item, empty);

                buttons.getChildren().clear();

                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                ExtensionRow row = getTableRow().getItem();

                if ("Pending".equalsIgnoreCase(row.getStatus())) {
                    buttons.getChildren().addAll(
                            approveButton, rejectButton);
                }

                setGraphic(buttons);
            }
        });

        extensionRequestsTable.setItems(extensionList);

        extensionRequestsTable.setColumnResizePolicy(
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

            // PENDING BORROW REQUESTS
            String pendingSql =
                    "SELECT COUNT(*) AS total "
                  + "FROM borrow_requests "
                  + "WHERE status = 'Pending'";

            pendingLabel.setText(
                    String.valueOf(getCount(connection, pendingSql)));

            loadRequests(connection);
            loadExtensionRequests(connection);
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

    // LOAD BORROW REQUESTS
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

    // LOAD EXTENSION REQUESTS
    private void loadExtensionRequests(Connection connection)
            throws SQLException {

        extensionList.clear();

        String sql =
                "SELECT er.extension_id, er.record_id, er.user_id, "
              + "u.full_name, u.student_id, i.item_name, "
              + "er.requested_days, er.request_date, er.status "
              + "FROM extension_requests er "
              + "JOIN users u ON er.user_id = u.user_id "
              + "JOIN borrow_records br ON er.record_id = br.record_id "
              + "JOIN items i ON br.item_id = i.item_id "
              + "ORDER BY CASE WHEN er.status = 'Pending' "
              + "THEN 0 ELSE 1 END, er.request_date DESC";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                String requestDate =
                        result.getTimestamp("request_date") == null
                        ? "Not available"
                        : result.getTimestamp("request_date").toString();

                extensionList.add(new ExtensionRow(
                        result.getInt("extension_id"),
                        result.getInt("record_id"),
                        result.getInt("user_id"),
                        result.getString("full_name")
                                + " (" + result.getString("student_id") + ")",
                        result.getString("item_name"),
                        result.getInt("requested_days") + " days",
                        requestDate,
                        result.getString("status")
                ));
            }
        }
    }

    // APPROVE OR REJECT EXTENSION REQUEST
    private void processExtensionRequest(
            ExtensionRow row,
            String decision) {

        Alert confirmation =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Extension Request");
        confirmation.setHeaderText(decision + " Extension Request?");
        confirmation.setContentText(
                "Student: " + row.getStudent()
                + "\nItem: " + row.getItem()
                + "\nRequested extension: " + row.getRequestedDays()
        );

        if (confirmation.showAndWait().orElse(null) != ButtonType.OK) {
            return;
        }

        String lockExtensionSql =
                "SELECT record_id, user_id, requested_days "
              + "FROM extension_requests "
              + "WHERE extension_id = ? AND status = 'Pending' "
              + "FOR UPDATE";

        String lockRecordSql =
                "SELECT due_date, return_date "
              + "FROM borrow_records "
              + "WHERE record_id = ? AND user_id = ? "
              + "FOR UPDATE";

        String updateRecordSql =
                "UPDATE borrow_records "
              + "SET due_date = DATE_ADD(due_date, INTERVAL ? DAY) "
              + "WHERE record_id = ? AND return_date IS NULL";

        String updateExtensionSql =
                "UPDATE extension_requests "
              + "SET status = ? "
              + "WHERE extension_id = ? AND status = 'Pending'";

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                int recordId;
                int userId;
                int requestedDays;

                // LOCK AND VERIFY THE EXTENSION REQUEST
                try (PreparedStatement statement =
                             connection.prepareStatement(lockExtensionSql)) {

                    statement.setInt(1, row.getExtensionId());

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new SQLException(
                                    "This extension request is no longer pending."
                            );
                        }

                        recordId = result.getInt("record_id");
                        userId = result.getInt("user_id");
                        requestedDays = result.getInt("requested_days");
                    }
                }

                // VERIFY THAT THE BORROW RECORD IS STILL ACTIVE
                try (PreparedStatement statement =
                             connection.prepareStatement(lockRecordSql)) {

                    statement.setInt(1, recordId);
                    statement.setInt(2, userId);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new SQLException(
                                    "The borrow record could not be found."
                            );
                        }

                        if (result.getDate("return_date") != null) {
                            throw new SQLException(
                                    "This item has already been returned. "
                                    + "The extension cannot be processed."
                            );
                        }

                        if (result.getDate("due_date") == null) {
                            throw new SQLException(
                                    "The borrow record has no due date."
                            );
                        }
                    }
                }

                // EXTEND THE DUE DATE ONLY IF APPROVED
                if ("Approved".equals(decision)) {

                    try (PreparedStatement statement =
                                 connection.prepareStatement(updateRecordSql)) {

                        statement.setInt(1, requestedDays);
                        statement.setInt(2, recordId);

                        if (statement.executeUpdate() != 1) {
                            throw new SQLException(
                                    "Unable to update the borrow record due date."
                            );
                        }
                    }
                }

                // UPDATE EXTENSION REQUEST STATUS
                try (PreparedStatement statement =
                             connection.prepareStatement(updateExtensionSql)) {

                    statement.setString(1, decision);
                    statement.setInt(2, row.getExtensionId());

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Unable to update the extension request status."
                        );
                    }
                }

                connection.commit();

                showAlert(
                        "Extension request #"
                        + row.getExtensionId()
                        + " has been "
                        + decision.toLowerCase()
                        + "."
                        + ("Approved".equals(decision)
                        ? "\nThe due date has been extended by "
                                + requestedDays + " days."
                        : "")
                );

                loadDashboard();

            } catch (SQLException e) {

                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    "Unable to process extension request:\n"
                    + e.getMessage()
            );
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

    // APPROVE OR REJECT BORROW REQUEST
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

    // EXTENSION REQUEST ROW
    public static class ExtensionRow {

        private final int extensionId;
        private final int recordId;
        private final int userId;
        private final String student;
        private final String item;
        private final String requestedDays;
        private final String requestDate;
        private final String status;

        public ExtensionRow(
                int extensionId,
                int recordId,
                int userId,
                String student,
                String item,
                String requestedDays,
                String requestDate,
                String status) {

            this.extensionId = extensionId;
            this.recordId = recordId;
            this.userId = userId;
            this.student = student;
            this.item = item;
            this.requestedDays = requestedDays;
            this.requestDate = requestDate;
            this.status = status;
        }

        public int getExtensionId() {
            return extensionId;
        }

        public int getRecordId() {
            return recordId;
        }

        public int getUserId() {
            return userId;
        }

        public String getStudent() {
            return student;
        }

        public String getItem() {
            return item;
        }

        public String getRequestedDays() {
            return requestedDays;
        }

        public String getRequestDate() {
            return requestDate;
        }

        public String getStatus() {
            return status;
        }
    }

    // BORROW REQUEST ROW
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

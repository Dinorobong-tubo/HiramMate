package com.dinurobongtubo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StudentDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label profileName;
    @FXML private Label profileDetails;
    @FXML private Label profileInitials;

    @FXML private TextField dashboardSearchField;

    @FXML private Label borrowedCount;
    @FXML private Label pendingCount;
    @FXML private Label overdueCount;
    @FXML private Label reservedCount;

    @FXML private VBox activeRecordsContainer;

    @FXML
    public void initialize() {

        String name = StudentSession.fullName;
        String studentId = StudentSession.studentId;
        String department = StudentSession.department;

        if (name == null || name.isBlank()) {
            name = "Student";
        }

        if (studentId == null || studentId.isBlank()) {
            studentId = "No Student ID";
        }

        if (department == null || department.isBlank()) {
            department = "No Department";
        }

        welcomeLabel.setText(
            "Welcome back, " + name +
            " — Track your campus equipment borrows and requests."
        );

        profileName.setText(name);
        profileDetails.setText(studentId + " • " + department);

        String[] nameParts = name.trim().split("\\s+");
        String initials = "";

        if (nameParts.length >= 2) {
            initials = nameParts[0].substring(0, 1)
                + nameParts[nameParts.length - 1].substring(0, 1);
        } else if (nameParts.length == 1 && !nameParts[0].isEmpty()) {
            initials = nameParts[0].substring(0, 1);
        }

        profileInitials.setText(initials.toUpperCase());

        if (dashboardSearchField != null) {
            dashboardSearchField.setOnAction(event -> {
                try {
                    showBrowseItems();
                } catch (IOException e) {
                    showError("Unable to open Browse Items.");
                    e.printStackTrace();
                }
            });
        }

        loadDashboardData();
    }

    // LOAD DASHBOARD DATA

    private void loadDashboardData() {

        if (StudentSession.studentId == null
                || StudentSession.studentId.isBlank()) {
            showError("Student information is missing. Please log in again.");
            return;
        }

        try (Connection connection = DatabaseConnection.getConnection()) {

            // 1. Currently borrowed quantity
            String borrowedSQL =
                "SELECT COALESCE(SUM(r.quantity), 0) " +
                "FROM borrow_records r " +
                "INNER JOIN users u ON r.user_id = u.user_id " +
                "WHERE u.student_id = ? " +
                "AND r.status = 'Borrowed' " +
                "AND r.return_date IS NULL";

            borrowedCount.setText(
                String.valueOf(getNumber(connection, borrowedSQL))
            );

            // 2. Pending requests
            String pendingSQL =
                "SELECT COUNT(*) " +
                "FROM borrow_requests br " +
                "INNER JOIN users u ON br.user_id = u.user_id " +
                "WHERE u.student_id = ? " +
                "AND br.status = 'Pending'";

            pendingCount.setText(
                String.valueOf(getNumber(connection, pendingSQL))
            );

            // 3. Overdue borrowed quantity
            String overdueSQL =
                "SELECT COALESCE(SUM(r.quantity), 0) " +
                "FROM borrow_records r " +
                "INNER JOIN users u ON r.user_id = u.user_id " +
                "WHERE u.student_id = ? " +
                "AND r.status = 'Borrowed' " +
                "AND r.return_date IS NULL " +
                "AND r.due_date < CURDATE()";

            overdueCount.setText(
                String.valueOf(getNumber(connection, overdueSQL))
            );

            // 4. Approved or reserved quantity not yet borrowed
            String reservedSQL =
                "SELECT COALESCE(SUM(br.quantity), 0) " +
                "FROM borrow_requests br " +
                "INNER JOIN users u ON br.user_id = u.user_id " +
                "WHERE u.student_id = ? " +
                "AND br.status IN ('Approved', 'Reserved') " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM borrow_records r " +
                "    WHERE r.request_id = br.request_id " +
                "    AND r.status = 'Borrowed' " +
                "    AND r.return_date IS NULL" +
                ")";

            reservedCount.setText(
                String.valueOf(getNumber(connection, reservedSQL))
            );

            // 5. Load latest active requests and borrows
            loadActiveRecords(connection);

        } catch (SQLException e) {
            showError("Unable to load dashboard information: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // GET A NUMBER FROM THE DATABASE

    private int getNumber(Connection connection, String sql)
            throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, StudentSession.studentId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
        }

        return 0;
    }

    // LOAD ACTIVE BORROWS AND REQUESTS

    private void loadActiveRecords(Connection connection)
            throws SQLException {

        activeRecordsContainer.getChildren().clear();

        String sql =
            "SELECT br.request_id, i.item_name, i.category, " +
            "COALESCE(r.due_date, br.due_date) AS display_due_date, " +
            "CASE WHEN r.status = 'Borrowed' THEN 'Borrowed' " +
            "ELSE br.status END AS display_status " +
            "FROM borrow_requests br " +
            "INNER JOIN users u ON br.user_id = u.user_id " +
            "INNER JOIN items i ON br.item_id = i.item_id " +
            "LEFT JOIN borrow_records r ON br.request_id = r.request_id " +
            "WHERE u.student_id = ? " +
            "AND (" +
            "    br.status IN ('Pending', 'Approved', 'Reserved') " +
            "    OR (r.status = 'Borrowed' AND r.return_date IS NULL)" +
            ") " +
            "ORDER BY br.request_date DESC " +
            "LIMIT 5";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, StudentSession.studentId);

            try (ResultSet result = statement.executeQuery()) {

                int rowCount = 0;

                while (result.next()) {

                    rowCount++;

                    String itemName = result.getString("item_name");
                    String category = result.getString("category");
                    String dueDate = result.getString("display_due_date");
                    String status = result.getString("display_status");

                    if (dueDate == null) {
                        dueDate = "Not yet assigned";
                    }

                    GridPane row = new GridPane();
                    row.getStyleClass().add("dashboard-record-row");

                    row.setHgap(10);
                    row.setMaxWidth(Double.MAX_VALUE);

                    Label itemLabel = new Label(itemName);
                    Label categoryLabel = new Label(category);
                    Label dueLabel = new Label(dueDate);
                    Label statusLabel = new Label(status);
                    Label actionLabel = new Label("View requests");

                    itemLabel.setWrapText(true);
                    categoryLabel.setWrapText(true);

                    itemLabel.setMaxWidth(Double.MAX_VALUE);
                    categoryLabel.setMaxWidth(Double.MAX_VALUE);
                    dueLabel.setMaxWidth(Double.MAX_VALUE);
                    statusLabel.setMaxWidth(Double.MAX_VALUE);
                    actionLabel.setMaxWidth(Double.MAX_VALUE);

                    GridPane.setHgrow(itemLabel, Priority.ALWAYS);
                    GridPane.setHgrow(categoryLabel, Priority.ALWAYS);
                    GridPane.setHgrow(dueLabel, Priority.ALWAYS);
                    GridPane.setHgrow(statusLabel, Priority.ALWAYS);
                    GridPane.setHgrow(actionLabel, Priority.ALWAYS);

                    row.add(itemLabel, 0, 0);
                    row.add(categoryLabel, 1, 0);
                    row.add(dueLabel, 2, 0);
                    row.add(statusLabel, 3, 0);
                    row.add(actionLabel, 4, 0);

                    row.setColumnSpan(itemLabel, 1);

                    activeRecordsContainer.getChildren().add(row);
                }

                if (rowCount == 0) {
                    Label emptyMessage = new Label(
                        "Your active borrows and pending requests will appear here."
                    );

                    emptyMessage.getStyleClass().add("empty-message");
                    emptyMessage.setWrapText(true);

                    activeRecordsContainer.setAlignment(
                        javafx.geometry.Pos.CENTER
                    );

                    activeRecordsContainer.getChildren().add(emptyMessage);
                }
            }
        }
    }

    // NAVIGATION

    @FXML
    private void showDashboard() {
        // Already on the dashboard.
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

    @FXML
    private void showLiabilities() throws IOException {
        App.setRoot("liabilities");
    }

    // DASHBOARD SHORTCUTS

    @FXML
    private void openBrowseItems() throws IOException {
        showBrowseItems();
    }

    @FXML
    private void openMyRequests() throws IOException {
        showMyRequests();
    }

    // LOG OUT

    @FXML
    private void handleLogout() throws IOException {

        StudentSession.fullName = null;
        StudentSession.studentId = null;
        StudentSession.department = null;

        App.setRoot("primary");
    }

    // ERROR MESSAGE

    private void showError(String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("HiramMate");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // COMING SOON MESSAGE

    @FXML
    private void showComingSoon(ActionEvent event) {

        Button clickedButton = (Button) event.getSource();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("HiramMate");
        alert.setHeaderText(clickedButton.getText());
        alert.setContentText(
            "This feature will be implemented in the next development stage."
        );

        alert.showAndWait();
    }
}

package com.dinurobongtubo;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class StudentDashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label profileName;

    @FXML
    private Label profileDetails;

    @FXML
    private Label profileInitials;

    @FXML
    private TextField dashboardSearchField;

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

        // Welcome message
        welcomeLabel.setText(
            "Welcome back, " + name +
            " — Track your campus equipment borrows and requests."
        );

        // Student profile
        profileName.setText(name);
        profileDetails.setText(studentId + " • " + department);

        // Generate student initials
        String[] nameParts = name.trim().split("\\s+");
        String initials = "";

        if (nameParts.length >= 2) {
            initials = nameParts[0].substring(0, 1)
                    + nameParts[nameParts.length - 1].substring(0, 1);
        } else if (nameParts.length == 1 && !nameParts[0].isEmpty()) {
            initials = nameParts[0].substring(0, 1);
        }

        profileInitials.setText(initials.toUpperCase());

        // Search field: press Enter to open Browse Items
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

        // Clear student session
        StudentSession.fullName = null;
        StudentSession.studentId = null;
        StudentSession.department = null;

        // Return to login screen
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

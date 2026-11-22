
package com.dinurobongtubo;

import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SecondaryController {

    @FXML
    private TextField studentIdField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void login() throws IOException {

        String studentId = studentIdField.getText().trim();
        String password = passwordField.getText();

        if (studentId.isEmpty() || password.isEmpty()) {
            showAlert("Please enter your Student ID and password.");
            return;
        }

        String sql = "SELECT full_name, student_id, department, block, "
                   + "password_hash, user_role "
                   + "FROM users WHERE student_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentId);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {

                    String storedHash = result.getString("password_hash");

                    if (verifyPassword(password, storedHash)) {

                        String fullName = result.getString("full_name");
                        String actualStudentId = result.getString("student_id");
                        String department = result.getString("department");
                        String block = result.getString("block");
                        String userRole = result.getString("user_role");

                        // Keep the existing student session information.
                        StudentSession.fullName = fullName;
                        StudentSession.studentId = actualStudentId;
                        StudentSession.department = department;
                        StudentSession.block = block;

                        // Route the user based on their role.
                        if (userRole == null) {
                            showAlert("Your account has no assigned role. Please contact the administrator.");
                            return;
                        }

                        switch (userRole.trim().toLowerCase()) {

                            case "student":
                                App.setRoot("student-dashboard");
                                break;

                            case "staff":
                                StaffSession.fullName = fullName;
                                StaffSession.studentId = actualStudentId;
                                App.setRoot("staff-dashboard");
                                break;

                            case "admin":
                                showAlert("Admin Dashboard is not available yet.");
                                break;

                            default:
                                showAlert("Unrecognized account role: " + userRole);
                                break;
                        }

                    } else {
                        showAlert("Incorrect Student ID or password.");
                    }

                } else {
                    showAlert("Incorrect Student ID or password.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Unable to log in. Please check your database connection.");
        }
    }

    private boolean verifyPassword(String password, String storedHash)
            throws Exception {

        String[] parts = storedHash.split(":");

        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                256
        );

        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] actualHash = factory.generateSecret(spec).getEncoded();

        spec.clearPassword();

        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("HiramMate Login");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }
}

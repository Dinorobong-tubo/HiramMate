package com.dinurobongtubo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class PrimaryController {

    // FULL NAME
    @FXML
    private TextField fullNameField;

    // STUDENT ID
    @FXML
    private TextField studentIdField;

    // CONTACT NUMBER
    @FXML
    private TextField contactNumberField;

    // DEPARTMENT DROPDOWN
    @FXML
    private ComboBox<String> departmentComboBox;

    // BLOCK INPUT
    @FXML
    private TextField blockTextField;

    // PASSWORD FIELDS
    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    // BORROW AND DONATE RADIO BUTTONS
    @FXML
    private RadioButton borrowRadio;

    @FXML
    private RadioButton donateRadio;

    // BORROW AND DONATE CONTAINERS
    @FXML
    private HBox borrowOptionBox;

    @FXML
    private HBox donateOptionBox;

    // BUTTON AND LOGIN LINK
    @FXML
    private Button createAccountButton;

    @FXML
    private Hyperlink loginLink;


    // INITIALIZE THE FORM
    @FXML
    public void initialize() {

        // DEPARTMENT CHOICES
        departmentComboBox.getItems().addAll(
            "CICS - College of Informatics and Computing Sciences",
            "CAFAD - College of Architecture, Fine Arts and Design",
            "COE - College of Engineering",
            "CET - College of Engineering Technology"
        );

        // CREATE RADIO BUTTON GROUP
        ToggleGroup accountTypeGroup = new ToggleGroup();

        borrowRadio.setToggleGroup(accountTypeGroup);
        donateRadio.setToggleGroup(accountTypeGroup);

        // DEFAULT SELECTION
        borrowRadio.setSelected(true);

        // WATCH BORROW SELECTION
        borrowRadio.selectedProperty().addListener(
            (observable, oldValue, newValue) -> updateOptionStyles()
        );

        // WATCH DONATE SELECTION
        donateRadio.selectedProperty().addListener(
            (observable, oldValue, newValue) -> updateOptionStyles()
        );

        // MAKE BORROW CARD CLICKABLE
        borrowOptionBox.setOnMouseClicked(event -> {
            borrowRadio.setSelected(true);
        });

        // MAKE DONATE CARD CLICKABLE
        donateOptionBox.setOnMouseClicked(event -> {
            donateRadio.setSelected(true);
        });

        // APPLY INITIAL STYLE
        updateOptionStyles();
    }


    // UPDATE BORROW AND DONATE HIGHLIGHTS
    private void updateOptionStyles() {

        String selectedStyle =
            "-fx-background-color: #FBE9ED;" +
            "-fx-border-color: #AD2638;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 15;" +
            "-fx-background-radius: 15;" +
            "-fx-padding: 12;";

        String unselectedStyle =
            "-fx-background-color: white;" +
            "-fx-border-color: #DDDDDD;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 15;" +
            "-fx-background-radius: 15;" +
            "-fx-padding: 12;";

        if (borrowRadio.isSelected()) {

            borrowOptionBox.setStyle(selectedStyle);
            donateOptionBox.setStyle(unselectedStyle);

        } else {

            borrowOptionBox.setStyle(unselectedStyle);
            donateOptionBox.setStyle(selectedStyle);
        }
    }


    // CREATE ACCOUNT
    @FXML
    private void createAccount() {

        // GET FORM INFORMATION
        String fullName = fullNameField.getText().trim();
        String studentId = studentIdField.getText().trim();
        String contactNumber = contactNumberField.getText().trim();
        String department = departmentComboBox.getValue();
        String block = blockTextField.getText().trim();

        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        String accountType = borrowRadio.isSelected()
            ? "Borrow"
            : "Donate";


        // CHECK EMPTY FIELDS
        if (fullName.isEmpty() ||
            studentId.isEmpty() ||
            contactNumber.isEmpty() ||
            department == null ||
            block.isEmpty() ||
            password.isEmpty() ||
            confirmPassword.isEmpty()) {

            showAlert(
                Alert.AlertType.WARNING,
                "Incomplete Information",
                "Please fill in all fields."
            );

            return;
        }


        // CHECK PASSWORD MATCH
        if (!password.equals(confirmPassword)) {

            showAlert(
                Alert.AlertType.WARNING,
                "Password Mismatch",
                "Your passwords do not match."
            );

            return;
        }


        // CHECK PASSWORD LENGTH
        if (password.length() < 8) {

            showAlert(
                Alert.AlertType.WARNING,
                "Weak Password",
                "Your password must have at least 8 characters."
            );

            return;
        }


        // HASH PASSWORD
        String passwordHash;

        try {

            passwordHash = hashPassword(password);

        } catch (Exception e) {

            e.printStackTrace();

            showAlert(
                Alert.AlertType.ERROR,
                "Password Error",
                "Unable to process your password."
            );

            return;
        }


        // SQL INSERT STATEMENT
        String sql =
            "INSERT INTO users " +
            "(full_name, student_id, contact_number, " +
            "department, block, account_type, password_hash) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";


        // CONNECT AND SAVE TO DATABASE
        try (
            Connection connection = DatabaseConnection.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {

            statement.setString(1, fullName);
            statement.setString(2, studentId);
            statement.setString(3, contactNumber);
            statement.setString(4, department);
            statement.setString(5, block);
            statement.setString(6, accountType);
            statement.setString(7, passwordHash);

            statement.executeUpdate();


            // SUCCESS MESSAGE
            showAlert(
                Alert.AlertType.INFORMATION,
                "Registration Successful",
                "Your HiramMate account has been created!"
            );

            // CLEAR FORM
            clearFields();


        } catch (SQLException e) {

            e.printStackTrace();

            // CHECK DUPLICATE STUDENT ID
            if (e.getSQLState() != null &&
                e.getSQLState().startsWith("23")) {

                showAlert(
                    Alert.AlertType.WARNING,
                    "Registration Failed",
                    "This Student ID is already registered."
                );

            } else {

                showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Unable to save your account. Please try again."
                );
            }
        }
    }


    // PASSWORD HASHING USING PBKDF2
    private String hashPassword(String password) throws Exception {

        int iterations = 120000;

        byte[] salt = new byte[16];

        new SecureRandom().nextBytes(salt);

        KeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            256
        );

        SecretKeyFactory factory =
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] hash = factory.generateSecret(spec).getEncoded();

        return iterations + ":" +
            Base64.getEncoder().encodeToString(salt) + ":" +
            Base64.getEncoder().encodeToString(hash);
    }


    // CLEAR FORM AFTER SUCCESSFUL REGISTRATION
    private void clearFields() {

        fullNameField.clear();
        studentIdField.clear();
        contactNumberField.clear();

        departmentComboBox.getSelectionModel().clearSelection();

        blockTextField.clear();

        passwordField.clear();
        confirmPasswordField.clear();

        borrowRadio.setSelected(true);
    }


    // DISPLAY ALERT MESSAGES
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


    // SWITCH TO SECONDARY SCREEN
    @FXML
    private void switchToSecondary() throws IOException {

        App.setRoot("secondary");
    }
}

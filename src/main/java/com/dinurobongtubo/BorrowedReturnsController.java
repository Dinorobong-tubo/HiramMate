package com.dinurobongtubo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class BorrowedReturnsController {

    @FXML
    private Label profileInitials;

    @FXML
    private Label profileName;

    @FXML
    private Label profileDetails;

    @FXML
    private GridPane borrowedItemsGrid;

    private final DateTimeFormatter dateFormatter =
        DateTimeFormatter.ofPattern("MMMM d, yyyy");

    @FXML
    public void initialize() {
        loadProfile();
        loadBorrowedItems();
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

        profileInitials.setText(
            initials.length() > 2
                ? initials.substring(0, 2).toUpperCase()
                : initials.toUpperCase()
        );

        String studentId = StudentSession.studentId;

        profileDetails.setText(
            studentId != null ? studentId : "No Student ID"
        );
    }

    // ==========================================
    // LOAD BORROWED ITEMS FROM DATABASE
    // ==========================================

    private void loadBorrowedItems() {

        borrowedItemsGrid.getChildren().clear();

        String sql =
            "SELECT br.request_id, br.quantity, br.purpose, " +
            "br.pickup_date, br.due_date, br.status, " +
            "i.item_code, i.item_name, i.category, " +
            "r.borrow_date, r.due_date AS record_due_date, " +
            "r.status AS record_status " +
            "FROM borrow_requests br " +
            "INNER JOIN users u ON br.user_id = u.user_id " +
            "INNER JOIN items i ON br.item_id = i.item_id " +
            "LEFT JOIN borrow_records r ON br.request_id = r.request_id " +
            "WHERE u.student_id = ? " +
            "AND (br.status IN ('Approved', 'Reserved') " +
            "OR r.status = 'Borrowed') " +
            "ORDER BY br.request_date DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, StudentSession.studentId);

            try (ResultSet result = statement.executeQuery()) {

                int column = 0;
                int row = 0;
                int count = 0;

                while (result.next()) {

                    int requestId = result.getInt("request_id");

                    String itemCode = result.getString("item_code");
                    String itemName = result.getString("item_name");
                    String category = result.getString("category");
                    String purpose = result.getString("purpose");

                    int quantity = result.getInt("quantity");

                    String requestStatus =
                        result.getString("status");

                    String recordStatus =
                        result.getString("record_status");

                    java.sql.Date borrowDate =
                        result.getDate("borrow_date");

                    LocalDate pickupDate =
                        borrowDate != null
                            ? borrowDate.toLocalDate()
                            : getLocalDate(result, "pickup_date");

                    java.sql.Date recordDueDate =
                        result.getDate("record_due_date");

                    LocalDate dueDate =
                        recordDueDate != null
                            ? recordDueDate.toLocalDate()
                            : getLocalDate(result, "due_date");

                    String displayStatus =
                        recordStatus != null
                            ? recordStatus
                            : requestStatus;

                    VBox card = createBorrowedCard(
                        requestId,
                        itemCode,
                        itemName,
                        category,
                        purpose,
                        quantity,
                        pickupDate,
                        dueDate,
                        displayStatus,
                        requestStatus
                    );

                    borrowedItemsGrid.add(card, column, row);

                    column++;

                    if (column >= 2) {
                        column = 0;
                        row++;
                    }

                    count++;
                }

                if (count == 0) {
                    showEmptyMessage();
                }
            }

        } catch (SQLException e) {

            showAlert(
                Alert.AlertType.ERROR,
                "Database Error",
                "Unable to load your borrowed items.\n\n" +
                e.getMessage()
            );
        }
    }

    // ==========================================
    // CREATE BORROWED ITEM CARD
    // ==========================================

    private VBox createBorrowedCard(
        int requestId,
        String itemCode,
        String itemName,
        String category,
        String purpose,
        int quantity,
        LocalDate pickupDate,
        LocalDate dueDate,
        String displayStatus,
        String requestStatus
    ) {

        VBox card = new VBox(14);
        card.getStyleClass().add("borrow-card");
        card.setPadding(new Insets(18));

        // REQUEST NUMBER AND STATUS

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label requestLabel = new Label(
            String.format("REQ-%04d", requestId)
        );

        requestLabel.getStyleClass().add("request-id");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(displayStatus);
        statusLabel.getStyleClass().add(getStatusStyle(displayStatus));

        header.getChildren().addAll(
            requestLabel,
            spacer,
            statusLabel
        );

        // ITEM INFORMATION

        VBox itemInformation = new VBox(5);

        Label nameLabel = new Label(itemName);
        nameLabel.getStyleClass().add("item-title");

        Label descriptionLabel = new Label(
            category + " • " +
            (purpose == null || purpose.isBlank()
                ? "No borrowing purpose provided"
                : purpose)
        );

        descriptionLabel.getStyleClass().add("item-description");
        descriptionLabel.setWrapText(true);

        Label codeLabel = new Label(
            "Item Code: " + itemCode + " | Quantity: " + quantity
        );

        codeLabel.getStyleClass().add("date-label");

        itemInformation.getChildren().addAll(
            nameLabel,
            descriptionLabel,
            codeLabel
        );

        // PICKUP AND DUE DATES

        HBox dateSection = new HBox(25);

        VBox pickupBox = new VBox(5);
        HBox.setHgrow(pickupBox, Priority.ALWAYS);

        Label pickupTitle = new Label("Pickup date");
        pickupTitle.getStyleClass().add("date-label");

        Label pickupValue = new Label(formatDate(pickupDate));
        pickupValue.getStyleClass().add("date-value");

        pickupBox.getChildren().addAll(
            pickupTitle,
            pickupValue
        );

        VBox dueBox = new VBox(5);
        HBox.setHgrow(dueBox, Priority.ALWAYS);

        Label dueTitle = new Label("Return due date");
        dueTitle.getStyleClass().add("date-label");

        Label dueValue = new Label(formatDate(dueDate));
        dueValue.getStyleClass().add("date-value");

        dueBox.getChildren().addAll(
            dueTitle,
            dueValue
        );

        dateSection.getChildren().addAll(
            pickupBox,
            dueBox
        );

        // ACTION BUTTONS

        HBox buttons = new HBox(10);

        Button extensionButton =
            new Button("Request 2-day extension");

        extensionButton.getStyleClass().add("primary-button");
        extensionButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(extensionButton, Priority.ALWAYS);

        extensionButton.setOnAction(event -> showComingSoon());

        Button actionButton = new Button("Pass QR");
        actionButton.getStyleClass().add("secondary-button");

        if ("Reserved".equalsIgnoreCase(requestStatus)) {

            actionButton.setText("Cancel reservation");

            actionButton.setOnAction(event ->
                cancelReservation(requestId)
            );

        } else {

            actionButton.setOnAction(event ->
                showComingSoon()
            );
        }

        buttons.getChildren().addAll(
            extensionButton,
            actionButton
        );

        // ASSEMBLE CARD

        card.getChildren().addAll(
            header,
            itemInformation,
            dateSection,
            buttons
        );

        return card;
    }

    // ==========================================
    // CANCEL RESERVATION
    // ==========================================

    private void cancelReservation(int requestId) {

        Alert confirmation =
            new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Cancel Reservation");
        confirmation.setHeaderText("Cancel this reservation?");
        confirmation.setContentText(
            "Are you sure you want to cancel this reservation?"
        );

        if (confirmation.showAndWait().isEmpty() ||
            confirmation.getResult().getButtonData() !=
                ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        String sql =
            "UPDATE borrow_requests " +
            "SET status = 'Cancelled' " +
            "WHERE request_id = ? " +
            "AND status = 'Reserved' " +
            "AND NOT EXISTS (" +
            "SELECT 1 FROM borrow_records " +
            "WHERE borrow_records.request_id = borrow_requests.request_id" +
            ")";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setInt(1, requestId);

            int updated = statement.executeUpdate();

            if (updated > 0) {

                showAlert(
                    Alert.AlertType.INFORMATION,
                    "Reservation Cancelled",
                    "Your reservation has been cancelled successfully."
                );

                loadBorrowedItems();

            } else {

                showAlert(
                    Alert.AlertType.WARNING,
                    "Unable to Cancel",
                    "This reservation can no longer be cancelled."
                );
            }

        } catch (SQLException e) {

            showAlert(
                Alert.AlertType.ERROR,
                "Database Error",
                "Unable to cancel the reservation.\n\n" +
                e.getMessage()
            );
        }
    }

    // ==========================================
    // EMPTY MESSAGE - CENTERED
    // ==========================================

    private void showEmptyMessage() {

        VBox emptyBox = new VBox(10);

        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(35));
        emptyBox.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("No borrowed items yet");
        title.getStyleClass().add("section-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        title.setTextAlignment(TextAlignment.CENTER);

        Label description = new Label(
            "Your approved reservations and borrowed equipment " +
            "will appear here."
        );

        description.getStyleClass().add("section-subtitle");
        description.setWrapText(true);
        description.setMaxWidth(Double.MAX_VALUE);
        description.setAlignment(Pos.CENTER);
        description.setTextAlignment(TextAlignment.CENTER);

        emptyBox.getChildren().addAll(
            title,
            description
        );

        borrowedItemsGrid.add(emptyBox, 0, 0);

        GridPane.setColumnSpan(emptyBox, 2);
        GridPane.setHgrow(emptyBox, Priority.ALWAYS);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private LocalDate getLocalDate(
        ResultSet result,
        String column
    ) throws SQLException {

        java.sql.Date date = result.getDate(column);

        return date != null
            ? date.toLocalDate()
            : null;
    }

    private String formatDate(LocalDate date) {

        return date != null
            ? date.format(dateFormatter)
            : "Not available";
    }

    private String getStatusStyle(String status) {

        if (status == null) {
            return "status-reserved";
        }

        switch (status.toLowerCase()) {

            case "approved":
                return "status-approved";

            case "reserved":
                return "status-reserved";

            case "borrowed":
                return "status-approved";

            default:
                return "status-reserved";
        }
    }

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

    @FXML
    private void showComingSoon() {

        showAlert(
            Alert.AlertType.INFORMATION,
            "HiramMate",
            "This feature will be implemented in a future update."
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

    @FXML
    private void showLiabilities() throws IOException {
        App.setRoot("liabilities");
    }
}

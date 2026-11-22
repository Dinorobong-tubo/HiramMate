
package com.dinurobongtubo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class BrowseItemsController {

    @FXML private Label profileInitials;
    @FXML private Label profileName;
    @FXML private Label profileDetails;

    @FXML private TextField searchField;
    @FXML private FlowPane catalogGrid;

    private final ObservableList<Item> allItems =
            FXCollections.observableArrayList();

    private String selectedCategory = "All";

    @FXML
    public void initialize() {

        // DISPLAY LOGGED-IN STUDENT
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

        // SEARCH LISTENER
        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> displayItems()
        );

        // LOAD ITEMS
        loadItems();
    }

    // LOAD ITEMS FROM DATABASE
    private void loadItems() {

        allItems.clear();

        String sql =
            "SELECT item_id, item_code, item_name, category, " +
            "description, pickup_location, " +
            "total_quantity, available_quantity " +
            "FROM items " +
            "ORDER BY item_name";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                Item item = new Item(
                    result.getInt("item_id"),
                    result.getString("item_code"),
                    result.getString("item_name"),
                    result.getString("category"),
                    result.getString("description"),
                    result.getString("pickup_location"),
                    result.getInt("total_quantity"),
                    result.getInt("available_quantity")
                );

                allItems.add(item);
            }

            displayItems();

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                Alert.AlertType.ERROR,
                "Database Error",
                "Unable to load items from the database.\n\n"
                    + e.getMessage()
            );
        }
    }

    // DISPLAY AND FILTER ITEMS
    private void displayItems() {

        catalogGrid.getChildren().clear();

        String searchText = searchField.getText();

        if (searchText == null) {
            searchText = "";
        }

        searchText = searchText.toLowerCase().trim();

        for (Item item : allItems) {

            // CATEGORY FILTER
            if (!selectedCategory.equals("All")
                    && !item.getCategory().equalsIgnoreCase(selectedCategory)) {
                continue;
            }

            // SEARCH FILTER
            String itemName =
                safeText(item.getItemName()).toLowerCase();

            String itemCode =
                safeText(item.getItemCode()).toLowerCase();

            String description =
                safeText(item.getDescription()).toLowerCase();

            if (!itemName.contains(searchText)
                    && !itemCode.contains(searchText)
                    && !description.contains(searchText)) {
                continue;
            }

            catalogGrid.getChildren().add(createItemCard(item));
        }

        // EMPTY MESSAGE
        if (catalogGrid.getChildren().isEmpty()) {

            Label emptyLabel = new Label("No items found.");

            emptyLabel.setStyle(
                "-fx-font-size: 15px; -fx-text-fill: #777777;"
            );

            catalogGrid.getChildren().add(emptyLabel);
        }
    }

    // CREATE ITEM CARD
    private VBox createItemCard(Item item) {

        VBox card = new VBox(10);

        card.getStyleClass().add("item-card");
        card.setPadding(new Insets(18));
        card.setPrefWidth(250);
        card.setMinWidth(230);

        Label category = new Label(item.getCategory());
        category.getStyleClass().add("item-category");

        Label availability = new Label(
            item.getAvailableQuantity() > 0
                ? "Available"
                : "Unavailable"
        );

        availability.getStyleClass().add("available-badge");

        if (item.getAvailableQuantity() <= 0) {

            availability.setStyle(
                "-fx-background-color: #FCE8E8;"
                + "-fx-text-fill: #B42318;"
            );
        }

        HBox topRow = new HBox(8, category, availability);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(item.getItemName());
        name.getStyleClass().add("item-name");
        name.setWrapText(true);

        Label description = new Label(
            safeText(item.getDescription())
        );

        description.getStyleClass().add("item-description");
        description.setWrapText(true);

        Label code = new Label(
            "Item Code: " + safeText(item.getItemCode())
        );

        Label pickup = new Label(
            "Pickup: " + safeText(item.getPickupLocation())
        );

        Label stock = new Label(
            "Available Stock: " + item.getAvailableQuantity()
            + " / " + item.getTotalQuantity()
        );

        stock.getStyleClass().add("item-stock");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button requestButton = new Button("Request to Borrow");

        requestButton.getStyleClass().add("item-button");
        requestButton.setMaxWidth(Double.MAX_VALUE);

        requestButton.setDisable(item.getAvailableQuantity() <= 0);

        requestButton.setOnAction(
            event -> openBorrowRequestForm(item)
        );

        card.getChildren().addAll(
            topRow,
            name,
            description,
            code,
            pickup,
            stock,
            spacer,
            requestButton
        );

        return card;
    }

    // BORROWING REQUEST FORM
    private void openBorrowRequestForm(Item item) {

        Dialog<ButtonType> dialog = new Dialog<>();

        dialog.setTitle("Borrowing Request");

        dialog.setHeaderText(
            "Complete the form below to request this item."
        );

        ButtonType submitButton = new ButtonType(
            "Submit Request",
            ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
            submitButton,
            ButtonType.CANCEL
        );

        // ITEM INFORMATION
        Label itemNameLabel = new Label(item.getItemName());

        itemNameLabel.setStyle(
            "-fx-font-weight: bold; -fx-font-size: 14px;"
        );

        Label availableStockLabel = new Label(
            item.getAvailableQuantity() + " unit(s)"
        );

        // QUANTITY TO BORROW
        Spinner<Integer> quantitySpinner = new Spinner<>(
            1,
            item.getAvailableQuantity(),
            1
        );

        quantitySpinner.setEditable(false);
        quantitySpinner.setPrefWidth(180);

        // REASON FOR BORROWING
        TextArea purposeField = new TextArea();

        purposeField.setPromptText(
            "Explain why you need to borrow this item..."
        );

        purposeField.setWrapText(true);
        purposeField.setPrefRowCount(3);

        // BORROWING / PICKUP DATE
        DatePicker pickupDatePicker = new DatePicker();

        pickupDatePicker.setValue(LocalDate.now());
        pickupDatePicker.setPrefWidth(180);

        // EXPECTED RETURN DATE
        DatePicker dueDatePicker = new DatePicker();

        dueDatePicker.setValue(LocalDate.now().plusDays(7));
        dueDatePicker.setPrefWidth(180);

        // FORM LAYOUT
        GridPane form = new GridPane();

        form.setHgap(18);
        form.setVgap(14);
        form.setPadding(new Insets(20));

        // ITEM TO BORROW
        Label itemLabel = new Label("Item to Borrow:");

        itemLabel.setStyle("-fx-font-weight: bold;");

        form.add(itemLabel, 0, 0);
        form.add(itemNameLabel, 1, 0);

        // AVAILABLE QUANTITY
        Label stockLabel = new Label("Available Quantity:");

        stockLabel.setStyle("-fx-font-weight: bold;");

        form.add(stockLabel, 0, 1);
        form.add(availableStockLabel, 1, 1);

        // QUANTITY TO BORROW
        Label quantityLabel = new Label("Quantity to Borrow:");

        quantityLabel.setStyle("-fx-font-weight: bold;");

        form.add(quantityLabel, 0, 2);
        form.add(quantitySpinner, 1, 2);

        // REASON FOR BORROWING
        Label purposeLabel = new Label("Reason for Borrowing:");

        purposeLabel.setStyle("-fx-font-weight: bold;");

        form.add(purposeLabel, 0, 3);
        form.add(purposeField, 1, 3);

        // BORROWING DATE
        Label pickupLabel = new Label(
            "Borrowing Date (Pickup Date):"
        );

        pickupLabel.setStyle("-fx-font-weight: bold;");

        form.add(pickupLabel, 0, 4);
        form.add(pickupDatePicker, 1, 4);

        // EXPECTED RETURN DATE
        Label dueLabel = new Label(
            "Expected Return Date (Due Date):"
        );

        dueLabel.setStyle("-fx-font-weight: bold;");

        form.add(dueLabel, 0, 5);
        form.add(dueDatePicker, 1, 5);

        GridPane.setHgrow(purposeField, Priority.ALWAYS);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(600);

        // VALIDATE AND SAVE
        Node submitNode =
            dialog.getDialogPane().lookupButton(submitButton);

        submitNode.addEventFilter(
            ActionEvent.ACTION,
            event -> {

                String purpose = purposeField.getText().trim();

                LocalDate pickupDate = pickupDatePicker.getValue();
                LocalDate dueDate = dueDatePicker.getValue();

                // PURPOSE VALIDATION
                if (purpose.isEmpty()) {

                    event.consume();

                    showAlert(
                        Alert.AlertType.WARNING,
                        "Missing Purpose",
                        "Please enter your reason for borrowing."
                    );

                    return;
                }

                // DATE VALIDATION
                if (pickupDate == null || dueDate == null) {

                    event.consume();

                    showAlert(
                        Alert.AlertType.WARNING,
                        "Missing Date",
                        "Please select both the borrowing date "
                        + "and expected return date."
                    );

                    return;
                }

                if (pickupDate.isBefore(LocalDate.now())) {

                    event.consume();

                    showAlert(
                        Alert.AlertType.WARNING,
                        "Invalid Borrowing Date",
                        "The borrowing date cannot be in the past."
                    );

                    return;
                }

                if (!dueDate.isAfter(pickupDate)) {

                    event.consume();

                    showAlert(
                        Alert.AlertType.WARNING,
                        "Invalid Return Date",
                        "The expected return date must be after "
                        + "the borrowing date."
                    );

                    return;
                }

                int quantity = quantitySpinner.getValue();

                boolean saved = saveBorrowRequest(
                    item,
                    quantity,
                    purpose,
                    pickupDate,
                    dueDate
                );

                if (saved) {

                    event.consume();
                    dialog.close();

                } else {

                    event.consume();
                }
            }
        );

        dialog.show();
    }

    // SAVE REQUEST TO MYSQL
    private boolean saveBorrowRequest(
            Item item,
            int quantity,
            String purpose,
            LocalDate pickupDate,
            LocalDate dueDate) {

        if (StudentSession.studentId == null) {

            showAlert(
                Alert.AlertType.ERROR,
                "Session Error",
                "Student information is missing. Please log in again."
            );

            return false;
        }

        String userSql =
            "SELECT user_id FROM users WHERE student_id = ?";

        String stockSql =
            "SELECT available_quantity FROM items WHERE item_id = ?";

        String insertSql =
            "INSERT INTO borrow_requests " +
            "(user_id, item_id, quantity, purpose, " +
            "pickup_date, due_date, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, 'Pending')";

        try (Connection connection = DatabaseConnection.getConnection()) {

            int userId;

            // FIND LOGGED-IN USER
            try (PreparedStatement userStatement =
                    connection.prepareStatement(userSql)) {

                userStatement.setString(1, StudentSession.studentId);

                try (ResultSet userResult =
                        userStatement.executeQuery()) {

                    if (!userResult.next()) {

                        showAlert(
                            Alert.AlertType.ERROR,
                            "User Not Found",
                            "Your account could not be found."
                        );

                        return false;
                    }

                    userId = userResult.getInt("user_id");
                }
            }

            // CHECK LATEST AVAILABLE STOCK
            try (PreparedStatement stockStatement =
                    connection.prepareStatement(stockSql)) {

                stockStatement.setInt(1, item.getItemId());

                try (ResultSet stockResult =
                        stockStatement.executeQuery()) {

                    if (!stockResult.next()) {

                        showAlert(
                            Alert.AlertType.ERROR,
                            "Item Not Found",
                            "This item no longer exists."
                        );

                        return false;
                    }

                    int currentStock =
                        stockResult.getInt("available_quantity");

                    if (currentStock <= 0 || quantity > currentStock) {

                        showAlert(
                            Alert.AlertType.WARNING,
                            "Insufficient Stock",
                            "The available quantity has changed. "
                            + "Please refresh the Browse Items page."
                        );

                        loadItems();

                        return false;
                    }
                }
            }

            // INSERT BORROWING REQUEST
            try (PreparedStatement insertStatement =
                    connection.prepareStatement(insertSql)) {

                insertStatement.setInt(1, userId);
                insertStatement.setInt(2, item.getItemId());
                insertStatement.setInt(3, quantity);
                insertStatement.setString(4, purpose);

                insertStatement.setDate(
                    5, Date.valueOf(pickupDate)
                );

                insertStatement.setDate(
                    6, Date.valueOf(dueDate)
                );

                int rowsInserted =
                    insertStatement.executeUpdate();

                if (rowsInserted > 0) {

                    showAlert(
                        Alert.AlertType.INFORMATION,
                        "Request Submitted",
                        "Your borrowing request has been submitted!\n\n"
                        + "Item: " + item.getItemName()
                        + "\nQuantity: " + quantity
                        + "\nStatus: Pending"
                    );

                    loadItems();

                    return true;
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                Alert.AlertType.ERROR,
                "Request Error",
                "Unable to submit your borrowing request.\n\n"
                + e.getMessage()
            );
        }

        return false;
    }

    // CATEGORY FILTERS
    @FXML
    private void showAllItems() {

        selectedCategory = "All";
        displayItems();
    }

    @FXML
    private void showElectronics() {

        selectedCategory = "Electronics";
        displayItems();
    }

    @FXML
    private void showLaboratory() {

        selectedCategory = "Laboratory";
        displayItems();
    }

    @FXML
    private void showEngineering() {

        selectedCategory = "Engineering";
        displayItems();
    }

    @FXML
    private void showSports() {

        selectedCategory = "Sports";
        displayItems();
    }

    // SIDEBAR NAVIGATION
    @FXML
    private void showDashboard() throws IOException {

        App.setRoot("student-dashboard");
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

    // COMING SOON
    @FXML
    private void showComingSoon() {

        showAlert(
            Alert.AlertType.INFORMATION,
            "HiramMate",
            "This feature will be implemented soon."
        );
    }

    // HELPER METHODS
    private String safeText(String value) {

        return value == null ? "" : value;
    }

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

    // ITEM MODEL
    private static class Item {

        private final int itemId;
        private final String itemCode;
        private final String itemName;
        private final String category;
        private final String description;
        private final String pickupLocation;
        private final int totalQuantity;
        private final int availableQuantity;

        public Item(
                int itemId,
                String itemCode,
                String itemName,
                String category,
                String description,
                String pickupLocation,
                int totalQuantity,
                int availableQuantity) {

            this.itemId = itemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.category = category;
            this.description = description;
            this.pickupLocation = pickupLocation;
            this.totalQuantity = totalQuantity;
            this.availableQuantity = availableQuantity;
        }

        public int getItemId() {
            return itemId;
        }

        public String getItemCode() {
            return itemCode;
        }

        public String getItemName() {
            return itemName;
        }

        public String getCategory() {
            return category;
        }

        public String getDescription() {
            return description;
        }

        public String getPickupLocation() {
            return pickupLocation;
        }

        public int getTotalQuantity() {
            return totalQuantity;
        }

        public int getAvailableQuantity() {
            return availableQuantity;
        }
    }
}

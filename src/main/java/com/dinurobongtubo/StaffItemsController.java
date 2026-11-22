package com.dinurobongtubo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

public class StaffItemsController {

    @FXML private Label staffNameLabel;
    @FXML private Label staffIdLabel;
    @FXML private Label itemCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;

    @FXML private TableView<ItemRow> itemsTable;

    @FXML private TableColumn<ItemRow, String> itemCodeColumn;
    @FXML private TableColumn<ItemRow, String> itemNameColumn;
    @FXML private TableColumn<ItemRow, String> categoryColumn;
    @FXML private TableColumn<ItemRow, String> conditionColumn;
    @FXML private TableColumn<ItemRow, Integer> totalColumn;
    @FXML private TableColumn<ItemRow, Integer> availableColumn;
    @FXML private TableColumn<ItemRow, String> locationColumn;
    @FXML private TableColumn<ItemRow, String> statusColumn;

    private final ObservableList<ItemRow> itemList =
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

        categoryFilter.setItems(
                FXCollections.observableArrayList("All Categories")
        );
        categoryFilter.setValue("All Categories");

        loadCategories();

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> loadItems()
        );

        categoryFilter.setOnAction(event -> loadItems());

        loadItems();
    }

    private void setupTable() {

        itemCodeColumn.setCellValueFactory(
                new PropertyValueFactory<>("itemCode"));

        itemNameColumn.setCellValueFactory(
                new PropertyValueFactory<>("itemName"));

        categoryColumn.setCellValueFactory(
                new PropertyValueFactory<>("category"));

        conditionColumn.setCellValueFactory(
                new PropertyValueFactory<>("condition"));

        totalColumn.setCellValueFactory(
                new PropertyValueFactory<>("totalQuantity"));

        availableColumn.setCellValueFactory(
                new PropertyValueFactory<>("availableQuantity"));

        locationColumn.setCellValueFactory(
                new PropertyValueFactory<>("pickupLocation"));

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status"));

        itemsTable.setItems(itemList);
    }

    private void loadCategories() {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            String sql =
                    "SELECT DISTINCT category FROM items "
                  + "WHERE category IS NOT NULL "
                  + "ORDER BY category";

            try (PreparedStatement statement =
                         connection.prepareStatement(sql);
                 ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    String category = result.getString("category");

                    if (!categoryFilter.getItems().contains(category)) {
                        categoryFilter.getItems().add(category);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Unable to load item categories.");
        }
    }

    @FXML
    private void loadItems() {

        itemList.clear();

        String search = searchField.getText() == null
                ? ""
                : searchField.getText().trim();

        String selectedCategory = categoryFilter.getValue();

        if (selectedCategory == null) {
            selectedCategory = "All Categories";
        }

        String sql =
                "SELECT item_id, item_code, item_name, category, "
              + "description, pickup_location, total_quantity, "
              + "available_quantity, item_condition "
              + "FROM items "
              + "WHERE (? = '' OR item_code LIKE ? "
              + "OR item_name LIKE ? OR category LIKE ?) "
              + "AND (? = 'All Categories' OR category = ?) "
              + "ORDER BY item_id DESC";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            String searchPattern = "%" + search + "%";

            statement.setString(1, search);
            statement.setString(2, searchPattern);
            statement.setString(3, searchPattern);
            statement.setString(4, searchPattern);
            statement.setString(5, selectedCategory);
            statement.setString(6, selectedCategory);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    int total = result.getInt("total_quantity");
                    int available = result.getInt("available_quantity");

                    String condition =
                            result.getString("item_condition");

                    if (condition == null || condition.isBlank()) {
                        condition = "Good";
                    }

                    String status = determineStatus(
                            condition, total, available
                    );

                    itemList.add(new ItemRow(
                            result.getInt("item_id"),
                            result.getString("item_code"),
                            result.getString("item_name"),
                            result.getString("category"),
                            condition,
                            total,
                            available,
                            result.getString("pickup_location"),
                            status
                    ));
                }
            }

            itemCountLabel.setText(
                    itemList.size() + " item(s) found"
            );

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Unable to load inventory items.");
        }
    }

    private String determineStatus(
            String condition, int total, int available) {

        if ("Maintenance".equalsIgnoreCase(condition)
                || "Damaged".equalsIgnoreCase(condition)) {
            return "Maintenance";
        }

        if (available <= 0) {
            return "Out of Stock";
        }

        if (available < total) {
            return "Partially Borrowed";
        }

        return "Available";
    }

    @FXML
    private void addItem() {
        showItemDialog(null);
    }

    @FXML
    private void editItem() {

        ItemRow selected = itemsTable.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            showAlert("Please select an item to update.");
            return;
        }

        showItemDialog(selected);
    }

    private void showItemDialog(ItemRow existingItem) {

        boolean isNew = existingItem == null;

        Dialog<ItemForm> dialog = new Dialog<>();

        dialog.setTitle(isNew ? "Add New Item" : "Update Item");
        dialog.setHeaderText(
                isNew
                ? "Enter the new inventory item details."
                : "Update the selected inventory item."
        );

        ButtonType saveButton = new ButtonType(
                isNew ? "Add Item" : "Save Changes",
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                saveButton, ButtonType.CANCEL
        );

        TextField nameField = new TextField();
        nameField.setPromptText("Enter item name");

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.setEditable(true);
        categoryBox.getItems().addAll(
                "Electronics",
                "Lab Gear",
                "Sports",
                "Audio/Visual",
                "School Supplies",
                "Other"
        );

        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("Enter item description");
        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);

        TextField locationField = new TextField();
        locationField.setPromptText("Enter pickup location");

        ComboBox<String> conditionBox = new ComboBox<>();
        conditionBox.getItems().addAll(
                "Good",
                "Fair",
                "Damaged",
                "Maintenance"
        );
        conditionBox.setValue("Good");

        TextField totalField = new TextField("1");
        totalField.setPromptText("Total quantity");

        TextField availableField = new TextField("1");
        availableField.setPromptText("Available quantity");

        if (!isNew) {

            nameField.setText(existingItem.getItemName());
            categoryBox.setValue(existingItem.getCategory());

            descriptionField.setText(
                    getItemDescription(existingItem.getItemId())
            );

            locationField.setText(existingItem.getPickupLocation());
            conditionBox.setValue(existingItem.getCondition());

            totalField.setText(
                    String.valueOf(existingItem.getTotalQuantity())
            );

            availableField.setText(
                    String.valueOf(existingItem.getAvailableQuantity())
            );
        }

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new javafx.geometry.Insets(15));

        grid.add(new Label("Item Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryBox, 1, 1);

        grid.add(new Label("Description:"), 0, 2);
        grid.add(descriptionField, 1, 2);

        grid.add(new Label("Pickup Location:"), 0, 3);
        grid.add(locationField, 1, 3);

        grid.add(new Label("Condition:"), 0, 4);
        grid.add(conditionBox, 1, 4);

        grid.add(new Label("Total Quantity:"), 0, 5);
        grid.add(totalField, 1, 5);

        grid.add(new Label("Available Quantity:"), 0, 6);
        grid.add(availableField, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(560);

        Node saveNode = dialog.getDialogPane().lookupButton(saveButton);

        saveNode.addEventFilter(ActionEvent.ACTION, event -> {

            String name = nameField.getText().trim();
            String category = categoryBox.getValue();

            if (name.isEmpty()) {
                showAlert("Please enter an item name.");
                event.consume();
                return;
            }

            if (category == null || category.isBlank()) {
                showAlert("Please select or enter a category.");
                event.consume();
                return;
            }

            if (locationField.getText().trim().isEmpty()) {
                showAlert("Please enter a pickup location.");
                event.consume();
                return;
            }

            try {

                int total = Integer.parseInt(totalField.getText().trim());
                int available = Integer.parseInt(
                        availableField.getText().trim()
                );

                if (total < 1) {
                    showAlert("Total quantity must be at least 1.");
                    event.consume();
                    return;
                }

                if (available < 0 || available > total) {
                    showAlert(
                            "Available quantity must be between 0 "
                          + "and the total quantity."
                    );
                    event.consume();
                    return;
                }

            } catch (NumberFormatException e) {
                showAlert("Please enter valid whole numbers for quantities.");
                event.consume();
            }
        });

        dialog.setResultConverter(button -> {

            if (button == saveButton) {

                return new ItemForm(
                        nameField.getText().trim(),
                        categoryBox.getValue().trim(),
                        descriptionField.getText().trim(),
                        locationField.getText().trim(),
                        conditionBox.getValue(),
                        Integer.parseInt(totalField.getText().trim()),
                        Integer.parseInt(availableField.getText().trim())
                );
            }

            return null;
        });

        Optional<ItemForm> result = dialog.showAndWait();

        result.ifPresent(form -> {

            if (isNew) {
                insertItem(form);
            } else {
                updateItem(existingItem.getItemId(), form);
            }
        });
    }

    private String getItemDescription(int itemId) {

        String sql =
                "SELECT description FROM items WHERE item_id = ?";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, itemId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    String description = result.getString("description");
                    return description == null ? "" : description;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "";
    }

    private void insertItem(ItemForm form) {

        String sql =
                "INSERT INTO items "
              + "(item_code, item_name, category, description, "
              + "pickup_location, total_quantity, available_quantity, "
              + "item_condition) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            String itemCode = generateItemCode(connection);

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, itemCode);
                statement.setString(2, form.name);
                statement.setString(3, form.category);
                statement.setString(4, form.description);
                statement.setString(5, form.location);
                statement.setInt(6, form.total);
                statement.setInt(7, form.available);
                statement.setString(8, form.condition);

                statement.executeUpdate();
            }

            showAlert("Item " + itemCode + " was added successfully.");

            loadCategories();
            loadItems();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Unable to add item. Please check the database.");
        }
    }

    private String generateItemCode(Connection connection)
            throws SQLException {

        int nextNumber = 1;

        String sql = "SELECT COALESCE(MAX(item_id), 0) + 1 AS next_id FROM items";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            if (result.next()) {
                nextNumber = result.getInt("next_id");
            }
        }

        String candidate;

        while (true) {

            candidate = String.format(
                    Locale.ROOT, "ITM-%02d", nextNumber
            );

            String checkSql =
                    "SELECT COUNT(*) FROM items WHERE item_code = ?";

            try (PreparedStatement statement =
                         connection.prepareStatement(checkSql)) {

                statement.setString(1, candidate);

                try (ResultSet result = statement.executeQuery()) {

                    if (result.next() && result.getInt(1) == 0) {
                        return candidate;
                    }
                }
            }

            nextNumber++;
        }
    }

    private void updateItem(int itemId, ItemForm form) {

        String sql =
                "UPDATE items SET item_name = ?, category = ?, "
              + "description = ?, pickup_location = ?, "
              + "item_condition = ?, total_quantity = ?, "
              + "available_quantity = ? "
              + "WHERE item_id = ?";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, form.name);
            statement.setString(2, form.category);
            statement.setString(3, form.description);
            statement.setString(4, form.location);
            statement.setString(5, form.condition);
            statement.setInt(6, form.total);
            statement.setInt(7, form.available);
            statement.setInt(8, itemId);

            statement.executeUpdate();

            showAlert("Item details updated successfully.");

            loadCategories();
            loadItems();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Unable to update item.");
        }
    }

    @FXML
    private void removeItem() {

        ItemRow selected = itemsTable.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            showAlert("Please select an item to remove.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);

        confirmation.setTitle("Remove Inventory Item");
        confirmation.setHeaderText("Remove " + selected.getItemName() + "?");
        confirmation.setContentText(
                "This will permanently remove the item if it has "
              + "no existing borrowing or liability records."
        );

        Optional<ButtonType> answer = confirmation.showAndWait();

        if (answer.isEmpty()
                || answer.get() != ButtonType.OK) {
            return;
        }

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            if (hasExistingRecords(connection, selected.getItemId())) {

                showAlert(
                        "This item cannot be removed because it is "
                      + "already connected to borrowing or liability records."
                );

                return;
            }

            String sql = "DELETE FROM items WHERE item_id = ?";

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, selected.getItemId());

                statement.executeUpdate();
            }

            showAlert("Item removed successfully.");

            loadCategories();
            loadItems();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Unable to remove this item.");
        }
    }

    private boolean hasExistingRecords(
            Connection connection, int itemId) throws SQLException {

        String sql =
                "SELECT "
              + "(SELECT COUNT(*) FROM borrow_requests WHERE item_id = ?) "
              + "+ (SELECT COUNT(*) FROM borrow_records WHERE item_id = ?) "
              + "+ (SELECT COUNT(*) FROM liabilities WHERE item_id = ?) "
              + "AS total";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, itemId);
            statement.setInt(2, itemId);
            statement.setInt(3, itemId);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return result.getInt("total") > 0;
                }
            }
        }

        return false;
    }

    @FXML
    private void refreshItems() {
        loadCategories();
        loadItems();
    }

    @FXML
    private void showDashboard() {
        openPage("staff-dashboard");
    }

    @FXML
    private void showItems() {
        loadItems();
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

    private static class ItemForm {

        private final String name;
        private final String category;
        private final String description;
        private final String location;
        private final String condition;
        private final int total;
        private final int available;

        private ItemForm(
                String name,
                String category,
                String description,
                String location,
                String condition,
                int total,
                int available) {

            this.name = name;
            this.category = category;
            this.description = description;
            this.location = location;
            this.condition = condition;
            this.total = total;
            this.available = available;
        }
    }

    public static class ItemRow {

        private final int itemId;
        private final String itemCode;
        private final String itemName;
        private final String category;
        private final String condition;
        private final int totalQuantity;
        private final int availableQuantity;
        private final String pickupLocation;
        private final String status;

        public ItemRow(
                int itemId,
                String itemCode,
                String itemName,
                String category,
                String condition,
                int totalQuantity,
                int availableQuantity,
                String pickupLocation,
                String status) {

            this.itemId = itemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.category = category;
            this.condition = condition;
            this.totalQuantity = totalQuantity;
            this.availableQuantity = availableQuantity;
            this.pickupLocation = pickupLocation;
            this.status = status;
        }

        public int getItemId() { return itemId; }
        public String getItemCode() { return itemCode; }
        public String getItemName() { return itemName; }
        public String getCategory() { return category; }
        public String getCondition() { return condition; }
        public int getTotalQuantity() { return totalQuantity; }
        public int getAvailableQuantity() { return availableQuantity; }
        public String getPickupLocation() { return pickupLocation; }
        public String getStatus() { return status; }
    }
}

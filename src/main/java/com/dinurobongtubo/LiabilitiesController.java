package com.dinurobongtubo;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

public class LiabilitiesController {

    @FXML private Label profileInitials;
    @FXML private Label profileName;
    @FXML private Label profileDetails;

    @FXML private Label totalOutstandingLabel;
    @FXML private Label pendingChargesLabel;
    @FXML private Label settledLabel;

    @FXML private Label noticeDescription;
    @FXML private Label recordsFooter;

    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;

    @FXML private TableView<LiabilityRow> liabilitiesTable;

    @FXML private TableColumn<LiabilityRow, String> referenceColumn;
    @FXML private TableColumn<LiabilityRow, String> itemColumn;
    @FXML private TableColumn<LiabilityRow, String> reasonColumn;
    @FXML private TableColumn<LiabilityRow, String> dateColumn;
    @FXML private TableColumn<LiabilityRow, String> amountColumn;
    @FXML private TableColumn<LiabilityRow, String> statusColumn;

    private final ObservableList<LiabilityRow> liabilityList =
            FXCollections.observableArrayList();

    private FilteredList<LiabilityRow> filteredLiabilities;

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    // ==========================================
    // INITIALIZE
    // ==========================================

    @FXML
    public void initialize() {
        loadProfile();
        setupStatusFilter();
        setupTableColumns();
        setupSearchAndFilter();
        loadLiabilities();
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
        StringBuilder initials = new StringBuilder();

        for (String part : nameParts) {
            if (!part.isEmpty()) {
                initials.append(part.charAt(0));
            }
        }

        String result = initials.toString();

        if (result.length() > 2) {
            result = result.substring(0, 2);
        }

        profileInitials.setText(result.toUpperCase());

        String studentId = StudentSession.studentId;

        profileDetails.setText(
                studentId != null ? studentId : "No Student ID"
        );
    }

    // ==========================================
    // STATUS FILTER
    // ==========================================

    private void setupStatusFilter() {

        statusFilter.getItems().clear();

        statusFilter.getItems().addAll(
                "All statuses",
                "Pending",
                "Outstanding",
                "Settled"
        );

        statusFilter.setValue("All statuses");
    }

    // ==========================================
    // TABLE COLUMNS
    // ==========================================

    private void setupTableColumns() {

        referenceColumn.setCellValueFactory(
                new PropertyValueFactory<>("reference")
        );

        itemColumn.setCellValueFactory(
                new PropertyValueFactory<>("item")
        );

        reasonColumn.setCellValueFactory(
                new PropertyValueFactory<>("reason")
        );

        dateColumn.setCellValueFactory(
                new PropertyValueFactory<>("date")
        );

        amountColumn.setCellValueFactory(
                new PropertyValueFactory<>("amount")
        );

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );
    }

    // ==========================================
    // SEARCH AND FILTER
    // ==========================================

    private void setupSearchAndFilter() {

        filteredLiabilities =
                new FilteredList<>(liabilityList, row -> true);

        liabilitiesTable.setItems(filteredLiabilities);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> applyFilters()
        );

        statusFilter.valueProperty().addListener(
                (observable, oldValue, newValue) -> applyFilters()
        );
    }

    private void applyFilters() {

        if (filteredLiabilities == null) {
            return;
        }

        String searchText = searchField.getText();

        if (searchText == null) {
            searchText = "";
        }

        final String search = searchText.toLowerCase().trim();
        String selectedStatus = statusFilter.getValue();

        filteredLiabilities.setPredicate(row -> {

            boolean matchesStatus =
                    selectedStatus == null ||
                    selectedStatus.equals("All statuses") ||
                    (row.getStatus() != null &&
                     row.getStatus().equalsIgnoreCase(selectedStatus));

            boolean matchesSearch =
                    search.isEmpty() ||
                    containsIgnoreCase(row.getReference(), search) ||
                    containsIgnoreCase(row.getItem(), search);

            return matchesStatus && matchesSearch;
        });

        updateRecordsFooter();
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null &&
                value.toLowerCase().contains(search);
    }

    // ==========================================
    // LOAD LIABILITY RECORDS FROM MYSQL
    // ==========================================

    private void loadLiabilities() {

        liabilityList.clear();

        if (StudentSession.studentId == null ||
                StudentSession.studentId.isBlank()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "No Student Session",
                    "Unable to identify the currently logged-in student."
            );

            return;
        }

        String sql =
                "SELECT l.liability_id, l.reason, l.amount, " +
                "l.status, l.created_at, " +
                "COALESCE(i.item_name, 'General Liability') AS item_name " +
                "FROM liabilities l " +
                "INNER JOIN users u ON l.user_id = u.user_id " +
                "LEFT JOIN items i ON l.item_id = i.item_id " +
                "WHERE u.student_id = ? " +
                "ORDER BY l.created_at DESC";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, StudentSession.studentId);

            try (ResultSet result = statement.executeQuery()) {

                BigDecimal totalOutstanding = BigDecimal.ZERO;
                BigDecimal pendingCharges = BigDecimal.ZERO;
                BigDecimal settledAmount = BigDecimal.ZERO;

                while (result.next()) {

                    int liabilityId = result.getInt("liability_id");

                    String reason = result.getString("reason");
                    String item = result.getString("item_name");

                    BigDecimal amount = result.getBigDecimal("amount");

                    if (amount == null) {
                        amount = BigDecimal.ZERO;
                    }

                    String status = result.getString("status");

                    Timestamp timestamp = result.getTimestamp("created_at");

                    String formattedDate = "Not available";

                    if (timestamp != null) {
                        formattedDate =
                                timestamp.toLocalDateTime()
                                        .format(dateFormatter);
                    }

                    String reference =
                            String.format("LIAB-%04d", liabilityId);

                    LiabilityRow row = new LiabilityRow(
                            reference,
                            item,
                            reason,
                            formattedDate,
                            formatCurrency(amount),
                            status
                    );

                    liabilityList.add(row);

                    if (status != null) {

                        if (status.equalsIgnoreCase("Pending") ||
                                status.equalsIgnoreCase("Outstanding")) {

                            totalOutstanding =
                                    totalOutstanding.add(amount);
                        }

                        if (status.equalsIgnoreCase("Pending")) {
                            pendingCharges =
                                    pendingCharges.add(amount);
                        }

                        if (status.equalsIgnoreCase("Settled")) {
                            settledAmount =
                                    settledAmount.add(amount);
                        }
                    }
                }

                totalOutstandingLabel.setText(
                        formatCurrency(totalOutstanding)
                );

                pendingChargesLabel.setText(
                        formatCurrency(pendingCharges)
                );

                settledLabel.setText(
                        formatCurrency(settledAmount)
                );

                noticeDescription.setText(
                        "You currently have an outstanding balance of " +
                        formatCurrency(totalOutstanding) +
                        ". Please coordinate with the property office " +
                        "for verification and settlement."
                );

                applyFilters();
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "Unable to load liability records.\n\n" +
                    e.getMessage()
            );
        }
    }

    // ==========================================
    // RECORDS FOOTER
    // ==========================================

    private void updateRecordsFooter() {

        if (filteredLiabilities == null) {
            return;
        }

        recordsFooter.setText(
                "Showing " + filteredLiabilities.size() + " record(s)"
        );
    }

    // ==========================================
    // FORMAT CURRENCY
    // ==========================================

    private String formatCurrency(BigDecimal amount) {

        return String.format(
                Locale.US,
                "₱%,.2f",
                amount
        );
    }

    // ==========================================
    // DOWNLOAD LIABILITY STATEMENT
    // ==========================================

    @FXML
    private void downloadStatement() {

        if (StudentSession.studentId == null ||
                StudentSession.studentId.isBlank()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Student Not Found",
                    "Unable to identify the currently logged-in student."
            );

            return;
        }

        if (liabilityList.isEmpty()) {

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "No Liability Records",
                    "You currently have no liability records to download."
            );

            return;
        }

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Save My Liability Statement");

        String safeStudentId =
                StudentSession.studentId.replaceAll(
                        "[^a-zA-Z0-9_-]",
                        "_"
                );

        fileChooser.setInitialFileName(
                "My_Liability_Statement_" + safeStudentId + ".pdf"
        );

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "PDF Files (*.pdf)",
                        "*.pdf"
                )
        );

        File file = fileChooser.showSaveDialog(
                liabilitiesTable.getScene().getWindow()
        );

        if (file == null) {
            return;
        }

        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getAbsolutePath() + ".pdf");
        }

        BigDecimal totalRecorded = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (LiabilityRow row : liabilityList) {

            BigDecimal amount = parseAmount(row.getAmount());

            totalRecorded = totalRecorded.add(amount);

            if ("Pending".equalsIgnoreCase(row.getStatus()) ||
                    "Outstanding".equalsIgnoreCase(row.getStatus())) {

                totalOutstanding =
                        totalOutstanding.add(amount);
            }
        }

        Document document = new Document(
                PageSize.A4.rotate(),
                36,
                36,
                36,
                36
        );

        try (FileOutputStream output = new FileOutputStream(file)) {

            PdfWriter.getInstance(document, output);

            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    20,
                    Font.BOLD
            );

            Font headingFont = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    12,
                    Font.BOLD
            );

            Font normalFont = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    10,
                    Font.NORMAL
            );

            Font smallFont = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    9,
                    Font.NORMAL
            );

            Font headerFont = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    9,
                    Font.BOLD,
                    Color.WHITE
            );

            Paragraph title = new Paragraph(
                    "HIRAMMATE",
                    titleFont
            );

            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph(
                    "STUDENT LIABILITY STATEMENT",
                    headingFont
            );

            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph(" ", normalFont));

            document.add(
                    new Paragraph(
                            "Student Name: " +
                            safeText(StudentSession.fullName),
                            normalFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Student ID: " +
                            safeText(StudentSession.studentId),
                            normalFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Date Generated: " +
                            LocalDateTime.now().format(
                                    DateTimeFormatter.ofPattern(
                                            "MMMM dd, yyyy hh:mm a"
                                    )
                            ),
                            normalFont
                    )
            );

            document.add(new Paragraph(" ", normalFont));

            PdfPTable table = new PdfPTable(6);

            table.setWidthPercentage(100);

            table.setWidths(new float[]{
                    1.2f, 2.0f, 2.4f, 1.4f, 1.5f, 1.2f
            });

            table.setHeaderRows(1);

            Color headerBackground = new Color(181, 31, 54);

            addPdfCell(
                    table, "Reference", headerFont,
                    headerBackground, Element.ALIGN_CENTER
            );

            addPdfCell(
                    table, "Affected Item", headerFont,
                    headerBackground, Element.ALIGN_CENTER
            );

            addPdfCell(
                    table, "Reason", headerFont,
                    headerBackground, Element.ALIGN_CENTER
            );

            addPdfCell(
                    table, "Date Recorded", headerFont,
                    headerBackground, Element.ALIGN_CENTER
            );

            addPdfCell(
                    table, "Amount", headerFont,
                    headerBackground, Element.ALIGN_CENTER
            );

            addPdfCell(
                    table, "Status", headerFont,
                    headerBackground, Element.ALIGN_CENTER
            );

            for (LiabilityRow row : liabilityList) {

                addPdfCell(
                        table, row.getReference(), smallFont,
                        null, Element.ALIGN_CENTER
                );

                addPdfCell(
                        table, row.getItem(), smallFont,
                        null, Element.ALIGN_LEFT
                );

                addPdfCell(
                        table, row.getReason(), smallFont,
                        null, Element.ALIGN_LEFT
                );

                addPdfCell(
                        table, row.getDate(), smallFont,
                        null, Element.ALIGN_CENTER
                );

                addPdfCell(
                        table, row.getAmount(), smallFont,
                        null, Element.ALIGN_RIGHT
                );

                addPdfCell(
                        table, row.getStatus(), smallFont,
                        null, Element.ALIGN_CENTER
                );
            }

            document.add(table);

            document.add(new Paragraph(" ", normalFont));

            document.add(
                    new Paragraph(
                            "Total Assessed Liabilities: " +
                            formatCurrency(totalRecorded),
                            headingFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Total Outstanding Balance: " +
                            formatCurrency(totalOutstanding),
                            headingFont
                    )
            );

            document.add(new Paragraph(" ", normalFont));

            document.add(
                    new Paragraph(
                            "This statement contains the liability records " +
                            "associated with the student's account.",
                            smallFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Generated by the HiramMate Student Portal.",
                            smallFont
                    )
            );

            document.close();

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Statement Downloaded",
                    "Your liability statement was saved successfully:\n\n" +
                    file.getAbsolutePath()
            );

        } catch (DocumentException | IOException e) {

            e.printStackTrace();

            if (document.isOpen()) {
                document.close();
            }

            showAlert(
                    Alert.AlertType.ERROR,
                    "PDF Generation Error",
                    "Unable to generate your liability statement.\n\n" +
                    e.getMessage()
            );
        }
    }

    // ==========================================
    // PDF TABLE CELL HELPER
    // ==========================================

    private void addPdfCell(
            PdfPTable table,
            String value,
            Font font,
            Color background,
            int alignment
    ) {

        PdfPCell cell = new PdfPCell(
                new Phrase(safeText(value), font)
        );

        cell.setPadding(7);
        cell.setHorizontalAlignment(alignment);

        if (background != null) {
            cell.setBackgroundColor(background);
        }

        table.addCell(cell);
    }

    // ==========================================
    // CONVERT DISPLAYED AMOUNT TO BIGDECIMAL
    // ==========================================

    private BigDecimal parseAmount(String amountText) {

        if (amountText == null || amountText.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {

            String cleanedAmount = amountText
                    .replace("₱", "")
                    .replace(",", "")
                    .trim();

            return new BigDecimal(cleanedAmount);

        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    // ==========================================
    // SAFE TEXT
    // ==========================================

    private String safeText(String value) {

        if (value == null || value.isBlank()) {
            return "N/A";
        }

        return value;
    }

    // ==========================================
    // ALERT
    // ==========================================

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

    // ==========================================
    // LIABILITY ROW MODEL
    // ==========================================

    public static class LiabilityRow {

        private final javafx.beans.property.SimpleStringProperty reference;
        private final javafx.beans.property.SimpleStringProperty item;
        private final javafx.beans.property.SimpleStringProperty reason;
        private final javafx.beans.property.SimpleStringProperty date;
        private final javafx.beans.property.SimpleStringProperty amount;
        private final javafx.beans.property.SimpleStringProperty status;

        public LiabilityRow(
                String reference,
                String item,
                String reason,
                String date,
                String amount,
                String status
        ) {

            this.reference =
                    new javafx.beans.property.SimpleStringProperty(reference);

            this.item =
                    new javafx.beans.property.SimpleStringProperty(item);

            this.reason =
                    new javafx.beans.property.SimpleStringProperty(reason);

            this.date =
                    new javafx.beans.property.SimpleStringProperty(date);

            this.amount =
                    new javafx.beans.property.SimpleStringProperty(amount);

            this.status =
                    new javafx.beans.property.SimpleStringProperty(status);
        }

        public String getReference() {
            return reference.get();
        }

        public String getItem() {
            return item.get();
        }

        public String getReason() {
            return reason.get();
        }

        public String getDate() {
            return date.get();
        }

        public String getAmount() {
            return amount.get();
        }

        public String getStatus() {
            return status.get();
        }
    }
}

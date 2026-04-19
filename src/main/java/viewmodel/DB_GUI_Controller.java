package viewmodel;

import dao.DbConnectivityClass;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Person;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import service.MyLogger;
import service.UserSession;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DB_GUI_Controller implements Initializable {

    // ── Validation patterns ──────────────────────────────────────────────────
    private static final Pattern NAME_PATTERN  = Pattern.compile("^[A-Za-z]{2,25}$");
    private static final Pattern DEPT_PATTERN  = Pattern.compile("^[A-Za-z ]{2,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,6}$");

    // ── FXML fields ──────────────────────────────────────────────────────────
    @FXML private TextField first_name, last_name, department, email, imageURL;
    @FXML private ComboBox<Major> major;
    @FXML private ImageView img_view;
    @FXML private MenuBar menuBar;
    @FXML private TableView<Person> tv;
    @FXML private TableColumn<Person, Integer> tv_id;
    @FXML private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;
    @FXML private Button addBtn, deleteBtn, editBtn;
    @FXML private MenuItem editItem, deleteItem;
    @FXML private Label statusLabel;
    @FXML private TextField searchField;

    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data  = cnUtil.getData();

    // ── Initialize ───────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            // Table column bindings
            tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
            tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
            tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
            tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
            tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));

            // Major ComboBox (form panel)
            major.setItems(FXCollections.observableArrayList(Major.values()));

            // Search / filter (Your touch #2)
            FilteredList<Person> filtered = new FilteredList<>(data, p -> true);
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filtered.setPredicate(person -> {
                    if (newVal == null || newVal.isBlank()) return true;
                    String lower = newVal.toLowerCase();
                    return person.getFirstName().toLowerCase().contains(lower)
                            || person.getLastName().toLowerCase().contains(lower)
                            || person.getDepartment().toLowerCase().contains(lower)
                            || person.getMajor().toLowerCase().contains(lower)
                            || person.getEmail().toLowerCase().contains(lower);
                });
            });
            tv.setItems(filtered);

            // ── UI State: disable Edit/Delete unless row selected ────────────
            BooleanBinding noSelection = tv.getSelectionModel().selectedItemProperty().isNull();
            editBtn.disableProperty().bind(noSelection);
            deleteBtn.disableProperty().bind(noSelection);
            editItem.disableProperty().bind(noSelection);
            deleteItem.disableProperty().bind(noSelection);

            // Disable Add unless all required fields are valid
            addBtn.disableProperty().bind(createValidationBinding());

            // Visual validation borders (Your touch #4)
            wireValidator(first_name, NAME_PATTERN);
            wireValidator(last_name,  NAME_PATTERN);
            wireValidator(department, DEPT_PATTERN);
            wireValidator(email,      EMAIL_PATTERN);

            // ── Extra Credit: Inline table editing ──────────────────────────
            tv.setEditable(true);

            tv_fn.setCellFactory(TextFieldTableCell.forTableColumn());
            tv_fn.setOnEditCommit(e -> commitInlineEdit(e, Person::setFirstName));

            tv_ln.setCellFactory(TextFieldTableCell.forTableColumn());
            tv_ln.setOnEditCommit(e -> commitInlineEdit(e, Person::setLastName));

            tv_department.setCellFactory(TextFieldTableCell.forTableColumn());
            tv_department.setOnEditCommit(e -> commitInlineEdit(e, Person::setDepartment));

            tv_email.setCellFactory(TextFieldTableCell.forTableColumn());
            tv_email.setOnEditCommit(e -> commitInlineEdit(e, Person::setEmail));

            ObservableList<String> majorOptions = FXCollections.observableArrayList(
                    Arrays.stream(Major.values()).map(Major::name).collect(Collectors.toList()));
            tv_major.setCellFactory(ComboBoxTableCell.forTableColumn(majorOptions));
            tv_major.setOnEditCommit(e -> commitInlineEdit(e, Person::setMajor));

            // ── Extra Credit: Click empty row to start adding ────────────────
            tv.setRowFactory(tableView -> {
                TableRow<Person> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (row.isEmpty()) {
                        clearForm();
                        first_name.requestFocus();
                        setStatus("Ready to add a new record — fill in the form and click Add.");
                    }
                });
                return row;
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── Inline edit commit helper ────────────────────────────────────────────
    private void commitInlineEdit(TableColumn.CellEditEvent<Person, String> e,
                                  BiConsumer<Person, String> setter) {
        Person p = e.getRowValue();
        setter.accept(p, e.getNewValue());
        cnUtil.editUser(p.getId(), p);
        tv.refresh();
        // Sync right-hand form panel
        first_name.setText(p.getFirstName());
        last_name.setText(p.getLastName());
        department.setText(p.getDepartment());
        email.setText(p.getEmail());
        setStatus("Updated: " + p.getFirstName() + " " + p.getLastName());
        MyLogger.makeLog("Inline edit: " + p);
    }

    // ── Validation helpers ───────────────────────────────────────────────────
    private BooleanBinding createValidationBinding() {
        return Bindings.createBooleanBinding(
                () -> !isFormValid(),
                first_name.textProperty(),
                last_name.textProperty(),
                department.textProperty(),
                major.valueProperty(),
                email.textProperty()
        );
    }

    private boolean isFormValid() {
        return NAME_PATTERN.matcher(first_name.getText().trim()).matches()
                && NAME_PATTERN.matcher(last_name.getText().trim()).matches()
                && DEPT_PATTERN.matcher(department.getText().trim()).matches()
                && major.getValue() != null
                && EMAIL_PATTERN.matcher(email.getText().trim()).matches();
    }

    private void wireValidator(TextField field, Pattern pattern) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                field.setStyle(null);
            } else if (pattern.matcher(newVal.trim()).matches()) {
                field.setStyle("-fx-border-color: #00aa00; -fx-border-width: 2px;");
            } else {
                field.setStyle("-fx-border-color: #cc0000; -fx-border-width: 2px;");
            }
        });
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    // ── CRUD actions ─────────────────────────────────────────────────────────
    @FXML
    protected void addNewRecord() {
        if (!isFormValid()) {
            setStatus("Please fill in all required fields correctly.");
            return;
        }
        Person p = new Person(
                first_name.getText(), last_name.getText(), department.getText(),
                major.getValue().toString(), email.getText(), imageURL.getText());
        cnUtil.insertUser(p);
        p.setId(cnUtil.retrieveId(p));
        data.add(p);
        clearForm();
        setStatus("Record added: " + p.getFirstName() + " " + p.getLastName());
        MyLogger.makeLog("Added: " + p);
    }

    @FXML
    protected void editRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p == null) return;
        int index = data.indexOf(p);
        String majorStr = major.getValue() != null ? major.getValue().toString() : p.getMajor();
        Person updated = new Person(p.getId(), first_name.getText(), last_name.getText(),
                department.getText(), majorStr, email.getText(), imageURL.getText());
        cnUtil.editUser(p.getId(), updated);
        data.set(index, updated);
        tv.getSelectionModel().select(index);
        setStatus("Record updated: " + updated.getFirstName() + " " + updated.getLastName());
        MyLogger.makeLog("Edited: " + updated);
    }

    @FXML
    protected void deleteRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p == null) return;

        // Confirmation dialog (Your touch #3)
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Record");
        confirm.setContentText("Permanently delete " + p.getFirstName() + " " + p.getLastName() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        int index = data.indexOf(p);
        cnUtil.deleteRecord(p);
        data.remove(index);
        setStatus("Record deleted: " + p.getFirstName() + " " + p.getLastName());
    }

    @FXML
    protected void clearForm() {
        first_name.setText("");
        last_name.setText("");
        department.setText("");
        major.setValue(null);
        email.setText("");
        imageURL.setText("");
        first_name.setStyle(null);
        last_name.setStyle(null);
        department.setStyle(null);
        email.setStyle(null);
        img_view.setImage(new Image(getClass().getResourceAsStream("/images/profile.png")));
        tv.getSelectionModel().clearSelection();
        setStatus("Form cleared.");
    }

    @FXML
    protected void selectedItemTV(MouseEvent mouseEvent) {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p == null) return;
        first_name.setText(p.getFirstName());
        last_name.setText(p.getLastName());
        department.setText(p.getDepartment());
        email.setText(p.getEmail());
        imageURL.setText(p.getImageURL() != null ? p.getImageURL() : "");
        try {
            major.setValue(Major.valueOf(p.getMajor()));
        } catch (IllegalArgumentException e) {
            major.setValue(null);
        }
    }

    // ── Image ────────────────────────────────────────────────────────────────
    @FXML
    protected void showImage() {
        File file = new FileChooser().showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            img_view.setImage(new Image(file.toURI().toString()));
            imageURL.setText(file.toURI().toString());
        }
    }

    // ── CSV import / export ──────────────────────────────────────────────────
    @FXML
    public void importCSV(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showOpenDialog(menuBar.getScene().getWindow());
        if (file == null) return;

        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                String[] f = line.split(",", -1);
                if (f.length < 5) continue;
                Person p = new Person(
                        f[0].trim(), f[1].trim(), f[2].trim(),
                        f[3].trim(), f[4].trim(),
                        f.length > 5 ? f[5].trim() : "");
                cnUtil.insertUser(p);
                p.setId(cnUtil.retrieveId(p));
                data.add(p);
                count++;
            }
            setStatus(count + " record(s) imported from " + file.getName());
        } catch (IOException e) {
            setStatus("Import failed: " + e.getMessage());
        }
    }

    @FXML
    public void exportCSV(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("students.csv");
        File file = chooser.showSaveDialog(menuBar.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("First Name,Last Name,Department,Major,Email,Image URL");
            for (Person p : data) {
                pw.printf("%s,%s,%s,%s,%s,%s%n",
                        p.getFirstName(), p.getLastName(), p.getDepartment(),
                        p.getMajor(), p.getEmail(),
                        p.getImageURL() != null ? p.getImageURL() : "");
            }
            setStatus("Exported " + data.size() + " record(s) to " + file.getName());
        } catch (IOException e) {
            setStatus("Export failed: " + e.getMessage());
        }
    }

    // ── Extra Credit: PDF Report by Major ────────────────────────────────────
    @FXML
    public void generatePDFReport(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save PDF Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("AcademicTrack_Report.pdf");
        File file = chooser.showSaveDialog(menuBar.getScene().getWindow());
        if (file == null) return;

        Map<String, Long> byMajor = data.stream()
                .collect(Collectors.groupingBy(Person::getMajor, Collectors.counting()));
        long total = data.size();

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // ── Title block ──────────────────────────────────────────────
                cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
                cs.beginText();
                cs.newLineAtOffset(72, 722);
                cs.showText("AcademicTrack — Student Registry");
                cs.endText();

                cs.setFont(PDType1Font.HELVETICA_BOLD, 15);
                cs.beginText();
                cs.newLineAtOffset(72, 698);
                cs.showText("Enrollment Report by Major");
                cs.endText();

                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.beginText();
                cs.newLineAtOffset(72, 678);
                cs.showText("Generated: " + LocalDate.now()
                        + "   |   Total Students: " + total);
                cs.endText();

                // Title underline
                cs.setLineWidth(1.5f);
                cs.moveTo(72, 668);
                cs.lineTo(540, 668);
                cs.stroke();

                // ── Column headers ───────────────────────────────────────────
                float y = 650;
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                drawText(cs, "Major",      72,  y);
                drawText(cs, "Students",  260,  y);
                drawText(cs, "Percentage", 340, y);
                drawText(cs, "Distribution", 430, y);

                y -= 5;
                cs.setLineWidth(0.75f);
                cs.moveTo(72, y);
                cs.lineTo(540, y);
                cs.stroke();
                y -= 18;

                // ── Data rows ────────────────────────────────────────────────
                cs.setFont(PDType1Font.HELVETICA, 11);
                List<Map.Entry<String, Long>> sorted = byMajor.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .collect(Collectors.toList());

                for (Map.Entry<String, Long> entry : sorted) {
                    double pct = total > 0 ? (entry.getValue() * 100.0 / total) : 0;

                    drawText(cs, entry.getKey(),                   72,  y);
                    drawText(cs, String.valueOf(entry.getValue()), 260, y);
                    drawText(cs, String.format("%.1f%%", pct),    340, y);

                    // Blue proportional bar
                    float barWidth = (float) (pct / 100.0 * 90);
                    if (barWidth > 0) {
                        cs.setNonStrokingColor(0.18f, 0.42f, 0.78f);
                        cs.addRect(430, y - 1, barWidth, 10);
                        cs.fill();
                        cs.setNonStrokingColor(0f, 0f, 0f);
                    }

                    y -= 22;
                }

                // ── Total row ────────────────────────────────────────────────
                cs.setLineWidth(0.75f);
                cs.moveTo(72, y + 14);
                cs.lineTo(540, y + 14);
                cs.stroke();

                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                drawText(cs, "TOTAL",             72,  y);
                drawText(cs, String.valueOf(total), 260, y);
                drawText(cs, "100.0%",            340, y);

                // ── Legend ───────────────────────────────────────────────────
                y -= 40;
                cs.setFont(PDType1Font.HELVETICA, 9);
                cs.setNonStrokingColor(0.18f, 0.42f, 0.78f);
                cs.addRect(72, y, 10, 10);
                cs.fill();
                cs.setNonStrokingColor(0f, 0f, 0f);
                cs.setFont(PDType1Font.HELVETICA, 9);
                drawText(cs, "= Student count (bar width proportional to %)", 88, y + 1);

                // ── Footer ───────────────────────────────────────────────────
                cs.setLineWidth(0.5f);
                cs.moveTo(72, 85);
                cs.lineTo(540, 85);
                cs.stroke();
                cs.setFont(PDType1Font.HELVETICA, 9);
                drawText(cs, "AcademicTrack v2.0  |  CSC311 Advanced Programming  |  Confidential", 72, 72);
            }

            doc.save(file);
            setStatus("PDF report saved: " + file.getName()
                    + " (" + byMajor.size() + " major(s), " + total + " student(s))");
            MyLogger.makeLog("PDF report generated: " + file.getAbsolutePath());

        } catch (IOException e) {
            setStatus("PDF report failed: " + e.getMessage());
        }
    }

    private void drawText(PDPageContentStream cs, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    // ── Navigation / theme ───────────────────────────────────────────────────
    @FXML
    protected void logOut(ActionEvent actionEvent) {
        try {
            UserSession session = UserSession.getInstance();
            if (session != null) session.cleanUserSession();
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void closeApplication() {
        System.exit(0);
    }

    @FXML
    protected void displayAbout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/about.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void lightTheme(ActionEvent actionEvent) {
        try {
            Scene scene = menuBar.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void darkTheme(ActionEvent actionEvent) {
        try {
            Scene scene = menuBar.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/darkTheme.css").toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── File > New dialog ────────────────────────────────────────────────────
    @FXML
    protected void addRecord() {
        showSomeone();
    }

    public void showSomeone() {
        Dialog<Results> dialog = new Dialog<>();
        dialog.setTitle("New User");
        dialog.setHeaderText("Please specify…");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField textField1 = new TextField("Name");
        TextField textField2 = new TextField("Last Name");
        TextField textField3 = new TextField("Email");
        ObservableList<Major> options = FXCollections.observableArrayList(Major.values());
        ComboBox<Major> comboBox = new ComboBox<>(options);
        comboBox.getSelectionModel().selectFirst();
        dialogPane.setContent(new VBox(8, textField1, textField2, textField3, comboBox));
        Platform.runLater(textField1::requestFocus);
        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK)
                return new Results(textField1.getText(), textField2.getText(), comboBox.getValue());
            return null;
        });
        Optional<Results> optionalResult = dialog.showAndWait();
        optionalResult.ifPresent((Results results) ->
                MyLogger.makeLog(results.fname + " " + results.lname + " " + results.major));
    }

    // ── Major enum ───────────────────────────────────────────────────────────
    public enum Major { Business, CS, CSC, CPIS, English, IT, Math }

    private static class Results {
        String fname, lname;
        Major major;
        Results(String fname, String lname, Major major) {
            this.fname = fname; this.lname = lname; this.major = major;
        }
    }
}

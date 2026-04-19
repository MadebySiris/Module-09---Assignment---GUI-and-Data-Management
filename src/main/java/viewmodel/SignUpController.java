package viewmodel;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.UserSession;

import java.util.prefs.Preferences;
import java.util.regex.Pattern;

public class SignUpController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,6}$");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{6,}$");

    @FXML
    public void createNewAccount(ActionEvent actionEvent) {
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            statusLabel.setText("All fields are required.");
            return;
        }
        if (username.length() < 3) {
            statusLabel.setText("Username must be at least 3 characters.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            statusLabel.setText("Please enter a valid email address.");
            return;
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            statusLabel.setText("Password must be 6+ characters with at least one letter and one number.");
            return;
        }
        if (!password.equals(confirm)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        Preferences prefs = Preferences.userRoot().node("academictrack");
        prefs.put("USERNAME", username);
        prefs.put("EMAIL", email);
        prefs.put("PASSWORD", password);

        UserSession.getInstance(username, password, "USER");

        statusLabel.setStyle("-fx-text-fill: #00aa00; -fx-font-size: 12px;");
        statusLabel.setText("Account created! Redirecting to login...");

        // Brief pause then navigate back to login
        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> navigateToLogin(actionEvent));
        }).start();
    }

    @FXML
    public void goBack(ActionEvent actionEvent) {
        navigateToLogin(actionEvent);
    }

    private void navigateToLogin(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

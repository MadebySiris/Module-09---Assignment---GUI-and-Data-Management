package viewmodel;

import dao.DbConnectivityClass;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainApplication extends Application {

    private static Scene scene;
    private static DbConnectivityClass cnUtil;
    private Stage primaryStage;

    public static void main(String[] args) {
        cnUtil = new DbConnectivityClass();
        launch(args);

    }

    public void start(Stage primaryStage) {
        Image icon = new Image(getClass().getResourceAsStream("/images/DollarClouddatabase.png"));
        this.primaryStage = primaryStage;
        this.primaryStage.setResizable(false);
        primaryStage.getIcons().add(icon);
        primaryStage.setTitle("AcademicTrack — Student Registry");
        showScene1();
    }

    private void showScene1() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/splashscreen.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.show();
            changeScene();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeScene() {
        try {
            Parent newRoot = FXMLLoader.load(getClass().getResource("/view/login.fxml").toURI().toURL());
            Scene currentScene = primaryStage.getScene();
            Parent currentRoot = currentScene.getRoot();
            currentScene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(3), currentRoot);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                Scene newScene = new Scene(newRoot, 900, 600);
                primaryStage.setScene(newScene);
                primaryStage.show();
            });
            fadeOut.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
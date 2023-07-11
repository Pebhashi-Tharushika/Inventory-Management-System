package lk.mbpt.ims.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class AppInitializer extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            primaryStage.setTitle("Log In Prompt");
            primaryStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/view/loginView.fxml"))));
            primaryStage.getIcons().add(new Image("image/logo.png"));

            primaryStage.setResizable(false);
            primaryStage.show();
            primaryStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error occurred. Failed to initialize system.").showAndWait();
        }
    }
}

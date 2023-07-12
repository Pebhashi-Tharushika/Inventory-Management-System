package lk.mbpt.ims.app.controller;


import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;


public class SplashScreenViewController {

    public ProgressBar prg;
    public Label lblStatus;

    public void initialize(){

        Timeline timeline = new Timeline();
        var keyFrame1 = new KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                lblStatus.setText("Initializing the UI..!");
                prg.setProgress(prg.getProgress() + 0.2);
            }
        });
        var keyFrame2 = new KeyFrame(Duration.millis(1000), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                lblStatus.setText("Loading Plugins..!");
                prg.setProgress(prg.getProgress() + 0.3);
            }
        });
        var keyFrame3 = new KeyFrame(Duration.millis(2000), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                lblStatus.setText("Setup the Database Connections!");
                prg.setProgress(prg.getProgress() + 0.45);
            }
        });
        var keyFrame4 = new KeyFrame(Duration.millis(2500), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    URL resource = this.getClass().getResource("/view/MainView.fxml");
                    Parent container = FXMLLoader.load(resource);
                    Scene mainScene = new Scene(container);
                    Stage stage = new Stage();
                    stage.setScene(mainScene);
                    stage.setTitle("Inventory Management System");
                    stage.getIcons().add(new Image("image/logo.png"));
                    stage.setResizable(false);
                    stage.setMaximized(true);
                    stage.show();
                    stage.centerOnScreen();
                    lblStatus.getScene().getWindow().hide();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        timeline.getKeyFrames().addAll(keyFrame1, keyFrame2, keyFrame3, keyFrame4);
        timeline.playFromStart();

    }

}

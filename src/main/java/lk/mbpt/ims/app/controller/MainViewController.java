package lk.mbpt.ims.app.controller;

import com.jfoenix.controls.JFXButton;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;

import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import lk.mbpt.ims.app.model.User;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;


public class MainViewController {

    public Label lblLoggedUser;
    public Label lblLogout;
    public Label lblDatetime;
    public AnchorPane rightPane;
    public JFXButton btnDashboard;
    public Label lblTitle;
    public ImageView imgTitle;

    private AnchorPane newRightPane = null;
    private JFXButton temp = null;
    private HashMap<String, String> FXML_URL = new HashMap<>();

    public void initialize(){
        mapNavAndFXML();

        lblLogout.setTooltip(new Tooltip("Click to logout"));

        /* access currently logged user from the environment variable */
        User principal = (User) System.getProperties().get("principal");
        lblLoggedUser.setText(String.format("%s: %s", principal.getRole().name(), principal.getFullName()));

        /* display time */
        lblDatetime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MMM-dd EEEE hh:mm:ss")));
        KeyFrame key = new KeyFrame(Duration.seconds(1), event -> {
            lblDatetime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MMM-dd EEEE hh:mm:ss")));
        });
        Timeline timeline = new Timeline(key);
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.playFromStart();

        btnDashboard.fire();
    }

    public void btnNavOnAction(ActionEvent event) throws IOException {
        /* Mark selected button of navigation bar */
        selectNav(event);

        JFXButton btn = (JFXButton) event.getSource();
        String btnText = btn.getText();

        /* load respective pane when a button of navigation bar is clicked */
        controlPaneRight(FXML_URL.get(btnText));

        lblTitle.setText(btnText);
        imgTitle.setImage(new Image("/image/"+btnText + ".png"));
    }

    public void lblLogoutOnMouseClicked(MouseEvent event) throws IOException {
        Stage current = (Stage)lblLogout.getScene().getWindow();
        current.close();

        Stage stage = new Stage();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/view/loginView.fxml"))));
        stage.show();
    }

    private void selectNav(ActionEvent event) {
        JFXButton btn = (JFXButton)event.getSource();
        if(temp !=null) temp.setStyle("");
        temp = btn;
        btn.setStyle("-fx-background-color: #637883");
    }

    private void controlPaneRight(String URL) throws IOException {
        rightPane.getChildren().clear();
        newRightPane = FXMLLoader.load(getClass().getResource(URL));

        newRightPane.setPrefHeight(rightPane.getHeight());
        newRightPane.setPrefWidth(rightPane.getWidth());

        rightPane.getChildren().add(newRightPane);

        // Listener to monitor any window size change
        rightPane.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
            // Resize some components of the scene automatically
            newRightPane.setPrefWidth(rightPane.getWidth());
            newRightPane.setPrefHeight(rightPane.getHeight());
        });
    }


    private void mapNavAndFXML() {
        FXML_URL.put("Dashboard", "/view/DashboardView.fxml");
        FXML_URL.put("Products", "/view/ProductView.fxml");
        FXML_URL.put("Customers", "/view/CustomerView.fxml");
        FXML_URL.put("Sales", "/view/SaleView.fxml");
        FXML_URL.put("Items", "/view/ItemView.fxml");
        FXML_URL.put("Suppliers", "/view/SupplierView.fxml");
        FXML_URL.put("Purchases", "/view/PurchaseView.fxml");
        FXML_URL.put("Administrative", "/view/UserView.fxml");
    }
}

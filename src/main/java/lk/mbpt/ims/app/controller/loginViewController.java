package lk.mbpt.ims.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lk.mbpt.ims.app.db.DBConnection;
import lk.mbpt.ims.app.model.User;
import lk.mbpt.ims.app.util.PasswordEncoder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class loginViewController {
    public JFXButton btnLogin;
    public JFXTextField txtUsername;
    public JFXPasswordField txtPassword;

    public void initialize(){
        txtUsername.textProperty().addListener((observableValue, previous, current) -> txtUsername.setFocusColor(Color.rgb(126,144,206)));
        txtPassword.textProperty().addListener((observableValue, previous, current) -> txtPassword.setFocusColor(Color.rgb(126,144,206)));
    }

    public void btnLoginOnAction(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        try {
            Connection connection = DBConnection.getInstance().getConnection();
            String sql = "SELECT * FROM user WHERE username=?";
            PreparedStatement stm = connection.prepareStatement(sql);
            stm.setString(1, username);
            ResultSet rst = stm.executeQuery();

            if (!rst.next()){
                new Alert(Alert.AlertType.ERROR, "Incorrect Username, Please try again").showAndWait();
                txtUsername.setFocusColor(Color.web("red"));
                txtUsername.requestFocus();
                txtUsername.selectAll();
            }else{
                if (!PasswordEncoder.matches(password, rst.getString("password"))){
                    new Alert(Alert.AlertType.ERROR, "Incorrect Password, Please try again").showAndWait();
                    txtPassword.setFocusColor(Color.web("red"));
                    txtPassword.requestFocus();
                    txtPassword.selectAll();
                    return;
                }

                String fullName = rst.getString("full_name");
                User.Role role = User.Role.valueOf(rst.getString("role"));
                User principal = new User(fullName, username, password, role);
                System.getProperties().put("principal", principal); // store currently logged user as the environment variable

                Stage stage = new Stage();
                stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/view/SplashScreenView.fxml"))));
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.getIcons().add(new Image("image/logo.png"));
                stage.centerOnScreen();
                stage.setResizable(false);
                stage.show();

                btnLogin.getScene().getWindow().hide();

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package lk.mbpt.ims.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;


public class UserViewController {


    public JFXButton btnNew;
    public JFXTextField txtFullName;
    public JFXTextField txtUsername;
    public JFXPasswordField txtPassword;
    public JFXPasswordField txtConfirmPassword;
    public JFXRadioButton rdoAdmin;
    public ToggleGroup role;
    public JFXRadioButton rdoUser;
    public JFXButton btnSave;
    public JFXButton btnDelete;
    public TableView tblUsers;
    public JFXTextField txtSearch;
    public JFXButton btnReport;


    public void btnNewOnAction(ActionEvent event) {
    }

    public void btnSaveOnAction(ActionEvent event) {
    }

    public void btnDeleteOnAction(ActionEvent event) {
    }

    public void tblUsersOnKeyReleased(KeyEvent event) {
    }

    public void btnReportOnAction(ActionEvent event) {
    }
}

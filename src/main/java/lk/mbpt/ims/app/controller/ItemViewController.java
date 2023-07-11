package lk.mbpt.ims.app.controller;


import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyEvent;


public class ItemViewController {

    public JFXButton btnNew;
    public JFXTextField txtCode;
    public JFXTextField txtDescription;
    public JFXTextField txtQty;
    public JFXComboBox<String> cmbUnits;
    public JFXButton btnSave;
    public JFXButton btnDelete;
    public TableView tblItems;
    public JFXTextField txtSearch;
    public JFXButton btnReport;
    public JFXTextField txtAlertQty;
    public JFXComboBox<String> cmbAlertUnits;


    public void btnNewOnAction(ActionEvent event) {
    }

    public void btnSaveOnAction(ActionEvent event) {
    }

    public void btnDeleteOnAction(ActionEvent event) {
    }

    public void tblItemsOnKeyReleased(KeyEvent event) {
    }

    public void btnReportOnAction(ActionEvent event) {
    }
}

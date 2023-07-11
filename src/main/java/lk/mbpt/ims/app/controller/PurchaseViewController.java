package lk.mbpt.ims.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;


public class PurchaseViewController {

    public JFXButton btnSave;
    public JFXButton btnDelete;
    public JFXButton btnNew;
    public TableView tblPurchases;
    public JFXTextField txtSearch;
    public JFXButton btnReport;
    public JFXTextField txtOrderId;
    public JFXTextField txtSupplierId;
    public JFXTextField txtItemCode;
    public JFXTextField txtUnitPrice;
    public JFXTextField txtQty;
    public JFXComboBox<String> cmbUnits;
    public DatePicker dtOrderDate;
    public JFXRadioButton rdoOpen;
    public JFXRadioButton rdoClosed;
    public ToggleGroup status;
    public Label lblTotal;


    public void btnNewOnAction(ActionEvent event) {
    }

    public void btnSaveOnAction(ActionEvent event) {
    }

    public void btnDeleteOnAction(ActionEvent event) {
    }

    public void tblPurchasesOnKeyReleased(KeyEvent event) {
    }

    public void btnReportOnAction(ActionEvent event) {
    }
}

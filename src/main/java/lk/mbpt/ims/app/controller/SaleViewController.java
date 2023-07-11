package lk.mbpt.ims.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;


public class SaleViewController {

    public JFXButton btnSave;
    public JFXButton btnDelete;
    public JFXButton btnNew;
    public JFXTextField txtSearch;
    public JFXButton btnReport;
    public JFXTextField txtOrderId;

    public JFXTextField txtCustomerId;
    public JFXTextField txtProductCode;
    public TableView tblSales;
    public JFXRadioButton rdoOpen;
    public JFXRadioButton rdoClosed;
    public DatePicker dtOrderDate;
    public Label lblUnitPrice;
    public Label lblTotal;
    public Spinner<Integer> spnQty;
    public ToggleGroup status;


    public void btnNewOnAction(ActionEvent event) {
    }

    public void btnSaveOnAction(ActionEvent event) {
    }

    public void btnDeleteOnAction(ActionEvent event) {
    }

    public void tblSalesOnKeyReleased(KeyEvent event) {
    }

    public void btnReportOnAction(ActionEvent event) {
    }
}

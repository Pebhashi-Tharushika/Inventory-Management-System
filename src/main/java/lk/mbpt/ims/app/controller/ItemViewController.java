package lk.mbpt.ims.app.controller;


import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import lk.mbpt.ims.app.db.DBConnection;
import lk.mbpt.ims.app.model.Item;
import org.controlsfx.control.textfield.TextFields;

import java.math.BigDecimal;
import java.sql.*;


public class ItemViewController {

    public JFXButton btnNew;
    public JFXTextField txtCode;
    public JFXTextField txtDescription;
    public JFXTextField txtQty;
    public JFXComboBox<String> cmbUnits;
    public JFXButton btnSave;
    public JFXButton btnDelete;
    public TableView<Item> tblItems;
    public JFXTextField txtSearch;
    public JFXButton btnReport;
    public JFXTextField txtAlertQty;
    public JFXComboBox<String> cmbAlertUnits;

    public void initialize(){
        cmbAlertUnits.setDisable(true);
        JFXComboBox<String>[] comboBoxes = new JFXComboBox[]{cmbUnits, cmbAlertUnits};
        for (JFXComboBox<String> cmb : comboBoxes) {
            ObservableList<String> unitList = cmb.getItems();
            unitList.addAll("L", "kg", "M3");
            TextFields.bindAutoCompletion(cmb.getEditor(), cmb.getItems()); // Bind auto-completion functionality to the combo box
        }

        /* Table column mapping */
        tblItems.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("displayItemCode"));
        tblItems.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("description"));
        tblItems.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("displayQty"));
        tblItems.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("uom"));
        tblItems.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("displayAlertQty"));

        tblItems.getColumns().get(2).setStyle("-fx-alignment: CENTER_RIGHT");
        tblItems.getColumns().get(3).setStyle("-fx-alignment: CENTER_RIGHT");
        tblItems.getColumns().get(4).setStyle("-fx-alignment: CENTER_RIGHT");

        txtCode.setDisable(true);
        btnSave.setDefaultButton(true);
        btnDelete.setDisable(true);
        Platform.runLater(txtDescription::requestFocus);

        tblItems.getSelectionModel().selectedItemProperty().addListener((ov, prev, current) -> {
            btnDelete.setDisable(current == null);
            if (current == null) {
                btnNew.fire();
                return;
            }

            txtCode.setText(current.getDisplayItemCode());
            txtDescription.setText(current.getDescription());
            txtQty.setText(current.getQty().toString());
            txtAlertQty.setText(current.getAlertQty().toString());

            cmbUnits.getSelectionModel().select(current.getUom());
        });

        cmbUnits.getSelectionModel().selectedItemProperty().addListener((observableValue, previous, current) -> {
            cmbAlertUnits.getSelectionModel().select(current);
        });

        /* reset fields when starting to type*/
        JFXTextField[] jfxTextFields = {txtDescription,txtQty,txtAlertQty};
        for (JFXTextField txt : jfxTextFields) {
            txt.textProperty().addListener((observableValue, previous, current) -> {
                setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
            });
        }

        cmbUnits.getSelectionModel().selectedItemProperty().addListener((observableValue, previous, current) -> {
            setFocusColorAndUnFocusColor(cmbUnits, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
        });

        txtSearch.textProperty().addListener(c -> {
            tblItems.getSelectionModel().clearSelection();
            loadItems();
        });

        loadItems();
    }

    private void setFocusColorAndUnFocusColor(Node node, Color focusColor, Color unFocusColor){
        if(node instanceof JFXTextField){
            JFXTextField txt = (JFXTextField)node;
            txt.setFocusColor(focusColor);
            txt.setUnFocusColor(unFocusColor);
        } else if (node instanceof JFXComboBox) {
            JFXComboBox cmb = (JFXComboBox) node;
            cmb.setFocusColor(focusColor);
            cmb.setUnFocusColor(unFocusColor);
        }
    }

    private void loadItems() {
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            String sql = "SELECT * FROM item WHERE code LIKE ? OR description LIKE ? OR qty LIKE ? OR unit_of_measure LIKE ? OR alert_qty LIKE ?";
            PreparedStatement stm = connection.prepareStatement(sql);
            String query = "%" + txtSearch.getText() + "%";
            for (var i = 1; i <= 5; i++) stm.setString(i, query);
            ResultSet rst = stm.executeQuery();

            ObservableList<Item> itemList = tblItems.getItems();
            itemList.clear();

            while (rst.next()){
                int itemCode = rst.getInt("code");
                String description = rst.getString("description");
                BigDecimal qty = rst.getBigDecimal("qty");
                String uom = rst.getString("unit_of_measure");
                BigDecimal alertQty = rst.getBigDecimal("alert_qty");
                Item item = new Item(itemCode, description, qty, uom,alertQty);
                itemList.add(item);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load stock items, try again").showAndWait();
            throw new RuntimeException(e);
        }
    }

    public void btnNewOnAction(ActionEvent event) {
        txtCode.clear();
        txtCode.setPromptText("Generated ID");
        txtDescription.clear();
        txtQty.clear();
        txtAlertQty.clear();

        tblItems.getSelectionModel().clearSelection();
        cmbUnits.getSelectionModel().clearSelection();
        if(!txtSearch.isFocused()) txtDescription.requestFocus();

        JFXTextField[] jfxTextFields = {txtDescription, txtQty, txtAlertQty};
        for (JFXTextField txt : jfxTextFields) {
            setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
        }

        setFocusColorAndUnFocusColor(cmbUnits, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));

    }

    public void btnSaveOnAction(ActionEvent event) {
        if (!isDataValid()) return;

        String description = txtDescription.getText();
        BigDecimal qty = new BigDecimal(txtQty.getText().strip());
        String uom = cmbUnits.getSelectionModel().getSelectedItem();
        BigDecimal alertQty = new BigDecimal(txtAlertQty.getText().strip());
        Item newItem = new Item(null, description, qty, uom, alertQty);

        Connection connection = DBConnection.getInstance().getConnection();
        try {
            String sql;
            PreparedStatement stm;

            Item selectedItem = tblItems.getSelectionModel().getSelectedItem();
            if(selectedItem == null){
                sql = "INSERT INTO item (description, qty, unit_of_measure, alert_qty) VALUES (?, ?, ?, ?)";
                stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stm.setString(1, newItem.getDescription());
                stm.setBigDecimal(2, newItem.getQty());
                stm.setString(3, newItem.getUom());
                stm.setBigDecimal(4,newItem.getAlertQty());

                stm.executeUpdate();
                ResultSet generatedKeys = stm.getGeneratedKeys();
                generatedKeys.next();
                int generatedId = generatedKeys.getInt(1);
                newItem.setCode(generatedId);
                tblItems.getItems().add(newItem);

            }else{
                sql = "UPDATE item SET description=?, qty=?, unit_of_measure=?, alert_qty=? WHERE code=?";
                stm = connection.prepareStatement(sql);
                stm.setString(1, newItem.getDescription());
                stm.setBigDecimal(2, newItem.getQty());
                stm.setString(3, newItem.getUom());
                stm.setBigDecimal(4,newItem.getAlertQty());
                stm.setInt(5, Integer.parseInt(txtCode.getText().substring(2)));

                stm.executeUpdate();
                selectedItem.setDescription(newItem.getDescription());
                selectedItem.setQty(newItem.getQty());
                selectedItem.setUom(newItem.getUom());
                selectedItem.setAlertQty(newItem.getAlertQty());
                tblItems.refresh();
            }


        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save the stock item, try again!").showAndWait();
            throw new RuntimeException(e);
        }

        btnNew.fire();
    }

    private boolean isDataValid() {
        boolean dataValid = true;

        if (!txtAlertQty.getText().matches("\\d+(\\.\\d{2})?")) {
            setFocusColorAndUnFocusColor(txtAlertQty, Color.web("red"), Color.web("red"));
            txtAlertQty.requestFocus();
            txtAlertQty.selectAll();
            dataValid = false;
        }

        if(cmbUnits.getSelectionModel().getSelectedItem() == null){
            setFocusColorAndUnFocusColor(cmbUnits, Color.web("red"), Color.web("red"));
            cmbUnits.requestFocus();
            dataValid = false;
        }

        if (!txtQty.getText().matches("\\d+(\\.\\d{2})?")) {
            setFocusColorAndUnFocusColor(txtQty, Color.web("red"), Color.web("red"));
            txtQty.requestFocus();
            txtQty.selectAll();
            dataValid = false;
        }

        if (!txtDescription.getText().matches(".{5,}")) {
            setFocusColorAndUnFocusColor(txtDescription, Color.web("red"), Color.web("red"));
            txtDescription.requestFocus();
            txtDescription.selectAll();
            dataValid = false;
        }

        return dataValid;
    }

    public void btnDeleteOnAction(ActionEvent event) {
        Item selectedItem = tblItems.getSelectionModel().getSelectedItem();
        ObservableList<Item> itemList = tblItems.getItems();
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            PreparedStatement stm = connection.prepareStatement("DELETE  FROM  item WHERE  code=?");
            stm.setInt(1, selectedItem.getCode());
            stm.executeUpdate();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR,"Failed to delete the stock item, try again").showAndWait();
            throw new RuntimeException(e);
        }

        itemList.remove(selectedItem);
        if (itemList.isEmpty()) btnNew.fire();
    }

    public void tblItemsOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE) btnDelete.fire();
    }

    public void btnReportOnAction(ActionEvent event) {
    }
}

package lk.mbpt.ims.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import lk.mbpt.ims.app.db.DBConnection;
import lk.mbpt.ims.app.model.Item;
import lk.mbpt.ims.app.model.PurchaseOrder;
import lk.mbpt.ims.app.model.Supplier;
import lk.mbpt.ims.app.util.Status;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JasperViewer;
import org.controlsfx.control.textfield.TextFields;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;


public class PurchaseViewController {

    public JFXButton btnSave;
    public JFXButton btnDelete;
    public JFXButton btnNew;
    public TableView<PurchaseOrder> tblPurchases;
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

    private final ArrayList<String> supplierHolder = new ArrayList<>();
    private final ArrayList<String> itemHolder = new ArrayList<>();

    public void initialize() {
        ObservableList<String> items = cmbUnits.getItems();
        items.addAll("L", "kg", "M3");
        TextFields.bindAutoCompletion(cmbUnits.getEditor(), cmbUnits.getItems()); // Bind auto-completion functionality to the combo box

        /* Table column mapping */
        tblPurchases.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("displayOrderId"));
        tblPurchases.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        tblPurchases.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("displaySupplierId"));
        tblPurchases.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("displayItemCode"));
        tblPurchases.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("displayUnitPrice"));
        tblPurchases.getColumns().get(5).setCellValueFactory(new PropertyValueFactory<>("displayQty"));
        tblPurchases.getColumns().get(6).setCellValueFactory(new PropertyValueFactory<>("uom"));
        tblPurchases.getColumns().get(7).setCellValueFactory(new PropertyValueFactory<>("displayTotal"));
        tblPurchases.getColumns().get(8).setCellValueFactory(new PropertyValueFactory<>("status"));

        tblPurchases.getColumns().get(4).setStyle("-fx-alignment: CENTER_RIGHT");
        tblPurchases.getColumns().get(5).setStyle("-fx-alignment: CENTER_RIGHT");
        tblPurchases.getColumns().get(7).setStyle("-fx-alignment: CENTER_RIGHT");
        tblPurchases.getColumns().get(8).setStyle("-fx-alignment: CENTER");

        txtOrderId.setDisable(true);
        btnDelete.setDisable(true);
        Platform.runLater(txtSupplierId::requestFocus);

        tblPurchases.getSelectionModel().selectedItemProperty().addListener((ov, prev, current) -> {
            btnDelete.setDisable(current == null);
            if (current == null) {
                btnNew.fire();
                return;
            }

            txtOrderId.setText(current.getDisplayOrderId());
            txtSupplierId.setText(current.getDisplaySupplierId());
            txtItemCode.setText(current.getDisplayItemCode());
            txtUnitPrice.setText(current.getUnitPrice().toString());
            txtQty.setText(current.getQty().toString());
            lblTotal.setText(current.getDisplayTotal());

            cmbUnits.getSelectionModel().select(current.getUom());
            dtOrderDate.setValue(current.getOrderDate());
            if (current.getStatus().equals(Status.OPEN)) rdoOpen.setSelected(true);
            else rdoClosed.setSelected(true);
        });

        /* reset fields when starting to type */
        JFXTextField[] jfxTextFields = {txtSupplierId, txtItemCode, txtUnitPrice, txtQty, txtItemCode, txtSupplierId};
        for (JFXTextField txt : jfxTextFields) {
            txt.textProperty().addListener((observableValue, previous, current) -> {
                setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
            });
        }

        JFXRadioButton[] radioButton = {rdoOpen, rdoClosed};
        for (JFXRadioButton rdo : radioButton) {
            rdo.selectedProperty().addListener((observableValue, previous, current) -> {
                rdoOpen.setUnSelectedColor(Color.rgb(90, 90, 90));
                rdoClosed.setUnSelectedColor(Color.rgb(90, 90, 90));
                rdoOpen.setTextFill(Color.rgb(0, 0, 0));
                rdoClosed.setTextFill(Color.rgb(0, 0, 0));
            });
        }

        cmbUnits.getSelectionModel().selectedItemProperty().addListener((observableValue, previous, current) -> {
            setFocusColorAndUnFocusColor(cmbUnits, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
        });

        txtSearch.textProperty().addListener(c -> {
            tblPurchases.getSelectionModel().clearSelection();
            loadPurchaseOrders();
        });

        dtOrderDate.valueProperty().addListener((observableValue, previous, current) -> {
            if (current != null) dtOrderDate.getStyleClass().remove("invalid");
        });

        EnableSuggestionOfSupplierIdAndItemCode();
        generateTotal();
        loadPurchaseOrders();
    }

    private void EnableSuggestionOfSupplierIdAndItemCode() {
        try {
            PreparedStatement stmSupplierList = DBConnection.getInstance().getConnection().prepareStatement("SELECT * FROM supplier");
            PreparedStatement stmItemList = DBConnection.getInstance().getConnection().prepareStatement("SELECT * FROM item");
            ResultSet rstSuppliers = stmSupplierList.executeQuery();
            ResultSet rstItems = stmItemList.executeQuery();
            while (rstSuppliers.next())
                supplierHolder.add(String.format("S-%08d", rstSuppliers.getInt(1)) + " | " + rstSuppliers.getString(2));
            while (rstItems.next())
                itemHolder.add(String.format("I-%08d", rstItems.getInt(1)) + " | " + rstItems.getString(2));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        /* Binds auto-completion functionality */
        TextFields.bindAutoCompletion(txtItemCode, itemHolder);
        TextFields.bindAutoCompletion(txtSupplierId, supplierHolder);
    }

    private void generateTotal() {
        for (JFXTextField txt : new JFXTextField[]{txtUnitPrice, txtQty}) {
            txt.textProperty().addListener((observableValue, previous, current) -> {
                if (!txtQty.getText().trim().isEmpty() && !txtUnitPrice.getText().trim().isEmpty()) {
                    String unitPrice = txtUnitPrice.getText().trim();
                    String qty = txtQty.getText().trim();
                    BigDecimal total = new BigDecimal(unitPrice).multiply(new BigDecimal(qty)).setScale(2);
                    lblTotal.setText(new DecimalFormat("#,#00.00").format(total));
                } else {
                    lblTotal.setText("");
                }
            });
        }
    }

    private void setFocusColorAndUnFocusColor(Node node, Color focusColor, Color unFocusColor) {
        if (node instanceof JFXTextField) {
            JFXTextField txt = (JFXTextField) node;
            txt.setFocusColor(focusColor);
            txt.setUnFocusColor(unFocusColor);
        } else if (node instanceof JFXComboBox) {
            JFXComboBox cmb = (JFXComboBox) node;
            cmb.setFocusColor(focusColor);
            cmb.setUnFocusColor(unFocusColor);
        }
    }

    private void loadPurchaseOrders() {
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            String sql = "SELECT * FROM purchase_order WHERE order_id LIKE ? OR order_date LIKE ? OR supplier_id LIKE ? OR item_code LIKE ? OR unit_price LIKE ? OR qty LIKE ? OR unit_of_measure LIKE ? OR total LIKE ? OR status LIKE ?";
            PreparedStatement stm = connection.prepareStatement(sql);
            String query = "%" + txtSearch.getText() + "%";
            for (var i = 1; i <= 9; i++) stm.setString(i, query);
            ResultSet rst = stm.executeQuery();

            ObservableList<PurchaseOrder> purchaseOrdersList = tblPurchases.getItems();
            purchaseOrdersList.clear();

            while (rst.next()) {
                int orderId = rst.getInt("order_id");
                int itemCode = rst.getInt("item_code");
                int supplierId = rst.getInt("supplier_id");
                BigDecimal qty = rst.getBigDecimal("qty");
                BigDecimal unitPrice = rst.getBigDecimal("unit_price");
                String uom = rst.getString("unit_of_measure");
                Date orderDate = rst.getDate("order_date");
                Status status = Status.valueOf(rst.getString("status"));
                PurchaseOrder purchaseOrder = new PurchaseOrder(orderId, itemCode, supplierId, qty, unitPrice, orderDate.toLocalDate(), uom, status);
                purchaseOrdersList.add(purchaseOrder);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load purchase orders, try again").showAndWait();
            throw new RuntimeException(e);
        }
    }

    public void btnNewOnAction(ActionEvent event) {
        txtOrderId.clear();
        txtOrderId.setPromptText("Generated ID");
        txtSupplierId.clear();
        txtItemCode.clear();
        txtUnitPrice.clear();
        txtQty.clear();
        dtOrderDate.setValue(null);
        status.selectToggle(null);
        lblTotal.setText("");

        tblPurchases.getSelectionModel().clearSelection();
        cmbUnits.getSelectionModel().clearSelection();
        if (!txtSearch.isFocused()) txtSupplierId.requestFocus();

        JFXTextField[] jfxTextFields = {txtSupplierId, txtItemCode, txtUnitPrice, txtQty, txtItemCode, txtSupplierId};
        for (JFXTextField txt : jfxTextFields) {
            setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
        }

        JFXRadioButton[] radioButton = {rdoOpen, rdoClosed};
        for (JFXRadioButton rdo : radioButton) {
            rdoOpen.setUnSelectedColor(Color.rgb(90, 90, 90));
            rdoClosed.setUnSelectedColor(Color.rgb(90, 90, 90));
            rdoOpen.setTextFill(Color.rgb(0, 0, 0));
            rdoClosed.setTextFill(Color.rgb(0, 0, 0));
        }

        setFocusColorAndUnFocusColor(cmbUnits, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));

        dtOrderDate.getStyleClass().remove("invalid");
    }

    public void btnSaveOnAction(ActionEvent event) {
        if (!isDataValid()) return;

        int itemCode = Integer.parseInt(txtItemCode.getText().strip().substring(2, 10));
        int supplierId = Integer.parseInt(txtSupplierId.getText().strip().substring(2, 10));
        BigDecimal qty = new BigDecimal(txtQty.getText().strip());
        BigDecimal unitPrice = new BigDecimal(txtUnitPrice.getText().strip());
        String uom = cmbUnits.getSelectionModel().getSelectedItem();
        LocalDate orderDate = dtOrderDate.getValue();
        Status status = rdoOpen.isSelected() ? Status.OPEN : Status.CLOSED;
        PurchaseOrder newPurchaseOrder = new PurchaseOrder(null, itemCode, supplierId, qty, unitPrice, orderDate, uom, status);

        try {
            Connection connection = DBConnection.getInstance().getConnection();
            String sql;
            PreparedStatement stm;
            PurchaseOrder selectedPurchaseOrder = tblPurchases.getSelectionModel().getSelectedItem();

            connection.setAutoCommit(false); // Starting a new transaction context


            if (selectedPurchaseOrder == null) {
                sql = "INSERT INTO purchase_order (item_code, unit_price, qty, unit_of_measure, supplier_id, order_date, total, status) VALUES (?, ?, ?, ?, ?, ?, ?,?)";
                stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stm.setInt(1, newPurchaseOrder.getItemCode());
                stm.setBigDecimal(2, newPurchaseOrder.getUnitPrice());
                stm.setBigDecimal(3, newPurchaseOrder.getQty());
                stm.setString(4, newPurchaseOrder.getUom());
                stm.setInt(5, newPurchaseOrder.getSupplierId());
                stm.setDate(6, Date.valueOf(newPurchaseOrder.getOrderDate()));
                stm.setBigDecimal(7, newPurchaseOrder.getTotal());
                stm.setString(8, String.valueOf(newPurchaseOrder.getStatus()));

                stm.executeUpdate();
                ResultSet generatedKeys = stm.getGeneratedKeys();
                generatedKeys.next();
                int generatedId = generatedKeys.getInt(1);
                newPurchaseOrder.setOrderId(generatedId);
                tblPurchases.getItems().add(newPurchaseOrder);

                BigDecimal currentQty = findItemByItemCode(newPurchaseOrder.getItemCode()).getQty();
                updateItemStock(currentQty.add(newPurchaseOrder.getQty()), newPurchaseOrder.getItemCode());

            } else {
                sql = "UPDATE purchase_order SET item_code=?, unit_price=?, qty=?, unit_of_measure=?, supplier_id=?, order_date=?, total=?, status=? WHERE order_id=?";
                stm = connection.prepareStatement(sql);
                stm.setInt(1, newPurchaseOrder.getItemCode());
                stm.setBigDecimal(2, newPurchaseOrder.getUnitPrice());
                stm.setBigDecimal(3, newPurchaseOrder.getQty());
                stm.setString(4, newPurchaseOrder.getUom());
                stm.setInt(5, newPurchaseOrder.getSupplierId());
                stm.setDate(6, Date.valueOf(newPurchaseOrder.getOrderDate()));
                stm.setBigDecimal(7, newPurchaseOrder.getTotal());
                stm.setString(8, String.valueOf(newPurchaseOrder.getStatus()));
                stm.setInt(9, Integer.parseInt(txtOrderId.getText().substring(3)));
                stm.executeUpdate();

                BigDecimal diff = selectedPurchaseOrder.getQty().subtract(newPurchaseOrder.getQty());
                BigDecimal currentQty = findItemByItemCode(newPurchaseOrder.getItemCode()).getQty();
                updateItemStock(selectedPurchaseOrder.getQty().compareTo(newPurchaseOrder.getQty())<0 ? currentQty.add(diff.abs()) : currentQty.subtract(diff.abs()), newPurchaseOrder.getItemCode());

                selectedPurchaseOrder.setItemCode(newPurchaseOrder.getItemCode());
                selectedPurchaseOrder.setUnitPrice(newPurchaseOrder.getUnitPrice());
                selectedPurchaseOrder.setQty(newPurchaseOrder.getQty());
                selectedPurchaseOrder.setUom(newPurchaseOrder.getUom());
                selectedPurchaseOrder.setSupplierId(newPurchaseOrder.getSupplierId());
                selectedPurchaseOrder.setOrderDate(newPurchaseOrder.getOrderDate());
                selectedPurchaseOrder.setStatus(newPurchaseOrder.getStatus());
                tblPurchases.refresh();

            }

            connection.commit();

        } catch (SQLException e) {
            try {
                DBConnection.getInstance().getConnection().rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            new Alert(Alert.AlertType.ERROR, "Failed to save the purchase order, try again!").showAndWait();
            throw new RuntimeException(e);
        } finally {
            try {
                DBConnection.getInstance().getConnection().setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        btnNew.fire();
    }

    private boolean isDataValid() {
        dtOrderDate.getStyleClass().remove("invalid");
        boolean dataValid = true;

        if (!rdoOpen.isSelected() && !rdoClosed.isSelected()) {
            rdoOpen.setUnSelectedColor(Color.rgb(255, 0, 0));
            rdoClosed.setUnSelectedColor(Color.rgb(255, 0, 0));
            rdoOpen.setTextFill(Color.rgb(255, 0, 0));
            rdoClosed.setTextFill(Color.rgb(255, 0, 0));
            rdoOpen.requestFocus();
            dataValid = false;
        }

        if (dtOrderDate.getValue() == null) {
            dtOrderDate.getStyleClass().add("invalid");
            dtOrderDate.requestFocus();
            dataValid = false;
        }

        if (cmbUnits.getSelectionModel().getSelectedItem() == null) {
            setFocusColorAndUnFocusColor(cmbUnits, Color.web("red"), Color.web("red"));
            cmbUnits.requestFocus();
            dataValid = false;
        }

        if (!txtQty.getText().matches("[1-9]\\d*(\\.\\d{2})?")) {
            setFocusColorAndUnFocusColor(txtQty, Color.web("red"), Color.web("red"));
            txtQty.requestFocus();
            txtQty.selectAll();
            dataValid = false;
        }

        if (!txtUnitPrice.getText().matches("[1-9]\\d*(\\.\\d{2})?")) {
            setFocusColorAndUnFocusColor(txtUnitPrice, Color.web("red"), Color.web("red"));
            txtUnitPrice.requestFocus();
            txtUnitPrice.selectAll();
            dataValid = false;
        }

        if (txtItemCode.getText().strip().isEmpty()) {
            setFocusColorAndUnFocusColor(txtItemCode, Color.web("red"), Color.web("red"));
            txtItemCode.requestFocus();
            txtItemCode.selectAll();
            dataValid = false;

        }

        if (txtSupplierId.getText().strip().isEmpty()) {
            setFocusColorAndUnFocusColor(txtSupplierId, Color.web("red"), Color.web("red"));
            txtSupplierId.requestFocus();
            txtSupplierId.selectAll();
            dataValid = false;

        }

        return dataValid;
    }

    private Item findItemByItemCode(int code) {
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            String sql = "SELECT * FROM item WHERE code=?";
            PreparedStatement stm = connection.prepareStatement(sql);
            stm.setInt(1, code);
            ResultSet rst = stm.executeQuery();
            if (rst.next()) {
                int itemCode = rst.getInt("code");
                String description = rst.getString("description");
                BigDecimal qty = rst.getBigDecimal("qty");
                String uom = rst.getString("unit_of_measure");
                BigDecimal alertQty = rst.getBigDecimal("alert_qty");
                System.out.println(itemCode);
                return new Item(itemCode, description, qty, uom, alertQty);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to find item, try again").showAndWait();
            throw new RuntimeException(e);
        }
        return null;
    }

    private void updateItemStock(BigDecimal qty, int itemCode) throws SQLException {
        Connection connection = DBConnection.getInstance().getConnection();
        PreparedStatement stm = connection.prepareStatement("UPDATE item SET qty=? WHERE code=?");
        stm.setBigDecimal(1, qty);
        stm.setInt(2, itemCode);
        stm.executeUpdate();
    }

    public void btnDeleteOnAction(ActionEvent event) {
        PurchaseOrder selectedPurchaseOrder = tblPurchases.getSelectionModel().getSelectedItem();
        ObservableList<PurchaseOrder> purchaseOrdersList = tblPurchases.getItems();
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            connection.setAutoCommit(false); // Starting a new transaction context

            PreparedStatement stm = connection.prepareStatement("DELETE  FROM  purchase_order WHERE  order_id=?");
            stm.setInt(1, selectedPurchaseOrder.getOrderId());
            stm.executeUpdate();

            BigDecimal currentQty = findItemByItemCode(selectedPurchaseOrder.getItemCode()).getQty();
            updateItemStock(currentQty.subtract(selectedPurchaseOrder.getQty()),selectedPurchaseOrder.getItemCode());

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            new Alert(Alert.AlertType.ERROR, "Failed to delete the purchase order, try again").showAndWait();
            throw new RuntimeException(e);
        }finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        purchaseOrdersList.remove(selectedPurchaseOrder);
        if (purchaseOrdersList.isEmpty()) btnNew.fire();
    }

    public void tblPurchasesOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE) btnDelete.fire();
    }

    public void btnReportOnAction(ActionEvent event) throws JRException {
        JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResource("/report/purchase-orders-report.jasper"));

        HashMap<String, Object> reportParams = new HashMap<>();
        Connection dataSource = DBConnection.getInstance().getConnection();

        String searchText = txtSearch.getText();
        reportParams.put("q", "%" + searchText + "%");

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParams, dataSource);
        JasperViewer.viewReport(jasperPrint, false);
    }
}

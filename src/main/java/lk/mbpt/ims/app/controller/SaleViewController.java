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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import lk.mbpt.ims.app.db.DBConnection;
import lk.mbpt.ims.app.model.SaleOrder;
import lk.mbpt.ims.app.util.Status;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JasperViewer;
import org.controlsfx.control.textfield.TextFields;

import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class SaleViewController {

    public JFXButton btnSave;
    public JFXButton btnDelete;
    public JFXButton btnNew;
    public JFXTextField txtSearch;
    public JFXButton btnReport;
    public JFXTextField txtOrderId;

    public JFXTextField txtCustomerId;
    public JFXTextField txtProductCode;
    public TableView<SaleOrder> tblSales;
    public JFXRadioButton rdoOpen;
    public JFXRadioButton rdoClosed;
    public DatePicker dtOrderDate;
    public Label lblUnitPrice;
    public Label lblTotal;
    public Spinner<Integer> spnQty;
    public ToggleGroup status;

    private final ArrayList<String> customerHolder = new ArrayList<>();
    private final Map<String, Integer> productHolder = new HashMap<>();

    public void initialize() {
        /* Table column mapping */
        tblSales.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("displayOrderId"));
        tblSales.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        tblSales.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("displayCustomerId"));
        tblSales.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("displayProductCode"));
        tblSales.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("displayQty"));
        tblSales.getColumns().get(5).setCellValueFactory(new PropertyValueFactory<>("displayUnitPrice"));
        tblSales.getColumns().get(6).setCellValueFactory(new PropertyValueFactory<>("displayTotal"));
        tblSales.getColumns().get(7).setCellValueFactory(new PropertyValueFactory<>("status"));

        tblSales.getColumns().get(4).setStyle("-fx-alignment: CENTER_RIGHT");
        tblSales.getColumns().get(5).setStyle("-fx-alignment: CENTER_RIGHT");
        tblSales.getColumns().get(6).setStyle("-fx-alignment: CENTER_RIGHT");
        tblSales.getColumns().get(7).setStyle("-fx-alignment: CENTER");

        txtOrderId.setDisable(true);
        btnDelete.setDisable(true);
        Platform.runLater(txtCustomerId::requestFocus);

        spnQty.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 0));

        tblSales.getSelectionModel().selectedItemProperty().addListener((ov, prev, current) -> {
            btnDelete.setDisable(current == null);
            if (current == null) {
                btnNew.fire();
                return;
            }

            txtOrderId.setText(current.getDisplayOrderId());
            txtCustomerId.setText(current.getDisplayCustomerId());
            txtProductCode.setText(current.getDisplayProductCode());
            lblUnitPrice.setText(current.getDisplayUnitPrice());
            lblTotal.setText(current.getDisplayTotal());
            spnQty.getValueFactory().setValue(current.getQty());
            dtOrderDate.setValue(current.getOrderDate());
            if (current.getStatus().equals(Status.OPEN)) rdoOpen.setSelected(true);
            else rdoClosed.setSelected(true);

        });

        /* reset fields when starting to type*/
        JFXTextField[] jfxTextFields = {txtCustomerId, txtProductCode};
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

        txtSearch.textProperty().addListener(c -> {
            tblSales.getSelectionModel().clearSelection();
            loadSalesOrders();
        });

        dtOrderDate.valueProperty().addListener((observableValue, previous, current) -> {
            if(current != null) dtOrderDate.getStyleClass().remove("invalid");
        });

        EnableSuggestionOfSupplierIdAndItemCode();
        generateTotal();
        loadSalesOrders();
    }

    private void EnableSuggestionOfSupplierIdAndItemCode() {
        try {
            PreparedStatement stmCustomerList = DBConnection.getInstance().getConnection().prepareStatement("SELECT * FROM customer");
            PreparedStatement stmProductList = DBConnection.getInstance().getConnection().prepareStatement("SELECT * FROM product");
            ResultSet rstCustomers = stmCustomerList.executeQuery();
            ResultSet rstProducts = stmProductList.executeQuery();
            while (rstCustomers.next())
                customerHolder.add(String.format("C-%08d", rstCustomers.getInt(1)) + " | " + rstCustomers.getString(2));
            while (rstProducts.next())
                productHolder.put(String.format("P-%08d", rstProducts.getInt(1)) + " | " + rstProducts.getString(2), rstProducts.getInt(4));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        /* Binds auto-completion functionality */
        TextFields.bindAutoCompletion(txtProductCode, productHolder.keySet());
        TextFields.bindAutoCompletion(txtCustomerId, customerHolder);
    }

    private void generateTotal() {
        spnQty.valueProperty().addListener((observableValue, previous, current) -> {
            if(current == 0 ) {
                lblTotal.setText("0.00");
            }else{
                spnQty.getStyleClass().remove("invalid");
                if(!lblUnitPrice.getText().strip().isEmpty()) {
                    BigDecimal total = new BigDecimal(current).multiply(new BigDecimal(lblUnitPrice.getText().strip().replaceAll(",",""))).setScale(2);
                    lblTotal.setText(new DecimalFormat("#,#00.00").format(total));
                }
            }
        });

        txtProductCode.textProperty().addListener((observableValue, previous, current) -> {
            if (!productHolder.containsKey(current)) {
                lblUnitPrice.setText("");
                lblTotal.setText("");
                return;
            }
            Integer unitPrice = productHolder.get(current);
            Integer qty = spnQty.getValue();
            lblUnitPrice.setText(new DecimalFormat("#,#00.00").format(unitPrice));
            BigDecimal total = new BigDecimal(unitPrice).multiply(new BigDecimal(qty)).setScale(2);
            lblTotal.setText(new DecimalFormat("#,#00.00").format(total));
        });
    }

    private void setFocusColorAndUnFocusColor(JFXTextField txt, Color focusColor, Color unFocusColor){
        txt.setFocusColor(focusColor);
        txt.setUnFocusColor(unFocusColor);
    }

    private void loadSalesOrders() {
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            String sql = "SELECT * FROM sales_order WHERE order_id LIKE ? OR order_date LIKE ? OR customer_id LIKE ? OR product_code LIKE ? OR unit_price LIKE ? OR qty LIKE ? OR total LIKE ? OR status LIKE ?";
            PreparedStatement stm = connection.prepareStatement(sql);
            String query = "%" + txtSearch.getText() + "%";
            for (var i = 1; i <= 8; i++) stm.setString(i, query);
            ResultSet rst = stm.executeQuery();

            ObservableList<SaleOrder> salesOrdersList = tblSales.getItems();
            salesOrdersList.clear();

            while (rst.next()) {
                int orderId = rst.getInt("order_id");
                int productCode = rst.getInt("product_code");
                int customerId = rst.getInt("customer_id");
                int qty = rst.getInt("qty");
                BigDecimal unitPrice = rst.getBigDecimal("unit_price");
                Date orderDate = rst.getDate("order_date");
                Status status = Status.valueOf(rst.getString("status"));
                SaleOrder saleOrder = new SaleOrder(orderId, productCode, customerId, qty, unitPrice, orderDate.toLocalDate(), status);
                salesOrdersList.add(saleOrder);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load sales orders, try again").showAndWait();
            throw new RuntimeException(e);
        }
    }

    public void btnNewOnAction(ActionEvent event) {
        txtOrderId.clear();
        txtOrderId.setPromptText("Generated ID");
        txtCustomerId.clear();
        txtProductCode.clear();
        spnQty.getValueFactory().setValue(0);
        dtOrderDate.setValue(null);
        status.selectToggle(null);
        lblUnitPrice.setText("");
        lblTotal.setText("");

        tblSales.getSelectionModel().clearSelection();
        if (!txtSearch.isFocused()) txtCustomerId.requestFocus();

        JFXTextField[] jfxTextFields = {txtCustomerId, txtProductCode};
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

        spnQty.getStyleClass().remove("invalid");
        dtOrderDate.getStyleClass().remove("invalid");
    }

    public void btnSaveOnAction(ActionEvent event) {
        if (!isDataValid()) return;

        int productCode = Integer.parseInt(txtProductCode.getText().strip().substring(2, 10));
        int customerId = Integer.parseInt(txtCustomerId.getText().strip().substring(2, 10));
        Integer qty = spnQty.getValue();
        BigDecimal unitPrice = new BigDecimal(lblUnitPrice.getText().strip().replaceAll(",",""));
        LocalDate orderDate = dtOrderDate.getValue();
        Status status = rdoOpen.isSelected() ? Status.OPEN : Status.CLOSED;
        SaleOrder newSaleOrder = new SaleOrder(null, productCode, customerId, qty, unitPrice, orderDate, status);

        Connection connection = DBConnection.getInstance().getConnection();
        try {
            String sql;
            PreparedStatement stm;

            SaleOrder selectedSaleOrder = tblSales.getSelectionModel().getSelectedItem();
            if (selectedSaleOrder == null) {
                sql = "INSERT INTO sales_order (product_code, unit_price, qty, customer_id, order_date, total, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
                stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stm.setInt(1, newSaleOrder.getProductCode());
                stm.setBigDecimal(2, newSaleOrder.getUnitPrice());
                stm.setInt(3, newSaleOrder.getQty());
                stm.setInt(4, newSaleOrder.getCustomerId());
                stm.setDate(5, Date.valueOf(newSaleOrder.getOrderDate()));
                stm.setBigDecimal(6, newSaleOrder.getTotal());
                stm.setString(7, String.valueOf(newSaleOrder.getStatus()));

                stm.executeUpdate();
                ResultSet generatedKeys = stm.getGeneratedKeys();
                generatedKeys.next();
                int generatedId = generatedKeys.getInt(1);
                newSaleOrder.setOrderId(generatedId);
                tblSales.getItems().add(newSaleOrder);

            } else {
                sql = "UPDATE sales_order SET product_code=?, unit_price=?, qty=?, customer_id=?, order_date=?, total=?, status=? WHERE order_id=?";
                stm = connection.prepareStatement(sql);
                stm.setInt(1, newSaleOrder.getProductCode());
                stm.setBigDecimal(2, newSaleOrder.getUnitPrice());
                stm.setInt(3, newSaleOrder.getQty());
                stm.setInt(4, newSaleOrder.getCustomerId());
                stm.setDate(5, Date.valueOf(newSaleOrder.getOrderDate()));
                stm.setBigDecimal(6, newSaleOrder.getTotal());
                stm.setString(7, String.valueOf(newSaleOrder.getStatus()));
                stm.setInt(8, Integer.parseInt(txtOrderId.getText().substring(3)));

                stm.executeUpdate();
                selectedSaleOrder.setProductCode(newSaleOrder.getProductCode());
                selectedSaleOrder.setUnitPrice(newSaleOrder.getUnitPrice());
                selectedSaleOrder.setQty(newSaleOrder.getQty());
                selectedSaleOrder.setCustomerId(newSaleOrder.getCustomerId());
                selectedSaleOrder.setOrderDate(newSaleOrder.getOrderDate());
                selectedSaleOrder.setStatus(newSaleOrder.getStatus());
                tblSales.refresh();
            }


        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save the sales order, try again!").showAndWait();
            throw new RuntimeException(e);
        }

        btnNew.fire();
    }

    private boolean isDataValid() {
        spnQty.getStyleClass().remove("invalid");
        dtOrderDate.getStyleClass().remove("invalid");
        boolean dataValid = true;

        if (!rdoOpen.isSelected() && !rdoClosed.isSelected()) {
            rdoOpen.setUnSelectedColor(Color.rgb(255, 0, 0));
            rdoClosed.setUnSelectedColor(Color.rgb(255, 0, 0));
            rdoOpen.setTextFill(Color.rgb(255, 0, 0));
            rdoClosed.setTextFill(Color.rgb(255, 0, 0));
            dataValid = false;
        }

        if (dtOrderDate.getValue() == null) {
            dtOrderDate.getStyleClass().add("invalid");
            dtOrderDate.requestFocus();
            dataValid = false;
        }

        if(spnQty.getValue() == 0){
            spnQty.getStyleClass().add("invalid");
            spnQty.requestFocus();
            dataValid = false;
        }

        if (txtProductCode.getText().strip().isEmpty()) {
            setFocusColorAndUnFocusColor(txtProductCode, Color.web("red"), Color.web("red"));
            txtProductCode.requestFocus();
            txtProductCode.selectAll();
            dataValid = false;
        }

        if (txtCustomerId.getText().strip().isEmpty()) {
            setFocusColorAndUnFocusColor(txtCustomerId, Color.web("red"), Color.web("red"));
            txtCustomerId.requestFocus();
            txtCustomerId.selectAll();
            dataValid = false;
        }

        return dataValid;
    }

    public void btnDeleteOnAction(ActionEvent event) {
        SaleOrder selectedSaleOrder = tblSales.getSelectionModel().getSelectedItem();
        ObservableList<SaleOrder> salesOrderList = tblSales.getItems();
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            PreparedStatement stm = connection.prepareStatement("DELETE  FROM  sales_order WHERE  order_id=?");
            stm.setInt(1, selectedSaleOrder.getOrderId());
            stm.executeUpdate();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to delete the sales order, try again").showAndWait();
            throw new RuntimeException(e);
        }

        salesOrderList.remove(selectedSaleOrder);
        if (salesOrderList.isEmpty()) btnNew.fire();
    }

    public void tblSalesOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE) btnDelete.fire();
    }

    public void btnReportOnAction(ActionEvent event) throws JRException {
        JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResource("/report/sales-orders-report.jasper"));

        HashMap<String, Object> reportParams = new HashMap<>();
        Connection dataSource = DBConnection.getInstance().getConnection();

        String searchText = txtSearch.getText();
        reportParams.put("q", "%" + searchText + "%");

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParams, dataSource);
        JasperViewer.viewReport(jasperPrint, false);
    }
}

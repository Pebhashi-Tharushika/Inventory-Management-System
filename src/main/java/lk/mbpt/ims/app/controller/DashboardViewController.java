package lk.mbpt.ims.app.controller;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;

import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Font;
import lk.mbpt.ims.app.db.DBConnection;
import lk.mbpt.ims.app.model.Item;
import lk.mbpt.ims.app.util.Status;

import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;


public class DashboardViewController {
    public StackedBarChart<String,Integer> barChart;
    public PieChart pieChart;
    public Spinner<Integer> spnYear;
    public Spinner<String> spnMonth;
    public TableView<Item> tblStockAlert;
    public Label lblTodaySale;
    public Label lblTodayCustomers;
    public Label lblPendingSO;
    public Label lblTodayPurchase;
    public Label lblTodaySuppliers;
    public Label lblPendingPO;

    private String month;
    private Integer year;

    public void initialize(){
        /* Table column mapping */
        tblStockAlert.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("displayItemCode"));
        tblStockAlert.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("description"));
        tblStockAlert.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("qty"));
        tblStockAlert.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("uom"));
        tblStockAlert.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("alertQty"));

        setSpinners();
        generateSalesAndPurchaseBarChart();
        generateProductStockPieChart();
        generateItemStockAlertTable();
        seCountForTiles();

        spnMonth.valueProperty().addListener((obs, oldValue, newValue) -> {
            month = newValue;
            generateSalesAndPurchaseBarChart();
        });
        spnYear.valueProperty().addListener((obs, oldValue, newValue) -> {
            year = newValue;
            generateSalesAndPurchaseBarChart();
        });

    }

    private void getCounts(String sql,Object obj, String col, Label lbl){
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            PreparedStatement stm = connection.prepareStatement(sql);
            stm.setObject(1, obj);
            ResultSet rst = stm.executeQuery();
            int todaySale = 0;
            if(rst.next()){
                todaySale = rst.getInt(col);
            }
            lbl.setText(todaySale  + "");
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to get count, try again");
            throw new RuntimeException(e);
        }
    }

    private void seCountForTiles() {
        getCounts("SELECT COUNT(*) as today_sale FROM sales_order WHERE order_date=?", Date.valueOf(LocalDate.now()), "today_sale", lblTodaySale);
        getCounts("SELECT COUNT(DISTINCT customer_id) as today_customers FROM sales_order WHERE order_date=?", Date.valueOf(LocalDate.now()), "today_customers", lblTodayCustomers);
        getCounts("SELECT COUNT(*) as pending_so FROM sales_order WHERE status=?", Status.OPEN.name(), "pending_so", lblPendingSO);
        getCounts("SELECT COUNT(*) as today_purchase FROM purchase_order WHERE order_date=?", Date.valueOf(LocalDate.now()), "today_purchase", lblTodayPurchase);
        getCounts("SELECT COUNT(DISTINCT supplier_id) as today_suppliers FROM purchase_order WHERE order_date=?", Date.valueOf(LocalDate.now()), "today_suppliers", lblTodaySuppliers);
        getCounts("SELECT COUNT(*) as pending_po FROM purchase_order WHERE status=?", Status.OPEN.name(), "pending_po", lblPendingPO);
    }

    private void generateItemStockAlertTable() {
        tblStockAlert.getColumns().get(tblStockAlert.getColumns().size() - 1).getStyleClass().add("last-column");

        Connection connection = DBConnection.getInstance().getConnection();
        try {
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM item WHERE qty<=item.alert_qty");
            ResultSet rst = stm.executeQuery();
            ObservableList<Item> itemList = tblStockAlert.getItems();
            itemList.clear();
            while (rst.next()){
                int itemCode = rst.getInt("code");
                String description = rst.getString("description");
                BigDecimal qty = rst.getBigDecimal("qty");
                String uom = rst.getString("unit_of_measure");
                BigDecimal alertQty = rst.getBigDecimal("alert_qty");
                Item item = new Item(itemCode, description, qty, uom, alertQty);
                itemList.add(item);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load stock items in Item Stock Alert, try again").showAndWait();
            throw new RuntimeException(e);
        }
    }

    private void setSpinners(){
        SpinnerValueFactory<Integer> vfYear = new SpinnerValueFactory.IntegerSpinnerValueFactory(2020, 2040, LocalDateTime.now().getYear());
        spnYear.setValueFactory(vfYear);
        year = spnYear.getValue();

        ObservableList<String> months = FXCollections.observableArrayList("JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER");
        SpinnerValueFactory<String> vfMonth = new SpinnerValueFactory.ListSpinnerValueFactory<String>(months);
        vfMonth.setValue(LocalDateTime.now().getMonth().toString());
        spnMonth.setValueFactory(vfMonth);
        month = spnMonth.getValue();
    }

    private void generateSalesAndPurchaseBarChart() {
        barChart.getData().clear();
        Connection connection = DBConnection.getInstance().getConnection();

        try {
            PreparedStatement stmSale = connection.prepareStatement("SELECT order_date, sum(total) FROM sales_order WHERE MONTHNAME(order_date)=? AND YEAR(order_date)=? GROUP BY order_date ORDER BY UNIX_TIMESTAMP(order_date) ASC");
            stmSale.setString(1,month);
            stmSale.setInt(2, year);
            PreparedStatement stmPurchase = connection.prepareStatement("SELECT order_date, sum(total) FROM purchase_order WHERE MONTHNAME(order_date)=? AND YEAR(order_date)=? GROUP BY order_date ORDER BY UNIX_TIMESTAMP(order_date) ASC");
            stmPurchase.setString(1,month);
            stmPurchase.setInt(2,year);

            ResultSet rstSale = stmSale.executeQuery();
            ResultSet rstPurchase = stmPurchase.executeQuery();

            XYChart.Series sale = new XYChart.Series<>();
            XYChart.Series purchase = new XYChart.Series<>();

            sale.setName("Sales");
            purchase.setName("Purchases");

            while (rstSale.next()){
                sale.getData().add(new XYChart.Data(new SimpleDateFormat("dd-MMM-yyyy").format(rstSale.getDate(1)), rstSale.getBigDecimal(2)));
            }

            while(rstPurchase.next()) {
                purchase.getData().add(new XYChart.Data(new SimpleDateFormat("dd-MMM-yyyy").format(rstPurchase.getDate(1)), rstPurchase.getBigDecimal(2)));
            }

            barChart.getData().addAll(sale,purchase);


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateProductStockPieChart(){
        pieChart.getData().clear();
        ObservableList<PieChart.Data> chartData = pieChart.getData();
        pieChart.setLabelsVisible(false);
        Connection connection = DBConnection.getInstance().getConnection();

        try {
            PreparedStatement stmProduct = connection.prepareStatement("SELECT * FROM product");
            PreparedStatement stmSales = connection.prepareStatement("SELECT SUM(qty) FROM sales_order WHERE product_code=? GROUP BY product_code");

            ResultSet rstProduct = stmProduct.executeQuery();
            while (rstProduct.next()){
                int code = rstProduct.getInt("code");
                String product = rstProduct.getString("description");

                stmSales.setInt(1, code);
                ResultSet rstSales = stmSales.executeQuery();
                int totalProduction = 0;
                if (rstSales.next()) totalProduction = rstSales.getInt(1);

                chartData.add(new PieChart.Data(product, totalProduction));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        pieChart.getData().forEach(data -> {
            String percentage = String.format("%d (%.2f%%)", (int)data.getPieValue(),(data.getPieValue() / 100));
            Tooltip toolTip = new Tooltip(percentage);
            toolTip.setFont(new Font(14));
            Tooltip.install(data.getNode(), toolTip);
        });

    }

}

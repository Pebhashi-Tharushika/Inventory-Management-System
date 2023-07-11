package lk.mbpt.ims.app.controller;


import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;

import javafx.scene.control.*;


public class DashboardViewController {
    public StackedBarChart<String,Integer> barChart;
    public PieChart pieChart;
    public Spinner<Integer> spnYear;
    public Spinner<String> spnMonth;
    public TableView tblStockAlert;
    public Label lblTodaySale;
    public Label lblTodayCustomers;
    public Label lblPendingSO;
    public Label lblTodayPurchase;
    public Label lblTodaySuppliers;
    public Label lblPendingPO;

}

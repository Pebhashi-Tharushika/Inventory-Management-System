package lk.mbpt.ims.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import lk.mbpt.ims.app.db.DBConnection;
import lk.mbpt.ims.app.model.Customer;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JasperViewer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.HashMap;

public class CustomerViewController {
    public JFXTextField txtId;
    public JFXTextField txtName;
    public JFXTextField txtAddress;
    public JFXTextField txtContact;
    public ImageView imgPictureView;
    public JFXButton btnBrowse;
    public JFXButton btnClear;
    public JFXButton btnSave;
    public JFXButton btnDelete;
    public JFXButton btnNew;
    public TableView<Customer> tblCustomers;
    public JFXTextField txtSearch;
    public JFXButton btnReport;

    private final Image noImageAvailableImage = new Image("/image/No_Image_Available.jpg");

    public void initialize() {
        /* Table column mapping */
        tblCustomers.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("displayId"));
        tblCustomers.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("profilePicture"));
        tblCustomers.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("name"));
        tblCustomers.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("address"));
        tblCustomers.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("contact"));

        tblCustomers.getColumns().get(0).setStyle("-fx-alignment: TOP_CENTER");
        tblCustomers.getColumns().get(1).setStyle("-fx-alignment: CENTER");
        tblCustomers.getColumns().get(4).setStyle("-fx-alignment: TOP_CENTER");

        txtId.setDisable(true);
        btnSave.setDefaultButton(true);
        btnClear.setDisable(true);
        btnDelete.setDisable(true);
        Platform.runLater(txtName::requestFocus);

        imgPictureView.imageProperty().addListener((ov, prev, current) -> btnClear.setDisable(current == null));

        tblCustomers.getSelectionModel().selectedItemProperty().addListener((ov, prev, current) -> {
            btnDelete.setDisable(current == null);
            if (current == null) {
                btnNew.fire();
                return;
            }

            txtId.setText(current.getDisplayId());
            txtName.setText(current.getName());
            txtAddress.setText(current.getAddress());
            txtContact.setText(current.getContact());
            imgPictureView.setImage(current.getPicture());
        });

        /* reset fields when starting to type*/
        JFXTextField[] jfxTextFields = {txtName, txtAddress, txtContact};
        for (JFXTextField txt : jfxTextFields) {
            txt.textProperty().addListener((observableValue, previous, current) -> {
                setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
            });
        }

        txtSearch.textProperty().addListener(c -> {
            tblCustomers.getSelectionModel().clearSelection();
            loadCustomers();
        });

        loadCustomers();
    }

    private void setFocusColorAndUnFocusColor(JFXTextField txt, Color focusColor, Color unFocusColor){
        txt.setFocusColor(focusColor);
        txt.setUnFocusColor(unFocusColor);
    }

    private void loadCustomers() {
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            String sql = "SELECT * FROM customer WHERE id LIKE ? OR name LIKE ? or address LIKE ? or contact LIKE ?";
            PreparedStatement stm = connection.prepareStatement(sql);
            String query = "%" + txtSearch.getText() + "%";
            for (var i = 1; i <= 4; i++) stm.setString(i, query);
            ResultSet rst = stm.executeQuery();

            ObservableList<Customer> customerList = tblCustomers.getItems();
            customerList.clear();

            while (rst.next()) {
                int id = rst.getInt("id");
                String name = rst.getString("name");
                String address = rst.getString("address");
                String contact = rst.getString("contact");
                byte[] picture = rst.getBytes("picture");
                Image profilePicture = new Image(new ByteArrayInputStream(picture));
                Customer customer = new Customer(id, name, address, contact, profilePicture);
                customerList.add(customer);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load customers, try again").showAndWait();
            throw new RuntimeException(e);
        }
    }

    public void btnNewOnAction(ActionEvent event) {
        txtId.clear();
        txtId.setPromptText("Generated ID");
        txtName.clear();
        txtAddress.clear();
        txtContact.clear();
        imgPictureView.setImage(noImageAvailableImage);
        tblCustomers.getSelectionModel().clearSelection();
        btnClear.setDisable(true);
        if (!txtSearch.isFocused()) txtName.requestFocus();

        JFXTextField[] jfxTextFields = {txtName, txtAddress, txtContact};
        for (JFXTextField txt : jfxTextFields) {
            setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
        }
    }

    public void btnBrowseOnAction(ActionEvent event) throws MalformedURLException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home"), "Pictures"));
        fileChooser.setTitle("Select a profile picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpeg", "*.jpg", "*.gif", "*.bmp", "*.png"));
        File file = fileChooser.showOpenDialog(btnBrowse.getScene().getWindow());
        if (file == null) return;
        Image image = new Image(file.toURI().toURL().toString());
        imgPictureView.setImage(image);
    }

    public void btnClearOnAction(ActionEvent event) {
        imgPictureView.setImage(noImageAvailableImage);
    }

    public void btnSaveOnAction(ActionEvent event) {
        if (!isDataValid()) return;

        Customer newCustomer = new Customer(null, txtName.getText().strip(), txtAddress.getText().strip(), txtContact.getText().strip(), imgPictureView.getImage());

        Connection connection = DBConnection.getInstance().getConnection();
        try {
            PreparedStatement stmContactExist = connection.prepareStatement("SELECT * FROM customer WHERE contact = ?");
            stmContactExist.setString(1, newCustomer.getContact());
            ResultSet rst = stmContactExist.executeQuery();

            if (rst.next()) {
                if (txtId.getText().isEmpty() || (!txtId.getText().isEmpty() && rst.getInt("id") != Integer.parseInt(txtId.getText().substring(2)))) {
                    new Alert(Alert.AlertType.ERROR, "Contact number " + newCustomer.getContact() + " already exist").showAndWait();
                    txtContact.requestFocus();
                    txtContact.selectAll();
                    return;
                }
            }

            String sql;
            PreparedStatement stm;

            Customer selectedCustomer = tblCustomers.getSelectionModel().getSelectedItem();
            if (selectedCustomer == null) {
                sql = "INSERT INTO customer (name, address, contact, picture) VALUES (?, ?, ?, ?)";
                stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stm.setString(1, newCustomer.getName());
                stm.setString(2, newCustomer.getAddress());
                stm.setString(3, newCustomer.getContact());
                stm.setBytes(4, newCustomer.getPictureBytes());

                stm.executeUpdate();
                ResultSet generatedKeys = stm.getGeneratedKeys();
                generatedKeys.next();
                int generatedId = generatedKeys.getInt(1);
                newCustomer.setId(generatedId);
                tblCustomers.getItems().add(newCustomer);

            } else {
                sql = "UPDATE customer SET name=?, address=?, contact=?, picture=? WHERE id=?";
                stm = connection.prepareStatement(sql);
                stm.setString(1, newCustomer.getName());
                stm.setString(2, newCustomer.getAddress());
                stm.setString(3, newCustomer.getContact());
                stm.setBytes(4, newCustomer.getPictureBytes());
                stm.setInt(5, Integer.parseInt(txtId.getText().substring(2)));

                stm.executeUpdate();
                selectedCustomer.setName(newCustomer.getName());
                selectedCustomer.setAddress(newCustomer.getAddress());
                selectedCustomer.setContact(newCustomer.getContact());
                selectedCustomer.setPicture(newCustomer.getPicture());
                tblCustomers.refresh();
            }


        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save the customer, try again!").showAndWait();
            throw new RuntimeException(e);
        }

        btnNew.fire();
    }

    private boolean isDataValid() {
        boolean dataValid = true;

        if (!txtContact.getText().matches("\\d{3}-\\d{7}")) {
            setFocusColorAndUnFocusColor(txtContact, Color.web("red"), Color.web("red"));
            txtContact.requestFocus();
            txtContact.selectAll();
            dataValid = false;
        }

        if (txtAddress.getText().strip().length() < 3) {
            setFocusColorAndUnFocusColor(txtAddress, Color.web("red"), Color.web("red"));
            txtAddress.requestFocus();
            txtAddress.selectAll();
            dataValid = false;
        }

        if (!txtName.getText().matches("[A-Z a-z]+") || txtName.getText().isBlank()) {
            setFocusColorAndUnFocusColor(txtName, Color.web("red"), Color.web("red"));
            txtName.requestFocus();
            txtName.selectAll();
            dataValid = false;
        }

        return dataValid;
    }

    public void btnDeleteOnAction(ActionEvent event) {
        Customer selectedCustomer = tblCustomers.getSelectionModel().getSelectedItem();
        ObservableList<Customer> customerList = tblCustomers.getItems();
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            PreparedStatement stm = connection.prepareStatement("DELETE  FROM  customer WHERE  id=?");
            stm.setInt(1, selectedCustomer.getId());
            stm.executeUpdate();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to delete the customer, try again").showAndWait();
            throw new RuntimeException(e);
        }

        customerList.remove(selectedCustomer);
        if (customerList.isEmpty()) btnNew.fire();
    }

    public void tblCustomersOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE) btnDelete.fire();
    }

    public void btnReportOnAction(ActionEvent event) throws JRException {
        JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResource("/report/customers-report.jasper"));

        HashMap<String, Object> reportParams = new HashMap<>();
        Connection dataSource = DBConnection.getInstance().getConnection();

        String searchText = txtSearch.getText();
        reportParams.put("q", "%" + searchText + "%");

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParams, dataSource);
        JasperViewer.viewReport(jasperPrint, false);
    }
}

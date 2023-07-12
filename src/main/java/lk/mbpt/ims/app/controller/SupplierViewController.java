package lk.mbpt.ims.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import lk.mbpt.ims.app.db.DBConnection;
import lk.mbpt.ims.app.model.Supplier;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.sql.*;

public class SupplierViewController {
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
    public TableView<Supplier> tblSuppliers;
    public JFXTextField txtSearch;
    public JFXButton btnReport;

    private final Image noImageAvailableImage = new Image("/image/No_Image_Available.jpg");

    public void initialize() {
        /* Table column mapping */
        tblSuppliers.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("displayId"));
        tblSuppliers.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("profilePicture"));
        tblSuppliers.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("name"));
        tblSuppliers.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("address"));
        tblSuppliers.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("contact"));

        tblSuppliers.getColumns().get(0).setStyle("-fx-alignment: TOP_CENTER");
        tblSuppliers.getColumns().get(1).setStyle("-fx-alignment: CENTER");
        tblSuppliers.getColumns().get(4).setStyle("-fx-alignment: TOP_CENTER");

        txtId.setDisable(true);
        btnSave.setDefaultButton(true);
        btnClear.setDisable(true);
        btnDelete.setDisable(true);
        Platform.runLater(txtName::requestFocus);
        imgPictureView.imageProperty().addListener((ov, prev, current) -> btnClear.setDisable(current == null));

        tblSuppliers.getSelectionModel().selectedItemProperty().addListener((ov, prev, current) -> {
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

        /* reset fields when starting to type */
        JFXTextField[] jfxTextFields = {txtName, txtAddress, txtContact};
        for (JFXTextField txt : jfxTextFields) {
            txt.textProperty().addListener((observableValue, previous, current) -> {
                setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
            });
        }

        txtSearch.textProperty().addListener(c -> {
            tblSuppliers.getSelectionModel().clearSelection();
            loadSuppliers();
        });

        loadSuppliers();
    }

    private void setFocusColorAndUnFocusColor(JFXTextField txt, Color focusColor, Color unFocusColor){
        txt.setFocusColor(focusColor);
        txt.setUnFocusColor(unFocusColor);
    }

    private void loadSuppliers() {
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            String sql = "SELECT * FROM supplier WHERE id LIKE ? OR name LIKE ? or address LIKE ? or contact LIKE ?";
            PreparedStatement stm = connection.prepareStatement(sql);
            String query = "%" + txtSearch.getText() + "%";
            for (var i = 1; i <= 4; i++) stm.setString(i, query);
            ResultSet rst = stm.executeQuery();

            ObservableList<Supplier> suppliersList = tblSuppliers.getItems();
            suppliersList.clear();

            while (rst.next()) {
                int id = rst.getInt("id");
                String name = rst.getString("name");
                String address = rst.getString("address");
                String contact = rst.getString("contact");
                byte[] picture = rst.getBytes("picture");
                Image profilePicture = new Image(new ByteArrayInputStream(picture));
                Supplier supplier = new Supplier(id, name, address, contact, profilePicture);
                suppliersList.add(supplier);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load suppliers, try again").showAndWait();
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
        tblSuppliers.getSelectionModel().clearSelection();
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

        Supplier newSupplier = new Supplier(null,
                txtName.getText().strip(),
                txtAddress.getText().strip(),
                txtContact.getText().strip(),
                imgPictureView.getImage());

        Connection connection = DBConnection.getInstance().getConnection();
        try {
            PreparedStatement stmContactExist = connection.prepareStatement("SELECT * FROM supplier WHERE contact = ?");
            stmContactExist.setString(1, newSupplier.getContact());
            ResultSet rst = stmContactExist.executeQuery();

            if (rst.next()) {
                if (txtId.getText().isEmpty() || (!txtId.getText().isEmpty() && rst.getInt("id") != Integer.parseInt(txtId.getText().substring(2)))) {
                    new Alert(Alert.AlertType.ERROR, "Contact number " + newSupplier.getContact() + " already exist").showAndWait();
                    txtContact.requestFocus();
                    txtContact.selectAll();
                    return;
                }
            }

            String sql;
            PreparedStatement stm;

            Supplier selectedSupplier = tblSuppliers.getSelectionModel().getSelectedItem();
            if (selectedSupplier == null) {
                sql = "INSERT INTO supplier (name, address, contact, picture) VALUES (?, ?, ?, ?)";
                stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stm.setString(1, newSupplier.getName());
                stm.setString(2, newSupplier.getAddress());
                stm.setString(3, newSupplier.getContact());
                stm.setBytes(4, newSupplier.getPictureBytes());

                stm.executeUpdate();
                ResultSet generatedKeys = stm.getGeneratedKeys();
                generatedKeys.next();
                int generatedId = generatedKeys.getInt(1);
                newSupplier.setId(generatedId);
                tblSuppliers.getItems().add(newSupplier);

            } else {
                sql = "UPDATE supplier SET name=?, address=?, contact=?, picture=? WHERE id=?";
                stm = connection.prepareStatement(sql);
                stm.setString(1, newSupplier.getName());
                stm.setString(2, newSupplier.getAddress());
                stm.setString(3, newSupplier.getContact());
                stm.setBytes(4, newSupplier.getPictureBytes());
                stm.setInt(5, Integer.parseInt(txtId.getText().substring(2)));

                stm.executeUpdate();
                selectedSupplier.setName(newSupplier.getName());
                selectedSupplier.setAddress(newSupplier.getAddress());
                selectedSupplier.setContact(newSupplier.getContact());
                selectedSupplier.setPicture(newSupplier.getPicture());
                tblSuppliers.refresh();
            }


        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save the supplier, try again!").showAndWait();
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
        Supplier selectedSupplier = tblSuppliers.getSelectionModel().getSelectedItem();
        ObservableList<Supplier> suppliersList = tblSuppliers.getItems();
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            PreparedStatement stm = connection.prepareStatement("DELETE  FROM  supplier WHERE  id=?");
            stm.setInt(1, selectedSupplier.getId());
            stm.executeUpdate();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to delete the supplier, try again").showAndWait();
            throw new RuntimeException(e);
        }


        suppliersList.remove(selectedSupplier);
        if (suppliersList.isEmpty()) btnNew.fire();
    }

    public void tblSuppliersOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE) btnDelete.fire();
    }

    public void btnReportOnAction(ActionEvent event) {
    }
}

package lk.mbpt.ims.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import lk.mbpt.ims.app.db.DBConnection;
import lk.mbpt.ims.app.model.Product;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JasperViewer;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeException;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import net.sourceforge.barbecue.output.OutputException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.HashMap;


public class ProductViewController {
    public JFXButton btnNew;
    public JFXTextField txtCode;
    public JFXTextField txtDescription;
    public JFXTextField txtQty;
    public JFXTextField txtSellingPrice;
    public ImageView imgProductPreview;
    public JFXButton btnBrowse;
    public JFXButton btnClear;
    public JFXButton btnSave;
    public JFXButton btnDelete;
    public TableView<Product> tblProducts;
    public JFXTextField txtSearch;
    public JFXButton btnReport;
    public ImageView imgBarcodeView;
    public Label lblBarcodeDisplay;

    private final Image noImageAvailableImage = new Image("/image/No_Image_Available.jpg");

    public void initialize() {
        /* Table column mapping */
        tblProducts.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("displayProductCode"));
        tblProducts.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("productPreview"));
        tblProducts.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("description"));
        tblProducts.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("displayQty"));
        tblProducts.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("displaySellingPrice"));

        tblProducts.getColumns().get(3).setStyle("-fx-alignment: TOP_RIGHT");
        tblProducts.getColumns().get(4).setStyle("-fx-alignment: TOP_RIGHT");

        txtCode.setDisable(true);
        btnSave.setDefaultButton(true);
        btnClear.setDisable(true);
        btnDelete.setDisable(true);
        Platform.runLater(txtDescription::requestFocus);

        imgProductPreview.imageProperty().addListener((ov, prev, current) -> btnClear.setDisable(current == null));

        tblProducts.getSelectionModel().selectedItemProperty().addListener((ov, prev, current) -> {
            btnDelete.setDisable(current == null);
            if (current == null) {
                btnNew.fire();
                return;
            }

            txtCode.setText(current.getDisplayProductCode());
            txtDescription.setText(current.getDescription());
            txtQty.setText(current.getQty().toString());
            txtSellingPrice.setText(current.getSellingPrice().toString());
            imgProductPreview.setImage(current.getPreview());

            generateBarcode(current.getDisplayProductCode().replace("P-", "0000"));
        });

        imgBarcodeView.imageProperty().addListener((ov, prev, current) -> lblBarcodeDisplay.setVisible(current == null));

        /* reset fields when starting to type*/
        JFXTextField[] jfxTextFields = {txtDescription, txtQty, txtSellingPrice};
        for (JFXTextField txt : jfxTextFields) {
            txt.textProperty().addListener((observableValue, previous, current) -> {
                setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
            });
        }

        txtSearch.textProperty().addListener(c -> {
            tblProducts.getSelectionModel().clearSelection();
            loadProducts();
        });

        loadProducts();
    }

    private void setFocusColorAndUnFocusColor(JFXTextField txt, Color focusColor, Color unFocusColor){
        txt.setFocusColor(focusColor);
        txt.setUnFocusColor(unFocusColor);
    }

    private void loadProducts() {
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            String sql = "SELECT * FROM product WHERE code LIKE ? OR description LIKE ? OR qty LIKE ? OR selling_price LIKE ?";
            PreparedStatement stm = connection.prepareStatement(sql);
            String query = "%" + txtSearch.getText() + "%";
            for (var i = 1; i <= 4; i++) stm.setString(i, query);
            ResultSet rst = stm.executeQuery();

            ObservableList<Product> productsList = tblProducts.getItems();
            productsList.clear();

            while (rst.next()) {
                int productCode = rst.getInt("code");
                String description = rst.getString("description");
                BigDecimal qty = rst.getBigDecimal("qty");
                BigDecimal sellingPrice = rst.getBigDecimal("selling_price");
                byte[] picture = rst.getBytes("picture");
                Image productPreview = new Image(new ByteArrayInputStream(picture));
                Product product = new Product(productCode, description, qty, sellingPrice, productPreview);
                productsList.add(product);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load products, try again").showAndWait();
            throw new RuntimeException(e);
        }
    }

    private BufferedImage generateBarcode(String id) {
        try {
            Barcode barcode = BarcodeFactory.createEAN13(id); // use the EAN-13 barcode standard to generate barcode
            barcode.setBarWidth(1);
            barcode.setFont(new Font("Ubuntu", Font.PLAIN, 10));
            BufferedImage imgBarcode = BarcodeImageHandler.getImage(barcode);
            WritableImage fxImage = SwingFXUtils.toFXImage(imgBarcode, null); // Convert the barcode image to a JavaFX-compatible image format
            imgBarcodeView.setFitHeight(70);
            imgBarcodeView.setImage(fxImage);
            return imgBarcode;
        } catch (BarcodeException | OutputException e) {
            throw new RuntimeException(e);
        }
    }

    public void btnNewOnAction(ActionEvent event) {
        txtCode.clear();
        txtCode.setPromptText("Generated ID");
        txtDescription.clear();
        txtQty.clear();
        txtSellingPrice.clear();
        imgProductPreview.setImage(noImageAvailableImage);

        tblProducts.getSelectionModel().clearSelection();
        btnClear.setDisable(true);
        if (!txtSearch.isFocused()) txtDescription.requestFocus();

        imgBarcodeView.setImage(null);

        JFXTextField[] jfxTextFields = {txtDescription, txtQty, txtSellingPrice};
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
        imgProductPreview.setImage(image);
    }

    public void btnClearOnAction(ActionEvent event) {
        imgProductPreview.setImage(noImageAvailableImage);
    }

    public void btnSaveOnAction(ActionEvent event) {
        if (!isDataValid()) return;

        String description = txtDescription.getText();
        BigDecimal qty = new BigDecimal(txtQty.getText().strip());
        BigDecimal sellingPrice = new BigDecimal(txtSellingPrice.getText().strip());
        Image preview = imgProductPreview.getImage();
        Product newProduct = new Product(null, description, qty, sellingPrice, preview);

        Connection connection = DBConnection.getInstance().getConnection();
        try {
            String sql;
            PreparedStatement stm;

            Product selectedProduct = tblProducts.getSelectionModel().getSelectedItem();
            if (selectedProduct == null) {
                sql = "INSERT INTO product (description, qty, selling_price, picture) VALUES (?, ?, ?, ?)";
                stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stm.setString(1, newProduct.getDescription());
                stm.setBigDecimal(2, newProduct.getQty());
                stm.setBigDecimal(3, newProduct.getSellingPrice());
                stm.setBytes(4, newProduct.getPictureBytes());

                stm.executeUpdate();
                ResultSet generatedKeys = stm.getGeneratedKeys();
                generatedKeys.next();
                int generatedId = generatedKeys.getInt(1);
                newProduct.setCode(generatedId);
                tblProducts.getItems().add(newProduct);

            } else {
                sql = "UPDATE product SET description=?, qty=?, selling_price=?, picture=? WHERE code=?";
                stm = connection.prepareStatement(sql);
                stm.setString(1, newProduct.getDescription());
                stm.setBigDecimal(2, newProduct.getQty());
                stm.setBigDecimal(3, newProduct.getSellingPrice());
                stm.setBytes(4, newProduct.getPictureBytes());
                stm.setInt(5, Integer.parseInt(txtCode.getText().substring(2)));

                stm.executeUpdate();
                selectedProduct.setDescription(newProduct.getDescription());
                selectedProduct.setQty(newProduct.getQty());
                selectedProduct.setSellingPrice(newProduct.getSellingPrice());
                selectedProduct.setPreview(newProduct.getPreview());
                tblProducts.refresh();
            }


        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save the product, try again!").showAndWait();
            throw new RuntimeException(e);
        }

        btnNew.fire();
    }

    private boolean isDataValid() {
        boolean dataValid = true;

        if (!txtSellingPrice.getText().matches("[1-9]\\d*(\\.\\d{2})?")) {
            setFocusColorAndUnFocusColor(txtSellingPrice, Color.web("red"), Color.web("red"));
            txtSellingPrice.requestFocus();
            txtSellingPrice.selectAll();
            dataValid = false;
        }

        if (!txtQty.getText().matches("\\d+")) {
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
        Product selectedProduct = tblProducts.getSelectionModel().getSelectedItem();
        ObservableList<Product> productList = tblProducts.getItems();
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            PreparedStatement stm = connection.prepareStatement("DELETE  FROM  product WHERE  code=?");
            stm.setInt(1, selectedProduct.getCode());
            stm.executeUpdate();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to delete the product, try again").showAndWait();
            throw new RuntimeException(e);
        }

        productList.remove(selectedProduct);
        if (productList.isEmpty()) btnNew.fire();
    }

    public void tblProductsOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE) btnDelete.fire();
    }

    public void btnReportOnAction(ActionEvent event) throws JRException {
        JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResource("/report/products-report.jasper"));

        HashMap<String, Object> reportParams = new HashMap<>();
        Connection dataSource = DBConnection.getInstance().getConnection();

        String searchText = txtSearch.getText();
        reportParams.put("q", "%" + searchText + "%");

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParams, dataSource);
        JasperViewer.viewReport(jasperPrint, false);
    }
}

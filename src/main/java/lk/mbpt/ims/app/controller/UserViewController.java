package lk.mbpt.ims.app.controller;

import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import lk.mbpt.ims.app.db.DBConnection;
import lk.mbpt.ims.app.model.User;
import lk.mbpt.ims.app.util.PasswordEncoder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;


public class UserViewController {


    public JFXButton btnNew;
    public JFXTextField txtFullName;
    public JFXTextField txtUsername;
    public JFXPasswordField txtPassword;
    public JFXPasswordField txtConfirmPassword;
    public JFXRadioButton rdoAdmin;
    public ToggleGroup role;
    public JFXRadioButton rdoUser;
    public JFXButton btnSave;
    public JFXButton btnDelete;
    public TableView<User> tblUsers;
    public JFXTextField txtSearch;
    public JFXButton btnReport;

    public void initialize() {
        /* Table column mapping */
        tblUsers.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("fullName"));
        tblUsers.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("username"));
        tblUsers.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("role"));

        btnSave.setDefaultButton(true);
        btnDelete.setDisable(true);
        Platform.runLater(txtFullName::requestFocus);

        tblUsers.getSelectionModel().selectedItemProperty().addListener((ov, prev, current) -> {
            btnDelete.setDisable(current == null);
            if (current == null) {
                btnNew.fire();
                return;
            }

            txtFullName.setText(current.getFullName());
            txtUsername.setText(current.getUsername());
            for (JFXPasswordField txt : new JFXPasswordField[]{txtPassword, txtConfirmPassword}) {
                    setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));

            }

            if (current.getRole().equals(User.Role.ADMIN)) rdoAdmin.setSelected(true);
            else rdoUser.setSelected(true);
        });

        /* reset fields when starting to type */
        for (JFXTextField txt : new JFXTextField[]{txtFullName, txtUsername}) {
            txt.textProperty().addListener((observableValue, previous, current) -> {
                setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
            });
        }
        for (JFXPasswordField txt : new JFXPasswordField[]{txtPassword, txtConfirmPassword}) {
            txt.textProperty().addListener((observableValue, previous, current) -> {
                setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
            });
        }

        JFXRadioButton[] radioButton = {rdoAdmin, rdoUser};
        for (JFXRadioButton rdo : radioButton) {
            rdo.selectedProperty().addListener((observableValue, previous, current) -> {
                rdoAdmin.setUnSelectedColor(Color.rgb(90, 90, 90));
                rdoUser.setUnSelectedColor(Color.rgb(90, 90, 90));
                rdoAdmin.setTextFill(Color.rgb(0, 0, 0));
                rdoUser.setTextFill(Color.rgb(0, 0, 0));
            });
        }


        txtSearch.textProperty().addListener(c -> {
            tblUsers.getSelectionModel().clearSelection();
            loadUsers();
        });

        loadUsers();
    }

    private void setFocusColorAndUnFocusColor(Node node, Color focusColor, Color unFocusColor){
        if(node instanceof JFXTextField){
            JFXTextField txt = (JFXTextField)node;
            txt.setFocusColor(focusColor);
            txt.setUnFocusColor(unFocusColor);
        } else if (node instanceof JFXPasswordField) {
            JFXPasswordField txt = (JFXPasswordField) node;
            txt.setFocusColor(focusColor);
            txt.setUnFocusColor(unFocusColor);
        }
    }

    private void loadUsers() {
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            String sql = "SELECT * FROM user WHERE full_name LIKE ? OR username LIKE ? or role LIKE ? ";
            PreparedStatement stm = connection.prepareStatement(sql);
            String query = "%" + txtSearch.getText() + "%";
            for (var i = 1; i <= 3; i++) stm.setString(i, query);
            ResultSet rst = stm.executeQuery();

            ObservableList<User> usersList = tblUsers.getItems();
            usersList.clear();

            while (rst.next()) {
                String fullName = rst.getString("full_name");
                String username = rst.getString("username");
                String password = rst.getString("password");
                User.Role role = User.Role.valueOf(rst.getString("role"));
                User user = new User(fullName, username, password, role);
                usersList.add(user);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load users, try again").showAndWait();
            throw new RuntimeException(e);
        }
    }

    public void btnNewOnAction(ActionEvent event) {
        txtFullName.clear();
        txtUsername.clear();
        txtPassword.clear();
        txtConfirmPassword.clear();
        role.selectToggle(null);
        tblUsers.getSelectionModel().clearSelection();
        if (!txtSearch.isFocused()) txtFullName.requestFocus();

        for (JFXTextField txt : new JFXTextField[]{txtFullName, txtUsername}) {
            setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
        }
        for (JFXPasswordField txt : new JFXPasswordField[]{txtPassword, txtConfirmPassword}) {
            setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
        }

        JFXRadioButton[] radioButton = {rdoAdmin, rdoUser};
        for (JFXRadioButton rdo : radioButton) {
            rdoAdmin.setUnSelectedColor(Color.rgb(90, 90, 90));
            rdoUser.setUnSelectedColor(Color.rgb(90, 90, 90));
            rdoAdmin.setTextFill(Color.rgb(0, 0, 0));
            rdoUser.setTextFill(Color.rgb(0, 0, 0));
        }
    }

    public void btnSaveOnAction(ActionEvent event) {
        if (!isDataValid()) return;

        User newUser = new User(txtFullName.getText().strip(), txtUsername.getText().strip(), PasswordEncoder.encode(txtPassword.getText().strip()), rdoAdmin.isSelected() ? User.Role.ADMIN : User.Role.USER);

        Connection connection = DBConnection.getInstance().getConnection();
        String sql;
        PreparedStatement stm;

        try {
            User selectedUser = tblUsers.getSelectionModel().getSelectedItem();
            if (selectedUser == null) {
                sql = "INSERT INTO user (username, full_name, password, role) VALUES (?, ?, ?, ?)";
                stm = connection.prepareStatement(sql);
                stm.setString(1, newUser.getFullName());
                stm.setString(2, newUser.getUsername());
                stm.setString(3, newUser.getPassword());
                stm.setString(4, String.valueOf(newUser.getRole()));

                stm.executeUpdate();
                tblUsers.getItems().add(newUser);

            } else {
                sql = "UPDATE user SET username=?, password=?, role=? WHERE full_name=?";
                stm = connection.prepareStatement(sql);
                stm.setString(1, newUser.getUsername());
                stm.setString(2, newUser.getPassword());
                stm.setString(3, String.valueOf(newUser.getRole()));
                stm.setString(4, newUser.getFullName());

                stm.executeUpdate();
                selectedUser.setUsername(newUser.getUsername());
                selectedUser.setPassword(newUser.getPassword());
                selectedUser.setRole(newUser.getRole());
                tblUsers.refresh();
            }


        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save the user, try again!").showAndWait();
            throw new RuntimeException(e);
        }

        btnNew.fire();
    }

    private boolean isDataValid() {
        boolean dataValid = true;
        for (JFXTextField txt : new JFXTextField[]{txtFullName, txtUsername}) {
            setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));

        }
        for (JFXPasswordField txt : new JFXPasswordField[]{txtPassword, txtConfirmPassword}) {
            setFocusColorAndUnFocusColor(txt, Color.rgb(64, 89, 169), Color.rgb(77, 77, 77));
        }

        String fullName = txtFullName.getText();
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        Pattern regEx4UpperCaseLetters = Pattern.compile("[A-Z]");
        Pattern regEx4LowerCaseLetters = Pattern.compile("[a-z]");
        Pattern regEx4Digits = Pattern.compile("[0-9]");
        Pattern regEx4Symbols = Pattern.compile("[~!@#$%^&*()_+]");

        if (!rdoAdmin.isSelected() && !rdoUser.isSelected()) {
            rdoAdmin.setUnSelectedColor(Color.rgb(255, 0, 0));
            rdoUser.setUnSelectedColor(Color.rgb(255, 0, 0));
            rdoAdmin.setTextFill(Color.rgb(255, 0, 0));
            rdoUser.setTextFill(Color.rgb(255, 0, 0));
            rdoAdmin.requestFocus();
            dataValid = false;
        }

        if (password.isEmpty() || !password.equals(confirmPassword)) {
            txtConfirmPassword.requestFocus();
            txtConfirmPassword.selectAll();
            setFocusColorAndUnFocusColor(txtConfirmPassword, Color.web("red"), Color.web("red"));
            dataValid = false;
        }

        if (!(regEx4UpperCaseLetters.matcher(password).find() &&
                regEx4LowerCaseLetters.matcher(password).find() &&
                regEx4Digits.matcher(password).find() &&
                regEx4Symbols.matcher(password).find() &&
                password.length() >= 5)) {
            txtPassword.requestFocus();
            txtPassword.selectAll();
            setFocusColorAndUnFocusColor(txtPassword, Color.web("red"), Color.web("red"));
            dataValid = false;
        }

        if (!username.matches("[A-Za-z0-9]{3,}")) {
            txtUsername.requestFocus();
            txtUsername.selectAll();
            setFocusColorAndUnFocusColor(txtUsername, Color.web("red"), Color.web("red"));
            dataValid = false;
        }

        if (!fullName.matches("[A-Za-z ]+")) {
            txtFullName.requestFocus();
            txtFullName.selectAll();
            setFocusColorAndUnFocusColor(txtFullName, Color.web("red"), Color.web("red"));
            dataValid = false;
        }
        return dataValid;
    }

    public void btnDeleteOnAction(ActionEvent event) {
        User selectedUser = tblUsers.getSelectionModel().getSelectedItem();
        ObservableList<User> usersList = tblUsers.getItems();
        Connection connection = DBConnection.getInstance().getConnection();
        try {
            PreparedStatement stm = connection.prepareStatement("DELETE  FROM  user WHERE  full_name=?");
            stm.setString(1, selectedUser.getFullName());
            stm.executeUpdate();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to delete the user, try again").showAndWait();
            throw new RuntimeException(e);
        }
        usersList.remove(selectedUser);
        if (usersList.isEmpty()) btnNew.fire();
    }

    public void tblUsersOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE) btnDelete.fire();
    }

    public void btnReportOnAction(ActionEvent event) {
    }
}

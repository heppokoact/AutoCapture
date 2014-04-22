/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.heppokoact.autocapture;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author yoshidacojp
 */
public class FXMLDocumentController implements Initializable {
    
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private Label areaStartXLabel;
    @FXML
    private Label areaStartYLabel;
    @FXML
    private Label areaEndXLabel;
    @FXML
    private Label areaEndYLabel;
    @FXML
    private Label pointXLabel;
    @FXML
    private Label pointYLabel;
    @FXML
    private Label saveDirectoryLabel;
    
    private double areaStartX;
    private double areaStartY;
    private double areaEndX;
    private double areaEndY;
    private double pointX;
    private double pointY;
    private File saveDirectory;
    
    private double dragStartX;
    private double dragStartY;
    private Rectangle areaRect;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        areaStartXLabel.setText("0.0");
        areaStartYLabel.setText("0.0");
        areaEndXLabel.setText("0.0");
        areaEndYLabel.setText("0.0");
        pointXLabel.setText("0.0");
        pointYLabel.setText("0.0");
        saveDirectoryLabel.setText("");
    }    
    
    @FXML
    private void areaButtonClicked(ActionEvent event) {
        System.out.println("areaButtonClicked");
        Stage stage = createTransparentStage();
        Scene scene = stage.getScene();
        scene.setOnMousePressed(e -> {
            dragStartX = e.getScreenX();
            dragStartY = e.getScreenY();
            areaRect = new Rectangle(e.getX(), e.getY(), 0, 0);
            areaRect.setStroke(Color.RED);
            areaRect.setStrokeWidth(1.0);
            areaRect.setFill(Color.TRANSPARENT);
            ((Pane)scene.getRoot()).getChildren().add(areaRect);
        });
        scene.setOnMouseDragged(e -> {
            areaRect.setWidth(e.getScreenX() - dragStartX);
            areaRect.setHeight(e.getScreenY() - dragStartY);
            
        });
        scene.setOnMouseReleased(e -> {
            areaStartX = dragStartX;
            areaStartY = dragStartY;
            areaEndX = e.getScreenX();
            areaEndY = e.getScreenY();
            areaStartXLabel.setText(Double.toString(areaStartX));
            areaStartYLabel.setText(Double.toString(areaStartY));
            areaEndXLabel.setText(Double.toString(areaEndX));
            areaEndYLabel.setText(Double.toString(areaEndY));
            stage.close();
        });
        stage.setOnCloseRequest(e -> {
            dragStartX = 0.0;
            dragStartY = 0.0;
            areaRect = null;
        });
            
        stage.show();
    }
    
    @FXML
    private void pointButtonClicked(ActionEvent event) {
        System.out.println("pointButtonClicked");
        Stage stage = createTransparentStage();
        Scene scene = stage.getScene();
        scene.setOnMouseClicked(e -> {
            pointX = e.getScreenX();
            pointY = e.getScreenY();
            pointXLabel.setText(Double.toString(pointX));
            pointYLabel.setText(Double.toString(pointY));
            stage.close();
        });
        stage.show();
    }
    
    @FXML
    private void saveDirectoryButtonClicked(ActionEvent event) {
        System.out.println("saveDirectoryButtonClicked");
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("保存ディレクトリを選択");
        dc.setInitialDirectory(saveDirectory);
        saveDirectory = dc.showDialog(anchorPane.getScene().getWindow());
        if (saveDirectory != null) {
            saveDirectoryLabel.setText(saveDirectory.getName());
        }
    }
    
    @FXML
    private void startButtonClicked(ActionEvent event) {
        System.out.println("startButtonClicked");
    }
    
    @FXML
    private void stopButtonClicked(ActionEvent event) {
        System.out.println("stopButtonClicked");
    }
    
    private Stage createTransparentStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(anchorPane.getScene().getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        Rectangle2D rect = Screen.getPrimary().getVisualBounds();
        stage.setWidth(rect.getWidth());
        stage.setHeight(rect.getHeight());
        
        Pane pane = new Pane();
        pane.setBackground(Background.EMPTY);
        
        Scene scene = new Scene(pane);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        
        return stage;
    }
    
    
}

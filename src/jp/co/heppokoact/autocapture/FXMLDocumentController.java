/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.heppokoact.autocapture;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.imageio.ImageIO;

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
	@FXML
	private Button areaButton;
	@FXML
	private Button pointButton;
	@FXML
	private Button saveDirectoryButton;
	@FXML
	private Button startButton;
	@FXML
	private Button stopButton;

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

	private CaptureService captureService = new CaptureService();
	private Timeline captureTimeline;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		areaStartXLabel.setText("0.0");
		areaStartYLabel.setText("0.0");
		areaEndXLabel.setText("0.0");
		areaEndYLabel.setText("0.0");
		pointXLabel.setText("0.0");
		pointYLabel.setText("0.0");
		saveDirectoryLabel.setText("");
		stopButton.setDisable(true);
	}

	@FXML
	private void areaButtonClicked(ActionEvent event) {
		System.out.println("areaButtonClicked");
		Stage transparentStage = createTransparentStage();
		Scene scene = transparentStage.getScene();
		scene.setOnMousePressed(e -> {
			dragStartX = e.getScreenX();
			dragStartY = e.getScreenY();
			areaRect = new Rectangle(e.getX(), e.getY(), 0, 0);
			areaRect.setStroke(Color.RED);
			areaRect.setStrokeWidth(1.0);
			areaRect.setFill(Color.TRANSPARENT);
			((Pane) scene.getRoot()).getChildren().add(areaRect);
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
			transparentStage.close();
		});
		transparentStage.setOnCloseRequest(e -> {
			dragStartX = 0.0;
			dragStartY = 0.0;
			areaRect = null;
		});

		transparentStage.show();
	}

	@FXML
	private void pointButtonClicked(ActionEvent event) {
		System.out.println("pointButtonClicked");
		Stage transparentStage = createTransparentStage();
		Scene scene = transparentStage.getScene();
		scene.setOnMouseClicked(e -> {
			pointX = e.getScreenX();
			pointY = e.getScreenY();
			pointXLabel.setText(Double.toString(pointX));
			pointYLabel.setText(Double.toString(pointY));
			transparentStage.close();
		});
		transparentStage.show();
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

		areaButton.setDisable(true);
		pointButton.setDisable(true);
		saveDirectoryButton.setDisable(true);
		startButton.setDisable(true);
		stopButton.setDisable(false);

		captureTimeline = new Timeline(new KeyFrame(new Duration(1000), e -> {
			captureService.restart();
		}));
		captureTimeline.setCycleCount(Timeline.INDEFINITE);
		captureTimeline.play();
	}

	@FXML
	private void stopButtonClicked(ActionEvent event) {
		System.out.println("stopButtonClicked");

		captureTimeline.stop();

		areaButton.setDisable(false);
		pointButton.setDisable(false);
		saveDirectoryButton.setDisable(false);
		startButton.setDisable(false);
		stopButton.setDisable(true);
	}

	private Stage createTransparentStage() {
		try {
			Stage transparentStage = new Stage(StageStyle.TRANSPARENT);
			transparentStage.initOwner(anchorPane.getScene().getWindow());
			transparentStage.initModality(Modality.APPLICATION_MODAL);
			transparentStage.setResizable(false);
			Rectangle2D rect = Screen.getPrimary().getVisualBounds();
			transparentStage.setWidth(rect.getWidth());
			transparentStage.setHeight(rect.getHeight());

			Robot robot = new Robot();
			java.awt.Rectangle awtRect = new java.awt.Rectangle(
					(int) rect.getWidth(), (int) rect.getHeight());
			BufferedImage captureImage = robot.createScreenCapture(awtRect);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(captureImage, "bmp", out);
			ByteArrayInputStream in = new ByteArrayInputStream(
					out.toByteArray());

			Image image = new Image(in);
			BackgroundImage bgImage = new BackgroundImage(image,
					BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
					BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
			Background bg = new Background(bgImage);

			Pane pane = new Pane();
			pane.setBackground(bg);
			Scene scene = new Scene(pane);
			scene.setOnKeyPressed(e -> {
				if (e.getCode() == KeyCode.ESCAPE) {
					transparentStage.close();
				}
			});
			// scene.setFill(Color.TRANSPARENT);
			transparentStage.setScene(scene);

			return transparentStage;

		} catch (AWTException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	class CaptureService extends Service<Void> {

		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					System.out.println(areaStartX);
					return null;
				}
			};
		}

	}

}

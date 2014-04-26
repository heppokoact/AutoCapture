/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.heppokoact.autocapture;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
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
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.imageio.ImageIO;

import jp.co.heppokoact.util.ImageUtil;

import org.apache.commons.io.FileUtils;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * メインウィンドウのコントローラ
 *
 * @author M.Yoshida
 */
public class FXMLDocumentController implements Initializable {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(FXMLDocumentController.class);

	private static final double CAPTURE_INTERVAL = 2000.0;

	private Stage stage;

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

	/** キャプチャ取得サービス */
	private CaptureService captureService;
	/** キャプチャ取得サービスを定期実行するタイムライン */
	private Timeline captureTimeline;
	/** キャプチャ等を実施するロボット */
	private Robot robot;

	/** 完了時効果音 */
	private AudioClip clip;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		// 各種ラベルに初期値をセット
		areaStartXLabel.setText("0.0");
		areaStartYLabel.setText("0.0");
		areaEndXLabel.setText("0.0");
		areaEndYLabel.setText("0.0");
		pointXLabel.setText("0.0");
		pointYLabel.setText("0.0");
		saveDirectoryLabel.setText("");

		// 停止ボタンは非活性
		stopButton.setDisable(true);

		// キャプチャ取得サービスの構成
		captureService = new CaptureService();
		captureTimeline = new Timeline(new KeyFrame(new Duration(CAPTURE_INTERVAL), e -> {
			captureService.restart();
		}));
		captureTimeline.setCycleCount(Timeline.INDEFINITE);
		try {
			robot = new Robot();
		} catch (AWTException e) {
			throw new RuntimeException(e);
		}

		// 完了時効果音を読み込み
		clip = new AudioClip(ClassLoader.getSystemResource("ayashi.wav").toString());
	}

	@FXML
	private void areaButtonClicked(ActionEvent event) throws IOException {
		System.out.println("areaButtonClicked");

		// キャプチャ領域指定用ウィンドウを作成
		Stage transparentStage = createTransparentStage();

		// キャプチャ領域指定用ウィンドウでクリック（ドラッグ開始）した時の動作
		Scene scene = transparentStage.getScene();
		scene.setOnMousePressed(e -> {
			// ドラッグ開始点を記録
			dragStartX = e.getScreenX();
			dragStartY = e.getScreenY();
			// キャプチャ領域指定用の矩形を表示
			areaRect = new Rectangle(e.getX(), e.getY(), 0, 0);
			areaRect.setStroke(Color.RED);
			areaRect.setStrokeWidth(1.0);
			areaRect.setFill(Color.TRANSPARENT);
			((Pane) scene.getRoot()).getChildren().add(areaRect);
		});

		// キャプチャ領域指定用ウィンドウでドラッグした時の動作
		scene.setOnMouseDragged(e -> {
			// キャプチャ領域指定用の矩形をマウスに追随して変形
			areaRect.setWidth(e.getScreenX() - dragStartX);
			areaRect.setHeight(e.getScreenY() - dragStartY);

		});

		// キャプチャ領域指定用ウィンドウでマウスボタンを離した時（ドラッグ終了）の動作
		scene.setOnMouseReleased(e -> {
			// キャプチャ領域を記録
			areaStartX = dragStartX;
			areaStartY = dragStartY;
			areaEndX = e.getScreenX();
			areaEndY = e.getScreenY();
			// キャプチャ領域の座標をラベルに表示
			areaStartXLabel.setText(Double.toString(areaStartX));
			areaStartYLabel.setText(Double.toString(areaStartY));
			areaEndXLabel.setText(Double.toString(areaEndX));
			areaEndYLabel.setText(Double.toString(areaEndY));
			// キャプチャ領域指定用ウィンドウを閉じる
			transparentStage.close();
		});

		// キャプチャ領域指定用ウィンドウを閉じるときの動作
		transparentStage.setOnCloseRequest(e -> {
			// キャプチャ領域指定に使った変数を初期化
				dragStartX = 0.0;
				dragStartY = 0.0;
				areaRect = null;
			});

		transparentStage.show();
	}

	@FXML
	private void pointButtonClicked(ActionEvent event) throws IOException {
		System.out.println("pointButtonClicked");

		// クリックポイント指定用ウィンドウを作成
		Stage transparentStage = createTransparentStage();

		// クリックポイント指定用ウィンドウをクリックした時の動作
		Scene scene = transparentStage.getScene();
		scene.setOnMouseClicked(e -> {
			// クリックポイントを記録し、ラベルに表示
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

		// 保存ディレクトリを選択するダイアログを表示
		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("保存ディレクトリを選択");
		dc.setInitialDirectory(saveDirectory);
		saveDirectory = dc.showDialog(anchorPane.getScene().getWindow());

		// 保存ディレクトリが選択された場合、ラベルに表示
		if (saveDirectory != null) {
			saveDirectoryLabel.setText(saveDirectory.getName());
		}
	}

	@FXML
	private void startButtonClicked(ActionEvent event) {
		System.out.println("startButtonClicked");

		if (saveDirectory == null) {
			Dialogs.create()//
					.owner(stage)//
					.title("ERROR")//
					.masthead(null)//
					.message("保存ディレクトリが指定されていません。")//
					.showError();
			return;
		}

		// 停止ボタンを活性化、それ以外のボタンを非活性化
		areaButton.setDisable(true);
		pointButton.setDisable(true);
		saveDirectoryButton.setDisable(true);
		startButton.setDisable(true);
		stopButton.setDisable(false);

		// キャプチャを定期実行
		captureService.setWaitCount(0);
		captureTimeline.play();
	}

	@FXML
	private void stopButtonClicked(ActionEvent event) {
		System.out.println("stopButtonClicked");
		stopCapture();
	}

	private void stopCapture() {
		// キャプチャ終了
		captureTimeline.stop();

		// 停止ボタンを非活性化、それ以外のボタンを活性化
		areaButton.setDisable(false);
		pointButton.setDisable(false);
		saveDirectoryButton.setDisable(false);
		startButton.setDisable(false);
		stopButton.setDisable(true);

		clip.play();
	}

	private Stage createTransparentStage() throws IOException {
		// 画面いっぱいに最前面表示するウィンドウを作成
		Stage transparentStage = new Stage(StageStyle.TRANSPARENT);
		transparentStage.initOwner(anchorPane.getScene().getWindow());
		transparentStage.initModality(Modality.APPLICATION_MODAL);
		transparentStage.setResizable(false);
		Rectangle2D rect = Screen.getPrimary().getVisualBounds();
		transparentStage.setWidth(rect.getWidth());
		transparentStage.setHeight(rect.getHeight());

		// 現在のウィンドウのスクリーンショットを撮影
		java.awt.Rectangle awtRect = new java.awt.Rectangle(
				(int) rect.getWidth(), (int) rect.getHeight());
		BufferedImage captureImage = robot.createScreenCapture(awtRect);

		// 撮影したスクリーンショットをこのウィンドウの背景に表示
		ByteArrayInputStream in = ImageUtil.convToInputStream(captureImage);
		BackgroundImage bgImage = new BackgroundImage(new Image(in),
				BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
		Pane pane = new Pane();
		pane.setBackground(new Background(bgImage));

		// このウィンドウがESC押下で閉じるようにする
		Scene scene = new Scene(pane);
		transparentStage.setScene(scene);
		scene.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				transparentStage.close();
			}
		});

		return transparentStage;
	}

	/**
	 * キャプチャ取得サービス
	 *
	 * @author M.Yoshida
	 */
	class CaptureService extends Service<Void> {

		/** ファイルの連番 */
		private int seq;
		/** 前回のキャプチャ */
		private BufferedImage prevCapture;
		/** 待機している回数 */
		private int waitCount;

		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					System.out.println("Start task");

					// キャプチャ領域をキャプチャ
					java.awt.Rectangle awtRect = new java.awt.Rectangle(
							(int) areaStartX, (int) areaStartY,
							calcAreaWidth(),
							calcAreaHeight());
					BufferedImage currentCapture = robot
							.createScreenCapture(awtRect);

					// 前回のキャプチャと比較し、変化があればキャプチャを保存
					if (isCaptureChanged(currentCapture)) {
						waitCount = 0;

						// キャプチャをファイルに保存
						String stringSeq = new DecimalFormat("00000").format(++seq);
						File captureFile = FileUtils.getFile(saveDirectory, stringSeq + ".bmp");
						System.out.println("Save " + captureFile.getPath());
						ImageIO.write(currentCapture, "bmp", captureFile);

						// 今回のキャプチャを前回のキャプチャにする
						prevCapture = currentCapture;

						// クリックポイントをクリックし、その後マウス位置を元に戻す
						clickPoint();

					} else {
						waitCount++;
						System.out.println("Wait " + waitCount);

						if (waitCount == 5) {
							// ５回めの待機ではもう一度クリックポイントをクリックしてみる
							clickPoint();
						}
					}

					return null;
				}
			};
		}

		@Override
		protected void succeeded() {
			// １０回めの待機では完了、または失敗とみなして停止する
			if (waitCount == 10) {
				stopCapture();
			}
		}

		@Override
		protected void failed() {
			stopCapture();
			LOGGER.error("致命的なエラー", getException());
			Dialogs.create()//
					.title("FATAL")//
					.masthead("致命的なエラー")//
					.showException(getException());
		}

		private boolean isCaptureChanged(BufferedImage currentCapture) {
			// 初回は変化ありとする
			if (prevCapture == null) {
				return true;
			}

			// 比較する両キャプチャのピクセルを取得
			Raster currentRaster = currentCapture.getData();
			Raster prevRaster = prevCapture.getData();
			int[] currentPixels = currentRaster.getPixels(0, 0, currentCapture.getWidth(), currentCapture.getHeight(),
					(int[]) null);
			int[] prevPixels = prevRaster.getPixels(0, 0, prevCapture.getWidth(), prevCapture.getHeight(),
					(int[]) null);

			// ピクセルの長さが違えば変化あり（そんなことある？）
			if (currentPixels.length != prevPixels.length) {
				return true;
			}

			// ピクセルの中身を比較
			for (int i = 0; i < currentPixels.length; i++) {
				if (currentPixels[i] != prevPixels[i]) {
					return true;
				}
			}

			return false;
		}

		private void clickPoint() throws InterruptedException {
			Point mousePoint = MouseInfo.getPointerInfo().getLocation();
			robot.mouseMove((int) pointX, (int) pointY);
			robot.mousePress(InputEvent.BUTTON1_MASK);
			Thread.sleep(50);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
			robot.mouseMove(mousePoint.x, mousePoint.y);
		}

		public void setWaitCount(int waitCount) {
			this.waitCount = waitCount;
		}

	}

	private int calcAreaWidth() {
		return (int) (areaEndX - areaStartX);
	}

	private int calcAreaHeight() {
		return (int) (areaEndY - areaStartY);
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

}

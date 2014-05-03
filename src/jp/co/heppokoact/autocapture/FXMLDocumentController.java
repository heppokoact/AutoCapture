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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
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

	private static final File CONFIG_FILE = new File("config.xml");
	private static final double CAPTURE_INTERVAL = 1000.0;

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
	private Label nextPointXLabel;
	@FXML
	private Label nextPointYLabel;
	@FXML
	private Label prevPointXLabel;
	@FXML
	private Label prevPointYLabel;
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
	private double nextPointX;
	private double nextPointY;
	private double prevPointX;
	private double prevPointY;
	private File saveDirectory;

	private double dragStartX;
	private double dragStartY;
	private Rectangle areaRect;

	/** 設定 */
	private Properties prop;

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
		// 設定ファイルの読み込み
		prop = new Properties();
		if (CONFIG_FILE.exists()) {
			try (InputStream in = new FileInputStream(CONFIG_FILE)) {
				prop.loadFromXML(in);
			} catch (IOException e) {
				throw new RuntimeException("設定ファイルの読み込みに失敗しました。", e);
			}
		}

		// 設定ファイルに定義した保存ディレクトリが実在すれば使用する
		String saveDirectoryPath = prop.getProperty("saveDirectoryPath");
		String saveDirectoryName = "";
		if (saveDirectoryPath != null) {
			File tempSaveDirectory = new File(saveDirectoryPath);
			if (tempSaveDirectory.exists()) {
				saveDirectory = tempSaveDirectory;
				saveDirectoryName = saveDirectory.getName();
			}
		}
		saveDirectoryLabel.setText(saveDirectoryName);

		// 各種ラベルに初期値をセット
		areaStartXLabel.setText("0.0");
		areaStartYLabel.setText("0.0");
		areaEndXLabel.setText("0.0");
		areaEndYLabel.setText("0.0");
		nextPointXLabel.setText("0.0");
		nextPointYLabel.setText("0.0");
		prevPointXLabel.setText("0.0");
		prevPointYLabel.setText("0.0");

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
			throw new RuntimeException("ロボットが作成できません。", e);
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
		// クリック２回目
		EventHandler<? super MouseEvent> setPrevPoint = e -> {
			// クリックポイントを記録し、ラベルに表示
			prevPointX = e.getScreenX();
			prevPointY = e.getScreenY();
			prevPointXLabel.setText(Double.toString(prevPointX));
			prevPointYLabel.setText(Double.toString(prevPointY));
			transparentStage.close();
		};
		// クリック１回め
		EventHandler<MouseEvent> setNextPoint = e -> {
			// クリックポイントを記録し、ラベルに表示
			nextPointX = e.getScreenX();
			nextPointY = e.getScreenY();
			nextPointXLabel.setText(Double.toString(nextPointX));
			nextPointYLabel.setText(Double.toString(nextPointY));
			scene.setOnMouseClicked(setPrevPoint);
		};
		scene.setOnMouseClicked(setNextPoint);

		transparentStage.show();
	}

	@FXML
	private void saveDirectoryButtonClicked(ActionEvent event) throws IOException {
		System.out.println("saveDirectoryButtonClicked");

		// 保存ディレクトリを選択するダイアログを表示
		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("保存ディレクトリを選択");
		dc.setInitialDirectory(saveDirectory);
		saveDirectory = dc.showDialog(anchorPane.getScene().getWindow());

		// 保存ディレクトリが選択された場合、ラベルに表示
		if (saveDirectory != null) {
			saveDirectoryLabel.setText(saveDirectory.getName());

			// 選択したディレクトリを設定ファイルに保存
			prop.setProperty("saveDirectoryPath", saveDirectory.getAbsolutePath());
			try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
				prop.storeToXML(out, "AutoCapture設定ファイル");
			}
		}
	}

	@FXML
	private void startButtonClicked(ActionEvent event) {
		System.out.println("startButtonClicked");

		if (!validateStartButtonClicked()) {
			return;
		}

		// 停止ボタンを活性化、それ以外のボタンを非活性化
		areaButton.setDisable(true);
		pointButton.setDisable(true);
		saveDirectoryButton.setDisable(true);
		startButton.setDisable(true);
		stopButton.setDisable(false);

		// キャプチャを定期実行
		captureService.init();
		captureTimeline.play();
	}

	private boolean validateStartButtonClicked() {
		if (calcAreaHeight() == 0 || calcAreaWidth() == 0) {
			Dialogs.create()//
					.owner(stage)//
					.title("ERROR")//
					.masthead(null)//
					.message("キャプチャ領域を正しく指定してください。")//
					.showError();
			return false;
		}

		if (saveDirectory == null) {
			Dialogs.create()//
					.owner(stage)//
					.title("ERROR")//
					.masthead(null)//
					.message("保存ディレクトリが指定されていません。")//
					.showError();
			return false;
		}

		return true;
	}

	@FXML
	private void stopButtonClicked(ActionEvent event) {
		System.out.println("stopButtonClicked");
		stopCapture();
	}

	private void stopCapture() {
		// キャプチャ終了
		captureTimeline.stop();
		captureService.cancel();

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
		pane.setStyle("-fx-border-color: rgba(255, 255, 0, 0.5); -fx-border-style: solid; -fx-border-width: 15;");

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

		/** キャプチャ領域 */
		private java.awt.Rectangle captureRect;
		/** 現在表示中のページ */
		private int currentPageNumber;
		/** 現在処理中のページのうち、最も若いページ番号 */
		private int youngestPageNumber;
		/** 処理中のページ */
		Map<Integer, Page> pages = new HashMap<Integer, Page>();

		/**
		 * このサービスを初期化する。
		 */
		public void init() {
			captureRect = new java.awt.Rectangle(
					(int) areaStartX, (int) areaStartY,
					calcAreaWidth(),
					calcAreaHeight());

			youngestPageNumber = 1;
			currentPageNumber = 1;
			pages = new HashMap<Integer, Page>();
		}

		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					// 現在のページを取得、なければ作成
					Page currentPage = pages.get(currentPageNumber);
					if (currentPage == null) {
						currentPage = new Page(currentPageNumber);
						pages.put(currentPageNumber, currentPage);
					}

					// 現在のページが確定していなければキャプチャ実施
					if (!currentPage.isFixed()) {
						// キャプチャ領域をキャプチャしてページに与える
						currentPage.submitImage(capture());

						// ページが確定したら出力
						if (currentPage.isFixed()) {
							savePage(currentPage);
							updateYoungestPageNumber();
						}
					}

					// ページをめくる
					if (shouldGoForward()) {
						clickPoint(nextPointX, nextPointY);
						currentPageNumber++;
					} else {
						clickPoint(prevPointX, prevPointY);
						currentPageNumber--;
					}

					return null;
				}
			};
		}

		@Override
		protected void ready() {
			captureTimeline.pause();
			System.out.printf("cur: %d, you: %d", currentPageNumber, youngestPageNumber);
		}

		@Override
		protected void succeeded() {
			System.out.printf("cur: %d, you: %d", currentPageNumber, youngestPageNumber);

			if (captureTimeline.getStatus() == Status.PAUSED) {
				captureTimeline.play();
			}
		}

		@Override
		protected void failed() {
			System.out.printf("cur: %d, you: %d", currentPageNumber, youngestPageNumber);
			stopCapture();
			LOGGER.error("致命的なエラー", getException());
			Dialogs.create()//
					.title("FATAL")//
					.masthead("致命的なエラー")//
					.showException(getException());
		}

		/**
		 * キャプチャ領域に指定した領域をキャプチャする。
		 *
		 * @return キャプチャしたイメージ
		 */
		private BufferedImage capture() {
			return robot.createScreenCapture(captureRect);
		}

		/**
		 * 引数のページを保存する。
		 *
		 * @param page 保存するページ
		 * @throws IOException ページの保存に失敗した場合
		 */
		private void savePage(Page page) throws IOException {
			String stringSeq = new DecimalFormat("00000").format(page.getPageNumber());
			File captureFile = FileUtils.getFile(saveDirectory, stringSeq + ".bmp");
			System.out.println("Save " + captureFile.getPath());
			ImageIO.write(page.getImage(), "bmp", captureFile);
		}

		/**
		 * 現在処理中のページのうち、最も若いページ番号を更新する。
		 */
		private void updateYoungestPageNumber() {
			Page page = pages.get(youngestPageNumber);
			while (page != null && page.isFixed()) {
				youngestPageNumber++;
				page = pages.get(youngestPageNumber);
			}
		}

		/**
		 * 次のページめくりは前方向か後方向かを決める。
		 *
		 * @return 前方向ならtrue
		 */
		private boolean shouldGoForward() {
			return currentPageNumber <= youngestPageNumber;
		}

		/**
		 * 指定した座標をクリックする。
		 *
		 * @param x X座標
		 * @param y Y座標
		 */
		private void clickPoint(double x, double y) {
			Point mousePoint = MouseInfo.getPointerInfo().getLocation();
			robot.mouseMove((int) x, (int) y);
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
			robot.mouseMove(mousePoint.x, mousePoint.y);
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

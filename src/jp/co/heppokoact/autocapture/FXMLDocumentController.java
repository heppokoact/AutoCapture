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
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
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

	private IntegerProperty areaStartX;
	private IntegerProperty areaStartY;
	private IntegerProperty areaEndX;
	private IntegerProperty areaEndY;
	private IntegerProperty nextPointX;
	private IntegerProperty nextPointY;
	private IntegerProperty prevPointX;
	private IntegerProperty prevPointY;
	private File saveDirectory;

	private int dragStartX;
	private int dragStartY;
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
    areaStartXLabel.textProperty().bind(Bindings.convert(areaStartX));
		areaStartYLabel.textProperty().bind(Bindings.convert(areaStartY));
		areaEndXLabel.textProperty().bind(Bindings.convert(areaEndX));
		areaEndYLabel.textProperty().bind(Bindings.convert(areaEndY));
		nextPointXLabel.textProperty().bind(Bindings.convert(areaStartX));
		nextPointYLabel.textProperty().bind(Bindings.convert(areaStartX));
		prevPointXLabel.textProperty().bind(Bindings.convert(areaStartX));
		prevPointYLabel.textProperty().bind(Bindings.convert(areaStartX));

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

  /**
   * スタートボタン押下時に、キャプチャ実行が可能な状態になっているかどうかを検証する。
   * 可能な状態でない場合、ダイアログで通知する。
   * 
   * @return 可能な状態ならtrue
   */
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

  /**
   * キャプチャサービスとサービスを起動するタイムラインを停止する。
   */
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

  /**
   * 画面いっぱいに表示するウィンドウを作成して返す。
   * ウィンドウの背景には現在の画面のスクリーンショットを表示する。
   * このウィンドウはESCキーで閉じることができる。
   * 
   * @return 画面いっぱいに表示するウィンドウ
   * @throws IOException スクリーンショットの表示に失敗した場合
   */
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
		private Map<Integer, Page> pages = new HashMap<>();
    /** 前回の方向 */
    private Direction prevDirection1;
    /** 前々回の方向 */
    private Direction prevDirection2;
    /** 前回のイメージ */
    private BufferedImage prevImage;
    /** 前回と今回でイメージに変化はあったか */
    private boolean imageChanged1;
    /** 前々回と前回でイメージに変化はあったか */
    private boolean imageChanged2;

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
			pages = new HashMap<>();
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
          
          // キャプチャ実施
          BufferedImage image = capture();
          imageChanged2 = imageChanged1;
          imageChanged1 = ImageUtil.equals(prevImage, image);
          prevImage = image;
          
          // 終了判定
          if (isCompleted()) {
            stopCapture();
            return null;
          }
          
					// 現在のページが確定していなければキャプチャをページに与える
					if (!currentPage.isFixed()) {
						currentPage.submitImage(image);

						// ページが確定したら出力
						if (currentPage.isFixed()) {
							savePage(currentPage);
							updateYoungestPageNumber();
						}
					}

					// ページをめくる
          Direction direction = decideDirection();
          prevDirection2 = prevDirection1;
          prevDirection1 = direction;
          direction.turnPage();

					return null;
				}
			};
		}

		@Override
		protected void ready() {
			captureTimeline.pause();
			System.out.printf("RADY -> cur:%d, you:%d, dir:(%s,%s), cng:(%b,%b)", currentPageNumber, youngestPageNumber, prevDirection1, prevDirection2, imageChanged1, imageChanged2);
		}

		@Override
		protected void succeeded() {
			System.out.printf("SUCC -> cur:%d, you:%d, dir:(%s,%s), cng:(%b,%b)", currentPageNumber, youngestPageNumber, prevDirection1, prevDirection2, imageChanged1, imageChanged2);

			if (captureTimeline.getStatus() == Status.PAUSED) {
				captureTimeline.play();
			}
		}

		@Override
		protected void failed() {
			System.out.printf("FAIL -> cur:%d, you:%d, dir:(%s,%s), cng:(%b,%b)", currentPageNumber, youngestPageNumber, prevDirection1, prevDirection2, imageChanged1, imageChanged2);
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
     * キャプチャの終了判定を行う。
     * 
     * 下記のいずれかの場合、終了と判定する。
     * 
     * <ul>
     *   <li>次の動作1,2が連続した場合（最後のページがそのひとつ前のページより先にFIXした場合を想定）
     *     <ol>
     *       <li>前方向にページめくりをし、イメージに変化がなかった</li>
     *       <li>前方向にページめくりをし、イメージに変化がなかった</li>
     *     </ol>
     *   </li>
     *   <li>次の動作1,2が連続した場合（最後のページが最後までFIXせずに残った場合を想定）
     *     <ol>
     *       <li>前方向にページめくりをし、イメージに変化がなかった</li>
     *       <li>後方向にページめくりをし、イメージが変化した</li>
     *     </ol>
     *   </li>
     * </ul>
     * 
     * @return 終了している場合true
     */
    private boolean isCompleted() {
      if (prevDirection2 == FORWARD && !imageChanged2) {
        
        if (prevDirection1 == FORWARD && !imageChanged1) {
          return true;
        } else if (prevDirection1 == BACKWARD && imageChanged1) {
          return true;
        }
      }
      
      return false;
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
		private Direction decideDirection() {
			if (currentPageNumber <= youngestPageNumber) {
        return FORWARD;
      } else {
        return BACKWARD;
      }
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
    
    /**
     * ページめくりする方向を表す。
     * 本当はEnumが良いが、enumはstaticでない内部クラス内で定義できないので仕方なく抽象クラスを使用する。
     */
    private abstract class Direction {
      abstract void turnPage();
    }
    
    /** 前へ進む処理 */
    private final Direction FORWARD = new Direction() {
      @Override
      void turnPage() {
        clickPoint(nextPointX, nextPointY);
        currentPageNumber++;
      }

      @Override
      public String toString() {
        return "FORWARD";
      }
    };
    
    /** 後ろへ進む処理 */
    private final Direction BACKWARD = new Direction() {
      @Override
      void turnPage() {
        clickPoint(prevPointX, prevPointY);
        currentPageNumber--;
      }
      
      @Override
      public String toString() {
        return "BACKWARD";
      }
    };
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.heppokoact.autocapture;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author yoshidacojp
 */
public class AutoCapture extends Application {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(AutoCapture.class);

	@Override
	public void start(Stage stage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(
				"FXMLDocument.fxml"));
		Parent root = loader.load();
		FXMLDocumentController controller = (FXMLDocumentController) loader
				.getController();
		controller.setStage(stage);

		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			LOGGER.error("致命的なエラー", throwable);
			Dialogs.create()//
					.title("FATAL")//
					.masthead("致命的なエラー")//
					.showException(throwable);
		});
		launch(args);
	}

}

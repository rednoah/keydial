package ntu.csie.keydial;

import java.io.FileInputStream;
import java.io.InputStream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		Watch watch = new Watch();
		watch.setLayoutX(15);
		watch.setLayoutY(20);

		stage.setScene(new Scene(watch, Color.TRANSPARENT));
		stage.initStyle(StageStyle.TRANSPARENT);
		stage.show();

		stage.getScene().setOnKeyPressed((evt) -> {
			switch (evt.getCode()) {
			case SPACE:
				watch.enter();
				break;
			case RIGHT:
				watch.right();
				break;
			case LEFT:
				watch.left();
				break;
			case SHIFT:
				watch.select();
				break;
			default:
				break;
			}
		});

		new Thread(() -> {
			try (InputStream in = getSerialInputStream()) {
				int b = 0;
				while ((b = in.read()) > 0) {
					final KeyCode code = b == 'L' ? KeyCode.LEFT : b == 'R' ? KeyCode.RIGHT : KeyCode.SPACE;
					Platform.runLater(() -> {
						stage.getScene().getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	static InputStream getSerialInputStream() throws Exception {
		return new FileInputStream("serial.txt");
	}

}

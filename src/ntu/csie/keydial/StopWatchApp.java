package ntu.csie.keydial;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class StopWatchApp extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		Watch watch = new Watch();
		watch.setLayoutX(15);
		watch.setLayoutY(20);

		primaryStage.setScene(new Scene(watch, Color.WHITE));
		primaryStage.show();

		primaryStage.getScene().setOnKeyPressed((evt) -> {
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
			default:
				break;
			}
		});
	}

	public static void main(String[] args) {
		launch(args);
	}
}

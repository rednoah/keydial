package ntu.csie.keydial;

import static ntu.csie.keydial.Stats.*;

import java.io.InputStream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.RotateEvent;
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

		// touch input
		stage.getScene().setOnRotate(new EventHandler<RotateEvent>() {

			double sum = 0, speed = 0.5;

			@Override
			public void handle(RotateEvent evt) {
				sum += speed * evt.getAngle();
				if (sum < -1) {
					watch.left();
					sum = 0;
				}
				if (sum > +1) {
					watch.right();
					sum = 0;
				}
			}
		});

		// mouse input
		stage.getScene().setOnMouseClicked((evt) -> {
			if (evt.getButton() == MouseButton.PRIMARY)
				watch.enter();
			if (evt.getButton() == MouseButton.SECONDARY)
				watch.select();
		});
		stage.getScene().setOnScroll((evt) -> {
			if (evt.getDeltaY() < 0)
				watch.left();
			if (evt.getDeltaY() > 0)
				watch.right();
		});

		// keyboard input
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
			case TAB:
				stats.enterUser();
				break;
			}
		});

		// read input events via serial port
		Thread eventReader = new Thread(() -> {
			try (InputStream in = getSerialInputStream()) {
				int b = 0;
				while ((b = in.read()) > 0) {
					System.out.println("READ:" + b);
					final KeyCode code = b == 'L' ? KeyCode.LEFT : b == 'R' ? KeyCode.RIGHT : b == '*' ? KeyCode.SHIFT : KeyCode.SPACE;
					Platform.runLater(() -> {
						stage.getScene().getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		eventReader.start();

		stats.setUser("USER1");
	}

	static InputStream getSerialInputStream() throws Exception {
		// return new FileInputStream("serial.txt");

		// on Mac you may need to run the following commands to make RxTx work
		// $ sudo mkdir /var/lock
		// $ sudo chmod 777 /var/lock
		return Serial.connect("/dev/cu.usbmodem1421");
	}

}

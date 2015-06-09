package ntu.csie.keydial.ui;

import static ntu.csie.keydial.ui.Stats.*;

import java.io.InputStream;

import ntu.csie.keydial.io.Serial;
import javafx.animation.Animation.Status;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.RotateEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		Watch watch = new Watch();

		VBox pane = new VBox(watch, stats.getPrompter());
		VBox.setMargin(watch, new Insets(20));
		VBox.setMargin(stats.getPrompter(), new Insets(40));
		pane.setAlignment(Pos.CENTER);
		pane.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), new Insets(0))));

		stage.setScene(new Scene(pane));
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
			if (evt.getButton() == MouseButton.PRIMARY || evt.getButton() == MouseButton.MIDDLE)
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
		Timeline moveLeft = watch.repeater(watch::left);
		Timeline moveRight = watch.repeater(watch::right);

		stage.getScene().setOnKeyPressed((evt) -> {
			switch (evt.getCode()) {
			case SPACE:
				watch.enter();
				break;
			case RIGHT:
				if (moveRight.getStatus() != Status.RUNNING) {
					watch.right();
					moveRight.play();
				}
				break;
			case LEFT:
				if (moveLeft.getStatus() != Status.RUNNING) {
					watch.left();
					moveLeft.play();
				}
				break;
			case SHIFT:
				watch.select();
				break;
			default:
				break;
			}
		});
		stage.getScene().setOnKeyReleased((evt) -> {
			switch (evt.getCode()) {
			case RIGHT:
				moveRight.stop();
				break;
			case LEFT:
				moveLeft.stop();
				break;
			default:
				break;
			}
		});

		// serial input (on Mac you may need to run the following commands to make RxTx work: sudo mkdir /var/lock && sudo chmod 777 /var/lock)
		Runnable serialPortReader = () -> {
			try (InputStream in = Serial.open(Serial.getDefaultPort())) {
				int b = 0;
				while ((b = in.read()) > 0) {
					final char code = (char) b;
					System.out.println("SERIAL_READ: " + code);

					Platform.runLater(() -> {
						switch (code) {
						case '.':
							watch.enter();
							break;
						case 'R':
							watch.right();
							break;
						case 'L':
							watch.left();
							break;
						case '*':
							watch.select();
							break;
						default:
							break;
						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		};

		new Thread(serialPortReader).start();
	}
}

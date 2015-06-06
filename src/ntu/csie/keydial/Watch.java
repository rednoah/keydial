package ntu.csie.keydial;

import static java.util.Collections.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class Watch extends Parent {

	Dial alphaDial;
	Dial numberDial;
	Dial emojiDial;
	Dial predictionDial;

	Text inputDisplay = new Text();
	Text predictionDisplay = new Text();
	Group background = new Group();
	StopWatchButton startButton = new StopWatchButton(Color.web("#8cc700"), Color.web("#71a000"));
	StopWatchButton stopButton = new StopWatchButton(Color.web("#AA0000"), Color.web("#660000"));

	static Font TEXT_FONT = new Font(16);
	static Font EMOJI_FONT = Font.loadFont(Dial.class.getResourceAsStream("OpenSansEmoji.ttf"), 16);

	String buffer = "";

	int mode = 0;
	int index = 0;
	List<String> keys = getAlphaKeys();
	List<String> options = emptyList();

	List<String> getNumberKeys() {
		List<String> keys = new ArrayList<String>();
		for (char c = '0'; c <= '9'; c++) {
			keys.add(String.valueOf(c));
		}
		"$%@'.…!?".codePoints().forEach((c) -> {
			keys.add(new String(Character.toChars(c)));
		});
		return keys;
	}

	List<String> getEmojiKeys() {
		List<String> keys = new ArrayList<String>();
		for (int c = 0x1F601; c <= 0x1F60F; c += 1) {
			keys.add(new String(Character.toChars(c)));
		}
		return keys;
	}

	List<String> getAlphaKeys() {
		List<String> keys = new ArrayList<String>();
		for (char c = 'A'; c <= 'Z'; c++) {
			keys.add(String.valueOf(c));
		}

		keys.add(0, "⏎");
		keys.add(1, "⌴");
		keys.add("#");
		keys.add("😀");
		keys.add("⌫");

		return keys;
	}

	void apply(String key) throws Exception {
		switch (key) {
		case "⏎":
			submit(buffer);
			buffer = "";
			predict();
			break;
		case "⌴":
			buffer += " ";
			predict();
			break;
		case "⌫":
			if (buffer.length() > 0) {
				buffer = buffer.substring(0, buffer.length() - 1);
			}
			predict();
			break;
		case "#":
			mode = 1;
			index = 0;
			keys = getNumberKeys();
			break;
		case "😀":
			mode = 2;
			index = 0;
			keys = getEmojiKeys();
			break;
		default:
			if (mode != 3) {
				buffer = buffer + key.toLowerCase();
			} else {
				buffer = buffer.substring(0, buffer.lastIndexOf(" ") + 1) + key + " ";
			}
			predict();

			if (mode != 0) {
				mode = 0;
				index = 0;
				keys = getAlphaKeys();
			}
		}

		update();
	}

	void submit(String value) {
		System.out.println("SUBMIT = " + value);
	}

	Prediction predictor = new Prediction();

	void predict() {
		predict(6);
	}

	void predict(int limit) {
		if (buffer.matches("^.*[a-z]+$")) {
			String[] input = buffer.split("\\s");
			if (input.length > 0) {
				String prefix = input[input.length - 1];
				if (prefix.length() > 0) {
					options = predictor.complete(prefix, limit);
					return;
				}
			}
		}
		options = emptyList();
	}

	public Watch() {
		alphaDial = new Dial(117, Color.RED, getAlphaKeys(), TEXT_FONT);
		numberDial = new Dial(114, Color.GREENYELLOW, getNumberKeys(), TEXT_FONT);
		emojiDial = new Dial(114, Color.GOLD, getEmojiKeys(), EMOJI_FONT);
		predictionDial = new Dial(0, Color.BLACK, options, TEXT_FONT);

		inputDisplay.setBoundsType(TextBoundsType.VISUAL);
		inputDisplay.setTextAlignment(TextAlignment.CENTER);
		inputDisplay.setTextOrigin(VPos.BOTTOM);
		inputDisplay.setLayoutX(90);
		inputDisplay.setLayoutY(210);
		inputDisplay.setWrappingWidth(100);
		inputDisplay.setFont(EMOJI_FONT);

		predictionDisplay.setBoundsType(TextBoundsType.VISUAL);
		predictionDisplay.setTextAlignment(TextAlignment.LEFT);
		predictionDisplay.setTextOrigin(VPos.CENTER);
		predictionDisplay.setLayoutX(170);
		predictionDisplay.setLayoutY(130);
		predictionDisplay.setWrappingWidth(100);
		predictionDisplay.setFont(new Font(12));

		configureBackground();
		myLayout();
		getChildren().addAll(background, inputDisplay, predictionDisplay, alphaDial, numberDial, emojiDial, startButton, stopButton);

		update();
	}

	void update() {
		inputDisplay.setText(buffer + "_");
		predictionDisplay.setText(String.join("\n", options));

		double angleStep = (Math.PI * 2) / keys.size();
		double indexAngle = index * angleStep;
		switch (mode) {
		case 0:
			alphaDial.setAngle(Math.toDegrees(indexAngle));
			alphaDial.setVisible(true);
			numberDial.setVisible(false);
			emojiDial.setVisible(false);
			predictionDial.setVisible(false);
			break;
		case 1:
			numberDial.setAngle(Math.toDegrees(indexAngle));
			alphaDial.setVisible(false);
			numberDial.setVisible(true);
			emojiDial.setVisible(false);
			predictionDial.setVisible(false);
			break;
		case 2:
			emojiDial.setAngle(Math.toDegrees(indexAngle));
			alphaDial.setVisible(false);
			numberDial.setVisible(false);
			emojiDial.setVisible(true);
			predictionDial.setVisible(false);
			break;
		case 3:
			predictionDial.setAngle(Math.toDegrees(indexAngle));
			alphaDial.setVisible(false);
			numberDial.setVisible(false);
			emojiDial.setVisible(false);
			predictionDial.setVisible(true);
			break;
		}
	}

	void right() {
		index = (index + 1) % keys.size();
		update();

		doPressButton(startButton);
	}

	void left() {
		index = (index - 1) % keys.size();
		if (index < 0) {
			index += keys.size();
		}
		update();

		doPressButton(stopButton);
	}

	void enter() {
		String key = keys.get(index);
		System.out.println("KEY = " + key);

		try {
			apply(key);
		} catch (Exception e) {
			e.printStackTrace();
		}

		doPressButton(startButton, stopButton);
	}

	void select() {
		if (mode == 3) {
			predict(12);
			sort(options, String.CASE_INSENSITIVE_ORDER);
		}

		if ((mode != 0 && mode != 3) || options.isEmpty()) {
			return;
		}

		if (predictionDial != null) {
			getChildren().remove(predictionDial);
		}
		predictionDial = new Dial(100, Color.ROYALBLUE, options, Font.font(12));
		predictionDial.setLayoutX(140);
		predictionDial.setLayoutY(140);
		getChildren().add(predictionDial);

		mode = 3;
		index = 0;
		keys = options;
		options = emptyList();

		update();
		doPressButton(startButton, stopButton);
	}

	void doPressButton(StopWatchButton... button) {
		for (StopWatchButton it : button) {
			it.moveDown();
		}
		Timeline time = new Timeline();
		time.setCycleCount(1);
		KeyFrame keyFrame = new KeyFrame(Duration.millis(50), (event) -> {
			for (StopWatchButton it : button) {
				it.moveUp();
			}
		});
		time.getKeyFrames().add(keyFrame);
		time.play();
	}

	private void configureBackground() {
		ImageView imageView = new ImageView();
		Image image = loadImage();
		imageView.setImage(image);

		Circle circle1 = new Circle();
		circle1.setCenterX(140);
		circle1.setCenterY(140);
		circle1.setRadius(120);
		circle1.setFill(Color.TRANSPARENT);
		circle1.setStroke(Color.web("#0A0A0A"));
		circle1.setStrokeWidth(0.3);

		Circle circle2 = new Circle();
		circle2.setCenterX(140);
		circle2.setCenterY(140);
		circle2.setRadius(118);
		circle2.setFill(Color.TRANSPARENT);
		circle2.setStroke(Color.web("#0A0A0A"));
		circle2.setStrokeWidth(0.3);

		Circle circle3 = new Circle();
		circle3.setCenterX(140);
		circle3.setCenterY(140);
		circle3.setRadius(140);
		circle3.setFill(Color.TRANSPARENT);
		circle3.setStroke(Color.web("#818a89"));
		circle3.setStrokeWidth(1);

		Ellipse ellipse = new Ellipse(140, 95, 180, 95);
		Circle ellipseClip = new Circle(140, 140, 140);
		ellipse.setFill(Color.web("#535450"));
		ellipse.setStrokeWidth(0);
		GaussianBlur ellipseEffect = new GaussianBlur();
		ellipseEffect.setRadius(10);
		ellipse.setEffect(ellipseEffect);
		ellipse.setOpacity(0.1);
		ellipse.setClip(ellipseClip);
		background.getChildren().addAll(imageView, circle1, circle2, circle3, ellipse);
	}

	private void myLayout() {
		alphaDial.setLayoutX(140);
		alphaDial.setLayoutY(140);

		numberDial.setLayoutX(140);
		numberDial.setLayoutY(140);

		emojiDial.setLayoutX(140);
		emojiDial.setLayoutY(140);

		startButton.setLayoutX(223);
		startButton.setLayoutY(1);
		Rotate rotateRight = new Rotate(360 / 12);
		startButton.getTransforms().add(rotateRight);

		stopButton.setLayoutX(59.5);
		stopButton.setLayoutY(0);
		Rotate rotateLeft = new Rotate(-360 / 12);
		stopButton.getTransforms().add(rotateLeft);
	}

	public Image loadImage() {
		return new Image(Watch.class.getResourceAsStream("stopwatch.png"));
	}
}

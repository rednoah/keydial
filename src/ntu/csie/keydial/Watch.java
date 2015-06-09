package ntu.csie.keydial;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.regex.Pattern.*;
import static ntu.csie.keydial.Stats.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

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

	static final Font TEXT_FONT = new Font(16);
	static final Font TEXT_EMOJI_FONT = Font.loadFont(Dial.class.getResourceAsStream("OpenSansEmoji.ttf"), 16);
	static final Font EMOJI_FONT = Font.loadFont(Dial.class.getResourceAsStream("OpenSansEmoji.ttf"), 24);
	static final Font PREDICTION_FONT = new Font(12);

	static final String RETURN = "⏎";
	static final String SPACE = "⌴";
	static final String HASH = "#";
	static final String SMILEY = "😀";
	static final String BACKSPACE = "⌫";
	static final String CARET = "_";

	static final List<String> CONTROL_KEYS = asList(RETURN, HASH, SMILEY, BACKSPACE);

	enum Mode {

		Alpha {

			@Override
			List<String> getKeys(String input) {
				List<String> keys = new ArrayList<String>();
				keys.add(RETURN);
				keys.add(SPACE);
				for (char c = 'A'; c <= 'Z'; c++) {
					keys.add(String.valueOf(c));
				}
				keys.add(HASH);
				keys.add(SMILEY);
				keys.add(BACKSPACE);
				return keys;
			}
		},

		Number {

			@Override
			List<String> getKeys(String input) {
				List<String> keys = new ArrayList<String>();
				"0123456789$%:.…@'!?".codePoints().forEach((c) -> {
					keys.add(new String(Character.toChars(c)));
				});
				return keys;
			}
		},

		Emoji {

			@Override
			List<String> getKeys(String input) {
				List<String> keys = new ArrayList<String>();
				for (int c = 0x1F601; c <= 0x1F60F; c += 1) {
					keys.add(new String(Character.toChars(c)));
				}
				return keys;
			}
		},

		Prediction1 {

			@Override
			List<String> getKeys(String input) {
				return Prediction.getInstance().completeSentence(input, 6);
			}
		},

		Prediction2 {

			@Override
			List<String> getKeys(String input) {
				int offset = 6;
				int n = 12;
				List<String> options = Prediction.getInstance().completeSentence(input, offset + n);
				return options.stream().skip(offset).limit(n).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
			}
		},

		Prediction3 {

			@Override
			List<String> getKeys(String input) {
				int offset = 6 + 12;
				int n = 12;
				List<String> options = Prediction.getInstance().completeSentence(input, offset + n);
				return options.stream().skip(offset).limit(n).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
			}
		};

		abstract List<String> getKeys(String input);

	}

	Text inputDisplay = new Text();
	Text predictionDisplay = new Text();
	Group background = new Group();
	StopWatchButton startButton = new StopWatchButton(Color.web("#8cc700"), Color.web("#71a000"));
	StopWatchButton stopButton = new StopWatchButton(Color.web("#AA0000"), Color.web("#660000"));

	static final Mode DEFAULT_MODE = Mode.Alpha;
	static final Set<Mode> PREDICTION_MODES = EnumSet.of(Mode.Prediction1, Mode.Prediction2, Mode.Prediction3);

	String buffer = "";

	Dial dial;
	Mode mode;
	int index;
	List<String> options = emptyList();

	void setMode(Mode mode) {
		if (PREDICTION_MODES.contains(mode) && mode.getKeys(buffer).isEmpty()) {
			// force alpha mode if no prediction are available
			mode = DEFAULT_MODE;
			// or cycle back to previous prediction level that still had predictions available
			for (Mode it : PREDICTION_MODES) {
				if (it.getKeys(buffer).size() > 0) {
					mode = it;
					break;
				}
			}
		}

		if (this.mode != mode) {
			// update state
			this.mode = mode;
			this.index = 0;

			if (mode == Mode.Alpha) {
				this.options = Mode.Prediction1.getKeys(buffer);
			} else {
				this.options = emptyList();
			}

			// update dial
			if (dial != null) {
				getChildren().remove(dial);
			}
			dial = createDial(mode);
			if (mode == Mode.Emoji) {
				dial.setLayoutX(140);
				dial.setLayoutY(145);
			} else {
				dial.setLayoutX(140);
				dial.setLayoutY(140);
			}
			getChildren().add(dial);

			update();
		}
	}

	void apply(String key) throws Exception {
		if (stats.started()) {
			stats.record(key);
		}

		switch (key) {
		case RETURN:
			submit(buffer.trim());
			buffer = "";
			break;
		case SPACE:
			buffer += " ";
			break;
		case BACKSPACE:
			if (buffer.length() > 0) {
				buffer = buffer.substring(0, buffer.length() - 1);
			}
			break;
		case HASH:
			setMode(Mode.Number);
			break;
		case SMILEY:
			setMode(Mode.Emoji);
			break;
		default:
			if (PREDICTION_MODES.contains(mode)) {
				buffer = buffer.substring(0, buffer.lastIndexOf(" ") + 1) + key + " ";
			} else {
				buffer = buffer + key.toLowerCase();
			}
			setMode(DEFAULT_MODE);
		}

		if (mode == DEFAULT_MODE) {
			options = Mode.Prediction1.getKeys(buffer);
		}
		update();
	}

	void submit(String value) {
		if (value.length() > 0) {
			System.out.println("SUBMIT = " + value);
			stats.endRecord(value);
		}
		stats.reset();
	}

	Dial createDial(Mode mode) {
		switch (mode) {
		case Alpha:
			return new Dial(117, Color.RED, mode.getKeys(buffer), TEXT_FONT);
		case Number:
			return new Dial(114, Color.GREENYELLOW, mode.getKeys(buffer), TEXT_FONT);
		case Emoji:
			return new Dial(110, Color.GOLD, mode.getKeys(buffer), EMOJI_FONT);
		default:
			return new Dial(100, Color.ROYALBLUE, mode.getKeys(buffer), PREDICTION_FONT);
		}
	}

	public Watch() {
		// text displays
		inputDisplay.setBoundsType(TextBoundsType.VISUAL);
		inputDisplay.setTextAlignment(TextAlignment.CENTER);
		inputDisplay.setTextOrigin(VPos.BOTTOM);
		inputDisplay.setLayoutX(90);
		inputDisplay.setLayoutY(210);
		inputDisplay.setWrappingWidth(100);
		inputDisplay.setFont(TEXT_EMOJI_FONT);

		predictionDisplay.setBoundsType(TextBoundsType.VISUAL);
		predictionDisplay.setTextAlignment(TextAlignment.LEFT);
		predictionDisplay.setTextOrigin(VPos.CENTER);
		predictionDisplay.setLayoutX(170);
		predictionDisplay.setLayoutY(130);
		predictionDisplay.setWrappingWidth(70);
		predictionDisplay.setFont(PREDICTION_FONT);

		// buttons
		startButton.setLayoutX(223);
		startButton.setLayoutY(1);
		Rotate rotateRight = new Rotate(360 / 12);
		startButton.getTransforms().add(rotateRight);

		stopButton.setLayoutX(59.5);
		stopButton.setLayoutY(0);
		Rotate rotateLeft = new Rotate(-360 / 12);
		stopButton.getTransforms().add(rotateLeft);

		// background
		ImageView imageView = new ImageView();
		imageView.setImage(new Image(Watch.class.getResourceAsStream("stopwatch.png")));

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

		// update stage
		getChildren().addAll(background, inputDisplay, predictionDisplay, startButton, stopButton);

		// init mode
		setMode(DEFAULT_MODE);
	}

	void update() {
		inputDisplay.setText(truncateLeft(buffer, 20, "\\s+") + CARET);
		predictionDisplay.setText(String.join("\n", options));

		double angleStep = (Math.PI * 2) / mode.getKeys(buffer).size();
		double indexAngle = index * angleStep;
		dial.setAngle(Math.toDegrees(indexAngle));
	}

	void right() {
		if (!stats.started()) {
			stats.startRecord();
		}

		int total = mode.getKeys(buffer).size();
		index = (index + 1) % total;

		update();
		doPressButton(startButton);
	}

	void left() {
		if (!stats.started()) {
			stats.startRecord();
		}

		int total = mode.getKeys(buffer).size();
		index = (index - 1) % total;
		if (index < 0) {
			index += total;
		}

		update();
		doPressButton(stopButton);
	}

	void enter() {
		String key = mode.getKeys(buffer).get(index);
		System.out.println("KEY = " + key);

		try {
			apply(key);
		} catch (Exception e) {
			e.printStackTrace();
		}

		doPressButton(startButton, stopButton);
	}

	void select() {
		switch (this.mode) {
		case Prediction1:
			setMode(Mode.Prediction2);
			break;
		case Prediction2:
			setMode(Mode.Prediction3);
			break;
		case Prediction3:
			setMode(DEFAULT_MODE);
			break;
		default:
			setMode(Mode.Prediction1);
			break;
		}

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

	public static String truncateLeft(String self, int hardLimit, String nonWordPattern) {
		if (hardLimit >= self.length()) {
			return self;
		}

		int minStartIndex = self.length() - hardLimit;
		Matcher matcher = compile(nonWordPattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS).matcher(self).region(minStartIndex, self.length());
		if (matcher.find()) {
			return self.substring(matcher.start(), self.length());
		} else {
			return self.substring(minStartIndex, self.length());
		}
	}

}

package ntu.csie.keydial;

import java.util.List;

import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;

public class Dial extends Parent {

	private final double radius;
	private final Color color;
	private final Color fill;
	private final Font font;

	private final Group hand = new Group();
	private final Group handEffectGroup = new Group(hand);
	private final DropShadow handEffect = new DropShadow();

	public Dial(double radius, Color color, List<String> keys, Font font) {
		this.color = color;
		this.radius = radius;

		this.fill = Color.web("#0A0A0A");
		this.font = font;

		getChildren().add(createNumbers(keys));

		configureHand();
		configureEffect();
		getChildren().addAll(handEffectGroup);
	}

	private Group createNumbers(List<String> keys) {
		Group group = new Group();

		double angle = -Math.PI / 2;
		double angleStep = (Math.PI * 2) / keys.size();
		for (String a : keys) {
			double x = (radius * 0.9) * Math.cos(angle);
			double y = (radius * 0.9) * Math.sin(angle);
			group.getChildren().add(createNumber(a, x, y + 7));
			angle += angleStep;
		}

		return group;
	}

	private Text createNumber(String number, double layoutX, double layoutY) {
		Text text = new Text(number);
		text.setTextAlignment(TextAlignment.CENTER);
		text.setWrappingWidth(font.getSize());
		text.setTextOrigin(VPos.CENTER);
		text.setFill(fill);

		if ("😀".equals(number)) {
			text.setFont(Watch.EMOJI_FONT);
		} else {
			text.setFont(font);
		}

		text.setLayoutX(layoutX - text.getBoundsInLocal().getWidth() / 2);
		text.setLayoutY(layoutY - text.getBoundsInLocal().getHeight() / 2);
		return text;
	}

	public void setAngle(double angle) {
		Rotate rotate = new Rotate(angle);
		hand.getTransforms().clear();
		hand.getTransforms().add(rotate);
	}

	private void configureHand() {
		Circle circle = new Circle(0, 0, radius / 18);
		circle.setFill(color);
		Rectangle rectangle1 = new Rectangle(-0.5 - radius / 140, +radius / 7 - radius / 1.08, radius / 70 + 1, radius / 1.08);
		Rectangle rectangle2 = new Rectangle(-0.5 - radius / 140, +radius / 3.5 - radius / 7, radius / 70 + 1, radius / 7);
		rectangle1.setFill(color);
		rectangle2.setFill(Color.BLACK);
		hand.getChildren().addAll(circle, rectangle1, rectangle2);
	}

	private void configureEffect() {
		handEffect.setOffsetX(radius / 40);
		handEffect.setOffsetY(radius / 40);
		handEffect.setRadius(6);
		handEffect.setColor(Color.web("#000000"));

		Lighting lighting = new Lighting();
		Light.Distant light = new Light.Distant();
		light.setAzimuth(225);
		lighting.setLight(light);
		handEffect.setInput(lighting);

		handEffectGroup.setEffect(handEffect);
	}
}
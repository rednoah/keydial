package ntu.csie.keydial.stats;

import static java.util.Collections.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Modality;
import ntu.csie.keydial.prediction.Prediction;
import ntu.csie.keydial.ui.Watch;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class Stats {

	public static final Stats stats = new Stats();

	private static final int phrasesLimit = 20;
	private static final Path records = Paths.get("stats.tsv");

	private String user;
	private Queue<String> lines;

	private String input;
	private String output;
	private List<String> record;
	private Instant start;
	private Instant end;

	public void reset() {
		output = null;
		record = new ArrayList<String>();
		start = null;
		end = null;
	}

	public void setUser(String user, Queue<String> lines) {
		reset();

		this.user = user;
		this.lines = lines;
		this.input = lines.poll();

		if (prompter != null) {
			if (input != null) {
				counter.setText(String.format("(%d)", lines.size() + 1));
				prompter.setText(input);
			} else {
				counter.setText("");
				prompter.setText("COMPLETE");
			}
		}
	}

	public boolean started() {
		return start != null;
	}

	public void startRecord() {
		reset();
		start = Instant.now();
		System.out.println("START_RECORD: " + start);
	}

	public void record(String string) {
		record.add(string);
	}

	public void endRecord(String string) {
		if (start == null || input == null)
			return;

		output = string;
		end = Instant.now();

		// record
		Map<String, Object> stats = new LinkedHashMap<String, Object>();
		stats.put("user", user);
		stats.put("wpm", ((double) output.length() / 5) / ((double) Duration.between(start, end).toMillis() / 60000));
		stats.put("cps", (double) output.length() / ((double) Duration.between(start, end).toMillis() / 1000));
		stats.put("duration", Duration.between(start, end).toMillis());
		stats.put("entered", record.stream().filter((it) -> !Watch.CONTROL_KEYS.contains(it)).count());
		stats.put("deleted", record.stream().filter((it) -> it.equals(Watch.BACKSPACE)).count());
		stats.put("similarity", (double) new Levenshtein().getSimilarity(input.toLowerCase(), output.toLowerCase()));
		stats.put("distance", (int) new Levenshtein().getUnNormalisedSimilarity(input.toLowerCase(), output.toLowerCase()));
		stats.put("date", start);
		stats.put("input_length", input.length());
		stats.put("output_length", output.length());
		stats.put("input", input);
		stats.put("output", output);
		stats.put("record", record);

		System.out.println("STATS: " + stats);
		try {
			if (!Files.exists(records)) {
				Files.write(records, singleton(stats.keySet().stream().map(String::toUpperCase).collect(Collectors.joining("\t"))), StandardCharsets.UTF_8);
			}
			Files.write(records, singleton(stats.values().stream().map(s -> {
				if (s instanceof Double || s instanceof Float) {
					return String.format("%.2f", s);
				}
				return s.toString();
			}).collect(Collectors.joining("\t"))), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// reset and poll next line
		if (lines.size() > 0) {
			setUser(user, lines);
		} else {
			setUser(null, new LinkedList<String>());
		}
	}

	private HBox prompterNode;
	private Text prompter;
	private Text counter;

	public Node getPrompter() {
		if (prompterNode == null) {
			prompter = new Text();
			prompter.setFont(Font.font(Font.getDefault().getName(), 24));
			prompter.setText("Click to start ...");

			prompter.setBoundsType(TextBoundsType.VISUAL);
			prompter.setTextAlignment(TextAlignment.CENTER);
			prompter.setTextOrigin(VPos.TOP);
			prompter.setWrappingWidth(700);
			prompter.setCursor(Cursor.HAND);
			prompter.setOnMouseClicked(evt -> stats.enterUser((Node) evt.getSource()));

			counter = new Text();
			counter.setFont(Font.font(Font.getDefault().getName(), 16));
			counter.setFill(Color.GRAY);
			counter.setBoundsType(TextBoundsType.VISUAL);
			counter.setTextAlignment(TextAlignment.CENTER);
			counter.setTextOrigin(VPos.BOTTOM);
			counter.setWrappingWidth(100);

			prompterNode = new HBox(counter, prompter);
			prompterNode.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(5), new Insets(0))));
			prompterNode.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1), new Insets(0))));
			prompterNode.setAlignment(Pos.CENTER);
			prompterNode.setMaxWidth(700);
			HBox.setMargin(prompter, new Insets(20));
		}
		return prompterNode;
	}

	public void enterUser(Node owner) {
		TextInputDialog dialog = new TextInputDialog("USER");
		dialog.initOwner(owner.getScene().getWindow());
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.setHeaderText("Start Test");
		dialog.showAndWait().ifPresent(name -> {
			try {
				// set user and start test
				stats.setUser(name, getPhrasesForUserTrial());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public Queue<String> getPhrasesForUserTrial() {
		List<String> phrases = getPhrases();

		shuffle(phrases, new SecureRandom());

		return new LinkedList<String>(phrases.subList(0, phrasesLimit));
	}

	public List<String> getPhrases() {
		List<String> phrases = new ArrayList<String>();
		try (Scanner in = new Scanner(getClass().getResourceAsStream("phrases.txt"), "UTF-8")) {
			while (in.hasNextLine()) {
				String line = in.nextLine().replaceAll("\\s+", " ").trim();
				if (line.length() > 0) {
					phrases.add(line);
				}
			}
		}
		return phrases;
	}

	public static void main(String[] args) throws Exception {
		stats.getPhrases().stream().flatMap(it -> Stream.of(it.trim().split("\\s+"))).map(String::toLowerCase).sorted().distinct().map(s -> {
			List<String> line = new ArrayList<String>();
			line.add(s);

			int[] level = new int[] { 6, 18, 30 };
			for (int i = 0; i < level.length; i++) {
				for (int c = 1; c <= s.length() + 1; c++) {
					if (c >= s.length() + 1) {
						line.add(String.valueOf(Float.NaN));
						break;
					} else {
						List<String> options = Prediction.getInstance().completeWord(s.substring(0, c), level[i]);
						if (options.stream().filter(w -> w.equalsIgnoreCase(s)).findFirst().isPresent()) {
							line.add(String.valueOf(c));
							break;
						}
					}
				}
			}

			return line;
		}).forEach(l -> System.out.println(String.join("\t", l)));
	}
}

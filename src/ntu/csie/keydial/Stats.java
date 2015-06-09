package ntu.csie.keydial;

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
import java.util.Objects;
import java.util.Queue;
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
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class Stats {

	public static final Stats stats = new Stats();

	private static final int phrasesLimit = 20;
	private static final String phrasesOutput = "study/phrases-%s.txt";

	private static final Path phrases = Paths.get("study/phrases.txt");
	private static final Path records = Paths.get("study/stats.tsv");

	private String user;
	private Queue<String> lines;

	private String input;
	private String output;
	private List<String> record;
	private Instant start;
	private Instant end;

	public String normalize(String s) {
		return s.trim().toLowerCase();
	}

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
			prompter.setText(input != null ? input : "COMPLETE");
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

	public void record(String s) {
		record.add(s);
	}

	public void endRecord(String s) {
		if (start == null || input == null)
			return;

		output = s;
		end = Instant.now();

		// record
		Map<String, Object> stats = new LinkedHashMap<String, Object>();
		stats.put("user", user);
		stats.put("cps", output.length() / ((double) Duration.between(start, end).toMillis() / 1000));
		stats.put("wpm", ((double) output.length() / 5) / ((double) Duration.between(start, end).toMillis() / 60000));
		stats.put("entered", record.stream().filter((it) -> !Watch.CONTROL_KEYS.contains(it)).count());
		stats.put("deleted", record.stream().filter((it) -> it.equals(Watch.BACKSPACE)).count());
		stats.put("distance", new Levenshtein().getSimilarity(normalize(input), normalize(output)));
		stats.put("duration", Duration.between(start, end).toMillis());
		stats.put("date", start);
		stats.put("input", input);
		stats.put("output", output);
		stats.put("record", record);

		System.out.println("STATS: " + stats);
		try {
			Files.write(records, singleton(stats.values().stream().map(Objects::toString).collect(Collectors.joining("\t"))), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

			prompterNode = new HBox();
			prompterNode.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(5), new Insets(0))));
			prompterNode.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1), new Insets(0))));
			prompterNode.getChildren().addAll(prompter);
			prompterNode.setAlignment(Pos.CENTER);
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
				// create user-specific test set
				Path newFile = Paths.get(String.format(phrasesOutput, name));

				List<String> lines = Files.lines(phrases, StandardCharsets.UTF_8).collect(Collectors.toList());

				shuffle(lines, new SecureRandom());
				lines = lines.subList(0, phrasesLimit);

				Files.write(newFile, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);

				// set user and start test
				stats.setUser(name, new LinkedList<String>(lines));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public static void main(String[] args) throws Exception {
		Files.lines(phrases, StandardCharsets.UTF_8).flatMap(it -> Stream.of(it.trim().split("\\s+"))).map(String::toLowerCase).sorted().distinct().map(s -> {
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

package ntu.csie.keydial;

import static java.util.Arrays.*;
import static java.util.Collections.*;

import java.awt.Desktop;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.control.TextInputDialog;

public class Stats {

	public static final Stats stats = new Stats();

	private static final int phrasesLimit = 25;
	private static final String phrasesOutput = "study/phrases-%s.txt";

	private static final Path phrases = Paths.get("study/phrases.txt");
	private static final Path records = Paths.get("study/stats.tsv");

	private String user;

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

	public void setUser(String s) {
		user = s;
		System.out.println("USER = " + user);
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
		output = s;
		end = Instant.now();

		// record
		Map<String, Object> stats = new LinkedHashMap<String, Object>();
		stats.put("user", user);
		stats.put("date", start);
		stats.put("output", output);
		stats.put("entered", record.stream().filter((it) -> !Watch.CONTROL_KEYS.contains(it)).count());
		stats.put("deleted", record.stream().filter((it) -> it.equals(Watch.BACKSPACE)).count());
		stats.put("duration", Duration.between(start, end).toMillis());
		stats.put("cps", output.length() / ((double) Duration.between(start, end).toMillis() / 1000));
		stats.put("wpm", ((double) output.length() / 5) / ((double) Duration.between(start, end).toMillis() / 60000));
		stats.put("record", record);

		System.out.println("STATS: " + stats);
		try {
			Files.write(records, singleton(stats.values().stream().map(Objects::toString).collect(Collectors.joining("\t"))), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void enterUser() {
		TextInputDialog dialog = new TextInputDialog("USER");
		dialog.setHeaderText("Start Test");
		dialog.showAndWait().ifPresent(name -> {
			// set user
				stats.setUser(name);

				// create user-specific test set
				try {
					Path newFile = Paths.get(String.format(phrasesOutput, user));

					List<String> lines = Files.lines(phrases, StandardCharsets.UTF_8).collect(Collectors.toList());

					shuffle(lines, new SecureRandom());
					lines = lines.subList(0, phrasesLimit);

					Files.write(newFile, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);

					Desktop.getDesktop().browse(newFile.toUri());
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
	}

	public static void main(String[] args) throws Exception {
		Files.lines(phrases, StandardCharsets.UTF_8).flatMap(it -> Stream.of(it.trim().split("\\s+"))).map(String::toLowerCase).sorted().distinct().map(s -> {
			List<Object> line = new ArrayList<Object>();
			line.add(s);

			int[] level = new int[] { 6, 18, 30 };
			for (int i = 0; i < level.length; i++) {
				for (int c = 0; c <= s.length(); c++) {
					if (c == s.length()) {
						line.add(-1);
						break;
					} else {
						List<String> options = Prediction.getInstance().completeWord(s, level[i]);
						if (options.stream().filter(w -> w.equalsIgnoreCase(s)).findFirst().isPresent()) {
							line.add(i);
							break;
						}
					}
				}
			}

			return line;
		}).forEach(System.out::println);
	}
}

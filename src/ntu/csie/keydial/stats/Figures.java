package ntu.csie.keydial.stats;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class Figures extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	private List<Record> records = loadRecords();

	@Override
	public void start(Stage stage) throws Exception {
		VBox pane = new VBox();
		pane.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(0), new Insets(0))));

		pane.getChildren().add(AverageWordsPerMinuteByTrialNumber());
		pane.getChildren().add(AverageWordsPerMinuteByUser());
		pane.getChildren().add(AverageWordsPerMinuteForAllUsers());

		stage.setScene(new Scene(pane, 800, 800, Color.TRANSPARENT));
		stage.initStyle(StageStyle.DECORATED);
		// stage.initStyle(StageStyle.TRANSPARENT);
		stage.show();
	}

	Chart AverageWordsPerMinuteByTrialNumber() {
		XYChart.Series<Number, Number> series = new XYChart.Series<>();
		records.stream().collect(groupingBy(Record::getTrialNumber)).forEach((n, r) -> {
			double avg = r.stream().mapToDouble(Record::getWPM).average().orElse(0);
			series.getData().add(new XYChart.Data<Number, Number>(n, avg));
		});

		return createLineChart("Trial Number", 1, 20, "WPM", 0, 10, singleton(series));
	}

	Chart AverageWordsPerMinuteForAllUsers() {
		List<XYChart.Series<Number, Number>> users = new ArrayList<XYChart.Series<Number, Number>>();
		records.stream().collect(groupingBy(Record::getUser)).forEach((u, r) -> {
			XYChart.Series<Number, Number> series = new XYChart.Series<>();
			r.forEach(it -> series.getData().add(new XYChart.Data<Number, Number>(it.getTrialNumber(), it.getWPM())));
			users.add(series);
		});

		return createLineChart("Trial Number", 1, 20, "WPM", 0, 20, users);
	}

	Chart AverageWordsPerMinuteByUser() {
		BarChart.Series<String, Number> avg = new BarChart.Series<>();
		BarChart.Series<String, Number> min = new BarChart.Series<>();
		BarChart.Series<String, Number> max = new BarChart.Series<>();
		avg.setName("avg");
		min.setName("min");
		max.setName("max");

		records.stream().collect(groupingBy(Record::getUserNumber)).forEach((u, r) -> {
			avg.getData().add(new XYChart.Data<String, Number>(u.toString(), r.stream().mapToDouble(Record::getWPM).average().orElse(0)));
			min.getData().add(new XYChart.Data<String, Number>(u.toString(), r.stream().mapToDouble(Record::getWPM).min().orElse(0)));
			max.getData().add(new XYChart.Data<String, Number>(u.toString(), r.stream().mapToDouble(Record::getWPM).max().orElse(0)));
		});

		return createBarChart("User", "WPM", 0, 20, asList(min, max, avg));
	}

	Chart createLineChart(String xLabel, double xLower, double xUpper, String yLabel, double yLower, double yUpper, Iterable<LineChart.Series<Number, Number>> series) {
		NumberAxis xAxis = new NumberAxis(xLabel, xLower, xUpper, 1);
		NumberAxis yAxis = new NumberAxis(yLabel, yLower, yUpper, 1);
		LineChart<Number, Number> chart = new LineChart<Number, Number>(xAxis, yAxis);
		for (Series<Number, Number> it : series) {
			chart.getData().add(it);
		}
		chart.setLegendVisible(false);
		chart.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(0), new Insets(0))));
		return chart;
	}

	Chart createBarChart(String xLabel, String yLabel, double yLower, double yUpper, Iterable<BarChart.Series<String, Number>> series) {
		CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel(xLabel);
		NumberAxis yAxis = new NumberAxis(yLabel, yLower, yUpper, 1);
		BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
		for (BarChart.Series<String, Number> it : series) {
			chart.getData().add(it);
		}
		chart.setLegendVisible(true);
		chart.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(0), new Insets(0))));
		return chart;
	}

	List<Record> loadRecords() {
		File stats = new File("stats.tsv");
		List<Record> records = new ArrayList<Record>();

		try (Scanner in = new Scanner(stats, "UTF-8").useDelimiter("\\t|\\R")) {
			// USER WPM CPS DURATION ENTERED DELETED SIMILARITY DISTANCE DATE INPUT_LENGTH OUTPUT_LENGTH INPUT OUTPUT RECORD
			in.nextLine();

			Map<String, Integer> trialNumber = new HashMap<String, Integer>();
			Map<String, Integer> userNumber = new HashMap<String, Integer>();
			AtomicInteger userNumberSupplier = new AtomicInteger(0);

			while (in.hasNext()) {
				Record r = new Record();
				r.user = in.next();
				r.trialNumber = trialNumber.compute(r.user, (u, n) -> n == null ? 1 : n + 1);
				r.userNumber = userNumber.computeIfAbsent(r.user, (u) -> userNumberSupplier.incrementAndGet());
				in.next();
				in.next();
				r.duration = in.nextLong();
				r.entered = in.nextInt();
				r.deleted = in.nextInt();
				in.next();
				in.next();
				r.date = Instant.parse(in.next());
				in.next();
				in.next();
				r.input = in.next();
				r.output = in.next();
				r.record = in.next();
				records.add(r);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return records;
	}

	static class Record {

		int trialNumber;
		int userNumber;
		String user;
		long duration;
		int entered;
		int deleted;
		Instant date;
		String input;
		String output;
		String record;

		int getTrialNumber() {
			return trialNumber;
		}

		int getUserNumber() {
			return userNumber;
		}

		String getUser() {
			return user;
		}

		public double getWPM() {
			return ((double) output.length() / 5) / ((double) duration / 60000);
		}

		public double getCPS() {
			return (double) output.length() / ((double) duration / 1000);
		}

		public long getDuration() {
			return duration;
		}

		public int getEntered() {
			return entered;
		}

		public int getDeleted() {
			return deleted;
		}

		public double getSimilarity() {
			return (double) new Levenshtein().getSimilarity(input.toLowerCase(), output.toLowerCase());
		}

		public int getDistance() {
			return (int) new Levenshtein().getUnNormalisedSimilarity(input.toLowerCase(), output.toLowerCase());
		}

		public Instant getDate() {
			return date;
		}

		public String getInput() {
			return input;
		}

		public String getOutput() {
			return output;
		}

		@Override
		public String toString() {
			return asList(user, trialNumber).toString();
		}

	}

}

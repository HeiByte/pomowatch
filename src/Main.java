import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.media.AudioClip;


import java.awt.Toolkit;


public class Main extends Application {

    private enum Mode { WORK, SHORT_BREAK, LONG_BREAK }

    // default durations in minutes
    private int workMinutes = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes = 15;
    private int cyclesUntilLongBreak = 4;

    private Mode currentMode = Mode.WORK;
    private boolean running = false;

    private int remainingSeconds = workMinutes * 60;
    private int completedWorkSessions = 0;

    private Timeline timeline;

    // UI nodes
    private Label timerLabel;
    private Label modeLabel;
    private Button startPauseBtn;
    private Button resetBtn;
    private Circle progressRing;
    private AudioClip beepSound;

    @Override
    public void start(Stage primaryStage) {
        
        // load sound beep
        beepSound = new AudioClip(getClass().getResource("/beep.wav").toExternalForm());


        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        VBox centerBox = new VBox(12);
        centerBox.setAlignment(Pos.CENTER);

        modeLabel = new Label("Work");
        modeLabel.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 20));
        modeLabel.setId("mode-label");

        timerLabel = new Label(formatTime(remainingSeconds));
        timerLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 48));
        timerLabel.setId("timer-label");  // <-- pindahkan ke sini setelah deklarasi


        // timer label
        timerLabel = new Label(formatTime(remainingSeconds));
        timerLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 48));

        // progress ring
        progressRing = new Circle(100);
        progressRing.setStrokeWidth(12);
        progressRing.setStrokeLineCap(StrokeLineCap.ROUND);
        progressRing.setFill(Color.TRANSPARENT);
        progressRing.setStroke(Color.web("#4FD1C5")); // will be overridden by CSS theme
        progressRing.getStyleClass().add("progress-ring");

        StackPane ringStack = new StackPane(progressRing, timerLabel);
        ringStack.setPrefSize(220, 220);

        centerBox.getChildren().addAll(modeLabel, ringStack);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER);

        startPauseBtn = new Button("Start");
        startPauseBtn.setPrefWidth(100);
        startPauseBtn.setOnAction(e -> toggleStartPause());

        resetBtn = new Button("Reset");
        resetBtn.setPrefWidth(100);
        resetBtn.setOnAction(e -> resetTimer(false));

        Button settingsBtn = new Button("Settings");
        settingsBtn.setOnAction(e -> openSettingsDialog());

        controls.getChildren().addAll(startPauseBtn, resetBtn, settingsBtn);

        root.setCenter(centerBox);
        root.setBottom(controls);
        BorderPane.setAlignment(controls, Pos.CENTER);
        BorderPane.setMargin(controls, new Insets(20, 0, 0, 0));

        // top status
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_RIGHT);
        Label cyclesLabel = new Label();
        cyclesLabel.setId("cycles-label");
        cyclesLabel.setText("Completed: 0");
        cyclesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
        topBar.getChildren().add(cyclesLabel);
        root.setTop(topBar);

        Scene scene = new Scene(root, 420, 420);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);

        // keyboard shortcuts
        scene.setOnKeyPressed(k -> {
            if (k.getCode() == KeyCode.SPACE) toggleStartPause();
            if (k.getCode() == KeyCode.R) resetTimer(false);
        });

        primaryStage.setTitle("Pomodoro Stopwatch");
        primaryStage.setScene(scene);
        primaryStage.show();

        // timeline: ticks every second
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (running) {
                remainingSeconds -= 1;
                if (remainingSeconds <= 0) {
                    onTimerEnd();
                }
                updateUI();
                cyclesLabel.setText("Completed: " + completedWorkSessions);
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        updateUI();

    }

    private void toggleStartPause() {
        running = !running;
        startPauseBtn.setText(running ? "Pause" : "Start");
    }

    private void resetTimer(boolean keepMode) {
        running = false;
        startPauseBtn.setText("Start");
        if (!keepMode) currentMode = Mode.WORK;
        remainingSeconds = getModeDurationSeconds(currentMode);
        updateUI();
    }

    private void onTimerEnd() {
        // beep
        if (beepSound != null) beepSound.play();

        // switch modes
        if (currentMode == Mode.WORK) {
            completedWorkSessions++;
            if (completedWorkSessions % cyclesUntilLongBreak == 0) {
                currentMode = Mode.LONG_BREAK;
            } else {
                currentMode = Mode.SHORT_BREAK;
            }
        } else {
            currentMode = Mode.WORK;
        }
        remainingSeconds = getModeDurationSeconds(currentMode);
        // keep running to auto-start next session
    }

    private int getModeDurationSeconds(Mode mode) {
        return switch (mode) {
            case WORK -> workMinutes * 60;
            case SHORT_BREAK -> shortBreakMinutes * 60;
            case LONG_BREAK -> longBreakMinutes * 60;
        };
    }

    private void updateUI() {
        timerLabel.setText(formatTime(Math.max(0, remainingSeconds)));
        modeLabel.setText(modeText(currentMode));

        double total = getModeDurationSeconds(currentMode);
        double progress = (total - Math.max(0, remainingSeconds)) / total; // 0..1
        // animate stroke dash to show progress around the ring
        double circumference = 2 * Math.PI * progressRing.getRadius();
        progressRing.getStrokeDashArray().setAll(circumference, circumference);
        double offset = circumference - (circumference * progress);
        progressRing.setStrokeDashOffset(offset);

        // color by mode (class binding via inline style)
        switch (currentMode) {
            case WORK -> progressRing.setStroke(Color.web("#4FD1C5"));
            case SHORT_BREAK -> progressRing.setStroke(Color.web("#60A5FA"));
            case LONG_BREAK -> progressRing.setStroke(Color.web("#F472B6"));
        }
    }

    private String modeText(Mode mode) {
        return switch (mode) {
            case WORK -> "Work";
            case SHORT_BREAK -> "Short Break";
            case LONG_BREAK -> "Long Break";
        };
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void openSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Settings");

        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.setPadding(new Insets(12));

        TextField workField = new TextField(String.valueOf(workMinutes));
        TextField shortField = new TextField(String.valueOf(shortBreakMinutes));
        TextField longField = new TextField(String.valueOf(longBreakMinutes));
        TextField cyclesField = new TextField(String.valueOf(cyclesUntilLongBreak));

        g.addRow(0, new Label("Work (minutes):"), workField);
        g.addRow(1, new Label("Short break (minutes):"), shortField);
        g.addRow(2, new Label("Long break (minutes):"), longField);
        g.addRow(3, new Label("Cycles until long break:"), cyclesField);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    int w = Integer.parseInt(workField.getText());
                    int s = Integer.parseInt(shortField.getText());
                    int l = Integer.parseInt(longField.getText());
                    int c = Integer.parseInt(cyclesField.getText());
                    if (w <= 0 || s <= 0 || l <= 0 || c <= 0) throw new NumberFormatException();
                    workMinutes = w; shortBreakMinutes = s; longBreakMinutes = l; cyclesUntilLongBreak = c;
                    remainingSeconds = getModeDurationSeconds(currentMode);
                    updateUI();
                } catch (NumberFormatException ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Please enter positive integer values.", ButtonType.OK);
                    err.showAndWait();
                }
            }
        });
    }



    public static void main(String[] args) {
        launch(args);
    }
}

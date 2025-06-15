package museo;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import com.google.gson.Gson;

public class PlayVideo extends Application {

  private enum State {
    BLACK_SCREEN,
    SHOW_THUMBNAIL,
    PLAYING_VIDEO
  }

  private State currentState = State.BLACK_SCREEN;
  private javafx.scene.control.Label noContentLabel;

  private Rectangle blackScreen;
  private MediaPlayer mediaPlayer;
  private MediaView mediaView;
  private ImageView thumbnailView;

  private StackPane root;

  private String activeVideoPath = null;
  private String activeThumbnailPath = null;
  private String activeVideoName = null;

  private List<LightConfig> lightConfigList = new ArrayList<>();

  @Override
  public void start(Stage primaryStage) throws Exception {

    createScene(primaryStage);
    System.out.println("scena creata");

    new Thread(() -> {
      String user = System.getProperty("user.name");

      while (!loadMediaFromUsb(user)) {
        try {
          Thread.sleep(2000); // attesa per non sovraccaricare la CPU
        } catch (InterruptedException e) {
          return;
        }
      }

      // Quando trova i media, aggiorna la GUI
      Platform.runLater(() -> {
        createComponent(); // carica contenuti multimediali
        System.out.println("Componenti creati");
        primaryStage.setOnCloseRequest(event -> event.consume());

        EmbeddedSubsystem raspi;
        try {
          raspi = new EmbeddedSubsystem();
          /*
           * raspi.onTriggered(() -> {
           * System.out.println("BUTTON TRIGGERED");
           * returnToBlackScreen();
           * });
           */

          raspi.spawnSonarDetector(() -> {
            System.out.println("SONAR TRIGGERED");
            triggered();
          });
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      });
    }).start();

  }

  private void createScene(Stage primaryStage) {

    // ROOT
    root = new StackPane();
    root.setAlignment(Pos.CENTER);

    Rectangle2D screenBounds = Screen.getPrimary().getBounds();
    double screenWidth = screenBounds.getWidth();
    double screenHeight = screenBounds.getHeight();

    // SCENE
    Scene scene = new Scene(root, screenWidth, screenHeight);
    scene.setFill(Color.BLACK);

    primaryStage.setScene(scene);
    primaryStage.setTitle("Museo - Player");
    primaryStage.setFullScreen(true);
    primaryStage.show();

  }

  private void createComponent() {
    // BLACK SCREEN
    blackScreen = new Rectangle(640, 360, Color.BLACK);
    blackScreen.setRotate(90);

    // MEDIA VIEW
    mediaView = new MediaView();
    mediaView.setRotate(90);
    mediaView.setPreserveRatio(true);
    mediaView.fitWidthProperty().bind(root.widthProperty());
    mediaView.fitHeightProperty().bind(root.heightProperty());

    // THUMBNAIL VIEW
    thumbnailView = new ImageView();
    if (activeThumbnailPath != null) {
      try {
        Image image = new Image("file:" + activeThumbnailPath, false);
        thumbnailView.setImage(image);
      } catch (Exception e) {
        System.out.println("Errore caricamento immagine: " + e.getMessage());
      }
    }
    thumbnailView.fitWidthProperty().bind(root.widthProperty());
    thumbnailView.fitHeightProperty().bind(root.heightProperty());

    thumbnailView.setPreserveRatio(true);
    thumbnailView.setRotate(90);

    currentState = State.SHOW_THUMBNAIL;

    // NO CONTENT LABEL
    /*
     * noContentLabel = new javafx.scene.control.Label("Nessun contenuto trovato");
     * noContentLabel.setTextFill(Color.WHITE);
     * noContentLabel.setStyle("-fx-font-size: 24px;");
     * noContentLabel.setRotate(90);
     * noContentLabel.setVisible(false);
     */

    root.getChildren().addAll(thumbnailView, mediaView);

    if (activeVideoPath != null) {
      createVideo();
    }

    updateViewForState();
  }

  private void createVideo() {
    if (mediaPlayer != null) {
      mediaPlayer.stop();
      mediaPlayer.dispose();
    }

    Media media = new Media(new File(activeVideoPath).toURI().toString());
    mediaPlayer = new MediaPlayer(media);
    mediaPlayer.setAutoPlay(false);
    mediaPlayer.setVolume(1.0);
    mediaView.setMediaPlayer(mediaPlayer);

    mediaPlayer.setOnPlaying(() -> notifyEvent("triggered"));

    mediaPlayer.setOnEndOfMedia(() -> {
      System.out.println("Video terminato, ripristino miniatura.");
      notifyEvent("ended");
      mediaPlayer.stop();
      currentState = State.SHOW_THUMBNAIL;
      updateViewForState();
    });

  }

  public void triggered() {
    Platform.runLater(() -> {
      switch (currentState) {
        case BLACK_SCREEN:
          currentState = State.SHOW_THUMBNAIL;
          break;
        case SHOW_THUMBNAIL:
          if (mediaPlayer != null) {
            mediaPlayer.play();
            currentState = State.PLAYING_VIDEO;
          }
          break;
        case PLAYING_VIDEO:
          // No action
          break;
      }
      updateViewForState();
    });
  }

  public void returnToBlackScreen() {
    Platform.runLater(() -> {
      if (mediaPlayer != null) {
        mediaPlayer.stop();
      }
      currentState = State.BLACK_SCREEN;
      updateViewForState();
    });
  }

  private void updateViewForState() {
    // blackScreen.setVisible(currentState == State.BLACK_SCREEN);
    thumbnailView.setVisible(currentState == State.SHOW_THUMBNAIL);
    mediaView.setVisible(currentState == State.PLAYING_VIDEO);
  }

  private Boolean loadMediaFromUsb(String user) {
    List<File> searchDirs = searchingDir(user);
    Boolean videoFinded = false;
    Boolean imgFinded = false;
    Boolean lightJsonFinded = false;

    for (File dir : searchDirs) {
      File[] videoFiles = dir.listFiles((f) -> f.isFile() && f.getName().toLowerCase().endsWith(".mp4"));
      if (videoFiles != null && videoFiles.length > 0) {
        File videoFile = videoFiles[0];
        activeVideoPath = videoFile.getAbsolutePath();
        activeVideoName = videoFile.getName();
        System.out.println("Trovato video: " + activeVideoPath);
        videoFinded = true;
      }
      File[] imageFiles = dir.listFiles((f) -> f.isFile() && f.getName().toLowerCase().endsWith(".jpg"));
      if (imageFiles != null && imageFiles.length > 0) {
        activeThumbnailPath = imageFiles[0].getAbsolutePath();
        System.out.println("Trovata immagine: " + (activeThumbnailPath != null ? activeThumbnailPath : "nessuna"));
        imgFinded = true;
      }
      File configFile = new File(dir, "light.json");
      if (configFile.exists()) {
        try {
          String json = new String(Files.readAllBytes(configFile.toPath()));
          Gson gson = new Gson();
          LightConfig[] configArray = gson.fromJson(json, LightConfig[].class);
          lightConfigList = Arrays.asList(configArray);
          System.out.println("Caricate " + lightConfigList.size() + " configurazioni luci.");
          lightJsonFinded = true;
        } catch (Exception e) {
          System.err.println("Errore nella lettura di light.json: " + e.getMessage());
          lightConfigList = new ArrayList<>();
        }
      }

      return videoFinded && imgFinded && lightJsonFinded;
    }
    return false;
  }

  public static List<File> searchingDir(String user) {
    List<File> searchDirs = new ArrayList<>();

    File mediaRoot = new File("/media/" + user);
    File mntRoot = new File("/mnt/");

    if (mediaRoot.exists()) {
      File[] mediaDevices = mediaRoot.listFiles(File::isDirectory);
      if (mediaDevices != null) {
        searchDirs.addAll(Arrays.asList(mediaDevices));
      }
    }

    if (mntRoot.exists()) {
      File[] mntDevices = mntRoot.listFiles(File::isDirectory);
      if (mntDevices != null) {
        searchDirs.addAll(Arrays.asList(mntDevices));
      }
    }

    return searchDirs;
  }

  private void notifyEvent(String event) {
    try {
      URL url = new URL("http://localhost:8080/event/" + event);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setDoOutput(true);

      String json = "[]"; // default: niente luci
      if (event.equals("triggered")) {
        Gson gson = new Gson();
        json = gson.toJson(lightConfigList);
      }

      conn.getOutputStream().write(json.getBytes());

      conn.getResponseCode(); // forza la connessione
      conn.disconnect();
      System.out.println("Evento '" + event + "' notificato con payload: " + json);
    } catch (IOException e) {
      System.err.println("Errore invio evento '" + event + "': " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}

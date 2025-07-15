package museo;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
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
import museo.UsbMediaScanner.ScanResult;

import com.google.gson.Gson;

public class PlayVideoFX extends Application {

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

    // L'applicazione deve partire quando viene trovata una USB concontenuto valido.
    // Il thread JavaFx non puù però stare fermo in attesa, quindi viene creato un
    // nuovo thread solo per questa fase iniziale di atttesa
    new Thread(() -> {
      String user = System.getProperty("user.name");

      ScanResult result = null;

      while (result == null || !result.isComplete()) {
        result = UsbMediaScanner.scanForMedia(user);
        try {
          Thread.sleep(2000); // attesa per non sovraccaricare la CPU
        } catch (InterruptedException e) {
          return;
        }
      }

      // Quando trova i media, aggiorna la GUI
      ScanResult finalResult = result;

      Platform.runLater(() -> {
        this.activeVideoPath = finalResult.videoPath;
        this.activeVideoName = finalResult.videoName;
        this.activeThumbnailPath = finalResult.thumbnailPath;
        this.lightConfigList = finalResult.lightConfigList;

        createComponent(); // carica contenuti multimediali

        System.out.println("Componenti creati");
        primaryStage.setOnCloseRequest(event -> event.consume());

        EmbeddedSubsystem raspi;
        try {
          raspi = new EmbeddedSubsystem();
          /*
           * raspi.onTriggered(() -> {
           * System.out.println("BUTTON TRIGGERED");
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

  // Formiamo la GUI singolarmente, il risultato finale sarà :
  // Scene -> Root ->BlackRectangle, MediaView, ThumbnailView
  // MediaView -> MediaPlayer -> Media
  // ThumbnailView -> Image
  // Quindi iniziamo creando la finestra GUI, a cui aggiungiamo mano a mano i 3
  // componenti.
  // In base a cosa deve mostrare rende visibile un elemento e nasconde gli altri

  // Metodo per creare la finestra della GUI
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

  // Metodo per aggiungere alla finestra le componenti (Img,video,black screen)
  private void createComponent() {
    // BLACK SCREEN
    blackScreen = new Rectangle(640, 360, Color.BLACK);
    blackScreen.setRotate(90);

    // MEDIA VIEW
    mediaView = new MediaView();
    mediaView.fitWidthProperty().bind(root.widthProperty());
    mediaView.fitHeightProperty().bind(root.heightProperty());
    mediaView.setPreserveRatio(true);
    mediaView.setRotate(90);

    // THUMBNAIL VIEW
    thumbnailView = new ImageView();
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
    if (activeThumbnailPath != null) {
      createThumbnail();
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

  private void createThumbnail() {
    if (activeThumbnailPath != null) {
      try {
        Image image = new Image("file:" + activeThumbnailPath, false);
        thumbnailView.setImage(image);
      } catch (Exception e) {
        System.out.println("Errore caricamento immagine: " + e.getMessage());
      }
    }
  }

  // Metodo chiamato quando il Sonar viene triggerato, cambai lo stato e la
  // visibilità delle componenti della GUI
  public void triggered() {
    Platform.runLater(() -> {
      switch (currentState) {
        case BLACK_SCREEN:
          currentState = State.SHOW_THUMBNAIL;
          break;
        case SHOW_THUMBNAIL:
          if (mediaPlayer != null) {
            // mediaPlayer.play();
            // currentState = State.PLAYING_VIDEO;
            playGpu();
          }
          break;
        case PLAYING_VIDEO:
          // Il trigger viene ignorato se il video è in riproduzione
          break;
      }
      updateViewForState();
    });
  }

  private void updateViewForState() {
    // blackScreen.setVisible(currentState == State.BLACK_SCREEN);
    thumbnailView.setVisible(currentState == State.SHOW_THUMBNAIL);
    mediaView.setVisible(currentState == State.PLAYING_VIDEO);
  }

  // metodo usato pe notificare il progetto primario (mqtt) tramite HTTP in
  // localhost
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

  private void playGpu() {
    new Thread(() -> {
      try {
        // Nasconde la miniatura
        Platform.runLater(() -> {
          currentState = State.PLAYING_VIDEO;
          // updateViewForState();
          notifyEvent("triggered");
        });

        // Avvia il video con omxplayer in fullscreen
        ProcessBuilder pb = new ProcessBuilder(
            "mpv",
            "--fs",
            "--no-border",
            "--osd-level=0",
            "--vo=gpu",
            "--video-rotate=90",
            "--audio-device=alsa/sysdefault:CARD=vc4hdmi",
            activeVideoPath);
        pb.inheritIO();
        Process videoProcess = pb.start();
        videoProcess.waitFor();

        // Al termine, torna alla miniatura e notifica
        Platform.runLater(() -> {
          currentState = State.SHOW_THUMBNAIL;
          // updateViewForState();
          notifyEvent("ended");
        });

      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }).start();

  }

  public static void main(String[] args) {
    launch(args);
  }
}

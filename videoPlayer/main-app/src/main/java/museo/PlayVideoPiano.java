package museo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

import museo.UsbMediaScanner.ScanResult;

public class PlayVideoPiano {

    // Lo schermo mostra un immagine SHOW_THUMBNAIL, poi viene riprodotto il video
    // PLAYING_VIDEO, poi essendo la stazione del pianoforte al secondo sonar deve
    // comunque notificare il trigger SECOND_TRIGGER e poi la stazione viene
    // disabilitata mostrando schermata nera DISABLED
    private enum State {
        SHOW_THUMBNAIL,
        PLAYING_VIDEO,
        SECOND_SONAR,
        DISABLED
    }

    private State currentState = State.SHOW_THUMBNAIL;

    private String activeVideoPath = null;
    private String activeThumbnailPath = null;
    private String activeVideoName = null;

    private String blackPath = "image/black.jpeg";

    private List<LightConfig> lightConfigList = new ArrayList<>();

    public static void main(String[] args) {
        new PlayVideo().start();
    }

    public void start() {
        System.out.println("Avvio sistema...");

        String user = System.getProperty("user.name");
        ScanResult result = null;

        // non parte fino a quando non trova una USB con i path al .mp4, .jpg e il json
        // di configurazione

        while (result == null || !result.isComplete()) {
            result = UsbMediaScanner.scanForMedia(user);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
        }

        this.activeVideoPath = result.videoPath;
        this.activeVideoName = result.videoName;
        this.activeThumbnailPath = result.thumbnailPath;
        this.lightConfigList = result.lightConfigList;

        try {
            warmup();
            showThumbnail();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            EmbeddedSubsystem raspi = new EmbeddedSubsystem();
            raspi.spawnSonarDetector(() -> {
                System.out.println("SONAR TRIGGERED");
                triggered();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showThumbnail() throws InterruptedException {
        currentState = State.SHOW_THUMBNAIL;
        if (activeThumbnailPath != null) {
            try {
                String rotatedPath = "/tmp/rotated_thumbnail.jpg";
                new ProcessBuilder(
                        "convert", activeThumbnailPath, "-rotate", "90", rotatedPath).start().waitFor();

                new ProcessBuilder("feh", "-F", "-Z", rotatedPath).start();
            } catch (IOException e) {
                System.err.println("Errore visualizzazione miniatura: " + e.getMessage());
            }
        }
    }

    private void warmup() {
        // All'inizio apre e chiude subito i due processi per "riscaldare" l'ambiente,
        // dalla seconda volta l'aperturà sarà infatti più veloce
        try {
            System.out.println("Esecuzione warm-up di feh e mpv...");

            // Mostra un'immagine con feh e la chiude subito
            new ProcessBuilder("bash", "-c", "feh -F -Z " + activeThumbnailPath + " & sleep 1 && pkill feh").start()
                    .waitFor();

            // Esegue mpv e lo chiude subito
            new ProcessBuilder("bash", "-c",
                    "mpv --fs --no-audio --video-rotate=90 " + activeVideoPath + " & sleep 1 && pkill mpv").start()
                    .waitFor();

            System.out.println("Warm-up completato.");
        } catch (Exception e) {
            System.err.println("Errore nel warm-up: " + e.getMessage());
        }
    }

    private void triggered() {
        if (currentState == State.SHOW_THUMBNAIL && activeVideoPath != null) {
            playVideo();
        } else if (currentState == State.SECOND_SONAR) {
            // la seconda volta lo notifica comunque che è stato triggerato ma non
            // fa più partire video
            notifyEvent("triggered");
            notifyEvent("ended");
            currentState = State.DISABLED;
        }
    }

    private void playVideo() {
        currentState = State.PLAYING_VIDEO;

        try {
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
            Thread.sleep(3000);
            notifyEvent("triggered");

            videoProcess.waitFor();

            currentState = State.SECOND_SONAR;
            showBlack();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showBlack() throws InterruptedException {

        try {
            Runtime.getRuntime().exec("pkill feh");
            new ProcessBuilder("feh", "-F", "-Z", blackPath).start();
        } catch (IOException e) {
            System.err.println("Errore visualizzazione miniatura: " + e.getMessage());
        }

    }

    private void notifyEvent(String event) {
        try {
            URL url = new URL("http://localhost:8080/event/" + event);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = "[]";
            if (event.equals("triggered")) {
                Gson gson = new Gson();
                json = gson.toJson(lightConfigList);
            }

            conn.getOutputStream().write(json.getBytes());
            conn.getResponseCode();
            conn.disconnect();
            System.out.println("Evento '" + event + "' notificato con payload: " + json);
        } catch (IOException e) {
            System.err.println("Errore invio evento '" + event + "': " + e.getMessage());
        }
    }
}

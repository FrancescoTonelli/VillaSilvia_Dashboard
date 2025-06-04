package com.example.audioplayer;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;

public class AudioPlayer {
    private MediaPlayer mediaPlayer;

    // Metodo per avviare la riproduzione dell'audio
    public void start(String filename) {
        // Crea un oggetto Media e MediaPlayer per il file audio
        File file = new File("audios/" + filename);
        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnEndOfMedia(() -> {
            System.out.println("Riproduzione terminata!");
            stop();
        });
        mediaPlayer.play();
        System.out.println("Riproduzione avviata: " + filename);
    }

    // Metodo per fermare la riproduzione
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            System.out.println("Riproduzione fermata");
        }
    }

    // Metodo per mettere in pausa la riproduzione
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            System.out.println("Riproduzione in pausa");
        }
    }

    // Metodo per riprendere la riproduzione (resume)
    public void resume() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            System.out.println("Riproduzione ripresa");
        }
    }

    // Metodo per impostare il volume
    public void setVolume(double volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume); // da 0.0 a 1.0
            System.out.println("Volume impostato a " + (int) (volume * 100) + "%");
        }
    }

    // Metodo per ottenere il volume corrente
    public double getVolume() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVolume();
        }
        return 0.0;
    }

    public MediaPlayer.Status getStatus() {
        if (mediaPlayer != null) {
            return mediaPlayer.getStatus();
        }
        return MediaPlayer.Status.STOPPED;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}

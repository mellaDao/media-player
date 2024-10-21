import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.io.File;
import java.util.List;

public class Main extends Application {
    FileChooser fileChooser;

    public void start(final Stage primaryStage) {
        fileChooser = new FileChooser();

        //configure file chooser to only allow selection of music files
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Music Files", "*.mp3", "*.wav")
        );

        //show file chooser to select multiple music files
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);

        //create a scene with the player and set it to the primary stage
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            Player player = new Player(selectedFiles);

            Scene scene = new Scene(player, 650, 500);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Media Player");
            primaryStage.show();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
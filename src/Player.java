import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import java.io.File;
import java.util.List;


public class Player extends BorderPane {
    Media media;
    MediaPlayer player;
    MediaView mediaView;
    Pane mediaPane;
    MediaBar bar;
    ListView<String> fileListView;
    ObservableList<String> fileNames;
    
    //create labels with default values if needed
    private Label artist = new Label("Unknown Artist");
    private Label album = new Label("Unknown Album");
    private Label title = new Label("Unknown");
    private Label year = new Label();
    private ImageView albumCover = new ImageView();

    public Player(List<File> files) {
    	//set up the initial media player with the first file in the list
        File initialFile = files.get(0);
        String initialFilePath = initialFile.toURI().toString();
        media = new Media(initialFilePath);
        player = new MediaPlayer(media);
        
        mediaView = new MediaView(player);
        mediaPane = new Pane();
        bar = new MediaBar(player, files, this); //pass the player and file list to the MediaBar
        ObservableMap<String,Object> meta_data = media.getMetadata();
        
        //default title will be the file name
        String titleName = initialFile.getName();
        title.setText(titleName);
        
        formatLabels();
        
        //vbox for other song info (artist, album, year)
        VBox songInfoVbox = new VBox(10);
        songInfoVbox.getChildren().addAll(artist, album, year);
        BorderPane.setMargin(songInfoVbox, new Insets(0, 20, 20, 20));

        //borderpane to hold song info components
        BorderPane songInfoPane = new BorderPane();
        BorderPane.setAlignment(songInfoPane, Pos.CENTER);
        songInfoPane.setTop(title); 
        songInfoPane.setLeft(songInfoVbox);
        BorderPane.setMargin(title, new Insets(0, 100, 20, 20));
        songInfoPane.setPadding(new Insets(10, 10, 10, 10));
        
        //borderpane to hold song info components and album cover
        BorderPane topPane = new BorderPane();
        topPane.setLeft(albumCover);
        topPane.setCenter(songInfoPane);
        topPane.setPadding(new Insets(10, 10, 10, 10));
        
        //borderpane for entire player
        setCenter(mediaPane);
        setBottom(bar);
        setTop(topPane);
        setStyle("-fx-background-color:#AAB3C1");

        //play player status to play
        player.play();
        
        //initialize the list view of all files selected by user
        initializeListView(files);
        
        //mouse click event on the list view
        fileListView.setOnMouseClicked((MouseEvent event) -> {
        	//if user double clicks on a different song
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
            	//retrieves the selected index from the ListView
                int selectedIndex = fileListView.getSelectionModel().getSelectedIndex();
                //check if index is valid
                if (selectedIndex >= 0 && selectedIndex < files.size()) {
                    File selectedFile = files.get(selectedIndex);
                    String selectedFilePath = selectedFile.toURI().toString();
                    media = new Media(selectedFilePath);
                    //stop the current media player and create a new one with the selected file
                    player.stop();
                    player = new MediaPlayer(media);
                    bar.setMediaPlayer(player); 					//set the new MediaPlayer in the MediaBar
                    mediaView.setMediaPlayer(player);
                    
                    player.setOnReady(() -> {
                    	bar.resetTimeSlider();						//reset time slider
                    	player.seek(Duration.ZERO); 				//reset the playback position to the beginning
                        player.play();								//start playing the new song
                        ObservableMap<String, Object> newMetadata = media.getMetadata();
                        updateSongInfoPane(newMetadata, files);		//update the metadata for the new song
                    });	
                }
            }
        });
        
        
        //when song is played, retrieve the metadata and update labels
        meta_data.addListener(new MapChangeListener<String,Object>(){
            @Override
            public void onChanged(Change<? extends String, ? extends Object> change) { 
            	//if change was an addition
               if(change.wasAdded()){
                     String key = change.getKey();						//retrieve the key of added entry
                     Object value = change.getValueAdded();				//retrieve value of added entry
                     final Reflection reflection = new Reflection();	
                     reflection.setFraction(0.4);						//add a reflection of the album cover
                     albumCover.setFitWidth(250);
                     albumCover.setFitHeight(250);
                     
                     //if else statements to determine what type of data the key is and assign string value to the corresponding label
                     if (key.equals("album")) {
                         album.setText(value.toString());
                       } else if (key.equals("artist")) {
                         artist.setText(value.toString());
                       } if (key.equals("title")) {
                         title.setText(value.toString());
                         mediaPane.requestLayout();
                         System.out.println(key);
                         System.out.println(value.toString());
                       } if (key.equals("year")) {
                         year.setText(value.toString());
                       } if (key.equals("image")) {
                         albumCover.setImage((Image)value);
                         albumCover.setFitWidth(250);
                         albumCover.setFitHeight(250);
                         albumCover.setPreserveRatio(true);
                         albumCover.setSmooth(true);
                         albumCover.setEffect(reflection);
                       }
                       setFonts();
                }
            }
       });
        
        player.setOnEndOfMedia(() -> {
            //current song is over, play the next song
            playNextSong(files);
        });

        
    }
    
    public void initializeListView(List<File> files)
    {
        //create a list view to display the selected music files
        fileListView = new ListView<>();
        fileNames = FXCollections.observableArrayList();

        //add the file names to the list view
        for (File file : files) {
            fileNames.add(file.getName());
        }
        fileListView.setItems(fileNames);
        setCenter(fileListView);   
    }

    private void updateSongInfoPane(ObservableMap<String, Object> metadata, List<File> files) {
    	//add a reflection to the album cover
        final Reflection reflection = new Reflection();
        reflection.setFraction(0.4);
        //get the file name as a string
        int selectedIndex = fileListView.getSelectionModel().getSelectedIndex();
        File selectedFile = files.get(selectedIndex);

        //default values
        String defaultAlbum = "Unknown Album";
        String defaultArtist = "Unknown Artist";
        String defaultTitle = selectedFile.getName();	//default title will be the file name
        String defaultYear = null;
        Image defaultImage = null;

        //if else statements to determine what type of data the key is and assign string value to the corresponding label
        //default values are also assigned if there is no available meta data
        if (metadata.containsKey("album")) {
            album.setText(metadata.get("album").toString());
        } else {
            album.setText(defaultAlbum);
        }

        if (metadata.containsKey("artist")) {
            artist.setText(metadata.get("artist").toString());
        } else {
            artist.setText(defaultArtist);
        }

        if (metadata.containsKey("title")) {
            title.setText(metadata.get("title").toString());
        } else {
            title.setText(defaultTitle);
        }

        if (metadata.containsKey("year")) {
            year.setText(metadata.get("year").toString());
        } else {
            year.setText(defaultYear);
        }

        if (metadata.containsKey("image")) {
            albumCover.setImage((Image) metadata.get("image"));
            albumCover.setFitWidth(250);
            albumCover.setFitHeight(250);
            albumCover.setPreserveRatio(true);
            albumCover.setSmooth(true);
            albumCover.setEffect(reflection);
        } else {
            albumCover.setImage(defaultImage);
            albumCover.setFitWidth(250);
            albumCover.setFitHeight(250);
        }
        
        setFonts();
    }
    
    public void playNextSong(List<File> files) {
        int selectedIndex = fileListView.getSelectionModel().getSelectedIndex();
        int nextIndex;

        if (selectedIndex == -1) {
            nextIndex = selectedIndex + 2; //offset for the first song
        } else {
            nextIndex = selectedIndex + 1; //offset for subsequent songs
        }

        if (nextIndex < files.size()) {
            File nextFile = files.get(nextIndex);
            String nextFilePath = nextFile.toURI().toString();

            media = new Media(nextFilePath);
            player.stop();
            player = new MediaPlayer(media);
            bar.setMediaPlayer(player);
            mediaView.setMediaPlayer(player);

            player.setOnReady(() -> {
                bar.resetTimeSlider();
                player.seek(Duration.ZERO);
                fileListView.getSelectionModel().select(nextIndex);
                ObservableMap<String, Object> newMetadata = media.getMetadata();
                updateSongInfoPane(newMetadata, files);
                player.play();
            });

            player.setOnEndOfMedia(() -> {
                playNextSong(files);
            });
        }
    }
    
    public void playPreviousSong(List<File> files) {
        
        int selectedIndex = fileListView.getSelectionModel().getSelectedIndex();
        int previousIndex;
        
        previousIndex = selectedIndex - 1;

        if (previousIndex >= 0) {
            //play the previous song
            File previousFile = files.get(previousIndex);
            String previousFilePath = previousFile.toURI().toString();
            
            player.stop(); //stop the current song
            media = new Media(previousFilePath);
            player = new MediaPlayer(media);
            bar.setMediaPlayer(player);
            mediaView.setMediaPlayer(player);
            
            player.setOnReady(() -> {
                bar.resetTimeSlider();
                player.seek(Duration.ZERO);
                fileListView.getSelectionModel().select(previousIndex);
                ObservableMap<String, Object> newMetadata = media.getMetadata();
                updateSongInfoPane(newMetadata, files);
                player.seek(Duration.ZERO);
                player.play();
            });
        }
    }

    
    public void formatLabels() {
        //wrap the text for all labels
        artist.setWrapText(true);
        album.setWrapText(true);
        title.setWrapText(true);
        year.setWrapText(true);
        
        //set min and preferred widths for labels
        title.setMinWidth(100);
        artist.setPrefWidth(300);
        artist.setMinWidth(150);
        album.setPrefWidth(300);
        album.setMinWidth(150);
        year.setPrefWidth(300);
        year.setMinWidth(150);
    }
    
    public void setFonts() {
    	album.setFont(new Font("Arial", 15));
    	artist.setFont(new Font("Arial", 15));
    	title.setFont(new Font("Arial", 25));
    	year.setFont(new Font("Arial", 15));
        artist.setFont(new Font("Arial", 15));
    }
    
    
    //set up the media player
    public void setMediaPlayer(MediaPlayer player) {
        this.player = player;
        
    }

}
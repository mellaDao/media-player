import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.io.File;
import java.util.List;
public class MediaBar extends HBox {

	//sliders for time and volume
    Slider time = new Slider(); 			
    Slider vol = new Slider(); 			
    //button for pausing/unpausing player
    Button playButton = new Button("||");
    Button rewindButton = new Button("⟲");
    Button skipButton = new Button("⏭");
    Button previousButton = new Button("⏮");
    //labels
    Label volumeLabel = new Label("Volume: ");	
    Label volumeNumLabel = new Label(); 	
    Label timeLabel = new Label("00:00"); 

    MediaPlayer mediaPlayer;
    List<File> fileList;
    private Player playerInstance;

    public MediaBar(MediaPlayer play, List<File> files, Player player) {
    	mediaPlayer = play;
        fileList = files;
        playerInstance = player;

        setAlignment(Pos.CENTER); //setting the HBox to center
        setPadding(new Insets(12, 7, 12, 7)); //add padding
        
        changeFonts();
        childrenSize();
        initialSliderValues();
        
        HBox.setHgrow(time, Priority.ALWAYS);				//set time slider to expand horizontally and occupy any available space
        //setting margins for certain buttons
        //top, right, bottom, and left
        Insets insets = new Insets(0, 5, 0, 5);
        HBox.setMargin(playButton, insets);
        HBox.setMargin(rewindButton, insets);
        HBox.setMargin(skipButton, insets);
        HBox.setMargin(timeLabel, insets);
        HBox.setMargin(volumeNumLabel, insets);

        //add all children
        getChildren().addAll(playButton, rewindButton, previousButton, skipButton, time, timeLabel, volumeLabel, vol, volumeNumLabel);
        
        //when rewind button is pressed
        rewindButton.setOnAction(new EventHandler<ActionEvent>() {
        	public void handle(ActionEvent e) {
        		mediaPlayer.seek(Duration.ZERO);			//start from the beginning of the song
        		resetTimeSlider();					//reset the slider for time
        	}
        });

        //skip button is pressed
        skipButton.setOnMouseClicked(event -> {
            playerInstance.playNextSong(fileList);
            playButton.setText("||");
        });
        
        //previous button is pressed
        previousButton.setOnMouseClicked(event -> {
            playerInstance.playPreviousSong(fileList);
            playButton.setText("||");
        });
        
        //when play button is pressed
        playButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                Status status = mediaPlayer.getStatus();	//retrieving status (paused/unpaused)
                //if at end of song, restart the song
                if (status == Status.PLAYING) {
                    if (mediaPlayer.getCurrentTime().greaterThanOrEqualTo(mediaPlayer.getTotalDuration())) {
                    	mediaPlayer.seek(mediaPlayer.getStartTime());
                    	mediaPlayer.play();
                    } 
                    //if status is playing, pause the song and change the play button text to ">"
                    else {
                    	mediaPlayer.pause();
                        playButton.setText(">");
                    }
                } 
                //if status is halted/stopped/paused, unpause the song and set the play button text to "||"
                else if (status == Status.PAUSED) {
                	mediaPlayer.play();
                    playButton.setText("||");
                }
            }
        });
        
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
            	Status status = mediaPlayer.getStatus();	//retrieving status (paused/unpaused)
                //if at end of song, restart the song
                if (status == Status.PLAYING) {
                	mediaPlayer.pause();
                    playButton.setText(">");
                } 
                //if status is halted/stopped/paused, unpause the song and set the play button text to "||"
                else if (status == Status.PAUSED) {
                	mediaPlayer.play();
                    playButton.setText("||");
                }
            }
        });
        
        //adding functionality to time slider
        mediaPlayer.currentTimeProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                updateValues();
            }
        });

        //jumping to certain part of song
        time.valueProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                if (time.isPressed()) {
                	//set time to user press
                	mediaPlayer.seek(mediaPlayer.getMedia().getDuration().multiply(time.getValue() / 100));
                }
            }
        });

        //adding functionality to volume slider
        vol.valueProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                if (vol.isPressed()) {
                	//if pressed, change the volume to user press
                	mediaPlayer.setVolume(vol.getValue() / 500);				//set volume
                    volumeNumLabel.setText("" + (int)vol.getValue());	//update the label
                }
            }
        });
        
        updateTimeValues(); //method to constantly update time for time label
       
    }
    
    //thread for updating time
    protected void updateTimeValues() {
        Thread thread = new Thread(() -> {
            while (true) {
                updateTimeLabel();
                try {
                    Thread.sleep(100); //update every 100 milliseconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    //method to update the time label constantly using the thread
    protected void updateTimeLabel() {
    	if (time != null) {
            Platform.runLater(new Runnable() {
                public void run() {
                	//set time label text
                    timeLabel.setText(formatTime(mediaPlayer.getCurrentTime()) + " / " + formatTime(mediaPlayer.getTotalDuration()));
                }
            });
        }
    }
    
    //method to format time
    protected String formatTime(Duration duration) {
        int seconds = (int) duration.toSeconds();
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    //always updating time slider while song is playing
    protected void updateValues() {
        if (time != null) {
            Platform.runLater(new Runnable() {
                public void run() {
                	//moves time slider while playing song
                	time.setValue(mediaPlayer.getCurrentTime().toSeconds() / mediaPlayer.getTotalDuration().toSeconds() * 100);
                }
            });
        }
    }
    
    public void changeFonts() {
        //create new fonts for labels and buttons
        Font labelFont = new Font("Arial", 12);
        Font buttonFont = new Font("Arial", 15);
        
    	//assign fonts
        volumeLabel.setFont(labelFont);
        volumeNumLabel.setFont(labelFont);
        timeLabel.setFont(labelFont);
        playButton.setFont(buttonFont);
        rewindButton.setFont(buttonFont);
        skipButton.setFont(buttonFont);
        
        //setting font styles
        playButton.setStyle("-fx-font-weight: bold;");
        rewindButton.setStyle("-fx-font-weight: bold;");
        skipButton.setStyle("-fx-font-weight: bold;");
        
        previousButton.setFont(buttonFont);
        previousButton.setStyle("-fx-font-weight: bold;");
        previousButton.setPrefWidth(30);
        previousButton.setMaxWidth(Button.USE_PREF_SIZE);
    }
    
    public void childrenSize() {
    	//setting sizes of children
        vol.setPrefWidth(60);
        vol.setMinWidth(25);
        volumeNumLabel.setPrefWidth(30);
        volumeNumLabel.setMinWidth(30);
        playButton.setPrefWidth(30);
        rewindButton.setPrefWidth(30);
        playButton.setMaxWidth(Button.USE_PREF_SIZE);
        rewindButton.setMaxWidth(Button.USE_PREF_SIZE);
        skipButton.setPrefWidth(30);
        skipButton.setMaxWidth(Button.USE_PREF_SIZE);
    }
    
	public void initialSliderValues() {
        //setting initial values of sliders
        vol.setValue(100); 									//set the default volume to 100
        time.setValue(0);
        mediaPlayer.setVolume(vol.getValue() / 500); 		//set the initial MediaPlayer volume
        volumeNumLabel.setText("" + (int)vol.getValue());	//display the volume in a label
	}
    
    //setting up mediaplayer
	public void setMediaPlayer(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
		mediaPlayer.setVolume(vol.getValue() / 500); 		//set the MediaPlayer volume
		mediaPlayer.currentTimeProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                updateValues();  							//updating time slider as song plays
            }
        });
	}
	
	//resetting time slider
	public void resetTimeSlider() {
		time.setValue(0);
	}

}
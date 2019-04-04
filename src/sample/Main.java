package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * `Main` class is responsible for starting the GUI
 * application with window size boundaries, calculated
 * as half of the screen width and half of the screen
 * height
 * */
public class Main extends Application {
    /**
     * Main JavaFX function, starting the
     * GUI application
     *
     * @param primaryStage Internal JavaFX argument
     * @throws Exception   Exception, throws in case of
     *                     critical errors
     */
    @Override
    public void start(Stage primaryStage) throws Exception{
        // Setting application title
        primaryStage.setTitle("Food diary");

        /*
        * Binding application to GUI structure
        * file `sample.fxml`
        * */
        Parent root = FXMLLoader.load(
            getClass().getResource("sample.fxml")
        );

        // Getting screen visual bounds
        Rectangle2D primaryScreenBounds =
            Screen
                .getPrimary()
                .getVisualBounds();

        // Buffering visual screen width and height
        double screenWidth  = primaryScreenBounds.getWidth();
        double screenHeight = primaryScreenBounds.getHeight();

        /*
        * Setting `Scene` with half the screen
        * size in width and height
        * */
        primaryStage.setScene(new Scene(
            root,
            screenWidth*0.5,
            screenHeight*0.5
        ));

        primaryStage.show();
    }


    /**
     * Main function, launching the program
     *
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }
}

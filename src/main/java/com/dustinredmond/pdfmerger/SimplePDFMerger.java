package com.dustinredmond.pdfmerger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;

/**
 * A lightweight JavaFX application to facilitate in merging
 * multiple PDF documents into one.
 */
public class SimplePDFMerger extends Application {

    @SuppressWarnings("RedundantThrows")
    public void start(Stage stage) throws Exception {
        stage.setTitle(APP_TITLE);
        // Add application icon
        stage.getIcons().add(new Image(getClass().getResource("pdf.png").toExternalForm()));
        BorderPane root = new BorderPane();
        GridPane grid = new GridPane();
        // Main pane (GridPane) is set as center of our root pane (BorderPane)
        root.setCenter(grid);
        stage.setScene(new Scene(root, 600, 400));
        // Initialise GridPane with various JavaFX controls
        setupLayout(grid);
        // Add MenuBar to top of our BorderPane
        setupMenuBar(root);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();
    }

    private void setupLayout(GridPane grid) {
        // Setup spacing (gap) and padding for our GridPane
        grid.setVgap(5);
        grid.setHgap(5);
        grid.setPadding(new Insets(10));

        Button buttonAdd = new Button("Add PDF");
        Button buttonRemove = new Button("Remove PDF");
        Button buttonMerge = new Button("Merge PDFs");

        grid.add(new HBox(5, buttonAdd, buttonRemove, buttonMerge), 0, 0);
        // a ListView control will hold our PDF files as they are added
        // when merging, the files will be merged in the order they appear
        // on screen
        ListView<String> listView = new ListView<>();
        listView.setPlaceholder(new Label("Click \"Add PDF\" to select PDFs to merge."));
        grid.add(listView, 0, 1);

        // Add right-click ContextMenu for out ListView
        listView.setContextMenu(getContextMenu(listView));
        // If ListView doesn't contain anything or nothing is selected, don't show "remove" context menu item
        listView.setOnContextMenuRequested(e -> {
            // listView.getItems().get(1) will be our second ContextMenu item (Remove PDF)
            listView.getContextMenu().getItems().get(1).setDisable(listView.getItems().size() == 0 ||
                    listView.getSelectionModel().isEmpty());
        });

        // Ensure our ListView always resizes when window is resized
        GridPane.setVgrow(listView, Priority.ALWAYS);
        GridPane.setHgrow(listView, Priority.ALWAYS);

        buttonMerge.setDisable(true); // can't do merge or remove without PDFs
        buttonRemove.setDisable(true);

        // Wire up the actions for our buttons
        buttonAdd.setOnAction(e -> {
            handleAdd(listView);
            buttonMerge.setDisable(listView.getItems().size() < 2);
            buttonRemove.setDisable(listView.getItems().size() == 0);
        });
        buttonRemove.setOnAction(e -> {
            handleRemove(listView);
            buttonMerge.setDisable(listView.getItems().size() < 2);
            buttonRemove.setDisable(listView.getItems().size() == 0);
        });
        buttonMerge.setOnAction(e -> {
            try {
                handleMerge(listView);
                buttonMerge.setDisable(true);
            } catch (IOException ioException) {
                alert("Unable to merge the PDF documents. Ensure " +
                        "that all files are readable, and that you have enough" +
                        " free memory to process the conversion.");
            }
        });
    }

    /**
     * Construct a context menu (right-click menu) for our ListView
     * @param listView The ListView to which to add the context menu
     * @return A context menu suitable for our ListView
     */
    private ContextMenu getContextMenu(ListView<String> listView) {
        ContextMenu cm = new ContextMenu();

        MenuItem miAdd = new MenuItem("Add PDF");
        miAdd.setOnAction(e -> handleAdd(listView));
        MenuItem miDelete = new MenuItem("Remove PDF");
        miDelete.setOnAction(e -> handleRemove(listView));
        cm.getItems().addAll(miAdd,miDelete);
        return cm;
    }

    /**
     * Initialise the top of our BorderPane with a MenuBar
     * @param root The root BorderPane of our application
     */
    private void setupMenuBar(BorderPane root) {
        MenuBar menuBar = new MenuBar();

        // Setup "File" menu and its children
        Menu menuFile = new Menu("File");
        MenuItem miExit = new MenuItem("Exit");
        miExit.setOnAction(e -> Platform.exit());
        menuFile.getItems().add(miExit);
        menuBar.getMenus().add(menuFile);

        // Setup "Help" menu and its children
        Menu menuHelp = new Menu("Help");
        MenuItem miAbout = new MenuItem("About this program");
        miAbout.setOnAction(e -> showAbout());
        menuHelp.getItems().add(miAbout);
        menuBar.getMenus().add(menuHelp);

        // Set the MenuBar as the top of our BorderPane (root)
        root.setTop(menuBar);
    }

    /**
     * Display a message in the form of a dialog to the user
     * @param msg Message to be displayed in the dialog
     */
    private void alert(String msg) {
        // Display a dialog to the user
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setTitle(APP_TITLE);
        ImageView icon = new ImageView(ICON_PATH);
        icon.setFitHeight(64); // scale image for best fit
        icon.setFitWidth(64);
        alert.setGraphic(icon);
        // Get the Alert's window and add Icon to it
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image(ICON_PATH));
        alert.setHeaderText(""); // setting as empty String causes this section to collapse
        alert.showAndWait();
    }

    /**
     * Add the selected file to our ListView if user selects one
     * NOTE: The file will be null if user cancels FileChooser dialog
     * @param listView ListView showing chosen PDF files
     */
    private void handleAdd(ListView<String> listView) {
        File file = getFile(true);
        if (file != null) {
            listView.getItems().add(file.getAbsolutePath());
        }
    }

    /**
     * Remove the selected PDF file from the ListView
     * @param listView The ListView containig PDF file names.
     */
    private void handleRemove(ListView<String> listView) {
        if (!listView.getSelectionModel().isEmpty()) {
            listView.getItems().remove(listView.getSelectionModel().getSelectedItem());
        } else {
            alert("Please select a PDF file before attempting to remove it.");
        }
    }

    /**
     * Uses Apache's PDFBox library to merge the files in our ListView
     * into one PDF file
     * @param listView The ListView containing PDF files
     * @throws IOException If the files are unable to be read or written
     */
    private void handleMerge(ListView<String> listView) throws IOException {
        // Check that our ListView isn't empty
        if (listView.getItems().size() == 0) {
            alert("Please add some PDFs before merging.");
            return;
        }
        boolean successful = true;
        PDFMergerUtility merger = new PDFMergerUtility();
        for (String item : listView.getItems()) {
            try {
                // Add each file in the ListView to the PDFMergerUtility
                merger.addSource(item);
            } catch (FileNotFoundException ignored) {
                // If one file fails, consider entire operation a failure
                successful = false;
            }
        }
        alert("Please select a save file location.");
        File file = getFile(false);
        if (file != null && successful) {
            // If user chose a save file destination and nothing failed,
            // then merge the PDFs into chosen destination
            merger.setDestinationFileName(file.getAbsolutePath());
            merger.mergeDocuments(null);
            alert(String.format("PDFs merged into: %s", file.getName()));
        } else if (!successful) {
            // Will reach here if FileNotFoundException is caught
            alert("Unable to merge selected PDFs, ensure that they exist and that " +
                    "you have read permission for each file.");
        }
        // Clear the individual PDFs listing from our ListView
        listView.getItems().clear();
    }

    /**
     * Displays a JavaFX FileChooser suitable for choosing PDF files
     * @param open Shows an Open File dialog if true, otherwise shows a Save File dialog
     * @return Returns a file if the user chose one, otherwise null
     */
    private File getFile(boolean open) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        return open ? fc.showOpenDialog(null) : fc.showSaveDialog(null);
    }

    /**
     * Create a new JavaFX Stage and show our
     * about/licensing text
     */
    private void showAbout() {
        Stage stage = new Stage();
        stage.getIcons().add(new Image(ICON_PATH));
        stage.setTitle(APP_TITLE + " - About");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(25));
        root.setCenter(new Label(ABOUT_TEXT));
        stage.setScene(new Scene(root));
        stage.show();
    }

    /**
     * Main entry point for our application. The JRE8 should execute JavaFX's Application.launch
     * without this but leaving it in for future compatibility.
     * @param args A String array of arguments passed as command line parameters
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    private final String ICON_PATH = getClass().getResource("pdf.png").toExternalForm();
    private static final String APP_TITLE = "Simple PDF Merger";
    private final String ABOUT_TEXT = "Simple PDF Merger\nVersion: " +
            getClass().getPackage().getImplementationVersion() + "\n" +
            "\n" +
            "Copyright \u00A9 " + LocalDate.now().getYear() + " Dustin K. Redmond <dustin@dustinredmond.com>\n" +
            "\n" +
            "Permission is hereby granted, free of charge, to any person obtaining a copy\n" +
            "of this software and associated documentation files (the \"Software\"), to deal\n" +
            "in the Software without restriction, including without limitation the rights\n" +
            "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" +
            "copies of the Software, and to permit persons to whom the Software is\n" +
            "furnished to do so, subject to the following conditions:\n" +
            "\n" +
            "The above copyright notice and this permission notice shall be included in all\n" +
            "copies or substantial portions of the Software.\n" +
            "\n" +
            "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n" +
            "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" +
            "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" +
            "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" +
            "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n" +
            "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n" +
            "SOFTWARE.";

}

package sample;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.controlsfx.control.textfield.TextFields;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * `Controller` class is responsible for managing all the data
 * operations and interaction with the UI elements, that were
 * defined and structured in `sample.fxml`
 * */
public class Controller implements Initializable {
    // -- UI component bindings to `sample.fxml` section --
    // "New record" Tab
    public TextField                     foodOptionMassValueTextField;
    public TextField                     foodOptionSelectTextField;
    public Button                        selectFoodOptionButton;
    public TableView<RecordDBFoodOption> selectedFoodOptionsTableView;
    public Button                        saveSelectedItemsButton;
    public Button                        clearSelectedFoodOptionsButton;

    // "View records" Tab
    public TableView<RecordDBFoodOption> recordTableView;
    public Button                        refreshRecordTableViewButton;

    // "Statistics" Tab
    public Label    pieChartLabel;
    public PieChart lastRecordedDayNutritionProportionPieChart;
    public Button   refreshPieChartButton;

    // -- Cached data section --
    /**
     * Memory efficient cache of food options, that user selects
     * in the running session.
     *
     * Format:
     *  `LinkedList<String>`s contain food option data rows in
     *  Record DB data format without any headers.
     * */
    private LinkedList<LinkedList<String>> selectedFoodOptions
        = new LinkedList<>();

    /**
     * Memory efficient cache of food options, that are fetched
     * from the `SourceDB.csv` file.
     *
     * Represents all possible food options, user can choose from.
     *
     * Format:
     *  First `LinkedList<String>` contains header columns
     *  in Source DB data format.
     *
     *  Second and other `LinkedList<String>`s contain food option
     *  data rows in Source DB data format.
     * */
    private LinkedList<LinkedList<String>> sourceDBFetchedData
        = new LinkedList<>();

    /**
     * Memory efficient cache of food option names, that
     * are extracted from `sourceDBFetchedData`.
     *
     * Represents all possible food option names, for auto-complete
     * TextField `foodOptionSelectTextField`.
     *
     * Format:
     *  `LinkedList<String>` that contains food option names
     *  from `sourceDBFetchedData`, with corresponding indexes
     *  decremented by 1, because it excludes
     *  `sourceDBFetchedData` header column "Food name".
     * */
    private LinkedList<String> sourceDBFetchedFoodNames
        = new LinkedList<>();

    /**
     * Memory efficient cache of food options, that are fetched
     * from the `RecordDB.csv` file.
     *
     * Represents all food options saved previously by user.
     *
     * Format:
     *  First `LinkedList<String>` contains header columns
     *  in Record DB data format.
     *
     *  Second and other `LinkedList<String>`s contain food option
     *  data rows in Record DB data format.
     * */
    private LinkedList<LinkedList<String>> recordDBFetchedData
        = new LinkedList<>();

    // -- DB files section --
    /**
     * Represents a `File` object of a DB containing all possible
     * food options
     * */
    private File sourceDBFile = new File("SourceDB.csv");

    /**
     * Represents a `File` object of a DB containing all food
     * options with additional values of mass and date, saved by user
     * */
    private File recordDBFile = new File("RecordDB.csv");

    // -- DB column amounts section --
    private final int COLUMN_AMOUNT_IN_SOURCE_DB = 56;
    private final int COLUMN_AMOUNT_IN_RECORD_DB = 58;

    // -- Record DB headers section --
    /**
     * Memory efficient cache of Record DB headers, that are extracted
     * from `recordDBFetchedData`
     * */
    private LinkedList<String> recordDBHeaders = new LinkedList<>();

    // -- Alert section --
    /**
     * Main `Alert` object that is reused all over the program when
     * notifying the user about:
     *
     *  INFORMATION  - when selected options are successfully saved,
     *                 or when selected options are already cleared.
     *  WARNING      - when the program can not proceed with a certain
     *                 action, but conditions are normal.
     *  ERROR        - when the program can not continue to work due to
     *                 critical conditions.
     * */
    private Alert alert;

    /**
     * Intermediary representation of food data (Record DB format)
     * in an `ObservableList<LinkedList<SimpleStringProperty>>`
     * for `TableView` internals
     * */
    class RecordDBFoodOption {
        private LinkedList<SimpleStringProperty> foodOption;

        private RecordDBFoodOption(LinkedList<String> foodOption) {
            this.foodOption = new LinkedList<>();

            /*
            * Basic conversion from LinkedList<String> to
            * LinkedList<SimpleStringProperty>
            * */
            for (String fo : foodOption)
                this.foodOption.add(new SimpleStringProperty(fo));
        }

        LinkedList<SimpleStringProperty> getFoodOption() {
            return foodOption;
        }
    }

    /**
     * Main internal JavaFX GUI initialization method.
     *
     * @param url Internal JavaFX argument
     * @param rb  Internal JavaFX argument
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Error safe fetching of Source DB data
        sourceDBFetchedData = fetchDBData(sourceDBFile);

        // Handling absence of the Source DB file
        if (sourceDBFetchedData != null) {
            // -- Data initialization section --
            /*
                Extracting food names from `sourceDBFetchedData`

                `i = 1`, because first element (`i = 0`) contains
                DB column headers

                Food names are stored in the 3rd column in the
                Source DB
            */
            for (int i = 1; i < sourceDBFetchedData.size(); i++)
                sourceDBFetchedFoodNames.add(
                    sourceDBFetchedData.get(i).get(2)
                );

            // Initializing `recordDBHeaders`
            recordDBHeaders = getRecordDBFetchedDataHeaders(
                sourceDBFetchedData
            );

            /*
            * Initializing `selectedFoodOptionsTableView`
            * columns and their data dependencies
            * */
            initializeTableView(
                selectedFoodOptionsTableView,
                recordDBHeaders
            );

            /*
             * Initializing `recordTableView` columns and
             * their data dependencies
             * */
            initializeTableView(
                recordTableView,
                recordDBHeaders
            );

            // -- UI control bindings section --
            /*
             *  Auto-complete food option names' binding to the
             *  `foodOptionSelectTextField`
             * */
            TextFields.bindAutoCompletion(
                foodOptionSelectTextField, // Bind UI object
                sourceDBFetchedFoodNames   // Bind data
            );

            /*
             *  New food option from the `foodOptionSelectTextField`
             *  is added to `selectedFoodOptions` when `Enter` is
             *  pressed in `foodOptionSelectTextField`
             * */
            foodOptionSelectTextField.addEventFilter(
                KeyEvent.KEY_PRESSED,
                e -> {
                    if (e.getCode().equals(KeyCode.ENTER)) {
                        selectFoodOption();
                        refreshTableViewData(
                            selectedFoodOptionsTableView,
                            selectedFoodOptions,
                            recordDBHeaders
                        );
                    }
                }
            );

            /*
             *  New food option from the `foodOptionSelectTextField`
             *  is added to `selectedFoodOptions` when
             *  this button is clicked
             * */
            selectFoodOptionButton.addEventFilter(
                MouseEvent.MOUSE_CLICKED,
                e -> {
                    selectFoodOption();
                    refreshTableViewData(
                        selectedFoodOptionsTableView,
                        selectedFoodOptions,
                        recordDBHeaders
                    );
                }
            );

            /*
             * Safely saves `selectedFoodOptions`
             * into Record DB, if it exists, or creates a new
             * one, if it does not exist, when this button is
             * clicked
             * */
            saveSelectedItemsButton.addEventFilter(
                MouseEvent.MOUSE_CLICKED,
                e -> {
                    if (
                        saveSelectedFoodOptionsIntoDBFile(
                            selectedFoodOptions,
                            recordDBFile
                        ) == 1
                    ) {
                        selectedFoodOptions.clear();
                        refreshTableViewData(
                            selectedFoodOptionsTableView,
                            selectedFoodOptions,
                            recordDBHeaders
                        );

                        /*
                         * When selected options were saved successfully,
                         * alerts the user about it
                         * */
                        alert = new Alert(
                            Alert.AlertType.INFORMATION,
                            "Selected options were saved"
                        );
                        alert.show();
                    } else {
                        /*
                         * When `selectedFoodOptions` is empty,
                         * alerts the user about it
                         * */
                        alert = new Alert(
                            Alert.AlertType.WARNING,
                            "No food options selected"
                        );
                        alert.show();
                    }
                }
            );

            /*
             * Clears `selectedFoodOptions` and refreshes
             * `selectedFoodOptionsTableView` data, when
             * this button is clicked
             * */
            clearSelectedFoodOptionsButton.addEventFilter(
                MouseEvent.MOUSE_CLICKED,
                e -> {
                    if (!selectedFoodOptions.isEmpty()) {
                        selectedFoodOptions.clear();
                        refreshTableViewData(
                            selectedFoodOptionsTableView,
                            selectedFoodOptions,
                            recordDBHeaders
                        );
                    } else {
                        /*
                         * When `selectedFoodOptions` is empty,
                         * alerts the user about it
                         * */
                        alert = new Alert(
                            Alert.AlertType.INFORMATION,
                            "Selected options are " +
                            "already cleared"
                        );
                        alert.show();
                    }
                }
            );

            /*
             * Refreshes `recordTableView` and re-fetches
             * `recordDBFetchedData`, when this button is clicked
             * */
            refreshRecordTableViewButton.addEventFilter(
                MouseEvent.MOUSE_CLICKED,
                e -> {
                    recordDBFetchedData = fetchDBData(recordDBFile);

                    // Handling absence of Record DB file
                    if (recordDBFetchedData != null) {
                        refreshTableViewData(
                            recordTableView,
                            recordDBFetchedData,
                            recordDBHeaders
                        );
                    } else {
                        /*
                         * When there is no Record DB file,
                         * alerts the user about it
                         * */
                        alert = new Alert(
                            Alert.AlertType.WARNING,
                            "No records found"
                        );
                        alert.show();
                    }
                }
            );

            /*
             * Refreshes `recordTableView` and re-fetches
             * `recordDBFetchedData`, when this button is clicked
             * */
            refreshPieChartButton.addEventFilter(
                MouseEvent.MOUSE_CLICKED,
                e -> {
                    recordDBFetchedData = fetchDBData(recordDBFile);

                    // Handling absence of Record DB file
                    if (recordDBFetchedData != null) {
                        /*
                        * Handling case, when `recordDBFetchedData`
                        * contains only headers without data
                        * */
                        if (recordDBFetchedData.size() > 1) {
                            LinkedList<LinkedList<String>> lastRecordedDayFoodOptionList =
                                new LinkedList<>();

                            /**
                             * Last day when food data was saved by the user
                             *
                             * Format: "YYYY-MM-DD"
                             * */
                            String lastRecordedDay =
                                recordDBFetchedData
                                    .getLast()
                                    .getLast();

                            /*
                            * Getting all the rows from the end of the Record DB,
                            * that have the `lastRecordedDay` date value
                            * */
                            for (int i = recordDBFetchedData.size() - 1; i > 0; i--) {
                                if (
                                    recordDBFetchedData
                                        .get(i)
                                        .getLast()
                                        .equals(lastRecordedDay)
                                ) {
                                    lastRecordedDayFoodOptionList.add(
                                        new LinkedList<>()
                                    );

                                    for (String s : recordDBFetchedData.get(i)) {
                                        lastRecordedDayFoodOptionList
                                            .getLast()
                                            .add(s);
                                    }
                                }
                            }

                            /*
                            * Output of the `lastRecordedDay` date above
                            * the `PieChart`
                            * */
                            pieChartLabel.setText(
                                "Pie chart corresponds to date: " +
                                lastRecordedDay
                            );

                            // Initializing `PieChart`
                            initializePieChart(
                                lastRecordedDayNutritionProportionPieChart,
                                lastRecordedDayFoodOptionList,
                                recordDBHeaders
                            );
                        } else {
                            /*
                             * When there are no food options saved in
                             * the Record DB file, alerts the user about it
                             * */
                            alert = new Alert(
                                Alert.AlertType.WARNING,
                                "No records found"
                            );
                            alert.show();
                        }
                    } else {
                        /*
                         * When there is no Record DB file,
                         * alerts the user about it
                         * */
                        alert = new Alert(
                            Alert.AlertType.WARNING,
                            "No records found"
                        );
                        alert.show();
                    }
                }
            );
        } else {
            /*
            * When there is no Source DB file, alerts the user
            * about it and closes the program
            * */
            alert = new Alert(
                Alert.AlertType.ERROR,
                "No source file found"
            );
            alert.showAndWait();

            Platform.exit();
            System.exit(0);
        }
    }


    /**
     * Initializes `chart` object reference with the data
     * from `data` object with corresponding `headers` object.
     *
     * @param chart   `PieChart` reference to initialize.
     * @param data    Record DB food option row extract of the
     *                last saved date without any headers.
     * @param headers Record DB data headers to render.
     */
    private void initializePieChart(
        PieChart                       chart,
        LinkedList<LinkedList<String>> data,
        LinkedList<String>             headers
    ) {
        /**
         * Contains elements to be displayed on the pie
         * chart as `Double`s
         * */
        List<List<Double>> doubleData = new ArrayList<>();

        /**
         * Contains indexes of the headers to be
         * displayed on the pie chart
         * */
        List<List<Integer>> headerIndexes = new ArrayList<>();

        /**
         * Contains headers to be displayed on the
         * pie chart as `String`s
         * */
        List<String> headerTitles = new ArrayList<>();

        /**
         * Contains objects to be passed in the
         * `PieChart.setData()` method
         * */
        List<Double> totalDoubleData = new ArrayList<>();

        // Initializing JavaFX specific list representation
        ObservableList<PieChart.Data> observablePieChartDataList =
            FXCollections.observableArrayList();

        /*
        * Populating `doubleData` and `headerIndexes` lists
        * according to their description
        * */
        for (int i = 0; i < data.size(); i++) {
            List<Double>  doubleDataItem    = new ArrayList<>();
            List<Integer> headerIndexesItem = new ArrayList<>();

            for (int j = 0; j < data.get(i).size(); j++) {
                try {
                    if (j != 0 && j != 7 && j != 56) {
                        if (data.get(i).get(j).equals("NULL")) {
                            data.get(i).set(j, "0.0");
                        }

                        doubleDataItem.add(
                            Double.parseDouble(
                                data.get(i).get(j)
                            )
                        );

                        headerIndexesItem.add(j);
                    }
                } catch (Exception ignored) {}
            }

            headerIndexes.add(headerIndexesItem);
            doubleData.add(doubleDataItem);
        }

        /*
        * Populating `headerTitles` list using the data
        * in `headerIndexes` and `headers`
        * */
        for (int i : headerIndexes.get(0)) {
            try {
                headerTitles.add(headers.get(i));
            } catch (Exception e) {
                // Skipping unnecessary data
            }
        }

        /*
        * Modifying the data in `doubleData` in order
        * to normalize the data to one unit (grams)
        * */
        for (int i = 0; i < doubleData.size(); i++) {
            for (int j = 0; j < doubleData.get(i).size(); j++) {
                if (headerTitles.get(j).contains("(mg)")) {
                    doubleData
                        .get(i)
                        .set(
                            j,
                            doubleData.get(i).get(j) / 1000
                        );
                } else if (headerTitles.get(j).contains("(mcg)")) {
                    doubleData
                        .get(i)
                        .set(
                            j,
                            doubleData.get(i).get(j) / 1000000
                        );
                } else if (headerTitles.get(j).contains("(IU)")) {
                    doubleData
                        .get(i)
                        .set(
                            j,
                            doubleData.get(i).get(j) / 1000000 * 0.3
                        );
                }
            }
        }

        /*
        * Populating `totalDoubleData` according to its
        * description using the data from `doubleData`
        * after the normalization process
        * */
        for (int i = 0; i < doubleData.get(0).size(); i++) {
            Double totalDoubleDataItem = 0.0;

            for (int j = 0; j < doubleData.size(); j++) {
                try {
                    totalDoubleDataItem += doubleData.get(j).get(i);
                } catch (Exception ignored) {}
            }
            totalDoubleData.add(totalDoubleDataItem);
        }

        /*
        * Populating `observablePieChartDataList`
        * according to its description
        * */
        for (int i = 0; i < headerTitles.size(); i++) {
            observablePieChartDataList.add(
                new PieChart.Data(
                    headerTitles.get(i),
                    totalDoubleData.get(i)
                )
            );
        }

        /*
        * Updating the data to be shown on
        * the `chart`
        * */
        chart.setData(
            observablePieChartDataList
        );
    }


    /**
     * Initializes `tableView` object reference
     * with the `headerList` data.
     *
     * @param tableView  `TableView` reference to be
     *                   initialized.
     * @param headerList Table headers to render.
     */
    private void initializeTableView(
        TableView<RecordDBFoodOption> tableView,
        LinkedList<String>            headerList
    ) {
        /**
         * Memory efficient list of table columns to be
         * initialized in the `tableView`
         * */
        LinkedList<TableColumn<RecordDBFoodOption, String>> tableColumnList
            = new LinkedList<>();

        /*
        * Initializing `TableView` column instances into
        * `tableColumnList` and setting their data
        * dependencies via `TableColumn.setCellValueFactory()`
        * */
        for (int i = 0; i < headerList.size(); i++) {
            tableColumnList.add(
                new TableColumn<>(
                    headerList.get(i)
                )
            );

            final int I = i;

            tableColumnList
                .getLast()
                .setCellValueFactory(
                    p -> p
                        .getValue()
                        .getFoodOption()
                        .get(I)
                );
        }

        // Binding columns to the table
        tableView
            .getColumns()
            .addAll(
                tableColumnList
            );
    }


    /**
     * Refreshes `tableView` object's data with data of
     * `rowLists` and `headerList`.
     *
     * @param tableView  `TableView` to be refreshed.
     * @param rowLists   Table rows to render.
     * @param headerList Table headers to render.
     */
    private void refreshTableViewData(
        TableView<RecordDBFoodOption>  tableView,
        LinkedList<LinkedList<String>> rowLists,
        LinkedList<String>             headerList
    ) {
        /**
         * Required to identify whether passed data
         * contains headers row or not
         * */
        boolean skipFirstLine;

        /**
         * Memory efficient list of table rows to be
         * initialized in the `tableView`
         * */
        LinkedList<RecordDBFoodOption> foodOptionList = new LinkedList<>();

        // Initializing `skipFirstLine`
        if (rowLists.size() > 0) {
            skipFirstLine = rowLists
                .getFirst()
                .getFirst().equals(
                    headerList.getFirst()
                );
        } else {
            skipFirstLine = false;
        }

        // Initializing `foodOptionList` without headers
        for (
            int i = skipFirstLine ? 1 : 0;
            i < rowLists.size();
            i++
        ) {
            foodOptionList.add(
                new RecordDBFoodOption(rowLists.get(i))
            );
        }

        // Initializing JavaFX specific list representation
        ObservableList<RecordDBFoodOption> tableViewData =
            FXCollections.observableArrayList(
                foodOptionList
            );

        // Setting the new data
        tableView.setItems(tableViewData);
    }


    /**
     * Fetches DB data from `DBFile`.
     *
     * @param DBFile DB `File` to be fetched.
     * @return       Either reference to fetched memory
     *               efficient DB file data or `null`,
     *               if there is no file.
     */
    private LinkedList<LinkedList<String>> fetchDBData(File DBFile) {
        /**
         * DB data object to return
         * */
        LinkedList<LinkedList<String>> DBData = new LinkedList<>();

        // Handling DB file absence
        if (DBFile.exists()) {
            try {
                /**
                 * Main reader
                 * */
                BufferedReader br = new BufferedReader(
                    new FileReader(DBFile)
                );

                /**
                 * String buffer for lines in the `.csv` DB
                 * */
                String line;

                /**
                 * `line` decomposed into String[] of DB
                 * columns of the current row
                 * */
                String[] decomposedLine;

                while ((line = br.readLine()) != null) {
                    /**
                     *  `String[]` of extracted column data using data
                     *  specific regex for `.csv` DB
                     *
                     *  This regex works, if there is only
                     *  1 or 0 strings in a DB row
                     *
                     *  DO NOT USE THIS REGEX ANYWHERE ELSE !
                     * */
                    decomposedLine = line.split(
                        "((?!(\")),[\",])|(\",)|(,(?=(.+,\")))|(,(?!(.*\"))(?=(.+,,)))"
                    );

                    /*
                     * Converting `decomposedLine` to
                     * `LinkedList<String>` and saving it
                     * into `DBData`
                     * */
                    DBData.add(
                        new LinkedList<>(
                            Arrays.asList(
                                Arrays.copyOfRange(
                                    decomposedLine,
                                    0,
                                    decomposedLine.length
                                )
                            )
                        )
                    );
                }

                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return DBData;
        } else {
            return null; // No file was found return value
        }
    }

    /**
     * Adds 2 extra column header values to
     * `sourceDBFetchedData` headers and returns
     * the resulting value.
     *
     * @param sourceDBFetchedData Source DB data.
     * @return                    Composed Record DB headers.
     */
    private LinkedList<String> getRecordDBFetchedDataHeaders(
        LinkedList<LinkedList<String>> sourceDBFetchedData
    ) {
        /**
         * Record DB column headers to be returned
         * */
        LinkedList<String> recordDBFetchedDataHeaders =
            sourceDBFetchedData.getFirst();

        recordDBFetchedDataHeaders.add("Mass (g)");
        recordDBFetchedDataHeaders.add("Date");

        return recordDBFetchedDataHeaders;
    }


    /**
     * Main search algorithm of the program.
     *
     * @param foodNames List of all Source DB food
     *                  names.
     * @return          Source DB row index
     *                  incremented by 1 or an error
     *                  code:
     *  -1 - `foodOptionSelectTextField` is empty.
     *  -2 - no food option in the Source DB
     *       matches the entered food name.
     */
    private int getSelectedFoodOptionIndexInSourceDBFetchedFoodNames(
        LinkedList<String> foodNames
    ) {
        String searchTextFieldContent =
            foodOptionSelectTextField
                .getCharacters()
                .toString();

        /*
         * Handling case, when `foodOptionSelectTextField`
         * is empty
         * */
        if (searchTextFieldContent.isEmpty()) {
            return -1; // "No input" error code
        } else {
            for (int i = 0; i < foodNames.size(); i++) {
                if (
                    foodNames
                        .get(i)
                        .equals(searchTextFieldContent)
                ) {
                    return i + 1;
                }
            }
        }

        /*
        * Handling case, when no food option
        * in the Source DB matches the entered
        * food name
        * */
        return -2; // "No option found" error code
    }


    /**
     * Manages GUI food option selection.
     *
     * Handles improper usage and alerts
     * the user, if something goes wrong.
     */
    private void selectFoodOption() {
        /**
         * Has value of a Source DB row index incremented
         * by 1 or value of an error code
         * */
        int foodOptionSourceDBFetchedDataIndex =
            getSelectedFoodOptionIndexInSourceDBFetchedFoodNames(
                sourceDBFetchedFoodNames
            );

        /*
        * Handling error codes of
        * `getSelectedFoodOptionIndexInSourceDBFetchedFoodNames()`
        * */
        if (foodOptionSourceDBFetchedDataIndex > 0) {
            int foodOptionMassValue = getFoodOptionMassValue();

            /*
             * Handling error codes of
             * `getFoodOptionMassValue()`
             * */
            if (foodOptionMassValue > 0) {
                selectedFoodOptions.add(
                    sourceDBFetchedData.get(
                        foodOptionSourceDBFetchedDataIndex
                    )
                );

                selectedFoodOptions.getLast()       // Current food option
                    .add("" + foodOptionMassValue); // Adding selected food option mass

                // Grouping date initialization
                LocalDate currentSessionDate = LocalDate.now();

                selectedFoodOptions.getLast()            // Current food option
                    .add(currentSessionDate.toString()); // Adding grouping date
            } else if (foodOptionMassValue == -1)  {
                /*
                 * When there is no mass input,
                 * alerts the user about it
                 * */
                alert = new Alert(
                    Alert.AlertType.WARNING,
                    "No mass input"
                );
                alert.show();
            } else if (foodOptionMassValue == -2)  {
                /*
                 * When mass input is invalid,
                 * alerts the user about it
                 * */
                alert = new Alert(
                    Alert.AlertType.WARNING,
                    "Invalid mass input. " +
                    "Please enter a positive integer"
                );
                alert.show();
            }
        } else if (foodOptionSourceDBFetchedDataIndex == -1) {
            /*
             * When there is no food option name input,
             * alerts the user about it
             * */
            alert = new Alert(
                Alert.AlertType.WARNING,
                "No food option selected"
            );
            alert.show();
        } else if (foodOptionSourceDBFetchedDataIndex == -2) {
            /*
             * When there is no matching food option name
             * in the Source DB, alerts the user about it
             * */
            alert = new Alert(
                Alert.AlertType.WARNING,
                "Food option not found"
            );
            alert.show();
        }
    }

    /**
     * Reads `String` value of
     * `foodOptionMassValueTextField`, parses
     * it to `int`. In case it is impossible
     * to parse, or if the parsed value in
     * non-positive, returns an error code.
     *
     * @return positive `int` mass value in grams,
     *         or an error code:
     *  -1 - no input.
     *  -2 - invalid input or non-positive `int`.
     */
    private int getFoodOptionMassValue() {
        /**
         * Positive mass value in grams to be
         * returned or an error code
         * */
        int foodOptionMassValue;

        try {
            foodOptionMassValue = Integer.parseInt(
                foodOptionMassValueTextField
                    .getCharacters()
                    .toString()
                    .replaceAll("\\s|_", "")
            );

            return
                (foodOptionMassValue > 0)
                    ? foodOptionMassValue
                    : -2;
        } catch (NumberFormatException e) {
            if (
                foodOptionMassValueTextField
                    .getCharacters()
                    .toString()
                    .isEmpty()
            ) {
                return -1; // "No input" error code
            } else {
                return -2; // "Invalid input" error code
            }
        }
    }

    /**
     * Safely saves `selectedFoodOptions` into `recordDBFile`,
     * and return either success or error code.
     *
     * @param selectedFoodOptions Food option rows in
     *                            Source DB format to save.
     * @param recordDBFile        Record DB file to save to.
     * @return                    Either success or error
     *                            code:
     *   1 - success
     *  -1 - error
     */
    private int saveSelectedFoodOptionsIntoDBFile(
        LinkedList<LinkedList<String>> selectedFoodOptions,
        File                           recordDBFile
    ) {
        /**
         * Flag, identifying whether `selectedFoodOptions`
         * is an empty object
         * */
        boolean foodOptionsEmpty = selectedFoodOptions.isEmpty();

        /**
         * Flag, identifying whether `recordDBFile` exists
         * */
        boolean DBFileExists = recordDBFile.exists();

        // Handling absence of selected options
        if (!foodOptionsEmpty) {
            try {
                BufferedWriter bw = new BufferedWriter(
                    new FileWriter(
                        recordDBFile,
                        DBFileExists
                    )
                );

                StringBuffer sb = new StringBuffer();

                /*
                * Handling absence of Record DB file
                * data by adding Record DB column headers
                * to the empty file
                * */
                if (!DBFileExists) {
                    for (int i = 0; i < COLUMN_AMOUNT_IN_SOURCE_DB; i++) {
                        sb
                            .append(sourceDBFetchedData.get(0).get(i))
                            .append(",");
                    }
                    sb.append("Mass (g),Date,,\r\n");
                    bw.write(sb.toString());
                    sb.delete(0, sb.length());
                }

                /*
                * Safely adding (appending) selected
                * food options to the Record DB file
                * */
                for (int i = 0; i < selectedFoodOptions.size(); i++) {
                    for (int j = 0; j < COLUMN_AMOUNT_IN_RECORD_DB; j++) {
                        sb
                            .append((j == 2)
                                ? ("\"" + selectedFoodOptions.get(i).get(2) + "\"")
                                : (selectedFoodOptions.get(i).get(j))
                            )
                            .append(",");
                    }
                    sb.append(",\r\n");
                    bw.write(sb.toString());
                    sb.delete(0, sb.length());
                }

                bw.close();
            } catch (IOException e) {
                /*
                * When there is an error, while writing
                * to the Record DB file, notifies the user
                * */
                alert = new Alert(
                    Alert.AlertType.ERROR,
                    "Error while saving data"
                );
                alert.show();
            }

            return 1; // `OK` return code
        } else {
            return -1; // `Error` return code
        }
    }
}

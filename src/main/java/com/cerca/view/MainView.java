package com.cerca.view;

import java.awt.Desktop;
import java.net.URI;

import com.cerca.model.ReferenceItem;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MainView {

	private final BorderPane layout;
	private final TableView<ReferenceItem> table;
	private final Button verifyButton;
	private final ProgressBar progressBar;
	private final Label statusLabel;
	private final Button saveButton;
	private final MenuItem aboutItem;
	private final MenuItem sponsorItem;
	private final MenuItem contributeItem;
	private final MenuItem licenseItem;
	private VBox resultsDashboard;
	private Label totalBadge;
	private Label passedBadge;
	private Label failedBadge;
	private Label rateBadge;

	public Button getSaveButton() {
		return saveButton;
	}

	private final Label fileTitleLabel;

	public Label getFileTitleLabel() {
		return fileTitleLabel;
	}

	public MenuItem getAboutItem() {
		return aboutItem;
	}

	public MenuItem getSponsorItem() {
		return sponsorItem;
	}

	public MenuItem getContributeItem() {
		return contributeItem;
	}

	public MainView() {
		layout = new BorderPane();
		resultsDashboard = createResultsDashboard();


		VBox topContainer = new VBox(10);
		topContainer.setPadding(new Insets(15));
		topContainer.setAlignment(Pos.CENTER);
		topContainer.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-style: dashed;");

		Label icon = new Label("📂");
		icon.setFont(Font.font(30));
		Label instructions = new Label("Drag PDF Here");
		instructions.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

		topContainer.getChildren().addAll(icon, instructions);

		progressBar = new ProgressBar();
		progressBar.setMaxWidth(Double.MAX_VALUE);
		progressBar.setVisible(false);

	
		fileTitleLabel = new Label("");
		fileTitleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
		fileTitleLabel.setTextFill(Color.DARKSLATEGRAY);
		fileTitleLabel.setPadding(new Insets(5, 0, 5, 0));

		
		VBox topWrapper = new VBox(5, topContainer, fileTitleLabel, resultsDashboard, progressBar);
		topWrapper.setPadding(new Insets(8));

		
		table = new TableView<>();
		createColumns();
		table.setPlaceholder(new Label("Drag & Drop a PDF to begin"));

		
		statusLabel = new Label("Ready");
		statusLabel.setFont(Font.font("Segoe UI", 14));
		statusLabel.setPadding(new Insets(0, 0, 0, 5)); 

		verifyButton = new Button("🔍 Verify");
		verifyButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 20 8 20; -fx-base: #e6f7ff;");

		saveButton = new Button("💾 Save");
		saveButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 20 8 20;");
		saveButton.setVisible(false);
		saveButton.setManaged(false);

		
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		HBox bottomBar = new HBox(10, statusLabel, spacer, saveButton, verifyButton);
		bottomBar.setAlignment(Pos.CENTER_LEFT); 
		bottomBar.setPadding(new Insets(10));

		MenuBar menuBar = new MenuBar();
		Menu helpMenu = new Menu("Help");

		aboutItem = new MenuItem("About Cerca");
		sponsorItem = new MenuItem("❤ Sponsor");
		contributeItem = new MenuItem("💻 Contribute");
		licenseItem = new MenuItem("🗐 License");

		helpMenu.getItems().addAll(sponsorItem, contributeItem, licenseItem, new SeparatorMenuItem(), 
				aboutItem);

		menuBar.getMenus().add(helpMenu);

		VBox combinedTop = new VBox(menuBar, topWrapper);

		layout.setTop(combinedTop);

		layout.setCenter(table);
		layout.setBottom(bottomBar);

		table.getSelectionModel().setCellSelectionEnabled(true);
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); 

		
		installCopyHandler(table);
	}

	public MenuItem getLicenseItem() {
		return licenseItem;
	}

	private void createColumns() {

		TableColumn<ReferenceItem, String> authorCol = new TableColumn<>("PDF Authors");
		authorCol.setCellValueFactory(cell -> cell.getValue().authorsProperty());
		authorCol.setPrefWidth(200);

		
		TableColumn<ReferenceItem, String> crAuthorCol = new TableColumn<>("DB Authors");
		crAuthorCol.setCellValueFactory(cell -> cell.getValue().dBAuthorsProperty());
		crAuthorCol.setPrefWidth(200);
		crAuthorCol.setStyle("-fx-text-fill: #0052cc;"); // Blue text to indicate "Cloud Data"

		TableColumn<ReferenceItem, Integer> idCol = new TableColumn<>("#");
		idCol.setCellValueFactory(cell -> cell.getValue().idProperty().asObject());
		idCol.setPrefWidth(40);

		TableColumn<ReferenceItem, String> statusCol = new TableColumn<>("Status");
		statusCol.setCellValueFactory(cell -> cell.getValue().statusProperty());
		statusCol.setPrefWidth(100);
		// Custom Color Renderer
		statusCol.setCellFactory(col -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else {
					setText(item);
					ReferenceItem row = getTableView().getItems().get(getIndex());
					setTextFill(row.statusColorProperty().get());
					setFont(Font.font("System", FontWeight.BOLD, 12));
				}
			}
		});

		TableColumn<ReferenceItem, String> titleCol = new TableColumn<>("PDF Title");
		titleCol.setCellValueFactory(cell -> cell.getValue().pdfTitleProperty());
		titleCol.setPrefWidth(400);

		TableColumn<ReferenceItem, String> crTitleCol = new TableColumn<>("DB Title");
		crTitleCol.setCellValueFactory(cell -> cell.getValue().crossrefTitleProperty());
		crTitleCol.setPrefWidth(400);
		crTitleCol.setStyle("-fx-text-fill: #0052cc;"); // Blue text for online data

		
		TableColumn<ReferenceItem, Number> matchCol = new TableColumn<>("Match Score");

		
		matchCol.setCellValueFactory(cell -> cell.getValue().matchScoreProperty());

		
		matchCol.setCellFactory(column -> new TableCell<ReferenceItem, Number>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle(""); // Reset style
				} else {
					setText(item.intValue() + "%");

					
					if (item.intValue() == 100) {
						setTextFill(javafx.scene.paint.Color.GREEN);
					} else if (item.intValue() < 50) {
						setTextFill(javafx.scene.paint.Color.RED);
					} else {
						setTextFill(javafx.scene.paint.Color.BLACK);
					}
				}
			}
		});
		matchCol.setPrefWidth(100);

		TableColumn<ReferenceItem, String> doiCol = new TableColumn<>("PDF DOI");
		doiCol.setCellValueFactory(cell -> cell.getValue().doiProperty());
		doiCol.setPrefWidth(120);

		
		doiCol.setCellFactory(col -> new TableCell<ReferenceItem, String>() {
			private final Hyperlink link = new Hyperlink();

			@Override
			protected void updateItem(String doiText, boolean empty) {
				super.updateItem(doiText, empty);

				if (empty || doiText == null || doiText.isEmpty()) {
					setGraphic(null);
				} else {
					link.setText(doiText);
					link.setStyle("-fx-text-fill: blue; -fx-underline: true;");

					
					link.setOnAction(e -> {
						try {
							
							String cleanDoi = doiText.trim();

							String url = cleanDoi.startsWith("http") ? cleanDoi : "https://doi.org/" + cleanDoi;

							openUrl(url);

						} catch (Exception ex) {
							System.err.println("Could not open browser: " + ex.getMessage());
						}
					});
					setGraphic(link);
				}
			}
		});

		
		ContextMenu contextMenu = new ContextMenu();
		MenuItem searchGoogleItem = new MenuItem("🔍 Search on Google");
		MenuItem searchScholarItem = new MenuItem("🎓 Search on Google Scholar");

		// Action: Search Google
		searchGoogleItem.setOnAction(e -> {
			ReferenceItem selected = table.getSelectionModel().getSelectedItem();
			if (selected != null) {
				String query = selected.getPdfTitle() + " " + selected.getAuthors();
				openUrl("https://www.google.com/search?q=" + query.replace(" ", "+"));
			}
		});

		// Action: Search Scholar
		searchScholarItem.setOnAction(e -> {
			ReferenceItem selected = table.getSelectionModel().getSelectedItem();
			if (selected != null) {
				String query = selected.getPdfTitle();
				openUrl("https://scholar.google.com/scholar?q=" + query.replace(" ", "+"));
			}
		});

		contextMenu.getItems().addAll(searchGoogleItem, searchScholarItem);

		
		table.setRowFactory(tv -> {
			TableRow<ReferenceItem> row = new TableRow<>();
			row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty())
					.then((ContextMenu) null).otherwise(contextMenu));
			return row;
		});

		TableColumn<ReferenceItem, Boolean> verifiedCol = new TableColumn<>("✔");
		verifiedCol.setCellValueFactory(cell -> cell.getValue().verifiedProperty());

		
		verifiedCol.setCellFactory(CheckBoxTableCell.forTableColumn(verifiedCol));
		verifiedCol.setEditable(true); // Allow user to click it
		verifiedCol.setPrefWidth(50);

		
		table.getColumns().addAll(idCol, verifiedCol, statusCol, matchCol, authorCol, crAuthorCol, titleCol, crTitleCol,
				doiCol);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		table.setEditable(true);

	}

	private void openUrl(String url) {
		try {
			
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {

				
				Desktop.getDesktop().browse(new URI(url));

			} else {
				
				System.err.println("Desktop browse not supported on this system.");
				showAlert("Browser Error", "Could not open browser. Please visit:\n" + url);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			showAlert("Connection Error", "Failed to open link:\n" + ex.getMessage());
		}
	}

	
	private void showAlert(String title, String content) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	private void installCopyHandler(TableView<ReferenceItem> table) {
		table.setOnKeyPressed(event -> {
			
			if (event.isShortcutDown() && event.getCode() == KeyCode.C) {

				StringBuilder clipboardString = new StringBuilder();
				var selectedCells = table.getSelectionModel().getSelectedCells();

				if (selectedCells.isEmpty())
					return;

				
				int prevRow = selectedCells.get(0).getRow();

				for (TablePosition pos : selectedCells) {
					int row = pos.getRow();

					
					if (row != prevRow) {
						clipboardString.append('\n');
						prevRow = row;
					} else if (clipboardString.length() > 0) {
						
						clipboardString.append('\t');
					}

					
					Object cellData = pos.getTableColumn().getCellData(row);

					
					clipboardString.append(cellData == null ? "" : cellData.toString());
				}

				
				final ClipboardContent content = new ClipboardContent();
				content.putString(clipboardString.toString());
				Clipboard.getSystemClipboard().setContent(content);

				event.consume(); 
			}
		});
	}

	public VBox createResultsDashboard() {
		resultsDashboard = new VBox(10);
		resultsDashboard.setPadding(new Insets(0, 0, 15, 0));
		resultsDashboard.setVisible(false); // Hidden by default
		resultsDashboard.setManaged(false); // Doesn't take space when hidden

		
		Label successAlert = new Label("Search complete!");
		successAlert.setMaxWidth(Double.MAX_VALUE);
		successAlert.setStyle("-fx-background-color: #e6fffa; -fx-text-fill: #047857;"
				+ "-fx-padding: 10 15; -fx-font-weight: bold; -fx-background-radius: 4;"
				+ "-fx-border-color: #10b981; -fx-border-width: 0 0 0 5;");

		
		Label resultsHeader = new Label("Results");
		resultsHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

		
		HBox statsRow = new HBox(15);
		statsRow.setAlignment(Pos.CENTER_LEFT);

		totalBadge = createBadge("References: -", "#f3f4f6", "#1f2937"); // Gray
		passedBadge = createBadge("Passed: -", "#d1fae5", "#065f46"); // Green
		failedBadge = createBadge("Failed: -", "#fee2e2", "#991b1b"); // Red
		rateBadge = createBadge("Pass Rate: -", "#f3f4f6", "#1f2937"); // Gray

		statsRow.getChildren().addAll(totalBadge, passedBadge, failedBadge, rateBadge);
		resultsDashboard.getChildren().addAll(successAlert, resultsHeader, statsRow);

		return resultsDashboard;
	}

	/** 
	 * 3. Helper for styling badges
	 * */
	private Label createBadge(String text, String bgColor, String textColor) {
		Label badge = new Label(text);
		badge.setStyle(
				"-fx-background-color: " + bgColor + ";" + "-fx-text-fill: " + textColor + ";" + "-fx-padding: 5 10;"
						+ "-fx-background-radius: 6;" + "-fx-font-weight: bold;" + "-fx-font-size: 13px;");
		return badge;
	}

	/** PUBLIC METHOD: The Controller calls this to show results**/
	public void updateTestResults(int total, int passed, int failed) {
		double rate = (total == 0) ? 0.0 : ((double) passed / total) * 100;

		totalBadge.setText("Total: " + total);
		passedBadge.setText("Passed: " + passed);
		failedBadge.setText("Failed: " + failed);
		rateBadge.setText(String.format("Pass Rate: %.1f%%", rate));

		
		resultsDashboard.setVisible(true);
		resultsDashboard.setManaged(true);
	}

	/** Hide results dashboard when a new file is uploaded before verification **/
	public void resetDashboard() {
		resultsDashboard.setVisible(false);
		resultsDashboard.setManaged(false);
	}

	public Parent getView() {
		return layout;
	}

	public TableView<ReferenceItem> getTable() {
		return table;
	}

	public Button getVerifyButton() {
		return verifyButton;
	}

	public ProgressBar getProgressBar() {
		return progressBar;
	}

	public Label getStatusLabel() {
		return statusLabel;
	}

	public Pane getDragTarget() {
		return (Pane) ((VBox) layout.getTop()).getChildren().get(0);
	}

}
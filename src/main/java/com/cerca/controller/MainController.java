package com.cerca.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.cerca.model.ReferenceItem;
import com.cerca.service.CermineService;
import com.cerca.service.ConfigService;
import com.cerca.service.CrossrefService;
import com.cerca.service.CsvService;
import com.cerca.service.LogService;
import com.cerca.service.OpenAlexService;
import com.cerca.service.ReportService;
import com.cerca.service.SemanticScholarService;
import com.cerca.service.ZenodoService;
import com.cerca.utils.ReferenceParser;
import com.cerca.view.MainView;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import pl.edu.icm.cermine.exception.AnalysisException;

/**
 * Controller for the main CERCA user interface.
 *
 * This controller manages user interactions such as loading PDF files,
 * initiating reference verification, and displaying results.
 * 
 * @author Lidiany Cerqueira
 * 
 */
public class MainController {

	private final MainView view;
	private final ObservableList<ReferenceItem> data;
	private final CermineService cermineService;
	private final CrossrefService crossrefService;
	private final CsvService csvService;
	private final ReportService reportService;
	private final LogService logService;
	private final ZenodoService zenodoService;
	private final SemanticScholarService semScholarService;

	private final OpenAlexService openAlexService;
	private final ConfigService configService;

	public MainController(MainView view) {
		this.view = view;
		this.data = FXCollections.observableArrayList();
		this.cermineService = new CermineService();
		this.csvService = new CsvService();
		this.reportService = new ReportService();
		this.logService = new LogService();
		this.crossrefService = new CrossrefService(logService);
		this.zenodoService = new ZenodoService(logService);
		this.openAlexService = new OpenAlexService(logService);

		this.semScholarService = new SemanticScholarService(logService);
		this.configService = new ConfigService(logService);
		this.semScholarService.setApiKey(configService.getProperty("SEMANTIC_SCHOLAR_API_KEY"));
		this.crossrefService.setEmail(configService.getProperty("USER_EMAIL"));
		this.openAlexService.setEmail(configService.getProperty("USER_EMAIL"));
		view.getTable().setItems(data);

		setupDragAndDrop();
		setupButtons();
	}

	private void setupDragAndDrop() {
		view.getView().setOnDragOver(event -> {
			if (event.getDragboard().hasFiles()) {
				event.acceptTransferModes(TransferMode.COPY);
			}
			event.consume();
		});

		view.getView().setOnDragDropped(event -> {
			var db = event.getDragboard();
			if (db.hasFiles()) {
				File file = db.getFiles().get(0);
				processPdf(file);
			}
			event.setDropCompleted(true);
			event.consume();
		});
	}

	private void openUrl(String url) {
		try {
			java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
		} catch (Exception ex) {

			System.err.println("Failed to open URL: " + ex.getMessage());

			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Could not open browser");
			alert.setContentText("Please visit: " + url);
			alert.show();
		}
	}

	private void processPdf(File file) {

		view.resetDashboard();

		logService.log("USER", "Loaded PDF file: " + file.getName());
		view.getFileTitleLabel().setText("Loaded PDF file: " + file.getName());
		view.getFileTitleLabel().setVisible(true);
		view.getSaveButton().setVisible(false);

		logService.log("SYSTEM", "Starting extraction for file: " + file.getName());
		view.getStatusLabel().setText("Extracting references from " + file.getName() + "...");
		view.getProgressBar().setVisible(true);

		data.clear();

		CompletableFuture.supplyAsync(() -> {
			try {
				return cermineService.extractReferences(file);
			} catch (Exception e) {

				throw new CompletionException(e);
			}
		}).thenAccept(items -> {
			Platform.runLater(() -> {

				if (items.isEmpty()) {
					view.getProgressBar().setVisible(false);
					view.getStatusLabel().setText("Extraction failed.");
					logService.log("WARN", "Extraction returned 0 references.");

					Alert alert = new Alert(Alert.AlertType.WARNING);
					alert.setTitle("No References Found");
					alert.setHeaderText("Could not extract text.");
					alert.setContentText("Cerca could not find any references in this PDF.\n\n" + "Possible reasons:\n"
							+ "1. The PDF is a scanned image (no text layer).\n"
							+ "2. The reference section format is unusual.\n\n" + "Try a standard digital PDF.");
					alert.showAndWait();
					return;
				}

				data.addAll(items);

				view.getStatusLabel().setText("Found " + items.size() + " references. Ready to verify.");
				logService.log("INFO", "Extraction successful. Found " + items.size() + " items.");
				view.getProgressBar().setVisible(false);

			});

		}).exceptionally(ex -> {
			Platform.runLater(() -> {
				view.getProgressBar().setVisible(false);

				Throwable cause = ex.getCause();

				if (cause instanceof AnalysisException) {
					logService.log("WARN", "Invalid PDF file detected: " + file.getName());
					view.getStatusLabel().setText("Error: Invalid PDF file.");

					Alert alert = new Alert(Alert.AlertType.ERROR);
					alert.setTitle("Cannot Read PDF");
					alert.setHeaderText("Invalid or Protected File");
					alert.setContentText("Cerca could not open this file.\n\n" + "Possible reasons:\n"
							+ "1. The file is password-protected (encrypted).\n" + "2. The file is corrupted.\n"
							+ "3. It is not a valid PDF document.");
					alert.showAndWait();
				}

				else {
					logService.log("ERROR", "Extraction crashed: " + ex.getMessage());
					view.getStatusLabel().setText("Error: " + ex.getMessage());

					Alert alert = new Alert(Alert.AlertType.ERROR);
					alert.setTitle("Error");
					alert.setHeaderText("Extraction Failed");
					alert.setContentText("An unexpected error occurred:\n" + ex.getMessage());
					alert.showAndWait();
					ex.printStackTrace();
				}
			});
			return null;
		});
	}

	private void setupButtons() {
		view.getPasteButton().setOnAction(e -> {
			view.showManualEntryDialog(pastedText -> {
				if (pastedText != null && !pastedText.isEmpty()) {
					loadManualReferences(pastedText);
				}
			});
		});

		view.getVerifyButton().setOnAction(e -> verifyAll());

		view.getSaveButton().setOnAction(e -> exportData());

		view.getAboutItem().setOnAction(e -> showAboutDialog());

		view.getLicenseItem().setOnAction(e -> showLicenseDialog());

		view.getSponsorItem().setOnAction(e -> openSponsorDialog());

		view.getContributeItem().setOnAction(e -> openContributeDialog());

		view.getPreferencesItem().setOnAction(e -> openSettingsDialog());

		view.getEmailItem().setOnAction(e -> openEmailDialog());
	}

	private void showAboutDialog() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("About Cerca");
		alert.setHeaderText("Cerca - Citation Extraction & Reference Checking Assistant v1.3.0-alpha");
		alert.setContentText(

				"\n ✦ CERCA is an experimental tool intended to help verify bibliographic "
						+ "references against the online databases but is not 100% accurate. "
						+ "It does not replace manual verification. Always check the original source.\n\n"
						+ "✦ CERCA is designed with privacy in mind. All file parsing and reference extraction are performed locally. "
						+ "Your manuscript is never uploaded, stored, or shared."
						+ "\n\n ✦ Developed by Lidiany Cerqueira, PhD");
		alert.showAndWait();
	}

	private void showLicenseDialog() {

		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("License Information");
		alert.setHeaderText("Cerca is Open Source Software");

		TextArea area = new TextArea("CERCA (c) 2025\n"
				+ "Licensed under the GNU Affero General Public License, Version 3.0 (AGPL-3.0).\n\n"
				+ "This program comes with ABSOLUTELY NO WARRANTY.\n"
				+ "You are welcome to redistribute it under certain conditions.\n\n"
				+ "--------------------------------------------------\n" + "THIRD PARTY CREDITS:\n"
				+ "This software uses the CERMINE library, which is also licensed under GNU AGPL v3.\n"
				+ "CERMINE Copyright (c) Centre for Open Science.\n"
				+ "Dominika Tkaczyk, Pawel Szostek, Mateusz Fedoryszak, Piotr Jan Dendek and Lukasz Bolikowski.\n "
				+ "CERMINE: automatic extraction of structured metadata from scientific literature.\n"
				+ "In International Journal on Document Analysis and Recognition, 2015,\n"
				+ "vol. 18, no. 4, pp. 317-335, doi: 10.1007/s10032-015-0249-8.");
		area.setEditable(false);
		alert.getDialogPane().setContent(area);
		alert.showAndWait();

	}

	private void recalculateDashboard() {

		if (data == null || data.isEmpty())
			return;

		Platform.runLater(() -> {
			int total = data.size();

			int passed = (int) data.stream().filter(ReferenceItem::isVerified).count();

			int failed = total - passed;

			view.updateTestResults(total, passed, failed);
		});
	}

	private void attachDashboardListeners() {
		for (ReferenceItem item : data) {

			item.verifiedProperty().addListener((obs, wasChecked, isChecked) -> {
				recalculateDashboard();
			});
		}
	}

	private void verifyAll() {
		if (data.isEmpty())
			return;

		view.getProgressBar().setVisible(true);
		view.getStatusLabel().setText("Searching online databases...");

		CompletableFuture.runAsync(() -> {
			for (ReferenceItem item : data) {

				Platform.runLater(() -> {
					item.statusProperty().set("SEARCHING...");
				});

				crossrefService.verifyItem(item);

				// Define what score counts as a "Pass" (e.g., 75%)
				int PASS_THRESHOLD = 75;

				if (item.getMatchScore() < PASS_THRESHOLD) {
					// Try OpenAlex
					openAlexService.verify(item);
				}

				// try zenodo
				if (item.getMatchScore() < PASS_THRESHOLD && item.getRawText().toLowerCase().contains("zenodo")) {
					zenodoService.verify(item);
				}

				if (item.getMatchScore() < PASS_THRESHOLD) {

					semScholarService.verify(item);
				}

				Platform.runLater(() -> {

					if (item.getMatchScore() >= 75) {
						item.setVerified(true);
					} else {
						item.setVerified(false);
					}
				});

				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}

			}
		}).thenRun(() -> Platform.runLater(() -> {

			view.getProgressBar().setVisible(false);
			view.getStatusLabel().setText("Verification Complete.");

			view.getSaveButton().setVisible(true);
			view.getSaveButton().setManaged(true);

			if (data != null && !data.isEmpty()) {
				int total = data.size();

				int passed = (int) data.stream().filter(item -> item.isVerified()).count();

				int failed = total - passed;

				view.updateTestResults(total, passed, failed);
				attachDashboardListeners();

				recalculateDashboard();
			}

		}));
	}

	private void exportData() {
		if (data.isEmpty()) {
			view.getStatusLabel().setText("Nothing to save!");
			return;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export Results");

		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
		fileChooser.setInitialFileName("cerca_results_" + timestamp);

		FileChooser.ExtensionFilter csvFilter = new FileChooser.ExtensionFilter("CSV Data (*.csv)", "*.csv");
		FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("Reviewer Report (*.txt)", "*.txt");

		fileChooser.getExtensionFilters().addAll(csvFilter, txtFilter);

		File file = fileChooser.showSaveDialog(view.getView().getScene().getWindow());

		if (file != null) {
			try {

				if (file.getName().endsWith(".txt")) {
					reportService.exportReport(data, file);
					view.getStatusLabel().setText("Report saved: " + file.getName());
				} else {

					csvService.exportToCsv(data, file);
					view.getStatusLabel().setText("Data saved: " + file.getName());
				}
				logService.log("USER Exported results to file", file.getName());
			} catch (IOException ex) {
				view.getStatusLabel().setText("Error saving: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	private void loadManualReferences(String text) {

		this.data.clear();
		this.view.resetDashboard();

		String[] lines = text.split("\\r?\\n");

		int idCounter = 1;
		for (String line : lines) {
			// Skip empty lines
			if (line.trim().length() < 5)
				continue;

			ReferenceParser.ParsedData parsedData = ReferenceParser.parse(line);

			ReferenceItem item = new ReferenceItem(idCounter++, "WAITING", parsedData.authors, parsedData.title, line,
					"");

			data.add(item);
		}

		this.view.getFileTitleLabel().setText("Source: Manual Entry");
		this.view.getStatusLabel().setText("Loaded " + data.size() + " references. Ready to verify.");
	}

	public void openSettingsDialog() {
		String currentKey = this.semScholarService.getApiKey();

		Dialog<String> dialog = new Dialog<>();
		dialog.setTitle("CERCA Settings");
		dialog.setHeaderText("Semantic Scholar API Integration");
		// Add standard Save and Cancel buttons
		ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

		VBox vbox = new VBox();
		vbox.setSpacing(10);
		vbox.setStyle("-fx-padding: 10px;");

		Label instructions = new Label("Adding an API key significantly increases your search speed and rate limits.");
		instructions.setWrapText(true);

		// Clickable link to the Semantic Scholar registration form
		Hyperlink link = new Hyperlink("Get your free Semantic Scholar API key here");
		link.setOnAction(e -> {
			try {
				if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
					Desktop.getDesktop().browse(new URI("https://www.semanticscholar.org/product/api#api-key-form"));
				} else {
					logService.log("ERROR", "Desktop browsing is not supported on this system.");
				}
			} catch (Exception ex) {
				logService.log("ERROR", "Failed to open browser: " + ex.getMessage());
			}
		});

		TextField keyInput = new TextField();
		keyInput.setPromptText("Paste your API Key here...");
		keyInput.setText(currentKey);
		keyInput.setPrefWidth(350);

		// Privacy disclaimer
		Label privacyNote = new Label(
				"🔒 Privacy Note: Your API key is stored locally on your machine.\nIt is never uploaded, distributed, or tracked by CERCA.");
		privacyNote.setStyle("-fx-text-fill: #555555; -fx-font-size: 12px;");
		privacyNote.setWrapText(true);

		vbox.getChildren().addAll(instructions, link, keyInput, privacyNote);
		dialog.getDialogPane().setContent(vbox);

		Platform.runLater(keyInput::requestFocus);

		// Convert the result when the user clicks "Save"
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == saveButtonType) {
				return keyInput.getText();
			}
			return null;
		});

		// Show the dialog and handle the result
		Optional<String> result = dialog.showAndWait();

		result.ifPresent(newKey -> {
			configService.setProperty("SEMANTIC_SCHOLAR_API_KEY", newKey);
			this.semScholarService.setApiKey(newKey);
			logService.log("INFO", "API Key updated successfully!");
		});
	}

	public void openEmailDialog() {
		// 1. Fetch current email from ConfigService
		String currentEmail = configService.getProperty("USER_EMAIL");

		Dialog<String> dialog = new Dialog<>();
		dialog.setTitle("CERCA Settings");
		dialog.setHeaderText("Polite Pool Email");

		ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

		VBox vbox = new VBox();
		vbox.setSpacing(10);
		vbox.setStyle("-fx-padding: 10px;");

		Label instructions = new Label("Providing your email routes your requests faster.");
		instructions.setWrapText(true);

		TextField emailInput = new TextField();
		emailInput.setPromptText("e.g., researcher@university.edu");
		emailInput.setText(currentEmail);
		emailInput.setPrefWidth(350);

		Label privacyNote = new Label(
				"🔒 Privacy Note: Your email is stored locally and only sent to Crossref/OpenAlex for API courtesy. CERCA does not track it.");
		privacyNote.setStyle("-fx-text-fill: #555555; -fx-font-size: 12px;");
		privacyNote.setWrapText(true);

		vbox.getChildren().addAll(instructions, emailInput, privacyNote);
		dialog.getDialogPane().setContent(vbox);

		Platform.runLater(emailInput::requestFocus);

		// Convert the result when the user clicks "Save"
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == saveButtonType) {
				return emailInput.getText();
			}
			return null;
		});

		// Show the dialog and capture the result
		Optional<String> result = dialog.showAndWait();

		result.ifPresent(newEmail -> {
			configService.setProperty("USER_EMAIL", newEmail);
			logService.log("SYSTEM", "User Email updated successfully!");
		});
	}

	public void openSponsorDialog() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Support CERCA");
		alert.setHeaderText("❤ Support the Project");

		alert.setContentText(null);

		VBox vbox = new VBox();
		vbox.setSpacing(12);
		vbox.setStyle("-fx-padding: 10px; -fx-font-size: 13px;");

		Label introLabel = new Label(
				"CERCA is free and open-source. If this tool has helped your research or editorial workflow, please consider supporting its development:");
		introLabel.setWrapText(true);
		introLabel.setPrefWidth(400);

		// 1. GitHub Star Link
		Hyperlink githubLink = new Hyperlink("⭐ Star the repository on GitHub");
		githubLink.setStyle("-fx-font-weight: bold;");
		githubLink.setOnAction(e -> {
			openUrl("https://github.com/lidianycs/cerca");
			githubLink.setVisited(false);
		});

		// 2. SourceForge Comment Link
		Hyperlink sourceforgeLink = new Hyperlink("💬 Leave a review on SourceForge");
		sourceforgeLink.setStyle("-fx-font-weight: bold;");
		sourceforgeLink.setOnAction(e -> {
			openUrl("https://sourceforge.net/projects/cerca/reviews/");
			sourceforgeLink.setVisited(false);
		});

		// 3. Share Recommendation
		Label shareLabel = new Label("📢 Share the tool with your colleagues and research group!");
		shareLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333; -fx-padding: 0 0 0 4px;");

		vbox.getChildren().addAll(introLabel, githubLink, sourceforgeLink, shareLabel);

		alert.getDialogPane().setContent(vbox);

		alert.showAndWait();
	}

	public void openContributeDialog() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Contribute to CERCA");
		alert.setHeaderText("💻 Get Involved");
		alert.setContentText(null); // Clear default text

		VBox vbox = new VBox();
		vbox.setSpacing(12);
		vbox.setStyle("-fx-padding: 10px; -fx-font-size: 13px;");

		Label introLabel = new Label("CERCA is an open-source tool. Contributions are welcomed!");
		introLabel.setWrapText(true);
		introLabel.setPrefWidth(400);

		// Option 1: Open an Issue
		Label issueLabel = new Label("Found a bug or have a feature request?");
		issueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");

		Hyperlink issueLink = new Hyperlink("🐛 Open an Issue on GitHub");
		issueLink.setOnAction(e -> {
			// Directs them straight to the issues tab
			openUrl("https://github.com/lidianycs/cerca/issues");
			issueLink.setVisited(false);
		});

		// Option 2: Write Code
		Label codeLabel = new Label("Want to write some code?");
		codeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");

		Hyperlink codeLink = new Hyperlink("🔧 Fork the repository and submit a Pull Request");
		codeLink.setOnAction(e -> {
			openUrl("https://github.com/lidianycs/cerca");
			codeLink.setVisited(false);
		});

		vbox.getChildren().addAll(introLabel, issueLabel, issueLink, codeLabel, codeLink);
		alert.getDialogPane().setContent(vbox);

		alert.showAndWait();
	}

}
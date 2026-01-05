package com.cerca.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.cerca.model.ReferenceItem;
import com.cerca.service.CermineService;
import com.cerca.service.CrossrefService;
import com.cerca.service.CsvService;
import com.cerca.service.LogService;
import com.cerca.service.OpenAlexService;
import com.cerca.service.ReportService;
import com.cerca.service.ZenodoService;
import com.cerca.view.MainView;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import pl.edu.icm.cermine.exception.AnalysisException;

/**
 * Controller for the main CERCA user interface.
 *
 * This controller manages user interactions such as loading PDF files,
 * initiating reference verification, and displaying results.
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

	private final OpenAlexService openAlexService;

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
		view.getVerifyButton().setOnAction(e -> verifyAll());

		view.getSaveButton().setOnAction(e -> exportData());

		view.getAboutItem().setOnAction(e -> showAboutDialog());
		view.getLicenseItem().setOnAction(e -> showLicenseDialog());

		view.getSponsorItem().setOnAction(e -> openUrl("https://github.com/lidianycs/cerca"));
		view.getContributeItem().setOnAction(e -> openUrl("https://github.com/lidianycs/cerca"));
	}

	private void showAboutDialog() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("About Cerca");
		alert.setHeaderText("Cerca - Citation Extraction & Reference Checking Assistant vbeta");
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

				// Try OpenAlex 
				if (item.getMatchScore() < PASS_THRESHOLD) {
				    openAlexService.verify(item);
				}

				
				if (item.getMatchScore() < PASS_THRESHOLD) {
				    zenodoService.verify(item);
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

}
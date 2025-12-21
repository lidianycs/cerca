package com.cerca.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public class ReferenceItem {
	private final IntegerProperty id;
	private final IntegerProperty matchScore;
	private final StringProperty status; 
	private final ObjectProperty<Color> statusColor;
	private final StringProperty authors;
	private final StringProperty pdfTitle;
	private final StringProperty dbTitle;
	private final StringProperty dbAuthors; 
	private final StringProperty doi;
	// Hidden data for logic
	// private String detectedDoi;
	private String rawText;
	private final BooleanProperty verified;

	public ReferenceItem(int id, String status, String authors, String pdfTitle, String rawText, String doi) {
		this.id = new SimpleIntegerProperty(id);
		this.status = new SimpleStringProperty(status);
		this.statusColor = new SimpleObjectProperty<>(Color.GRAY);
		this.authors = new SimpleStringProperty(authors);
		this.pdfTitle = new SimpleStringProperty(pdfTitle);
		this.dbTitle = new SimpleStringProperty("");
		this.matchScore = new SimpleIntegerProperty(0);
		this.rawText = rawText;
		// this.detectedDoi = doi;
		this.dbAuthors = new SimpleStringProperty(""); // <--- Initialize empty
		this.doi = new SimpleStringProperty(doi);
		this.verified = new SimpleBooleanProperty(false);
		
		this.verified.addListener((obs, oldVal, isChecked) -> {
            if (isChecked) {
                // If user checks the box, mark as Manually Verified
                this.status.set("✅ PASS");
                this.statusColor.set(Color.GREEN);
            } else {
                // If user UNCHECKS the box, revert status based on the score
                if (matchScore.get() >= 75) {
                	this.status.set("✅ PASS");
                    this.statusColor.set(Color.GREEN); 
                }else if (matchScore.get() > 50) {
                	this.status.set("! CHECK");
                    this.statusColor.set(Color.ORANGE);
                } else {
                	this.status.set("❌ FAIL");
                	this.statusColor.set(Color.RED);
                }
            }
        });
    
	}

	public IntegerProperty idProperty() {
		return id;
	}

	public StringProperty statusProperty() {
		return status;
	}

	public ObjectProperty<javafx.scene.paint.Color> statusColorProperty() {
		return statusColor;
	}

	public StringProperty authorsProperty() {
		return authors;
	}

	public StringProperty pdfTitleProperty() {
		return pdfTitle;
	}

	public StringProperty crossrefTitleProperty() {
		return dbTitle;
	}

	public int getMatchScore() {
		return matchScore.get();
	}

	public void setCrossrefData(String title, String authors, int score) {
		this.dbTitle.set(title);
		this.dbAuthors.set(authors);
		this.matchScore.set(score);
	}

	// Standard Getters
	public String getPdfTitle() {
		return pdfTitle.get();
	}

	public String getRawText() {
		return rawText;
	}

	

	public StringProperty dBAuthorsProperty() {
		return dbAuthors;
	}

	public String getAuthors() {
		return authors.get();
	}

	public StringProperty doiProperty() {
		return doi;
	}

	public String getDetectedDoi() {
		return doi.get();
	}

	public String getDbTitle() {
		return dbTitle.get();
	}

	public String getDbAuthors() {
		return dbAuthors.get();
	}

	public int getId() {
		return id.get();
	}

	public IntegerProperty matchScoreProperty() {
		return matchScore;
	}

	public BooleanProperty verifiedProperty() {
		return verified;
	}

	public boolean isVerified() {
		return verified.get();
	}

	public void setVerified(boolean value) {
		this.verified.set(value);
	}

}
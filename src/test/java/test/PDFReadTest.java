package test;

import com.cerca.service.CermineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.edu.icm.cermine.exception.AnalysisException;

import java.io.File;

public class PDFReadTest {

    private CermineService cermineService;

    @BeforeEach
    public void initialize() {
        cermineService = new CermineService();
    }

    @Test
    @DisplayName("Test that the CermineService can read a PDF in IEEE format and extract references")
    public void testIEEEPDFRead() throws Exception {
        String filePath = "src/test/resources/pdfs/Dummy_PDF_IEEE_Format.pdf";
        File file = new File(filePath);
        Assertions.assertFalse(cermineService.extractReferences(file).isEmpty());
    }

    @Test
    @DisplayName("Test that the CermineService can read a PDF in APA format and extract references")
    public void testAPAPDFRead() throws Exception {
        String filePath = "src/test/resources/pdfs/Dummy_PDF_APA_Format.pdf";
        File file = new File(filePath);
        Assertions.assertFalse(cermineService.extractReferences(file).isEmpty());
    }

    @Test
    @DisplayName("Test that a FileNotFoundException is thrown when the PDF file does not exist")
    public void testWhatExceptionIsThrown(){
        File file = new File("src/test/resources/pdfs/nonExistentFile.pdf");
        Assertions.assertThrows(java.io.FileNotFoundException.class, () -> {
            cermineService.extractReferences(file);
        });
    }

    @Test
    @DisplayName("Test that an AnalysisException is thrown when the file is not a PDF")
    public void testNonPDFFileThrowsException(){
        File file = new File("src/test/resources/not_a_pdf.txt");
        Assertions.assertThrows(AnalysisException.class, () -> {
            cermineService.extractReferences(file);
        });
    }
}

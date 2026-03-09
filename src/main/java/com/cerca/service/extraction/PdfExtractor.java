package com.cerca.service.extraction;

import com.cerca.model.ReferenceItem;

import java.io.File;
import java.util.List;

public interface PdfExtractor {
    List<ReferenceItem> extractReferences(File pdfFile) throws Exception;
}

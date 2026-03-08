# CERCA Export Formats

CERCA exports verification results as CSV and TXT files. This documentation provides a sample of what these generated files look like to help with open science, audit logs, and reproducibility.

## CSV Export Structure

The CSV export uses a semicolon (`;`) as the delimiter. Below is the explanation of each column:

| Column Name | Description | Example |
| :--- | :--- | :--- |
| **ID** | The internal identifier/row number for the reference. | `1` |
| **Verified** | Boolean indicating if the reference was successfully checked. | `true` |
| **Status** | The result status of the verification. | `Match Found` |
| **Match Score** | The confidence score of the match. | `98.5` |
| **PDF Title** | The title of the paper extracted from the uploaded PDF. | `Attention Is All You Need` |
| **PDF Authors** | The authors extracted from the uploaded PDF. | `Vaswani et al.` |
| **Crossref Title** | The official title found in the Crossref database. | `Attention Is All You Need` |
| **Crossref Authors**| The official authors found in the Crossref database. | `Ashish Vaswani, Noam Shazeer` |
| **DOI** | The Digital Object Identifier of the reference. | `10.5555/3295222` |

## TXT Export Sample

The TXT export generates a human-readable diagnostic report, highlighting references that need manual review. Here is a snippet of what the report looks like:

```text
CERCA - INTEGRITY DIAGNOSTIC REPORT
Generated: 2026-03-05 14:30
File: sample_paper.pdf
* DISCLAIMER: This software is an experimental tool intended
 to help verify bibliographic references, but is not 100% accurate. 
It does not replace manual verification. Always check the original source.
==================================================

SUMMARY
-------
Total References: 45
✅ Verified:       43
⚠️ Review Needed:  2

==================================================
DIAGNOSTICS: ITEMS REQUIRING ATTENTION
==================================================

#14
🔴 DIAGNOSIS: LOW CONFIDENCE MATCH. Verify spelling or formatting.
--------------------------------------------------
   PDF Title:   Attention Is All You Need
   PDF Authors: Vaswani et al.

   DB Title:    Attention is All you Need
   DB Authors:  Ashish Vaswani, Noam Shazeer
   Similarity:  75%
   PDF DOI:     10.5555/3295222

==================================================
End of Report
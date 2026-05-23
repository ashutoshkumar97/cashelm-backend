package com.bank.analyzer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.io.File;

public class PdfTest {

    @Test
    public void testExtraction() throws Exception {
        File file = new File("d:\\antigravity-projects\\Acct Statement_1838_02052026_03.46.46.pdf");
        try (PDDocument document = Loader.loadPDF(file, "175811759")) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            System.out.println("--- SORT BY POSITION TRUE ---");
            System.out.println(text.substring(0, Math.min(text.length(), 2000)));
            
            System.out.println("\n\n--- TESTING A FULL TRANSACTION ---");
            String[] lines = text.split("\\r?\\n");
            for(String line : lines) {
                if(line.contains("SURYAKANT")) {
                    System.out.println(line);
                }
            }
        }
    }
}

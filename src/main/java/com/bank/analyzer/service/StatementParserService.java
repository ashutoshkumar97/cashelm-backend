package com.bank.analyzer.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StatementParserService {
    public String extractRawText(MultipartFile file, String password) throws Exception {

        byte[] bytes = file.getBytes();

        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(bytes), password)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }
}

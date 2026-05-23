package com.bank.analyzer.service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bank.analyzer.model.Transaction;
import com.bank.analyzer.repository.TransactionRepository;
import com.opencsv.CSVReader;

@Service
public class StatementParserService {

    private final TransactionRepository transactionRepository;

    // HDFC Date format is typically dd/MM/yy
    private static final DateTimeFormatter HDFC_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Assuming
                                                                                                        // standard for
                                                                                                        // CSV for now

    public StatementParserService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> parseAndSave(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        List<Transaction> transactions = new ArrayList<>();

        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            transactions = parsePdf(file);
        } else if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            transactions = parseCsv(file);
        } else {
            throw new IllegalArgumentException("Unsupported file format");
        }

        return transactionRepository.saveAll(transactions);
    }

    private List<Transaction> parsePdf(MultipartFile file) throws Exception {
        List<Transaction> transactions = new ArrayList<>();
        byte[] pdfBytes = file.getBytes();
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader
                .loadPDF(new org.apache.pdfbox.io.RandomAccessReadBuffer(pdfBytes), "175811759")) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            stripper.setSortByPosition(true); // Crucial for multi-line column alignment
            String text = stripper.getText(document);
            String[] lines = text.split("\\r?\\n");

            // Regex for the FIRST line of a transaction: Date | Narration | RefNo |
            // ValueDate | Amount | Balance
            Pattern txPattern = Pattern.compile(
                    "^(\\d{2}/\\d{2}/\\d{2})\\s+(.*?)\\s+(\\S+)\\s+(\\d{2}/\\d{2}/\\d{2})\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})$");

            Transaction currentTx = null;

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || isJunkLine(line)) {
                    continue;
                }

                Matcher matcher = txPattern.matcher(line);
                if (matcher.find()) {
                    try {
                        String dateStr = matcher.group(1);
                        String narration = matcher.group(2);
                        // group 3 is refNo, group 4 is valueDate
                        String amountStr = matcher.group(5).replace(",", "");
                        String balanceStr = matcher.group(6).replace(",", "");

                        LocalDate date = LocalDate.parse(dateStr, HDFC_DATE_FORMAT);
                        BigDecimal amount = new BigDecimal(amountStr);
                        BigDecimal balance = new BigDecimal(balanceStr);

                        currentTx = new Transaction();
                        currentTx.setTransactionDate(date);
                        currentTx.setDescription(narration);
                        currentTx.setAmount(amount);
                        currentTx.setType("UNKNOWN");
                        currentTx.setBalance(balance);
                        transactions.add(currentTx);
                    } catch (Exception e) {
                        currentTx = null;
                    }
                } else if (currentTx != null) {
                    // It's a continuation of the previous transaction's narration!
                    // Check if it looks like a new date but failed the strict regex (just to be
                    // safe)
                    if (!line.matches("^\\d{2}/\\d{2}/\\d{2}.*")) {
                        currentTx.setDescription(currentTx.getDescription() + " " + line);
                    }
                }
            }

            // Post-process to guess CREDIT/DEBIT based on balance changes
            for (int i = 1; i < transactions.size(); i++) {
                Transaction prev = transactions.get(i - 1);
                Transaction curr = transactions.get(i);
                if (curr.getBalance().compareTo(prev.getBalance()) > 0) {
                    curr.setType("CREDIT");
                } else {
                    curr.setType("DEBIT");
                }
            }
            if (!transactions.isEmpty()) {
                transactions.get(0).setType("DEBIT"); // Default first one if we can't tell
            }

            System.out.println("Successfully parsed " + transactions.size() + " multi-line transactions from PDF.");
        }
        return transactions;
    }

    private boolean isJunkLine(String line) {
        String upper = line.toUpperCase();
        return upper.contains("PAGE NO") || upper.contains("HDFC BANK")
                || upper.contains("CLOSING BALANCE INCLUDES") || upper.contains("CONTENTS OF THIS STATEMENT")
                || upper.contains("REGISTERED OFFICE") || upper.contains("STATEMENT OF ACCOUNT")
                || upper.contains("DATE NARRATION") || upper.contains("JOINT HOLDERS")
                || upper.contains("NOMINATION :") || upper.startsWith("MR. ") || upper.startsWith("MRS. ")
                || upper.contains("ACCOUNT BRANCH") || upper.contains("ADDRESS :") || upper.contains("CITY :")
                || upper.contains("STATE :") || upper.contains("PHONE NO.") || upper.contains("OD LIMIT")
                || upper.contains("CURRENCY :") || upper.contains("EMAIL :") || upper.contains("CUST ID :")
                || upper.contains("ACCOUNT NO :") || upper.contains("A/C OPEN DATE") || upper.contains("ACCOUNT STATUS")
                || upper.contains("RTGS/NEFT") || upper.contains("BRANCH CODE") || upper.contains("ACCOUNT TYPE")
                || upper.equals(".") || upper.contains("INDIA") || upper.matches("^[\\d\\-]+$"); // skips plain
                                                                                                 // zipcodes/pins
    }

    private List<Transaction> parseCsv(MultipartFile file) throws Exception {
        List<Transaction> transactions = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream());
                CSVReader csvReader = new CSVReader(reader)) {

            String[] line;
            boolean isHeader = true;
            while ((line = csvReader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                if (line.length >= 4) {
                    try {
                        Transaction t = new Transaction();
                        t.setTransactionDate(LocalDate.parse(line[0], CSV_DATE_FORMAT));
                        t.setDescription(line[1]);
                        t.setAmount(new BigDecimal(line[2]));
                        t.setType(line[3]); // DEBIT or CREDIT
                        if (line.length > 4 && !line[4].isEmpty()) {
                            t.setBalance(new BigDecimal(line[4]));
                        }
                        transactions.add(t);
                    } catch (Exception e) {
                        // skip parse errors for now
                    }
                }
            }
        }
        return transactions;
    }
}

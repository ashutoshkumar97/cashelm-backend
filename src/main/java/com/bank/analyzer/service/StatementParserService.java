package com.bank.analyzer.service;

import com.bank.analyzer.model.Transaction;
import com.bank.analyzer.repository.TransactionRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StatementParserService {

    private final TransactionRepository transactionRepository;

    public StatementParserService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public String extractRawText(MultipartFile file, String password) throws Exception {

        byte[] bytes = file.getBytes();

        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(bytes), password)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    public List<Transaction> parseTransactions(String rawText) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = rawText.split("\\r?\\n");

        Pattern pattern = Pattern.compile(
                "^(\\d{2}/\\d{2}/\\d{2})" +     // Group 1 → Date
                        "\\s+(.+?)" +                   // Group 2 → Narration (first line)
                        "\\s+(\\d{15,16})" +            // Group 3 → Ref No (skip it)
                        "\\s+(\\d{2}/\\d{2}/\\d{2})" +  // Group 4 → Value Date (skip it)
                        "\\s+([\\d,]+\\.\\d{2})?" +     // Group 5 → Withdrawal (if not null → DEBIT)
                        "\\s*([\\d,]+\\.\\d{2})?" +     // Group 6 → Deposit (if not null → CREDIT)
                        "\\s+([\\d,]+\\.\\d{2})$"       // Group 7 → Closing Balance
        );

        Transaction current = null;

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty() || isJunkLine(line)) continue;

            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                current = new Transaction();
                current.setDate(LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd/MM/yy")));
                current.setDescription(matcher.group(2).trim());

                String withdrawal   = matcher.group(5);
                String deposit      = matcher.group(6);
                String balance      = matcher.group(7);

                if (withdrawal != null && !withdrawal.isBlank()) {
                    current.setAmount(new BigDecimal(withdrawal.replace(",", "")));
                    current.setType("DEBIT");
                } else {
                    current.setAmount(new BigDecimal(deposit.replace(",", "")));
                    current.setType("CREDIT");
                }

                current.setBalance(new BigDecimal(balance.replace(",", "")));

                transactions.add(current);
            } else if (current != null) {
                current.setDescription(current.getDescription() + " " + line.trim());
            }
        }
        transactionRepository.deleteAll();
        return transactionRepository.saveAll(transactions);
    }

    private boolean isJunkLine(String line) {
        String l = line.toUpperCase();
        return l.contains("STATEMENT OF ACCOUNT")
                || l.contains("DATE NARRATION")
                || l.contains("FROM :")
                || l.contains("OPENING BALANCE")
                || l.contains("CLOSING BALANCE")
                || l.contains("PAGE");
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}

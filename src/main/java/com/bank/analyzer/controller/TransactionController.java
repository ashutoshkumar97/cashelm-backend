package com.bank.analyzer.controller;

import com.bank.analyzer.model.Transaction;
import com.bank.analyzer.repository.TransactionRepository;
import com.bank.analyzer.service.StatementParserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final StatementParserService parserService;
    private final TransactionRepository transactionRepository;

    public TransactionController(StatementParserService parserService, TransactionRepository transactionRepository) {
        this.parserService = parserService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadStatement(@RequestParam("file") MultipartFile file) {
        try {
            long startTime = System.currentTimeMillis();
            List<Transaction> transactions = parserService.parseAndSave(file);
            long timeTakenMs = System.currentTimeMillis() - startTime;
            System.out.println("Total parsing and saving time: " + timeTakenMs + "ms");
            return ResponseEntity.ok(Map.of(
                "message", "File uploaded and parsed successfully", 
                "count", transactions.size(),
                "timeTakenMs", timeTakenMs,
                "transactions", transactions
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to parse file: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }

    @DeleteMapping
    public ResponseEntity<?> clearTransactions() {
        transactionRepository.deleteAll();
        return ResponseEntity.ok(Map.of("message", "All transactions cleared"));
    }
}

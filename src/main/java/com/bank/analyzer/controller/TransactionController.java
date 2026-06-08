package com.bank.analyzer.controller;

import com.bank.analyzer.model.Transaction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bank.analyzer.service.StatementParserService;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final StatementParserService statementParserService;

    public TransactionController(StatementParserService parserService) {
        this.statementParserService = parserService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Boolean> uploadPdf(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password) throws Exception {

        String rawText = statementParserService.extractRawText(file, password);
        List<Transaction> transactions = statementParserService.parseTransactions(rawText);
        System.out.println("Parsed " + transactions.size() + " transactions");
        return ResponseEntity.ok(true);
    }
}

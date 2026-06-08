package com.bank.analyzer.controller;

import com.bank.analyzer.dto.RequestRemark;
import com.bank.analyzer.model.Transaction;
import com.bank.analyzer.service.StatementParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final StatementParserService statementParserService;

    public TransactionController(StatementParserService parserService) {
        this.statementParserService = parserService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactions() {
        return ResponseEntity.ok(statementParserService.getAllTransactions());
    }

    @PostMapping("/upload")
    public ResponseEntity<List<Transaction>> uploadPdf(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(value = "password", required = false) String password) throws Exception {

        String rawText = statementParserService.extractRawText(file, password);
        List<Transaction> transactions = statementParserService.parseTransactions(rawText);
        System.out.println("Parsed " + transactions.size() + " transactions");
        return ResponseEntity.ok(transactions);
    }

    @PutMapping("/transactions/{id}/remark")
    public ResponseEntity<Transaction> updateRemark(
            @PathVariable Long id,
            @RequestBody RequestRemark requestRemark
    ) {
        return ResponseEntity.ok(statementParserService.updateRemark(id, requestRemark.getRemark()));
    }
}

package com.bank.analyzer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bank.analyzer.service.StatementParserService;

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

        String statements = statementParserService.extractRawText(file, password);
        System.out.println(statements);
        return ResponseEntity.ok(true);
    }
}

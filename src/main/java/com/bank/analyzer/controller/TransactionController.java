package com.bank.analyzer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TransactionController {

    @PostMapping("/upload")
    public ResponseEntity<Boolean> uploadPdf(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(true);
    }
}

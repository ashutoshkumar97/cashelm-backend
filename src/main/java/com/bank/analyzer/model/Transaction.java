package com.bank.analyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private String type;
    private BigDecimal balance;
    private String remark;

    public Transaction(LocalDate date, String description, BigDecimal amount, String type, BigDecimal balance, String remark) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.balance = balance;
        this.remark = remark;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}



package com.delmoralcristian.notifier.enums;

public enum EEventType {

    CREDIT_CARD_PAYMENT,
    DEBIT_CARD_WITHDRAWAL,
    CREDIT_TRANSFER,
    DEBIT_AUTOMATIC_PAYMENT,
    CREDIT_REFUND,
    DEBIT_TRANSFER,
    CREDIT_DEPOSIT,
    DEBIT_PURCHASE,
    CREDIT_CASHBACK,
    DEBIT_SUBSCRIPTION;

    public static EEventType fromString(String value) {
        return EEventType.valueOf(value.toUpperCase());
    }
}

package com.example.account.dto;

import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryTransactionResponse {
    private String accountNumber;
    private TransactionType transactionType;
    private TransactionResultType transactionResultType;
    private String transactionId;
    private Long amount;
    private LocalDateTime transactionAt;

    public static QueryTransactionResponse from(TransactionDto transactionDto) {
        return QueryTransactionResponse.builder()
                .accountNumber(transactionDto.getAccountNumber())
                .transactionType(transactionDto.getTransactionType())
                .transactionAt(transactionDto.getTransactedAt())
                .amount(transactionDto.getAmount())
                .transactionResultType(transactionDto.getTransactionResultType())
                .transactionId(transactionDto.getTransactionId())
                .build();
    }
}

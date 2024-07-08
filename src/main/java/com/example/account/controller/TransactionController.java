package com.example.account.controller;

import com.example.account.aop.AccountLock;
import com.example.account.dto.CancelBalance;
import com.example.account.dto.QueryTransactionResponse;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    //계좌의 잔액을 사용
    @PostMapping("/use")
    @AccountLock //Redisson 으로 lock 을 걸어 동시 접근으로 인한 문제를 예방한다.
    public UseBalance.Response useBalance(@Valid @RequestBody UseBalance.Request request) {
        try {
            TransactionDto transactionDto = transactionService.useBalance(
                    request.getUserId(),
                    request.getAccountNumber(),
                    request.getAmount()
            );

            return UseBalance.Response.from(transactionDto);

        } catch (AccountException e) {
            log.error("Failed to use balance");
            transactionService.saveFailedUseTransaction(request.getAccountNumber(), request.getAmount());
            throw e;
        }
    }

    //거래(transaction) 아이디를 통해 거래를 취소
    @PostMapping("/cancel")
    @AccountLock
    public CancelBalance.Response cancelBalance(@Valid @RequestBody CancelBalance.Request request) {
        try {
            TransactionDto transactionDto = transactionService.cancelBalance(
                    request.getTransactionId(),
                    request.getAccountNumber(),
                    request.getAmount()
            );

            return CancelBalance.Response.from(transactionDto);

        } catch (AccountException e) {
            log.error("Failed to cancel balance");

            //거래가 실패한 경우에도 실패한 기록을 저장
            transactionService.saveFailedCancelTransaction(request.getAccountNumber(), request.getAmount());
            throw e;
        }
    }

    //거래 아이디로 거래 내역 조회
    @GetMapping("/{transactionId}")
    public QueryTransactionResponse queryTransactionResponse(@PathVariable String transactionId) {
        return QueryTransactionResponse.from(transactionService.queryTransaction(transactionId));
    }
}
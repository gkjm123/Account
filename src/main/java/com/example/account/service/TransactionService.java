package com.example.account.service;

import static com.example.account.type.ErrorCode.ACCOUNT_NOT_FOUND;
import static com.example.account.type.ErrorCode.TRANSACTION_NOT_FOUND;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final AccountUserRepository accountUserRepository;
  private final AccountRepository accountRepository;

  @Transactional
  public TransactionDto useBalance(Long userId, String accountNumber, Long amount) {
    AccountUser user = accountUserRepository.findById(userId)
        .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

    //계좌 소유주 맞는지, 계좌 상태 정상인지 체크
    validateUseBalance(user, account);
    //계좌에서 금액 사용(계좌 잔액보다 큰 금액 사용하려는지 체크)
    account.useBalance(amount);

    //거래 내역을 저장하고 Dto 반환
    return TransactionDto.fromEntity(saveAndGetTransaction(USE, S, amount, account));
  }

  private void validateUseBalance(AccountUser user, Account account) {
    if (!Objects.equals(user.getId(), account.getAccountUser().getId())) {
      throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
    }

    if (account.getAccountStatus() != AccountStatus.IN_USE) {
      throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
    }
  }

  @Transactional
  public void saveFailedUseTransaction(String accountNumber, Long amount) {
    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

    saveAndGetTransaction(USE, F, amount, account);

  }

  //거래를 저장하고 Dto 반환
  private Transaction saveAndGetTransaction(
      TransactionType transactionType,
      TransactionResultType transactionResultType,
      Long amount,
      Account account
  ) {
    return transactionRepository.save(
        Transaction.builder()
            .transactionType(transactionType)
            .transactionResultType(transactionResultType)
            .amount(amount)
            .account(account)
            .balanceSnapshot(account.getBalance())
            .transactionId(UUID.randomUUID().toString().replace("-", ""))
            .transactedAt(LocalDateTime.now())
            .build());
  }

  @Transactional
  public TransactionDto cancelBalance(
      String transactionId,
      String accountNumber,
      Long amount
  ) {
    Transaction transaction = transactionRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND));

    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

    validateCancelBalance(transaction, account, amount);
    account.cancelBalance(amount);

    return TransactionDto.fromEntity(saveAndGetTransaction(CANCEL, S, amount, account));
  }

  //거래 취소 가능여부 체크
  private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
    //거래의 생성자가 맞는지
    if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
      throw new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
    }

    //취소하려는 금액은 거래 금액과 같아야함(금액 부분 취소 불가)
    if (!Objects.equals(transaction.getAmount(), amount)) {
      throw new AccountException(ErrorCode.CANCEL_MUST_FULLY);
    }

    //1년 이상 지난 거래는 취소 불가
    if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
      throw new AccountException(ErrorCode.TOO_OLD_ORDER_TO_CANCEL);
    }
  }

  @Transactional
  public void saveFailedCancelTransaction(String accountNumber, Long amount) {
    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

    saveAndGetTransaction(CANCEL, F, amount, account);
  }

  @Transactional
  public TransactionDto queryTransaction(String transactionId) {
    return TransactionDto.fromEntity(transactionRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND)));
  }
}

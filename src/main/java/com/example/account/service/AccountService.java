package com.example.account.service;

import static com.example.account.type.ErrorCode.ACCOUNT_ALREADY_UNREGISTERED;
import static com.example.account.type.ErrorCode.ACCOUNT_NOT_FOUND;
import static com.example.account.type.ErrorCode.BALANCE_NOT_EMPTY;
import static com.example.account.type.ErrorCode.MAX_ACCOUNT_PER_USER_10;
import static com.example.account.type.ErrorCode.USER_ACCOUNT_UN_MATCH;
import static com.example.account.type.ErrorCode.USER_NOT_FOUND;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AccountService {

  final private AccountRepository accountRepository;
  final private AccountUserRepository accountUserRepository;

  @Transactional
  public AccountDto createAccount(Long userId, Long initialBalance) {
    AccountUser accountUser = getAccountUser(userId);
    //계좌 생성 가능 체크
    validateCreateAccount(accountUser);

    //생성된 계좌가 없으면 1000000000 으로 첫 생성, 있으면 가장 최근 생성된 계좌번호 +1 해서 새 계좌 생성
    String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
        .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
        .orElse("1000000000");

    return AccountDto.fromEntity(
        accountRepository.save(Account.builder()
            .accountUser(accountUser)
            .accountNumber(newAccountNumber)
            .accountStatus(AccountStatus.IN_USE)
            .balance(initialBalance)
            .registeredAt(LocalDateTime.now())
            .build())
    );
  }

  private void validateCreateAccount(AccountUser accountUser) {
    //한 유저당 계좌 10개만 소유 가능
    if (accountRepository.countByAccountUser(accountUser) == 10) {
      throw new AccountException(MAX_ACCOUNT_PER_USER_10);
    }
  }

  @Transactional
  public Account getAccount(long id) {
    return accountRepository.findById(id)
        .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));
  }

  @Transactional
  public AccountDto deleteAccount(Long userId, String accountNumber) {
    AccountUser accountUser = getAccountUser(userId);

    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

    //계좌 삭제가 가능한지 확인후 계좌를 UNREGISTERED 상태로 바꿈
    validateDeleteAccount(accountUser, account);
    account.setAccountStatus(AccountStatus.UNREGISTERED);
    account.setUnRegisteredAt(LocalDateTime.now());

    accountRepository.save(account);
    return AccountDto.fromEntity(account);
  }

  private void validateDeleteAccount(AccountUser accountUser, Account account) {
    //계좌 소유주 일치 확인
    if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
      throw new AccountException(USER_ACCOUNT_UN_MATCH);
    }

    //계좌가 등록 상태인지 확인
    if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
      throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
    }

    //잔액이 0원 이어야 계좌 삭제 가능
    if (account.getBalance() > 0) {
      throw new AccountException(BALANCE_NOT_EMPTY);
    }
  }

  @Transactional
  public List<AccountDto> getAccountsByUserId(Long userId) {
    AccountUser accountUser = getAccountUser(userId);
    List<Account> accounts = accountRepository.findByAccountUser(accountUser);

    return accounts.stream()
        .map(AccountDto::fromEntity)
        .collect(Collectors.toList());
  }

  private AccountUser getAccountUser(Long userId) {
    return accountUserRepository.findById(userId)
        .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
  }
}

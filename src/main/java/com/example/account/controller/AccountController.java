package com.example.account.controller;

import com.example.account.domain.Account;
import com.example.account.dto.AccountDto;
import com.example.account.dto.AccountInfo;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  //유저 ID, 최초 잔액을 통해 계좌 생성
  @PostMapping("/create")
  public CreateAccount.Response createAccount(@RequestBody @Valid CreateAccount.Request request) {
    AccountDto accountDto = accountService.createAccount(request.getUserId(),
        request.getInitialBalance());
    return CreateAccount.Response.from(accountDto);
  }

  //유저 ID, 계좌번호를 통해 계좌 삭제
  @DeleteMapping("/delete")
  public DeleteAccount.Response deleteAccount(@RequestBody @Valid DeleteAccount.Request request) {
    AccountDto accountDto = accountService.deleteAccount(request.getUserId(),
        request.getAccountNumber());
    return DeleteAccount.Response.from(accountDto);
  }

  //유저 ID 를 통해 소유한 모든 계좌 조회
  @GetMapping("/account")
  public List<AccountInfo> getAccountsByUserId(@RequestParam("user_id") Long userId) {
    return accountService.getAccountsByUserId(userId)
        .stream().map(accountDto -> AccountInfo.builder()
            .accountNumber(accountDto.getAccountNumber())
            .balance(accountDto.getBalance()).build())
        .collect(Collectors.toList());
  }

  //계좌의 ID(계좌번호와 다름) 를 통해 계좌정보 조회
  @GetMapping("/account/{id}")
  public Account getAccount(@PathVariable Long id) {
    return accountService.getAccount(id);
  }
}

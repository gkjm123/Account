package com.example.account.service;

import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {

  private final RedissonClient redissonClient;

  public void lock(String accountNumber) {
    RLock lock = redissonClient.getLock(getLockKey(accountNumber));
    log.debug("Trying lock for accountNumber : {}", accountNumber);

    try {
      //사용하려는 계좌에 lock 이 걸렸는지(누군가 이미 사용중인지) 체크
      boolean isLock = lock.tryLock(1, 15, TimeUnit.SECONDS);

      //lock 이 걸려있으면 예외를 발생시켜 실제 접근하려던 메서드에 접근 불가능
      if (!isLock) {
        log.error("========Lock acquisition failed=====");
        throw new AccountException(ErrorCode.ACCOUNT_TRANSACTION_LOCK);
      }

    } catch (AccountException e) {
      throw e;

    } catch (Exception e) {
      log.error("Redis lock failed", e);
    }
  }

  //해당 계좌의 lock 해제
  public void unlock(String accountNumber) {
    log.debug("Trying unlock for accountNumber : {}", accountNumber);
    redissonClient.getLock(getLockKey(accountNumber)).unlock();
  }

  //lock 에 사용할 키 생성(prefix 붙이기)
  private static String getLockKey(String accountNumber) {
    return "ACLK:" + accountNumber;
  }
}

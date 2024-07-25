package com.example.account.service;


import com.example.account.aop.AccountLockIdInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {

  private final LockService lockService;

  @Around("@annotation(com.example.account.aop.AccountLock) && args(request)")
  public Object aroundMethod(ProceedingJoinPoint pjp, AccountLockIdInterface request)
      throws Throwable {
    //사용 또는 취소 요청의 계좌번호를 이용해 lock 취득 시도, 실패시 여기서 터져서 아래쪽 pjp(목적 메서드) 접근 불가
    lockService.lock(request.getAccountNumber());

    try {
      //lock 취득 성공시 pjp 에 접근시킨다.
      return pjp.proceed();
    } finally {
      //성공하든 실패하든 끝나고 lock 해제
      lockService.unlock(request.getAccountNumber());
    }
  }
}
package no.difi.meldingsutveksling.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Aspect
@Getter
@Component
@ConditionalOnProperty(name = "difi.move.feature.retryOnDeadLock", havingValue = "true")
public class DeadlockDetectAspect implements Ordered {

    /**
     * Order for this aspect, should be lower than for transaction manager which has 100
     **/
    @Getter
    private final int order = 99;

    /**
     * How many retries should be tried on deadlock
     **/
    private final int retryCount;

    /**
     * How big is delay between deadlock retry (in ms)
     **/
    private final int delay;

    public DeadlockDetectAspect(IntegrasjonspunktProperties properties) {
        this.retryCount = properties.getDeadlock().getRetryCount();
        this.delay = properties.getDeadlock().getDelay();
    }

    @Around(value = "@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object methodRetry(ProceedingJoinPoint pjp) throws Throwable {
        return detectDeadlocks(pjp);
    }

    @Around(value = "@within(org.springframework.transaction.annotation.Transactional)")
    public Object classRetry(ProceedingJoinPoint pjp) throws Throwable {
        return detectDeadlocks(pjp);
    }

    private Object detectDeadlocks(ProceedingJoinPoint pjp) throws Throwable {
        if (log.isTraceEnabled())
            log.trace("Before pointcut {} with transaction manager active: {}",
                    pjp.toString(), TransactionSynchronizationManager.isActualTransactionActive());

        try {
            int retryCount = getRetryCount();
            while (true) {
                try {
                    return pjp.proceed();
                } catch (LockAcquisitionException | CannotAcquireLockException ex) {
                    // if transaction manager is active, this means that we are in nested @Transactional call,
                    // but we want only make retry for the main @Transactional call, that starts the transaction again
                    if (TransactionSynchronizationManager.isActualTransactionActive()) {
                        if (log.isTraceEnabled())
                            log.trace("Deadlock pointcut detected, but transaction is still active - propagating");
                        throw ex;
                    } else {
                        // end of retries? throw exception to upper layer
                        if (retryCount-- == 0)
                            throw ex;

                        // otherwise, try to repeat this step
                        if (log.isDebugEnabled())
                            log.debug("Deadlock pointcut retry with retryCount={} (sleeping {} ms)",
                                    retryCount, getDelay());

                        Thread.sleep(getDelay());
                    }
                }
            }
        } finally {
            if (log.isTraceEnabled())
                log.trace("After pointcut {} with transaction manager active: {}",
                        pjp.toString(), TransactionSynchronizationManager.isActualTransactionActive());
        }
    }
}

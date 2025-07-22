package com.bulletonrails.rinha.service;

import com.bulletonrails.rinha.model.PaymentRequest;
import com.bulletonrails.rinha.model.PaymentSummary;
import com.bulletonrails.rinha.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentProcessorService processorService;
    private final HealthCheckService healthCheckService;

    private final ConcurrentLinkedQueue<PaymentRequest> paymentQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean processing = new AtomicBoolean(false);

    @Autowired
    public PaymentService(PaymentRepository repository, 
                         PaymentProcessorService processorService,
                         HealthCheckService healthCheckService) {
        this.repository = repository;
        this.processorService = processorService;
        this.healthCheckService = healthCheckService;

        startBackgroundProcessing();
    }

    public void receivePayment(PaymentRequest request) {
        paymentQueue.offer(request);
        triggerProcessing();
    }

    @Async("paymentExecutor")
    public void triggerProcessing() {
        if (!processing.compareAndSet(false, true)) {
            return;
        }

        try {
            processPaymentBatch();
        } finally {
            processing.set(false);
        }
    }

    private void processPaymentBatch() {
        PaymentRequest request;
        int batchCount = 0;

        while ((request = paymentQueue.poll()) != null && batchCount < 100) {
            processPaymentAsync(request);
            batchCount++;
        }

        if (!paymentQueue.isEmpty()) {
            CompletableFuture.runAsync(this::triggerProcessing);
        }
    }

    private void processPaymentAsync(PaymentRequest request) {
        HealthCheckService.ProcessorChoice choice = healthCheckService.getBestProcessor();
        Instant timestamp = Instant.now();

        processorService.processPayment(request)
            .thenAccept(success -> {
                if (success) {
                    if (choice == HealthCheckService.ProcessorChoice.DEFAULT) {
                        repository.recordDefaultPayment(request.correlationId(), request.amount(), timestamp);
                    } else {
                        repository.recordFallbackPayment(request.correlationId(), request.amount(), timestamp);
                    }
                } else {
                    paymentQueue.offer(request);
                }
            })
            .exceptionally(throwable -> {
                paymentQueue.offer(request);
                return null;
            });
    }

    private void startBackgroundProcessing() {
        CompletableFuture.runAsync(() -> {
            while (true) {
                try {
                    Thread.sleep(10); // 10ms entre verificações
                    if (!paymentQueue.isEmpty() && !processing.get()) {
                        triggerProcessing();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public PaymentSummary getSummary(Instant from, Instant to) {
        if (from == null || to == null) {
            return repository.getSummary();
        }
        return repository.getSummary(from, to);
    }

    public void purgePayments() {
        paymentQueue.clear();
        repository.purge();
    }
}

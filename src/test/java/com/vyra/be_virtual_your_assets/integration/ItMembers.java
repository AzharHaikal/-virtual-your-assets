package com.vyra.be_virtual_your_assets.integration;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.service.WalletService;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

/** Test data for the *IT classes: a fresh active member (with a wallet) per test. */
final class ItMembers {

    private ItMembers() {
    }

    static Member create(MemberRepository memberRepository, WalletService walletService) {
        String phone = "628" + (100_000_000 + ThreadLocalRandom.current().nextInt(899_999_999));
        Member member = Member.builder()
                .firstName("IT")
                .lastName("Member")
                .email("it-" + UUID.randomUUID().toString().substring(0, 18) + "@test.local")
                .phoneNumber(phone)
                .pin("not-used")
                .status(ActiveStatus.ACTIVE)
                .timezone("Asia/Jakarta")
                .build();
        memberRepository.saveAndFlush(member);

        CreateWalletRequest wallet = new CreateWalletRequest();
        wallet.setMemberId(member.getId());
        wallet.setPhoneNumber(phone);
        walletService.createWallet(wallet);
        return member;
    }

    /**
     * Runs the tasks at the same moment on their own threads and returns what each one threw (null when
     * it succeeded). The request pool is small (Hikari 10), so keep the number of tasks modest.
     */
    static List<Throwable> runTogether(List<Callable<?>> tasks) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (Callable<?> task : tasks) {
            futures.add(pool.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        start.countDown();
        List<Throwable> errors = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get(120, TimeUnit.SECONDS);
                errors.add(null);
            } catch (java.util.concurrent.ExecutionException e) {
                errors.add(e.getCause());
            } catch (java.util.concurrent.TimeoutException e) {
                errors.add(e);
            }
        }
        pool.shutdown();
        return errors;
    }
}

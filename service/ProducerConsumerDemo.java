package service;

import model.Account;
import model.CurrentAccount;

public class ProducerConsumerDemo {
    private static final Runnable STOP = () -> {};

    private ProducerConsumerDemo() {}

    public static void run() throws InterruptedException {
        Account account = new CurrentAccount("Producer Consumer", 1000, 0);
        TransactionBuffer buffer = new TransactionBuffer(3);

        Thread producer = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " started.");
            try {
                for (int i = 0; i < 5; i++) {
                    buffer.put(new AccountTransactionTask(account, 100, true));
                    buffer.put(new AccountTransactionTask(account, 50, false));
                }
                buffer.put(STOP);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                System.out.println(Thread.currentThread().getName() + " finished.");
            }
        }, "TransactionProducer");

        Thread consumer = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " started.");
            try {
                while (true) {
                    Runnable transaction = buffer.take();
                    if (transaction == STOP) {
                        break;
                    }
                    transaction.run();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                System.out.println(Thread.currentThread().getName() + " finished.");
            }
        }, "TransactionConsumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("Producer-consumer balance: Rs. " + account.getBalance());
    }
}

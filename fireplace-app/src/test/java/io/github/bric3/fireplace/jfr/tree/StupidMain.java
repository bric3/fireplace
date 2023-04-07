package io.github.bric3.fireplace.jfr.tree;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class StupidMain {
    static Random r = new Random();
    static MessageDigest instance;

    static {
        try {
            instance = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Hello World!");

        work();

        new A().foo();
        new Z().zap();
    }

    private static void work() throws InterruptedException {
        Thread.sleep(1000);
        for (int i = 0; i < 100_100_000; i++) {
            int r = StupidMain.r.nextInt();
            instance.update((byte) (r >> 24));
            instance.update((byte) (r >> 16));
            instance.update((byte) (r >> 8));
            instance.update((byte) (r));
        }
    }

    static class A {
        public void foo() throws InterruptedException {
            new B().bar();
            work();
        }
    }

    static class B {
        public void bar() throws InterruptedException {
            new C().qux();
            work();
        }

    }
    static class C {
        public void qux() throws InterruptedException {
            work();
        }
    }

    static class Z {
        public void zap() throws InterruptedException {
            new C().qux();
            work();
        }
    }
}

package org.exchange;

import java.util.concurrent.atomic.AtomicLong;

public final class Sequence {
    private final AtomicLong seq = new AtomicLong();

    public long next() {
        return seq.incrementAndGet();
    }
}

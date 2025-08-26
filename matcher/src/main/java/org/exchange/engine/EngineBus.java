package org.exchange.engine;

import com.lmax.disruptor.*;
import ex.Ex.Inbound;

public final class EngineBus {

    // 不可变数据载体
    public class Slot {
        public Inbound msg;
    }

    private final RingBuffer<Slot> rb;

    public EngineBus(int ringPow, EventHandler<Slot> handler) {
        rb = RingBuffer.createSingleProducer(Slot::new, 1 << ringPow, new BusySpinWaitStrategy());
        var seq = new Sequence();
        var barrier = rb.newBarrier();
        var proc = new BatchEventProcessor<>(rb, barrier, handler);
        rb.addGatingSequences(proc.getSequence());
        // 留给你的外部启动器：把 proc 提交到专用线程执行
    }

    public void publish(Inbound msg) {
        long s = rb.next();
        try {
            rb.get(s).msg = msg;
        } finally {
            rb.publish(s);
        }
    }
}
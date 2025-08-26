package org.exchange;

import java.math.BigDecimal;

public class OrderKey {
    public final long sequenceId;
    public final BigDecimal price;

    // 构造方法略

    public OrderKey(long sequenceId, BigDecimal price) {
        this.sequenceId = sequenceId;
        this.price = price;
    }





}



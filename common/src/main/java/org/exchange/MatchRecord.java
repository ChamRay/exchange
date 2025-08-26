package org.exchange;

import java.math.BigDecimal;

public class MatchRecord {
    public final BigDecimal price;
    public final BigDecimal amount;
    public final Order takerOrder;
    public final Order makerOrder;

    // 构造方法略

    public MatchRecord(BigDecimal price, BigDecimal amount, Order takerOrder, Order makerOrder) {
        this.price = price;
        this.amount = amount;
        this.takerOrder = takerOrder;
        this.makerOrder = makerOrder;
    }
}


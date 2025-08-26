package org.exchange;

import java.math.BigDecimal;

public class Order {
    public final long sequenceId; // 序号
    public final Direction direction; // 订单方向
    public final BigDecimal price; // 价格
    public BigDecimal amount; // 数量

    // 构造方法略:
    public Order(long sequenceId, Direction direction, BigDecimal price, BigDecimal amount) {
        this.sequenceId = sequenceId;
        this.direction = direction;
        this.price = price;
        this.amount = amount;
    }
}


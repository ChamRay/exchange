package org.exchange;

import java.util.Comparator;
import java.util.TreeMap;

public class OrderBook {
    public final Direction direction; // 方向
    public final TreeMap<OrderKey, Order> book; // 排序树

    public OrderBook(Direction direction) {
        this.direction = direction;
        this.book = new TreeMap<>(direction == Direction.BUY ? SORT_BUY : SORT_SELL);
    }


    public Order getFirst() {
        return this.book.isEmpty() ? null : this.book.firstEntry().getValue();
    }

    public boolean remove(Order order) {
        return this.book.remove(new OrderKey(order.sequenceId, order.price)) != null;
    }

    public boolean add(Order order) {
        return this.book.put(new OrderKey(order.sequenceId, order.price), order) == null;
    }


    // 卖盘
    private static final Comparator<OrderKey> SORT_SELL =
            Comparator
                    // 价格低在前
                    .comparing((OrderKey o) -> o.price)
                    // 时间早在前
                    .thenComparingLong(o -> o.sequenceId);

    // 买盘
    private static final Comparator<OrderKey> SORT_BUY =
            Comparator
                    // 价格高在前
                    .comparing((OrderKey o) -> o.price,Comparator.reverseOrder())
                    // 时间早在前
                    .thenComparingLong(o -> o.sequenceId);


}


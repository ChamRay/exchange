package org.exchange.order;

import ex.Ex;
import org.exchange.book.OrderBook;

import java.util.List;

// 抽象撮合流程
public abstract class AbstractOrder {

    // 撮合入口：统一的流程控制
    public final void process(OrderBook orderBook) {
        // 1. 寻找对手单
        List<Ex.Trade> trades = match(orderBook);

        // 2. 生成成交
        executeTrades(trades);

        // 3. 处理剩余
        handleRemaining(orderBook);
    }

    // 子类可以复用的共用逻辑
    protected void executeTrades(List<Ex.Trade> trades) {
        for (Ex.Trade trade : trades) {
            // 通知成交、生成事件等
        }
    }

    // 订单撮合逻辑，由子实现类实现
    protected abstract List<Ex.Trade> match(OrderBook orderBook);

    //
    protected abstract void handleRemaining(OrderBook orderBook);
}

package org.exchange.order;

import ex.Ex;
import org.exchange.book.OrderBook;

import java.util.List;

// 限价单
public class LimitOrder extends AbstractOrder {
    @Override
    protected List<Ex.Trade> match(OrderBook orderBook) {
        // 查找能满足价格条件的对手单
        return matchLimit(orderBook,this);
    }

    private List<Ex.Trade> matchLimit(OrderBook orderBook, LimitOrder limitOrder) {
        return null;
    }

    @Override
    protected void handleRemaining(OrderBook orderBook) {
        if (this.hasRemaining()) {
            orderBook.onAmend(null); // 挂单
        }
    }

    private boolean hasRemaining() {
        return false;
    }
}

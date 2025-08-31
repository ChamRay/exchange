package org.exchange.engine;

import ex.Ex;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;

import java.util.ArrayDeque;

final class PriceLevels {


    // 卖盘升序，买盘降序（买盘存负价或改比较器）

    // 买单簿
    final Long2ObjectRBTreeMap<ArrayDeque<Node>> bids;
    // 卖单簿
    final Long2ObjectRBTreeMap<ArrayDeque<Node>> asks;

    static final class Node {
        String orderId;
        long price;
        long qty; // 订单剩余数量
        Ex.Side side;
        String account;
        Ex.Tif tif;
    }

    PriceLevels() {
        bids = new Long2ObjectRBTreeMap<>((a, b) -> Long.compare(b, a));
        asks = new Long2ObjectRBTreeMap<>();
    }
}

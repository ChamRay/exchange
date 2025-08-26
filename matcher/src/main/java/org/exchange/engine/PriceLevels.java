package org.exchange.engine;

import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;

import java.util.ArrayDeque;

final class PriceLevels {
    // 卖盘升序，买盘降序（买盘存负价或改比较器）
    final Long2ObjectRBTreeMap<ArrayDeque<Node>> bids, asks;

    static final class Node {
        String oid;
        long qty; /* 链接/引用省略 */
    }

    PriceLevels() {
        bids = new Long2ObjectRBTreeMap<>((a, b) -> Long.compare(b, a));
        asks = new Long2ObjectRBTreeMap<>();
    }
}

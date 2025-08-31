package org.exchange.engine;

import ex.Ex;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Comparator;

final class PriceLevels implements Serializable{


    // 卖盘升序，买盘降序（买盘存负价或改比较器）

    // 买单簿
    final Long2ObjectRBTreeMap<ArrayDeque<Node>> bids;
    // 卖单簿
    final Long2ObjectRBTreeMap<ArrayDeque<Node>> asks;

    static final class Node implements Serializable {
        String orderId;
        long price;
        long qty; // 订单剩余数量
        Ex.Side side;
        String account;
        Ex.Tif tif;
    }

    PriceLevels() {
        bids = new Long2ObjectRBTreeMap<>(Comparator.reverseOrder());
        asks = new Long2ObjectRBTreeMap<>();
    }
}

package org.exchange.engine;

import ex.Ex.*;

import java.util.*;

public final class SingleSymbolEngine implements OrderBook {
    private final PriceLevels book = new PriceLevels();

    public void onNew(Inbound in) {
        if (!in.hasNewOrder()) return;
        NewOrder o = in.getNewOrder();
        if (o.getSide() == Side.BUY) matchBuy(o);
        else matchSell(o);
    }

    public void onCancel(Inbound in) { /* 定位并移除，发回Ack/Reject（略） */ }

    public void onAmend(Inbound in) { /* 减量就地改；改价=撤单+新单（略） */ }

    private void matchBuy(NewOrder o) {
        // 与卖方最优档撮合（核心逻辑示例——省去事件外发/账务）
        var it = book.asks.long2ObjectEntrySet().iterator();
        while (o.getQuantity() > 0 && it.hasNext()) {
            var e = it.next();
            long ask = e.getLongKey();
            if (o.getPrice() < ask) break;
            var q = e.getValue();
            while (o.getQuantity() > 0 && !q.isEmpty()) {
                var head = q.peekFirst();
                long t = Math.min(o.getQuantity(), head.qty);
                o = o.toBuilder().setQuantity(o.getQuantity() - t).build();
                head.qty -= t;
                if (head.qty == 0) q.removeFirst();
                // TODO: emit Trade / Ack via outbound
            }
            if (q.isEmpty()) it.remove();
        }
        if (o.getQuantity() > 0 && o.getTif() == Tif.GTC) {
            book.bids.computeIfAbsent(o.getPrice(), k -> new ArrayDeque<>()).addLast(nodeFrom(o));
        }
    }

    private void matchSell(NewOrder o) { /* 对称 */ }

    private PriceLevels.Node nodeFrom(NewOrder o) {
        var n = new PriceLevels.Node();
        n.oid = o.getClientOrderId();
        n.qty = o.getQuantity();
        return n;
    }

    public void snapshotTo(byte[] out) { /* 省略 */ }
}

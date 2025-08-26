package org.exchange.engine;

import ex.Ex;

public interface OrderBook {
    void onNew(Ex.Inbound inbound);         // 仅接收已通过风控的订单
    void onCancel(Ex.Inbound inbound);
    void onAmend(Ex.Inbound inbound);
    void snapshotTo(byte[] out);            // 快照（深度/挂单）
}

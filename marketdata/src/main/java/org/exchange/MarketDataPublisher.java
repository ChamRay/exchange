package org.exchange;

public interface MarketDataPublisher {
    void publishTrade(byte[] trade);   // 二进制或已编码的PB
    void publishL2(byte[] snapshotOrDelta);
}

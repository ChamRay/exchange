package org.exchange;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MatchResult {
    public final Order takerOrder;
    public final List<MatchRecord> matchRecords = new ArrayList<>();

    // 构造方法略

    public MatchResult(Order takerOrder) {
        this.takerOrder = takerOrder;
    }


    public void add(BigDecimal price, BigDecimal matchedAmount, Order makerOrder) {
        MatchRecord record = new MatchRecord(price, matchedAmount, this.takerOrder, makerOrder);
        matchRecords.add(record);
    }

    @Override
    public String toString() {
        if (matchRecords.isEmpty()) {
            return "no matched.";
        }
        return matchRecords.size() + " matched: " + String.join(", ", this.matchRecords.stream().map(MatchRecord::toString).toArray(String[]::new));
    }
}


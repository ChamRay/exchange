package org.exchange;

import java.nio.channels.Channel;
import java.util.function.Supplier;

public class WalCursor {
    private final Supplier<Channel> channelSupplier;
    private final long from;

    public WalCursor(Supplier<Channel> channelSupplier, long from) {
        this.channelSupplier = channelSupplier;
        this.from = from;
    }

    public Channel open() {
        return channelSupplier.get();
    }

    public long getFrom() {
        return from;
    }
}

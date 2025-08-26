package org.exchange;

import io.netty.channel.*;
import org.exchange.engine.EngineBus;
import ex.Ex.Inbound;

public final class RouteToEngine extends SimpleChannelInboundHandler<Inbound> {
    private final EngineBus bus;

    public RouteToEngine(EngineBus b) {
        this.bus = b;
    }

    protected void channelRead0(ChannelHandlerContext ctx, Inbound msg) {
        bus.publish(msg);
    }
}

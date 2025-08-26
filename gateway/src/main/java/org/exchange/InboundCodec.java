package org.exchange;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import ex.Ex.Inbound;
import io.netty.handler.codec.MessageToMessageCodec;

public final class InboundCodec extends MessageToMessageCodec<ByteBuf, Inbound> {
    protected void encode(ChannelHandlerContext ctx, Inbound msg, java.util.List<Object> out) {
        byte[] b = msg.toByteArray();
        var buf = ctx.alloc().buffer(b.length);
        buf.writeBytes(b);
        out.add(buf);
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, java.util.List<Object> out) throws Exception {
        byte[] b = new byte[in.readableBytes()];
        in.readBytes(b);
        out.add(Inbound.parseFrom(b));
    }
}

package org.exchange;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.exchange.engine.EngineBus;
import org.exchange.risk.PreRiskHandler;

public final class InboundServer {
    private final EngineBus bus;
    private final int port;

    public InboundServer(int port, EngineBus bus) {
        this.port = port;
        this.bus = bus;
    }

    public ChannelFuture start(ChannelHandler tailHandler) throws InterruptedException {
        var boss = new NioEventLoopGroup(1);
        var work = new NioEventLoopGroup();
        return new ServerBootstrap()
                .group(boss, work).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel ch) {
                        ch.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(8 << 20, 0, 4, 0, 4))
                                .addLast(new LengthFieldPrepender(4))
                                .addLast(new InboundCodec())     // ByteBuf <-> ex.Inbound (自定义)
                                .addLast(new PreRiskHandler())   // 余额/限流/基本校验（骨架）
                                .addLast(new RouteToEngine(bus)) // 转发至撮合总线
                                .addLast(tailHandler);           // 可选：回写 Ack/Reject
                    }
                })
                // 外部启动器负责 sync/closeFuture
                .bind(port);
    }
}

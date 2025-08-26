package org.exchange.risk;

import com.lmax.disruptor.EventHandler;
import ex.Ex;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.exchange.engine.EngineBus;

import static ex.Ex.Inbound.PayloadCase.NEW_ORDER;

public class PreRiskHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof Ex.Inbound inbound)) {
            super.channelRead(ctx, msg);
            return;
        }

        boolean passed = true;

        switch (inbound.getPayloadCase()) {
            case NEW_ORDER -> {
                Ex.NewOrder newOrder = inbound.getNewOrder();
                if (newOrder.getPrice() <= 0 || newOrder.getQuantity() <= 0) {
                    System.out.println("风险订单被拦截: " + newOrder.getClientOrderId());
                    passed = false;
                }
            }
            case CANCEL -> {
                Ex.CancelOrder cancel = inbound.getCancel();
                // 可选风控
            }
            case AMEND -> {
                Ex.AmendOrder amend = inbound.getAmend();
                // 可选风控
            }
            case PAYLOAD_NOT_SET -> {
                System.out.println("无效消息");
                passed = false;
            }
        }

        if (passed) {
            super.channelRead(ctx, msg); // 继续传递给下一个 handler
        } else {
            // 拦截消息，不往下传递
        }
    }
}
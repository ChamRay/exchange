package org.exchange;


import com.alibaba.fastjson2.JSON;
import ex.Ex;
import lombok.extern.slf4j.Slf4j;
import org.exchange.book.OrderBook;
import org.exchange.engine.SingleSymbolEngine;
import org.junit.Test;

@Slf4j
public class OderBookTest {

    private static final String TRANS_SYMBOL = "TRANS_1201";


    @Test
    public void test() {
        OrderBook orderBook = new SingleSymbolEngine(out->{
            log.debug("接受到撮合引擎返回的消息：{}", JSON.toJSONString(out));
        });
        Ex.NewOrder sellOrder = newBuyOrder("22222",TRANS_SYMBOL,10,10);
        Ex.Inbound sellInbound = newInbound(sellOrder);
        orderBook.onNew(sellInbound);
        orderBook.snapshotTo();
        Ex.NewOrder buyOrder = newSellOrder("11111",TRANS_SYMBOL,10,11);
        Ex.Inbound buyInbound = newInbound(buyOrder);
        orderBook.onNew(buyInbound);
        orderBook.snapshotTo();
    }

    private static Ex.Inbound newInbound(Ex.NewOrder sellOrder) {
        return Ex.Inbound.newBuilder()
                .setNewOrder(sellOrder)
                .build();
    }

    private static Ex.NewOrder newBuyOrder(String account,String symbol,long price,int quantity) {
        return Ex.NewOrder.newBuilder()
                .setSide(Ex.Side.BUY)
                .setAccount(account)
                .setClientOrderId("buy"+System.currentTimeMillis())
                .setQuantity(quantity)
                .setSymbol(symbol)
                .setPrice(price)
                .setTif(Ex.Tif.GTC)
                .setTsNs(System.nanoTime())
                .build();
    }


    private Ex.NewOrder newSellOrder(String account,String symbol,long price,int quantity) {
        return Ex.NewOrder.newBuilder()
                .setSide(Ex.Side.SELL)
                .setAccount(account)
                .setClientOrderId("sell"+System.currentTimeMillis())
                .setQuantity(quantity)
                .setSymbol(symbol)
                .setPrice(price)
                .setTif(Ex.Tif.GTC)
                .setTsNs(System.nanoTime())
                .build();
    }

}

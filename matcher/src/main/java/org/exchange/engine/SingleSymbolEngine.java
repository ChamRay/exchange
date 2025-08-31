package org.exchange.engine;

import com.google.gson.Gson;
import ex.Ex.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import lombok.extern.slf4j.Slf4j;
import org.exchange.book.OrderBook;
import org.w3c.dom.Node;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// 单交易对撮合引擎
@Slf4j
public final class SingleSymbolEngine implements OrderBook {

    // 转成从外部获取订单簿
    private final PriceLevels book = new PriceLevels();

    // 快速索引：orderId → Node
    final Map<String, PriceLevels.Node> orderIndex = new ConcurrentHashMap<>();


    // Outbound发送接口（可注入网络/MQ实现）
    private final Consumer<Outbound> sender;

    public SingleSymbolEngine(Consumer<Outbound> sender) {
        this.sender = sender;
    }


    @Override
    public void onNew(Inbound in) {
        if (in.hasNewOrder()) {
            NewOrder order = in.getNewOrder();
            Side side = order.getSide();
            if (side == Side.BUY) {
                log.debug("买单交易");
                matchBuy(order);
            } else {
                log.debug("卖单交易");
                matchSell(order);
            }
        }
    }

    @Override
    public void onCancel(Inbound in) {
        CancelOrder cancelOrder = in.getCancel();
        String orderId = cancelOrder.getClientOrderId();

        PriceLevels.Node node = orderIndex.get(orderId);
        if (node == null) {
            log.warn("撤单失败：订单不存在，orderId={}", orderId);
            onReject(orderId, "ORDER_NOT_FOUND");
            return;
        }

        // 确定所属订单簿
        Long2ObjectRBTreeMap<ArrayDeque<PriceLevels.Node>> bookOrder =
                (node.side == Side.BUY) ? book.bids : book.asks;

        // 定位价格档位
        ArrayDeque<PriceLevels.Node> queue = bookOrder.get(node.price);
        if (queue == null) {
            log.warn("撤单失败：未找到价格档位，orderId={}, price={}", orderId, node.price);
            onReject(orderId, "BOOK_LEVEL_NOT_FOUND");
            return;
        }

        // 从队列中移除订单
        boolean removed = queue.removeIf(n -> n.orderId.equals(orderId));
        if (!removed) {
            log.warn("撤单失败：队列中不存在，orderId={}", orderId);
            onReject(orderId, "ORDER_NOT_IN_QUEUE");
            return;
        }

        // 如果价格档位已经空了，则清理掉
        if (queue.isEmpty()) {
            bookOrder.remove(node.price);
        }
        // 更新索引
        orderIndex.remove(orderId);
        log.debug("撤单成功: orderId={}, side={}, price={}, qty={}",
                node.orderId, node.side, node.price, node.qty);
        onAck(orderId,"engine"+System.currentTimeMillis()); // 向交易者回报成功
    }

    @Override
    public void onAmend(Inbound in) {
        AmendOrder amendOrder = in.getAmend();
        long newQty = amendOrder.getNewQty();
        long newPrice = amendOrder.getNewPrice();
        String orderId = amendOrder.getClientOrderId();
        PriceLevels.Node node = orderIndex.get(orderId);
        if (node == null) {
            log.warn("订单不存在: {}", orderId);
            return;
        }
        /* 减量就地改；改价=撤单+新单（略） */
        // === 1. 改量逻辑 ===
        if (newPrice == node.price) {
            if (newQty < node.qty) {
                // 减量：就地修改
                long reduce = node.qty - newQty;
                node.qty = newQty;
                log.debug("订单减量: orderId={}, 原数量={}, 新数量={}", node.orderId, node.qty + reduce, node.qty);
            } else if (newQty > node.qty) {
                // 增量修改：通常不允许（防止篡改排队优先级）
                log.warn("不支持增量修改，忽略: orderId={}, 当前数量={}, 请求数量={}", node.orderId, node.qty, newQty);
            }
            return;
        }
        // === 2. 改价逻辑 ===
        // 改价 = 撤单 + 新单
        log.debug("订单改价: orderId={}, 原价格={}, 新价格={}", node.orderId, node.price, newPrice);
        // 确定所属订单簿
        Long2ObjectRBTreeMap<ArrayDeque<PriceLevels.Node>> bookNode =
                (node.side == Side.BUY) ? book.bids : book.asks;
        ArrayDeque<PriceLevels.Node> orderQueue = bookNode.get(node.price);
        if (orderQueue == null) {
            log.warn("未找到订单所在价格档位: orderId={}, price={}", orderId, node.price);
            return;
        }
        boolean found = false;
        for (PriceLevels.Node n : orderQueue) {
            if (n.orderId.equals(orderId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            log.warn("订单在队列中不存在: {}", orderId);
        }
        CancelOrder cancelOrder = CancelOrder.newBuilder()
                .setSymbol(amendOrder.getSymbol())
                .setClientOrderId(orderId)
                .setAccount(amendOrder.getAccount())
                .setTsNs(System.nanoTime())
                .build();
        in = in.toBuilder()
                .setCancel(cancelOrder)
                .build();
        onCancel(in);
        NewOrder newOrder = NewOrder.newBuilder()
                .setClientOrderId(amendOrder.getClientOrderId())
                .setSymbol(amendOrder.getSymbol())
                .setSide(node.side)
                .setPrice(amendOrder.getNewPrice())
                .setQuantity(newQty)
                .setTif(node.tif)
                .setAccount(node.account)
                .setAccount(amendOrder.getAccount())
                .setTsNs(System.nanoTime())
                .build();
        in = in.toBuilder()
                .setNewOrder(newOrder)
                .build();
        onNew(in);
    }

    private void matchBuy(NewOrder order) {
        Tif tif = order.getTif();
        // 与卖方最优档撮合（核心逻辑示例——省去事件外发/账务）
        var bookedSellOrderIter = book.asks.long2ObjectEntrySet().iterator();
        long quantity = order.getQuantity();
        long price = order.getPrice();
        while (quantity > 0 && bookedSellOrderIter.hasNext()) {
            // Map<Price,Node>
            Long2ObjectMap.Entry<ArrayDeque<PriceLevels.Node>> bookedSellEntry = bookedSellOrderIter.next();
            long currAskPrice = bookedSellEntry.getLongKey();
            // 订单买价小于订单簿卖价
            if (price < currAskPrice) {
                log.debug("新加入买单价小于订单簿当前卖单价格，撮合不成立！");
                break;
            }
            // 获取订单簿节点队列
            ArrayDeque<PriceLevels.Node> curAskOrderNode = bookedSellEntry.getValue();
            while (!curAskOrderNode.isEmpty()) {
                PriceLevels.Node headNode = curAskOrderNode.peekFirst();
                log.debug("订单请求交易数量：{}，当前订单簿卖单可交易数量：{}", quantity, headNode.qty);
                long tradeNum = Math.min(quantity, headNode.qty);
                log.debug("可撮合交易数量：{}", tradeNum);
                order = order.toBuilder()
                        .setQuantity(quantity - tradeNum)
                        .build();
                headNode.qty -= tradeNum;
                if (headNode.qty == 0) curAskOrderNode.removeFirst();
                Trade trade = Trade.newBuilder()
                        .setBuyOrderId(order.getClientOrderId())   // 买单 ID（当前订单）
                        .setSellOrderId(headNode.orderId) // 卖单 ID
                        .setPrice(headNode.price)          // 成交价（通常取挂单价）
                        .setQuantity(tradeNum)             // 成交数量
                        .setTsNs(System.nanoTime())
                        .build();
                onTrade(trade);
                quantity -= tradeNum;
                if (quantity <= 0) {
                    break;
                }
            }
            if (curAskOrderNode.isEmpty()) bookedSellOrderIter.remove();
        }
        if (quantity > 0 && tif == Tif.GTC) {
            book.bids.computeIfAbsent(price, k -> new ArrayDeque<>()).addLast(nodeFrom(order));
        }
    }

    private void matchSell(NewOrder order) {
        var bookedBuyOrderIter = book.bids.long2ObjectEntrySet().iterator();
        long quantity = order.getQuantity();
        long price = order.getPrice();
        Tif tif = order.getTif();
        while (quantity > 0 && bookedBuyOrderIter.hasNext()) {
            Long2ObjectMap.Entry<ArrayDeque<PriceLevels.Node>> bookedBuyEntry = bookedBuyOrderIter.next();
            long curBidPrice = bookedBuyEntry.getLongKey();
            // 订单卖价大于订单簿买价
            if (price > curBidPrice) {
                log.debug("新加入卖单价大于订单簿当前买单价格，撮合不成立！");
                break;
            }
            // 获取订单簿节点队列
            ArrayDeque<PriceLevels.Node> curBidOrderNode = bookedBuyEntry.getValue();
            while (!curBidOrderNode.isEmpty()) {
                PriceLevels.Node headNode = curBidOrderNode.getFirst();
                log.debug("订单请求交易数量：{}，当前订单簿买单可交易数量：{}", quantity, headNode.qty);
                long tradeNum = Math.min(quantity, headNode.qty);
                log.debug("可撮合交易数量：{}", tradeNum);
                order = order.toBuilder()
                        .setQuantity(quantity - tradeNum)
                        .build();
                headNode.qty -= tradeNum;
                // 挂单完全撮合，移除当前买单节点
                if (headNode.qty == 0) curBidOrderNode.removeFirst();
                // === 发起买单交易逻辑 ===
                Trade trade = Trade.newBuilder()
                        .setBuyOrderId(headNode.orderId)   // 买单 ID
                        .setSellOrderId(order.getClientOrderId()) // 卖单 ID（当前订单）
                        .setPrice(headNode.price)          // 成交价（通常取挂单价）
                        .setQuantity(tradeNum)             // 成交数量
                        .setTsNs(System.nanoTime())
                        .build();
                onTrade(trade);
                quantity -= tradeNum;
                if (quantity <= 0) {
                    log.debug("当前订单撮合交易完成，订单数量已全部成交。");
                    break;
                }
            }
            if (curBidOrderNode.isEmpty()) bookedBuyOrderIter.remove();
        }
        if (quantity > 0 && tif == Tif.GTC) {
            book.asks.computeIfAbsent(price, k -> new ArrayDeque<>()).addLast(nodeFrom(order));
        }
    }

    private PriceLevels.Node nodeFrom(NewOrder order) {
        PriceLevels.Node node = new PriceLevels.Node();
        node.orderId = order.getClientOrderId();
        node.qty = order.getQuantity();
        return node;
    }

    @Override
    public void snapshotTo() {
        try (FileWriter writer = new FileWriter("orderbook.json")) {
            Gson gson = new Gson();
            Map<String, Object> snapshot = Map.of(
                    "bids", book.bids,
                    "asks", book.asks
            );
            gson.toJson(snapshot, writer);
        } catch (Exception e) {
            log.error("生成快照失败", e);
        }
    }

    @Override
    public void onAck(String clientOrderId, String engineId) {
        Ack ack = Ack.newBuilder()
                .setClientOrderId(clientOrderId)
                .setEngineOrderId(engineId)
                .build();
        Outbound out = Outbound.newBuilder()
                .setAck(ack)
                .build();
        sender.accept(out);
    }

    @Override
    public void onReject(String clientOrderId, String msg) {
        Reject reject = Reject.newBuilder()
                .setClientOrderId(clientOrderId)
                .setReason(msg)
                .build();
        Outbound out = Outbound.newBuilder()
                .setReject(reject)
                .build();
        sender.accept(out);
    }

    @Override
    public void onTrade(Trade trade) {
        Outbound outbound = Outbound.newBuilder()
                .setTrade(trade)
                .build();
        this.sender.accept(outbound);
    }

}

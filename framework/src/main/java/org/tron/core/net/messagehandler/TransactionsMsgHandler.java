package org.bos.core.net.messagehandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bos.core.config.args.Args;
import org.bos.core.exception.P2pException;
import org.bos.core.exception.P2pException.TypeEnum;
import org.bos.core.net.BosNetDelegate;
import org.bos.core.net.message.TransactionMessage;
import org.bos.core.net.message.TransactionsMessage;
import org.bos.core.net.message.BosMessage;
import org.bos.core.net.peer.Item;
import org.bos.core.net.peer.PeerConnection;
import org.bos.core.net.service.AdvService;
import org.bos.protos.Protocol.Inventory.InventoryType;
import org.bos.protos.Protocol.ReasonCode;
import org.bos.protos.Protocol.Transaction;
import org.bos.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j(topic = "net")
@Component
public class TransactionsMsgHandler implements BosMsgHandler {

  private static int MAX_BOS_SIZE = 50_000;
  private static int MAX_SMART_CONTRACT_SUBMIT_SIZE = 100;
  @Autowired
  private BosNetDelegate bosNetDelegate;
  @Autowired
  private AdvService advService;

  //  private static int TIME_OUT = 10 * 60 * 1000;
  private BlockingQueue<BosEvent> smartContractQueue = new LinkedBlockingQueue(MAX_BOS_SIZE);

  private BlockingQueue<Runnable> queue = new LinkedBlockingQueue();

  private int threadNum = Args.getInstance().getValidateSignThreadNum();
  private ExecutorService trxHandlePool = new ThreadPoolExecutor(threadNum, threadNum, 0L,
      TimeUnit.MILLISECONDS, queue);

  private ScheduledExecutorService smartContractExecutor = Executors
      .newSingleThreadScheduledExecutor();

  public void init() {
    handleSmartContract();
  }

  public void close() {
    smartContractExecutor.shutdown();
  }

  public boolean isBusy() {
    return queue.size() + smartContractQueue.size() > MAX_BOS_SIZE;
  }

  @Override
  public void processMessage(PeerConnection peer, BosMessage msg) throws P2pException {
    TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
    check(peer, transactionsMessage);
    for (Transaction trx : transactionsMessage.getTransactions().getTransactionsList()) {
      int type = trx.getRawData().getContract(0).getType().getNumber();
      if (type == ContractType.TriggerSmartContract_VALUE
          || type == ContractType.CreateSmartContract_VALUE) {
        if (!smartContractQueue.offer(new BosEvent(peer, new TransactionMessage(trx)))) {
          logger.warn("Add smart contract failed, queueSize {}:{}", smartContractQueue.size(),
              queue.size());
        }
      } else {
        trxHandlePool.submit(() -> handleTransaction(peer, new TransactionMessage(trx)));
      }
    }
  }

  private void check(PeerConnection peer, TransactionsMessage msg) throws P2pException {
    for (Transaction trx : msg.getTransactions().getTransactionsList()) {
      Item item = new Item(new TransactionMessage(trx).getMessageId(), InventoryType.BOS);
      if (!peer.getAdvInvRequest().containsKey(item)) {
        throw new P2pException(TypeEnum.BAD_MESSAGE,
            "trx: " + msg.getMessageId() + " without request.");
      }
      peer.getAdvInvRequest().remove(item);
    }
  }

  private void handleSmartContract() {
    smartContractExecutor.scheduleWithFixedDelay(() -> {
      try {
        while (queue.size() < MAX_SMART_CONTRACT_SUBMIT_SIZE) {
          BosEvent event = smartContractQueue.take();
          trxHandlePool.submit(() -> handleTransaction(event.getPeer(), event.getMsg()));
        }
      } catch (Exception e) {
        logger.error("Handle smart contract exception.", e);
      }
    }, 1000, 20, TimeUnit.MILLISECONDS);
  }

  private void handleTransaction(PeerConnection peer, TransactionMessage trx) {
    if (peer.isDisconnect()) {
      logger.warn("Drop trx {} from {}, peer is disconnect.", trx.getMessageId(),
          peer.getInetAddress());
      return;
    }

    if (advService.getMessage(new Item(trx.getMessageId(), InventoryType.BOS)) != null) {
      return;
    }

    try {
      bosNetDelegate.pushTransaction(trx.getTransactionCapsule());
      advService.broadcast(trx);
    } catch (P2pException e) {
      logger.warn("Bos {} from peer {} process failed. type: {}, reason: {}",
          trx.getMessageId(), peer.getInetAddress(), e.getType(), e.getMessage());
      if (e.getType().equals(TypeEnum.BAD_BOS)) {
        peer.disconnect(ReasonCode.BAD_TX);
      }
    } catch (Exception e) {
      logger.error("Bos {} from peer {} process failed.", trx.getMessageId(), peer.getInetAddress(),
          e);
    }
  }

  class BosEvent {

    @Getter
    private PeerConnection peer;
    @Getter
    private TransactionMessage msg;
    @Getter
    private long time;

    public BosEvent(PeerConnection peer, TransactionMessage msg) {
      this.peer = peer;
      this.msg = msg;
      this.time = System.currentTimeMillis();
    }
  }
}
package org.bos.common.zksnark;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import org.bos.api.BosZksnarkGrpc;
import org.bos.api.ZksnarkGrpcAPI.ZksnarkRequest;
import org.bos.api.ZksnarkGrpcAPI.ZksnarkResponse.Code;
import org.bos.core.capsule.TransactionCapsule;
import org.bos.protos.Protocol.Transaction;

public class ZksnarkClient {

  public static final ZksnarkClient instance = new ZksnarkClient();

  private BosZksnarkGrpc.BosZksnarkBlockingStub blockingStub;

  public ZksnarkClient() {
    blockingStub = BosZksnarkGrpc.newBlockingStub(ManagedChannelBuilder
        .forTarget("127.0.0.1:60051")
        .usePlaintext()
        .build());
  }

  public static ZksnarkClient getInstance() {
    return instance;
  }

  public boolean checkZksnarkProof(Transaction transaction, byte[] sighash, long valueBalance) {
    String txId = new TransactionCapsule(transaction).getTransactionId().toString();
    ZksnarkRequest request = ZksnarkRequest.newBuilder()
        .setTransaction(transaction)
        .setTxId(txId)
        .setSighash(ByteString.copyFrom(sighash))
        .setValueBalance(valueBalance)
        .build();
    return blockingStub.checkZksnarkProof(request).getCode() == Code.SUCCESS;
  }
}

/*
 * java-bos is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-bos is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bos.core.capsule.utils;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bos.common.parameter.CommonParameter;
import org.bos.common.runtime.InternalTransaction;
import org.bos.common.runtime.ProgramResult;
import org.bos.common.runtime.vm.LogInfo;
import org.bos.common.utils.DecodeUtil;
import org.bos.core.capsule.BlockCapsule;
import org.bos.core.capsule.ReceiptCapsule;
import org.bos.core.capsule.TransactionCapsule;
import org.bos.core.capsule.TransactionInfoCapsule;
import org.bos.core.db.TransactionTrace;
import org.bos.protos.Protocol;
import org.bos.protos.Protocol.Transaction;
import org.bos.protos.Protocol.Transaction.Contract;
import org.bos.protos.Protocol.TransactionInfo;
import org.bos.protos.Protocol.TransactionInfo.Log;
import org.bos.protos.Protocol.TransactionInfo.code;
import org.bos.protos.contract.BalanceContract.TransferContract;

@Slf4j(topic = "capsule")
public class TransactionUtil {

  public static Transaction newGenesisTransaction(byte[] key, long value)
      throws IllegalArgumentException {

    if (!DecodeUtil.addressValid(key)) {
      throw new IllegalArgumentException("Invalid address");
    }
    TransferContract transferContract = TransferContract.newBuilder()
        .setAmount(value)
        .setOwnerAddress(ByteString.copyFrom("0x000000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(key))
        .build();

    return new TransactionCapsule(transferContract,
        Contract.ContractType.TransferContract).getInstance();
  }

  public static TransactionInfoCapsule buildTransactionInfoInstance(TransactionCapsule trxCap,
      BlockCapsule block, TransactionTrace trace) {

    TransactionInfo.Builder builder = TransactionInfo.newBuilder();
    ReceiptCapsule traceReceipt = trace.getReceipt();
    builder.setResult(code.SUCESS);
    if (StringUtils.isNoneEmpty(trace.getRuntimeError()) || Objects
        .nonNull(trace.getRuntimeResult().getException())) {
      builder.setResult(code.FAILED);
      builder.setResMessage(ByteString.copyFromUtf8(trace.getRuntimeError()));
    }
    builder.setId(ByteString.copyFrom(trxCap.getTransactionId().getBytes()));
    ProgramResult programResult = trace.getRuntimeResult();
    long fee =
        programResult.getRet().getFee() + traceReceipt.getEnergyFee()
            + traceReceipt.getNetFee() + traceReceipt.getMultiSignFee();
    ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
    ByteString ContractAddress = ByteString.copyFrom(programResult.getContractAddress());

    builder.setFee(fee);
    builder.addContractResult(contractResult);
    builder.setContractAddress(ContractAddress);
    builder.setUnfreezeAmount(programResult.getRet().getUnfreezeAmount());
    builder.setAssetIssueID(programResult.getRet().getAssetIssueID());
    builder.setExchangeId(programResult.getRet().getExchangeId());
    builder.setWithdrawAmount(programResult.getRet().getWithdrawAmount());
    builder.setExchangeReceivedAmount(programResult.getRet().getExchangeReceivedAmount());
    builder.setExchangeInjectAnotherAmount(programResult.getRet().getExchangeInjectAnotherAmount());
    builder.setExchangeWithdrawAnotherAmount(
        programResult.getRet().getExchangeWithdrawAnotherAmount());
    builder.setShieldedTransactionFee(programResult.getRet().getShieldedTransactionFee());

    List<Log> logList = new ArrayList<>();
    programResult.getLogInfoList().forEach(
        logInfo -> {
          logList.add(LogInfo.buildLog(logInfo));
        }
    );
    builder.addAllLog(logList);

    if (Objects.nonNull(block)) {
      builder.setBlockNumber(block.getInstance().getBlockHeader().getRawData().getNumber());
      builder.setBlockTimeStamp(block.getInstance().getBlockHeader().getRawData().getTimestamp());
    }

    builder.setReceipt(traceReceipt.getReceipt());

    if (CommonParameter.getInstance().isSaveInternalTx() && null != programResult.getInternalTransactions()) {
      for (InternalTransaction internalTransaction : programResult
          .getInternalTransactions()) {
        Protocol.InternalTransaction.Builder internalBosBuilder = Protocol.InternalTransaction
            .newBuilder();
        // set hash
        internalBosBuilder.setHash(ByteString.copyFrom(internalTransaction.getHash()));
        // set caller
        internalBosBuilder.setCallerAddress(ByteString.copyFrom(internalTransaction.getSender()));
        // set TransferTo
        internalBosBuilder
            .setTransferToAddress(ByteString.copyFrom(internalTransaction.getTransferToAddress()));
        //TODO: "for loop" below in future for multiple token case, we only have one for now.
        Protocol.InternalTransaction.CallValueInfo.Builder callValueInfoBuilder =
            Protocol.InternalTransaction.CallValueInfo.newBuilder();
        // trx will not be set token name
        callValueInfoBuilder.setCallValue(internalTransaction.getValue());
        // Just one transferBuilder for now.
        internalBosBuilder.addCallValueInfo(callValueInfoBuilder);
        internalTransaction.getTokenInfo().forEach((tokenId, amount) -> {
          internalBosBuilder.addCallValueInfo(Protocol.InternalTransaction.CallValueInfo.newBuilder().setTokenId(tokenId).setCallValue(amount));
        });
        // Token for loop end here
        internalBosBuilder.setNote(ByteString.copyFrom(internalTransaction.getNote().getBytes()));
        internalBosBuilder.setRejected(internalTransaction.isRejected());
        builder.addInternalTransactions(internalBosBuilder);
      }
    }

    return new TransactionInfoCapsule(builder.build());
  }
}

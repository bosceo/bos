package stest.bos.wallet.onlinestress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.bos.api.GrpcAPI;
import org.bos.api.GrpcAPI.EmptyMessage;
import org.bos.api.GrpcAPI.ProposalList;
import org.bos.api.WalletGrpc;
import org.bos.api.WalletSolidityGrpc;
import org.bos.core.Wallet;
import org.bos.protos.Protocol.Block;
import org.bos.protos.Protocol.ChainParameters;
import org.bos.protos.Protocol.Transaction;
import org.bos.protos.contract.BalanceContract.TransferContract;
import stest.bos.wallet.common.client.Configuration;
import stest.bos.wallet.common.client.Parameter.CommonConstant;
import stest.bos.wallet.common.client.utils.PublicMethed;


@Slf4j
public class TestApproveProposal {

  private static final long now = System.currentTimeMillis();
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  //Witness 47.93.33.201
  private final String witnessKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  //Witness 123.56.10.6
  private final String witnessKey003 = Configuration.getByPath("testng.conf")
      .getString("witness.key3");
  //Wtiness 39.107.80.135
  private final String witnessKey004 = Configuration.getByPath("testng.conf")
      .getString("witness.key4");
  //Witness 47.93.184.2
  private final String witnessKey005 = Configuration.getByPath("testng.conf")
      .getString("witness.key5");
  private final byte[] witness001Address = PublicMethed.getFinalAddress(witnessKey001);
  private final byte[] witness002Address = PublicMethed.getFinalAddress(witnessKey002);
  private final byte[] witness003Address = PublicMethed.getFinalAddress(witnessKey003);
  private final byte[] witness004Address = PublicMethed.getFinalAddress(witnessKey004);
  private final byte[] witness005Address = PublicMethed.getFinalAddress(witnessKey005);
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = true)
  public void testApproveProposal() {
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    //proposalMap.put(25L, 1L);
    proposalMap.put(27L, 0L);
    //proposalMap.put(28L, 1L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));

    //Get proposal list after approve
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    logger.info(Integer.toString(listProposals.get().getProposals(0).getApprovalsCount()));

    String[] witnessKey = {

        "369F095838EB6EED45D4F6312AF962D5B9DE52927DA9F04174EE49F9AF54BC77",
        "9FD8E129DE181EA44C6129F727A6871440169568ADE002943EAD0E7A16D8EDAC",

    };
    byte[] witnessAddress;
    for (String key : witnessKey) {
      witnessAddress = PublicMethed.getFinalAddress(key);
      PublicMethed.approveProposal(witnessAddress, key, proposalId,
          true, blockingStubFull);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Test(enabled = true)
  public void testGetAllNodeBlockNum() throws InterruptedException {
    String[] nodeIp = {
        "47.93.14.253:50051",
        "39.105.28.73:50051",
        "101.200.51.70:50051",
        "47.94.209.241:50051",
        "47.94.148.150:50051",
        "47.94.9.222:50051",
        "39.107.87.203:50051"
    };

    for (String ip : nodeIp) {
      fullnode = ip;
      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
      Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
      logger.info("ip " + ip + ", block num is : " + currentBlockNum);

      Integer times = 0;
      while (times++ <= -100) {
        currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
        Transaction.Contract contract;
        TransferContract transferContract;
        Integer triggerNum = 0;
        Integer transactionNum = 0;
        for (Transaction transaction : currentBlock.getTransactionsList()) {
          if (transaction.getRawData().getContract(0).getContractName().isEmpty()) {
            transactionNum++;
          } else {
            triggerNum++;

          }

        }

        logger.info("ip " + ip + ", block num is : " + currentBlockNum);
        logger.info("Transfer contract num is " + transactionNum);
        logger.info("Trigger contract num is " + triggerNum);
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }


      }


    }
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }

  @Test(enabled = true)
  public void testGetChainParameters() {
    //Set the default map
    HashMap<String, Long> defaultCommitteeMap = new HashMap<String, Long>();
    defaultCommitteeMap.put("MAINTENANCE_TIME_INTERVAL", 300000L);
    defaultCommitteeMap.put("ACCOUNT_UPGRADE_COST", 9999000000L);
    defaultCommitteeMap.put("CREATE_ACCOUNT_FEE", 100000L);
    defaultCommitteeMap.put("TRANSACTION_FEE", 10L);
    defaultCommitteeMap.put("ASSET_ISSUE_FEE", 1024000000L);
    defaultCommitteeMap.put("WITNESS_PAY_PER_BLOCK", 32000000L);
    defaultCommitteeMap.put("WITNESS_STANDBY_ALLOWANCE", 115200000000L);
    defaultCommitteeMap.put("CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT", 0L);
    defaultCommitteeMap.put("CREATE_NEW_ACCOUNT_BANDWIDTH_RATE", 1L);

    ChainParameters chainParameters = blockingStubFull
        .getChainParameters(EmptyMessage.newBuilder().build());
    Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      logger.info("Index is:" + i);
      logger.info(getChainParameters.get().getChainParameter(i).getKey());
      logger.info(Long.toString(getChainParameters.get().getChainParameter(i).getValue()));
    }

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}



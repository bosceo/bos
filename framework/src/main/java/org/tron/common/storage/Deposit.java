package org.bos.common.storage;

import org.bos.common.runtime.vm.DataWord;
import org.bos.core.capsule.AccountCapsule;
import org.bos.core.capsule.AssetIssueCapsule;
import org.bos.core.capsule.BlockCapsule;
import org.bos.core.capsule.BytesCapsule;
import org.bos.core.capsule.ContractCapsule;
import org.bos.core.capsule.ProposalCapsule;
import org.bos.core.capsule.TransactionCapsule;
import org.bos.core.capsule.VotesCapsule;
import org.bos.core.capsule.WitnessCapsule;
import org.bos.core.db.Manager;
import org.bos.core.vm.program.Storage;
import org.bos.core.vm.repository.Key;
import org.bos.core.vm.repository.Value;
import org.bos.protos.Protocol;

public interface Deposit {

  Manager getDbManager();

  AccountCapsule createAccount(byte[] address, Protocol.AccountType type);

  AccountCapsule createAccount(byte[] address, String accountName, Protocol.AccountType type);

  AccountCapsule getAccount(byte[] address);

  WitnessCapsule getWitness(byte[] address);

  VotesCapsule getVotesCapsule(byte[] address);

  ProposalCapsule getProposalCapsule(byte[] id);

  BytesCapsule getDynamic(byte[] bytesKey);

  void deleteContract(byte[] address);

  void createContract(byte[] address, ContractCapsule contractCapsule);

  ContractCapsule getContract(byte[] address);

  void updateContract(byte[] address, ContractCapsule contractCapsule);

  void updateAccount(byte[] address, AccountCapsule accountCapsule);

  void saveCode(byte[] address, byte[] code);

  byte[] getCode(byte[] address);

  void putStorageValue(byte[] address, DataWord key, DataWord value);

  DataWord getStorageValue(byte[] address, DataWord key);

  Storage getStorage(byte[] address);

  long getBalance(byte[] address);

  long addBalance(byte[] address, long value);

  Deposit newDepositChild();

  void setParent(Deposit deposit);

  void commit();

  void putAccount(Key key, Value value);

  void putTransaction(Key key, Value value);

  void putBlock(Key key, Value value);

  void putWitness(Key key, Value value);

  void putCode(Key key, Value value);

  void putContract(Key key, Value value);

  void putStorage(Key key, Storage cache);

  void putVotes(Key key, Value value);

  void putProposal(Key key, Value value);

  void putDynamicProperties(Key key, Value value);

  void putAccountValue(byte[] address, AccountCapsule accountCapsule);

  void putVoteValue(byte[] address, VotesCapsule votesCapsule);

  void putProposalValue(byte[] address, ProposalCapsule proposalCapsule);

  void putDynamicPropertiesWithLatestProposalNum(long num);

  long getLatestProposalNum();

  long getWitnessAllowanceFrozenTime();

  long getMaintenanceTimeInterval();

  long getNextMaintenanceTime();

  long addTokenBalance(byte[] address, byte[] tokenId, long value);

  long getTokenBalance(byte[] address, byte[] tokenId);

  AssetIssueCapsule getAssetIssue(byte[] tokenId);

  TransactionCapsule getTransaction(byte[] trxHash);

  BlockCapsule getBlock(byte[] blockHash);

  byte[] getBlackHoleAddress();

  public AccountCapsule createNormalAccount(byte[] address);


}


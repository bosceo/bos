package org.bos.core.vm.repository;

import org.bos.common.runtime.vm.DataWord;
import org.bos.core.capsule.AccountCapsule;
import org.bos.core.capsule.AssetIssueCapsule;
import org.bos.core.capsule.BlockCapsule;
import org.bos.core.capsule.BytesCapsule;
import org.bos.core.capsule.ContractCapsule;
import org.bos.core.store.AssetIssueStore;
import org.bos.core.store.AssetIssueV2Store;
import org.bos.core.store.DynamicPropertiesStore;
import org.bos.core.vm.program.Storage;
import org.bos.protos.Protocol;

public interface Repository {

  AssetIssueCapsule getAssetIssue(byte[] tokenId);

  AssetIssueV2Store getAssetIssueV2Store();

  AssetIssueStore getAssetIssueStore();

  DynamicPropertiesStore getDynamicPropertiesStore();

  AccountCapsule createAccount(byte[] address, Protocol.AccountType type);

  AccountCapsule createAccount(byte[] address, String accountName, Protocol.AccountType type);

  AccountCapsule getAccount(byte[] address);

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

  Repository newRepositoryChild();

  void setParent(Repository deposit);

  void commit();

  void putAccount(Key key, Value value);

  void putCode(Key key, Value value);

  void putContract(Key key, Value value);

  void putStorage(Key key, Storage cache);

  void putAccountValue(byte[] address, AccountCapsule accountCapsule);

  long addTokenBalance(byte[] address, byte[] tokenId, long value);

  long getTokenBalance(byte[] address, byte[] tokenId);

  long getAccountLeftEnergyFromFreeze(AccountCapsule accountCapsule);

  long calculateGlobalEnergyLimit(AccountCapsule accountCapsule);

  byte[] getBlackHoleAddress();

  BlockCapsule getBlockByNum(final long num);

  AccountCapsule createNormalAccount(byte[] address);

}

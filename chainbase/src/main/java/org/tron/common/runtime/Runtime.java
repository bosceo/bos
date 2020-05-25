package org.bos.common.runtime;

import org.bos.core.db.TransactionContext;
import org.bos.core.exception.ContractExeException;
import org.bos.core.exception.ContractValidateException;


public interface Runtime {

  void execute(TransactionContext context)
      throws ContractValidateException, ContractExeException;

  ProgramResult getResult();

  String getRuntimeError();

}

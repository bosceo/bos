package org.bos.core.actuator;

import org.bos.core.exception.ContractExeException;
import org.bos.core.exception.ContractValidateException;

public interface Actuator2 {

  void execute(Object object) throws ContractExeException;

  void validate(Object object) throws ContractValidateException;
}
package org.bos.common.logsfilter.capsule;

import static org.bos.common.logsfilter.EventPluginLoader.matchFilter;

import lombok.Getter;
import lombok.Setter;
import org.bos.common.logsfilter.EventPluginLoader;
import org.bos.common.logsfilter.trigger.ContractLogTrigger;

public class ContractLogTriggerCapsule extends TriggerCapsule {

  @Getter
  @Setter
  private ContractLogTrigger contractLogTrigger;

  public ContractLogTriggerCapsule(ContractLogTrigger contractLogTrigger) {
    this.contractLogTrigger = contractLogTrigger;
  }

  public void setLatestSolidifiedBlockNumber(long latestSolidifiedBlockNumber) {
    contractLogTrigger.setLatestSolidifiedBlockNumber(latestSolidifiedBlockNumber);
  }

  @Override
  public void processTrigger() {
    if (matchFilter(contractLogTrigger)) {
      EventPluginLoader.getInstance().postContractLogTrigger(contractLogTrigger);
    }
  }
}

package org.bos.core.utils;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.bos.core.actuator.AbstractActuator;

@Slf4j(topic = "TransactionRegister")
public class TransactionRegister {

  public static void registerActuator() {
    Reflections reflections = new Reflections("org.bos");
    Set<Class<? extends AbstractActuator>> subTypes = reflections
        .getSubTypesOf(AbstractActuator.class);
    for (Class clazz : subTypes) {
      try {
        clazz.newInstance();
      } catch (Exception e) {
        logger.error("{} contract actuator register fail!", clazz, e);
      }
    }
  }

}

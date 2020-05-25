package org.bos.core.services.ratelimiter.adapter;

import org.bos.core.services.ratelimiter.RuntimeData;

public interface IRateLimiter {

  boolean acquire(RuntimeData data);

}

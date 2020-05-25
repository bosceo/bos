package org.bos.core.services.ratelimiter.adapter;

public interface IPreemptibleRateLimiter extends IRateLimiter {

  void release();
}

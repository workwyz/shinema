package cn.shinema.core.lock;

import org.apache.curator.framework.recipes.locks.InterProcessLock;

public interface ZkLock {

	public int waitTimeOut = 5;

	public InterProcessLock tryLock(String lockName) throws Exception;

}

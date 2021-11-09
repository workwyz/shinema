package cn.shinema.core.port.adapter.lock;

import cn.shinema.core.lock.ZkLock;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@ConditionalOnBean({CuratorFramework.class})
public class CuratorLock implements ZkLock {

    private CuratorFramework curator;

    private String lockPath = "zklock";

    public CuratorLock(CuratorFramework curator, String lockPath) {
        super();
        this.curator = curator;
        this.lockPath = lockPath;
    }

    public InterProcessLock tryLock(String lockName) throws Exception {
        String lockPath = String.format("%s%s%s%s", "/", this.lockPath, "/", lockName);
        return new InterProcessMutex(curator, lockPath);
    }

}

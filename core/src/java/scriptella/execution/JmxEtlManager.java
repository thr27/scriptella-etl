/*
 * Copyright 2006-2012 The Scriptella Project Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scriptella.execution;

import scriptella.core.SystemException;
import scriptella.core.ThreadSafe;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link scriptella.execution.JmxEtlManagerMBean}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
@ThreadSafe
public class JmxEtlManager implements JmxEtlManagerMBean {
	private static final int MAX_MBEANS_PER_FILE = 1000;
    private static Logger LOG = LoggerFactory.getLogger(JmxEtlManager.class.getName());
    private static final String MBEAN_NAME_PREFIX = "scriptella:type=etl";
    private static volatile MBeanServer mbeanServer;
    private MBeanServer server;
    private EtlContext ctx;
    private Thread etlThread;
    private ObjectName name;
    private Date started;

    public JmxEtlManager(EtlContext ctx) {
        this.ctx = ctx;
    }

    public synchronized long getExecutedStatementsCount() {
        return ctx.getSession().getExecutedStatementsCount();
    }

    public synchronized Date getStartDate() {
        return started;
    }

    public synchronized double getThroughput() {
        long cnt = getExecutedStatementsCount();
        if (cnt > 0 && started != null) {
            double ti = System.currentTimeMillis() - started.getTime();
            return (1000 * cnt) / ti;
        }
        return 0;
    }

    /**
     * Sets mbean server to use when registering mbeans.
     * <p>By default {@link java.lang.management.ManagementFactory#getPlatformMBeanServer()} is used.
     *
     * @param mbeanServer mbean server.
     */
    public static void setMBeanServer(MBeanServer mbeanServer) {
        JmxEtlManager.mbeanServer = mbeanServer;
    }

    /**
     * Registers this manager as a JMX mbean.
     */
    public synchronized void register() {
        if (name != null) {
            throw new IllegalStateException("MBean already registered");
        }
        server = getMBeanServer();
        String url = ctx.getScriptFileURL().toString();
        boolean registered = false;
        //BUG-54489 Do additional synchronization on a platform mbean server to avoid race conditions.
        //Simply using synchronized will not help since classes could belong to different classloaders
        synchronized (getMBeanServer()) {
            for (int i = 0; i < MAX_MBEANS_PER_FILE; i++) {
                if (name == null || server.isRegistered(name)) {
                    registered = true;
                    name = toObjectName(url, i);
                } else {
                    registered = false;
                    break;
                }
            }
            etlThread = Thread.currentThread();
            if (!registered) {
                try {
                    server.registerMBean(this, name);
                    started = new Date();
                    LOG.info("Registered JMX mbean: " + name);
                } catch (Exception e) {
                    throw new SystemException("Unable to register mbean " + name, e);
                }
            } else {
                throw new SystemException("Unable to register mbean for url " + url
                        + ": too many equal tasks already registered");
            }
        }
    }

    private static MBeanServer getMBeanServer() {
        return mbeanServer == null ? ManagementFactory.getPlatformMBeanServer() : mbeanServer;
    }

    /**
     * Cancels all in-progress ETL tasks.
     *
     * @return number of cancelled ETL tasks.
     */
    public static int cancelAll() {
        Set<ObjectName> names = findEtlMBeans();
        MBeanServer srv = getMBeanServer();
        int cancelled = 0;
        for (ObjectName objectName : names) {
            try {
                srv.invoke(objectName, "cancel", null, null);
                cancelled++;
            } catch (Exception e) {
                LOG.error("Cannot cancel ETL, MBean " + objectName, e);
            }
        }
        return cancelled;
    }

    /**
     * Find ETL mbeans.
     *
     * @return set of object names.
     */
    public static Set<ObjectName> findEtlMBeans() {
        try {
            return getMBeanServer().queryNames(new ObjectName(MBEAN_NAME_PREFIX + ",*"), null);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    static ObjectName toObjectName(String url, int n) {
        try {
            return new ObjectName(MBEAN_NAME_PREFIX + ",url=" + ObjectName.quote(url) + (n > 0 ? ",n=" + n : ""));
        } catch (MalformedObjectNameException e) { //Should not happen
            throw new IllegalStateException("Cannot set MBean name", e);
        }

    }

    /**
     * Unregisters this manager from the JMX server.
     * <p>This method does not throws any exceptions.
     */
    public synchronized void unregister() {
        if (name != null && server != null) {
            try {
                server.unregisterMBean(name);
            } catch (Exception e) {
                LOG.error("Unable to unregister mbean " + name, e);
            }
            name = null;
        }
    }

    public synchronized void cancel() {
        if (etlThread != null && etlThread.isAlive() && !etlThread.isInterrupted()) {
            etlThread.interrupt();
            etlThread = null;
        }
    }

    /**
     * Returns name of the MBean assigned at registration time.
     *
     * @return name of the mbean.
     */
    public ObjectName getName() {
        return name;
    }
}

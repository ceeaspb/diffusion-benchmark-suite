/*
 * Copyright 2013 Push Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.pushtechnology.benchmarks.monitoring;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;

/**
 * Remote memory monitoring utility.
 * 
 * @author nitsanw
 * 
 */
public class RemoteMemoryMonitor implements MemoryMonitor {
    // CHECKSTYLE:OFF
    private static final int BYTES_IN_MB = 1000000;
    private final MBeanServerConnection connection;
    private final ObjectName osBeanName;
    private CompositeDataSupport currentMemoryUsage;

    /**
     * @param connection to remote process
     * @throws MalformedObjectNameException if bean is not found
     */
    public RemoteMemoryMonitor(MBeanServerConnection connection)
            throws MalformedObjectNameException {
        // CHECKSTYLE:ON
        this.connection = connection;
        this.osBeanName = new ObjectName("java.lang:type=Memory");
    }

    @Override
    public final int heapUsed() {
        if (getMemoryUsageData() == null) {
            return -1;
        }
        return (int) ((Long) getMemoryUsageData().get("used") / BYTES_IN_MB);
    }

    @Override
    public final int heapMax() {
        if (getMemoryUsageData() == null) {
            return -1;
        }
        return (int) ((Long) getMemoryUsageData().get("max") / BYTES_IN_MB);
    }

    /**
     * @return current heap memory use
     */
    private CompositeDataSupport getMemoryUsageData() {
        try {
            return currentMemoryUsage;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public final void sample() {
        try {
            currentMemoryUsage =
                    (CompositeDataSupport) connection.getAttribute(osBeanName,
                            "HeapMemoryUsage");
        } catch (Exception e) {
            currentMemoryUsage = null;
        }
    }
}
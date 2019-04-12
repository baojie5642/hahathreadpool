/*
 *  Copyright 2011 sunli [sunli1223@gmail.com][weibo.com@sunli1223]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.baojie.zk.example.clean;

import com.baojie.zk.example.util.JavaVersion;
import com.baojie.zk.example.util.LocalSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class LocalCleaner {

    private static final Logger log = LoggerFactory.getLogger(LocalCleaner.class);

    private static final boolean atLeastJvm9 = LocalSystem.isJavaVersionAtLeast(JavaVersion.JAVA_9);

    private static final String CLEAN_KEY = "cleaner";

    private LocalCleaner() {
        throw new IllegalArgumentException();
    }

    public static final void clean(final Object buffer) {
        if (null != buffer) {
            doClean(buffer);
        }
    }

    private static final void doClean(final Object buffer) {
        if (atLeastJvm9) {

        } else {
            AccessController.doPrivileged(new LocalAction(buffer));
        }
    }

    private static final Method cleanMethod(final Object buffer) {
        try {
            return buffer.getClass().getMethod(CLEAN_KEY, new Class[0]);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return null;
    }

    private static final void doInvoke(final Method clean, final Object buffer) {
        Cleaner cleaner = null;
        try {
            cleaner = (Cleaner) clean.invoke(buffer, new Object[0]);
            cleaner.clean();
        } catch (IllegalAccessException e) {
            log.error(e.toString(), e);
            unmap(buffer);
        } catch (InvocationTargetException e) {
            log.error(e.toString(), e);
            unmap(buffer);
        }
    }

    public static final void unmap(final Object buffer) {
        if (null != buffer) {
            if (buffer instanceof MappedByteBuffer) {
                Cleaner cl = ((DirectBuffer) buffer).cleaner();
                if (cl != null) {
                    cl.clean();
                }
            }
        }
    }

    private static final class LocalAction implements PrivilegedAction<Void> {

        private final Object buffer;

        public LocalAction(final Object buffer) {
            this.buffer = buffer;
        }

        @Override
        public Void run() {
            try {
                Method clean = cleanMethod(buffer);
                if (null == clean) {
                    unmap(buffer);
                } else {
                    clean.setAccessible(true);
                    doInvoke(clean, buffer);
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
                unmap(buffer);
            }
            return null;
        }

    }

}

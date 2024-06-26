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
package scriptella.driver.velocity;

import scriptella.driver.text.AbstractTextConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.log.JdkLogChute;

/**
 * Represents a session to velocity engine.
 */
public class VelocityConnection extends AbstractTextConnection {
    private final VelocityEngine engine;
    private final VelocityContextAdapter adapter;
    private Writer writer;//lazy initialized

    /**
     * Instantiates a velocity connection.
     *
     * @param parameters connection parameters.
     */
    public VelocityConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, parameters);
        engine = new VelocityEngine();
        engine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new JdkLogChute());
        engine.setProperty("velocimacro.library", "");//unnecessary file in our case
        try {
            engine.init();
        } catch (Exception e) {
            throw new VelocityProviderException("Unable to initialize engine", e);
        }
        adapter = new VelocityContextAdapter();
    }

    /**
     * Executes a script specified by its content.
     * <p>scriptContent may be used as a key for caching purposes, i.e.
     * provider may precompile scripts and use compiled versions for subsequent executions.
     * <p>This method is synchronized to to prevent multiple threads from working with the same writer.
     * Additionally single velocityEngine and context adapter instances are used.
     *
     * @param scriptContent      script content.
     * @param parametersCallback callback to get parameter values.
     */
    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        //todo Current solution is slow, use per scriptContent caching by providing a custom Velocity ResourceLoader
        //todo also make Resource identifiable, i.e. replace url.getFile with resource name/location
        adapter.setCallback(parametersCallback);//we may use single context+engine because method is synchronized
        Reader reader = null;
        try {
            reader = scriptContent.open();
            Writer w = getWriter();
            final URL url = getConnectionParameters().getUrl();
            engine.evaluate(adapter, w, url == null ? "System.out" : url.getFile(), reader);
            if (getConnectionParameters().isFlush()) {
                w.flush();
            }
        } catch (Exception e) {
            throw new VelocityProviderException("Unable to execute script", e);
        } finally {
            adapter.setCallback(null);//cleaning up to avoid mem leaks
            IOUtils.closeSilently(reader);
        }
    }

    /**
     * Executes a query specified by its content.
     * <p/>
     *
     * @param queryContent       query content.
     * @param parametersCallback callback to get parameter values.
     * @param queryCallback      callback to call for each result set element produced by this query.
     * @see #executeScript(scriptella.spi.Resource,scriptella.spi.ParametersCallback)
     */
    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {
        throw new UnsupportedOperationException("Query execution is not supported yet");
    }

    private Writer getWriter() {
        if (writer == null) {
            try {
                writer = IOUtils.asBuffered(newOutputWriter());
            } catch (IOException e) {
                throw new VelocityProviderException("Unable to open URL " + getConnectionParameters().getUrl() + " for output", e);
            }
        }
        return writer;
    }

    /**
     * Closes the connection and releases all related resources.
     */
    public synchronized void close() throws ProviderException {
        if (writer != null) {
            IOUtils.closeSilently(writer);
            writer = null;
        }
    }

    /**
     * Velocity Context adapter class for {@link ParametersCallback}.
     */
    private static class VelocityContextAdapter implements Context {
        private static final Object[] EMPTY_ARRAY = new Object[0];
        private ParametersCallback callback;
        private Map<Object, Object> localParameters;


        public void setCallback(ParametersCallback callback) {
            this.callback = callback;
        }

        public Object put(String key, Object value) {
            if (localParameters == null) {
                localParameters = new HashMap<>();
            }

            return localParameters.put(key, value);
        }

        public Object get(String key) {
            if (containsKey(key)) {
                return localParameters.get(key);
            }
            return callback.getParameter(key);
        }

        public boolean containsKey(Object key) {
            return localParameters != null && localParameters.containsKey(key);
        }

        public Object[] getKeys() {
            return localParameters == null ? EMPTY_ARRAY : localParameters.keySet().toArray();
        }

        public Object remove(Object key) {
            return localParameters == null ? null : localParameters.remove(key);
        }
    }

}

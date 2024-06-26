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
package scriptella;

import scriptella.configuration.ConfigurationFactory;
import scriptella.core.ConnectionManager;
import scriptella.core.SqlTestHelper;
import scriptella.execution.EtlContext;
import scriptella.execution.EtlExecutor;
import scriptella.execution.EtlExecutorException;
import scriptella.interactive.ProgressIndicator;
import scriptella.spi.ConnectionParameters;

import java.util.HashMap;
import java.util.Map;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class PropertiesTest extends AbstractTestCase {
    private EtlContext ctx; //execution context
    private ConnectionParameters params;

    public void test() throws EtlExecutorException {
        EtlExecutor se = prepareExecutor(null);
        se.execute();

        assertEquals("jdbc:hsqldb:mem:propertiestest", params.getUrl());
        assertEquals("sa", params.getUser());
        assertEquals("", params.getPassword());

        //check substituted properties in a context
        assertEquals("1", ctx.getParameter("a"));
        assertEquals("bar", ctx.getParameter("foo"));
        assertEquals("1", ctx.getParameter("var"));
        assertEquals("1|1|1|1|1|1", ctx.getParameter("b"));
        assertEquals("jdbc:hsqldb:mem", ctx.getParameter("url.prefix"));
        assertEquals("propertiestest", ctx.getParameter("dbname"));
        assertEquals("org.hsqldb.jdbcDriver", ctx.getParameter("driver"));
        assertEquals("jdbc:hsqldb:mem:propertiestest", ctx.getParameter("url"));
        assertEquals("sa", ctx.getParameter("user"));
        assertEquals("", ctx.getParameter("password"));
        Map<String,String> extra = new HashMap<>();
        extra.put("var", "2");
        se = prepareExecutor(extra);
        se.execute();
        assertEquals("2", ctx.getParameter("var"));
        assertEquals("2|2|2|2|2|2", ctx.getParameter("b"));
    }

    private EtlExecutor prepareExecutor(Map<String,String> props) {
        ConfigurationFactory cf = new ConfigurationFactory();
        cf.setResourceURL(getClass().getResource(getClass().getSimpleName()+".xml"));
        cf.setExternalParameters(props);
        return new EtlExecutor(cf.createConfiguration()) {
            //overrides prepare method to get ctx and params for connection
            @Override
            protected EtlContext prepare(final ProgressIndicator indicator) {
                ctx = super.prepare(indicator);
                Map<String, ConnectionManager> connections = SqlTestHelper.getConnections(ctx.getSession());
                ConnectionManager con = connections.entrySet().iterator().next().getValue();
                params = SqlTestHelper.getConnectionParameters(con);
                return ctx;
            }
        };

    }

}

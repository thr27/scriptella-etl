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
package scriptella.driver.xpath;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Represents a connection to an XML file.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class XPathConnection extends AbstractConnection {

    /**
     * Name of the <code>cache_queries</code> connection property.
     * Specifies flag to use queries cache, default is true.
	 * If set to "false" XML document under URL will be parsed for each
	 * <query> or <script> tag in etl script. This allows to update XML content dynamically.
     */
    public static final String CACHE_QUERIES = "cache_queries";

    /**
     * Name of the <code>return_arrays</code> connection property.
     * Value of <code>true</code> specifies that variables should return a string array, otherwise a single string is returned.
     */
    public static final String RETURN_ARRAYS = "return_arrays";

    static final DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();

    private Map<Resource, XPathQueryExecutor> queriesCache = new IdentityHashMap<>();
    private XPathExpressionCompiler compiler = new XPathExpressionCompiler();
    private Document document;
    private ThreadLocal<Node> queryContext= new ThreadLocal<>();
    private URL url;
    private final boolean returnArrays;
    protected final boolean cache_queries;
    /**
     * For testing purposes only.
     */
    protected XPathConnection() {
        cache_queries = true;
        returnArrays = false;
    }

    public XPathConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, parameters);
        url = parameters.getResolvedUrl();
        cache_queries = parameters.getBooleanProperty(CACHE_QUERIES, true);
        //TODO implement trim option

        returnArrays = parameters.getBooleanProperty(RETURN_ARRAYS, false);
    }

    public void executeScript(final Resource scriptContent, final ParametersCallback parametersCallback) throws ProviderException {
        throw new XPathProviderException("Script execution is not supported yet");
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {
        XPathQueryExecutor exec = queriesCache.get(queryContent);
        if (exec == null) {
            exec = new XPathQueryExecutor(queryContext, getDocument(), queryContent, compiler, counter, returnArrays);
            if (cache_queries) {
                queriesCache.put(queryContent, exec);
            }
        }
        exec.execute(queryCallback, parametersCallback);
    }

    private Document getDocument() {
        if (document == null || (!cache_queries)) {
            try {
                document = DBF.newDocumentBuilder().parse(new InputSource(url.toString()));
            } catch (Exception e) {
                throw new XPathProviderException("Unable to parse document " + url, e);
            }
        }
        return document;
    }

    public void close() throws ProviderException {
        queriesCache = null;
        document = null;
        queryContext.remove();
        queryContext = null;
    }
}

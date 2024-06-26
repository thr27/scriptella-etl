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
package scriptella.driver.csv;

import scriptella.AbstractTestCase;
import scriptella.configuration.MockConnectionEl;
import scriptella.configuration.StringResource;
import scriptella.driver.text.AbstractTextConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockConnectionParameters;
import scriptella.spi.MockDriverContext;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link CsvConnection}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class CsvConnectionTest extends AbstractTestCase {
    private int rows;

    private ByteArrayOutputStream out;
    private String testCsvInput;

    protected void setUp() throws Exception {
        super.setUp();
        testCsvInput = "c1;c2;c3\nc4;'c''5';c6\u0394";
        testURLHandler = new TestURLHandler() {
            public InputStream getInputStream(final URL u) {
                try {
                    return new ByteArrayInputStream(testCsvInput.getBytes("UTF8"));
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }

            public OutputStream getOutputStream(final URL u) {
                out = new ByteArrayOutputStream();
                return out;
            }

            public int getContentLength(final URL u) {
                return -1;
            }
        };

    }

    public void testQuery() {
        //Create a configuration with non default values
        Map<String, String> props = new HashMap<>();
        props.put(CsvConnection.ENCODING, "UTF8");
        props.put(CsvConnection.EOL, "\r\n");
        props.put(CsvConnection.HEADERS, "false");
        props.put(CsvConnection.QUOTE, "'");
        props.put(CsvConnection.ESCAPE, "\\");
        props.put(CsvConnection.SEPARATOR, ";");
        ConnectionParameters cp = new MockConnectionParameters(props, "tst://file");

        CsvConnection con = new CsvConnection(cp);
        //register handler for tst url
        rows = 0;
        con.executeQuery(new StringResource(""), MockParametersCallbacks.UNSUPPORTED, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                rows++;
                switch (rows) {
                    case 1:
                        assertEquals("c1", parameters.getParameter("1"));
                        assertEquals("c2", parameters.getParameter("2"));
                        assertEquals("c3", parameters.getParameter("3"));
                        break;
                    case 2:
                        assertEquals("c4", parameters.getParameter("1"));
                        assertEquals("c'5", parameters.getParameter("2"));
                        assertEquals("c6\u0394", parameters.getParameter("3"));
                        break;

                    default:
                        fail("unexpected row " + rows);
                }

            }
        });
        assertEquals(2, rows);
        assertNull("No output should be produced by a query", out);


    }

    public void testScript() throws UnsupportedEncodingException {
        //Create a configuration with non default values
        Map<String, String> props = new HashMap<>();
        props.put(CsvConnection.ENCODING, "UTF8");
        props.put(CsvConnection.EOL, "\r\n");
        props.put(CsvConnection.QUOTE, "");
        props.put(CsvConnection.SEPARATOR, ";");
        ConnectionParameters cp = new MockConnectionParameters(props, "tst://file");

        CsvConnection con = new CsvConnection(cp);
        //register handler for tst url
        rows = 0;
        con.executeScript(new StringResource("  $row1,\"value\",val${'ue'}\n$row2,,value\u0394"),
                MockParametersCallbacks.SIMPLE);
        con.close();
        String expected = "*row1*;value;value\r\n*row2*;;value\u0394\r\n";
        String actual = out.toString("UTF8");
        assertEquals(expected, actual);
    }

    public void testEscape() throws UnsupportedEncodingException {
        Map<String, String> props = new HashMap<>();
        props.put(CsvConnection.ESCAPE, "'");
        ConnectionParameters cp = new MockConnectionParameters(props, "tst://file");

        CsvConnection con = new CsvConnection(cp);
        //register handler for tst url
        rows = 0;
        con.executeScript(new StringResource("  $row1,\"value\",val${'ue'}\n$row2,'value',va'l'ue\n"),
                MockParametersCallbacks.SIMPLE);
        con.close();
        String expected = "\"*row1*\",\"value\",\"value\"\n\"*row2*\",\"''value''\",\"va''l''ue\"\n";
        String actual = out.toString("UTF8");
        assertEquals(expected, actual);
    }

    public void testQuoteall() throws UnsupportedEncodingException {
        Map<String, String> props = new HashMap<>();
        props.put(CsvConnection.QUOTEALL, "false");
        ConnectionParameters cp = new MockConnectionParameters(props, "tst://file");

        CsvConnection con = new CsvConnection(cp);
        //register handler for tst url
        rows = 0;
        con.executeScript(new StringResource("  $row1,va\"l\"ue,val${'ue'}\n$row2,'value',va\"l\"ue\n"),
                MockParametersCallbacks.SIMPLE);
        con.close();
        String expected = "*row1*,\"va\"\"l\"\"ue\",value\n*row2*,'value',\"va\"\"l\"\"ue\"\n";
        String actual = out.toString("UTF8");
        assertEquals(expected, actual);
    }

    /**
     * Tests CSV processing with trim mode switched off.
     */
    public void testNoTrim() {
        //Create a configuration with non default values
        Map<String, String> props = new HashMap<>();
        props.put(CsvConnection.TRIM, "no");
        props.put(CsvConnection.QUOTE, "");
        props.put(CsvConnection.EOL, "\r\n");
        ConnectionParameters cp = new MockConnectionParameters(props, "tst://file");

        rows = 0;
        CsvConnection con = new CsvConnection(cp);
        con.executeQuery(new StringResource(" c4.*"), //extra leading whitespace
                MockParametersCallbacks.SIMPLE, new QueryCallback() {
                    public void processRow(final ParametersCallback parameters) {
                        rows++;
                    }
                });
        con.executeScript(new StringResource(" $a,$b , $c "),
                MockParametersCallbacks.SIMPLE);

        con.close();
        String expected = " *a*,*b* , *c* \r\n";
        String actual = out.toString();
        assertEquals(expected, actual);
        assertEquals(1, rows);

    }

    public void testAutoFlush() {
        //Create a configuration with non default values
        Map<String, String> props = new HashMap<>();
        props.put(CsvConnection.FLUSH, "true");
        props.put(CsvConnection.QUOTE, "");
        ConnectionParameters cp = new MockConnectionParameters(props, "tst://file");

        CsvConnection con = new CsvConnection(cp);
        String str = "-test-";
        con.executeScript(new StringResource(str), MockParametersCallbacks.NULL);
        assertNotNull(out);
        assertEquals(str + "\n", new String(out.toByteArray()));
    }

    public void testNullStringScript() {
        //Create a configuration with non default values
        Map<String, String> props = new HashMap<>();
        props.put(AbstractTextConnection.NULL_STRING, "");
        props.put(CsvConnection.QUOTE, "");
        ConnectionParameters cp = new MockConnectionParameters(props, "tst://file");

        CsvConnection con = new CsvConnection(cp);
        String str = "$a,$b,$c";
        props = new HashMap<>();
        props.put("a", "1");
        props.put("c", "2");
        con.executeScript(new StringResource(str), MockParametersCallbacks.fromMap(props));
        con.close();
        assertEquals("1,,2\n", new String(out.toByteArray()));
    }

    public void testNullStringQuery() {
        //Create a configuration with non default values
        Map<String, String> props = new HashMap<>();
        props.put(AbstractTextConnection.NULL_STRING, "");
        props.put(CsvConnection.HEADERS, "false");
        ConnectionParameters cp = new MockConnectionParameters(props, "tst://file");

        CsvConnection con = new CsvConnection(cp);
        testCsvInput= "a,,c";
        final Map<String, Object> map = new HashMap<>();
        con.executeQuery(new StringResource(""), MockParametersCallbacks.SIMPLE, new QueryCallback() {
            @Override
            public void processRow(ParametersCallback parameters) {
                map.put("1", parameters.getParameter("1"));
                map.put("2", parameters.getParameter("2"));
                map.put("3", parameters.getParameter("3"));
                map.put("4", parameters.getParameter("4"));
            }
        });
        con.close();
        assertEquals("a", map.get("1"));
        assertNull("Null must be returned for a value=null_string", map.get("2"));
        assertEquals("c", map.get("3"));
        assertEquals("getParameter must delegate to the parent callback if parameter was not found", "*4*", map.get("4"));
    }


    /**
     * Tests if skip_lines is working.
     */
    public void testSkipLines() {
        //Create a configuration with non default values
        Map<String, String> props = new HashMap<>();
        props.put(CsvConnection.SKIP_LINES, "2");
        ConnectionParameters cp = new ConnectionParameters(new MockConnectionEl(props, "tst://file"), MockDriverContext.INSTANCE);

        CsvConnection con = new CsvConnection(cp);
        testCsvInput = "-skipped---,--\n-skipped---,--\nc1,c2\n11,12";
        rows = 0;
        con.executeQuery(new StringResource(""), MockParametersCallbacks.NULL, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                rows++;
                assertEquals("11", parameters.getParameter("1"));
                assertEquals("11", parameters.getParameter("c1"));
                assertEquals("12", parameters.getParameter("2"));
                assertEquals("12", parameters.getParameter("c2"));
            }
        });
        assertEquals(1, rows);
        //Now test if the number of lines to skip exceeds the file size
        testCsvInput = "v1,v2\nv3,v4";
        rows = 0;
        con.executeQuery(new StringResource(""), MockParametersCallbacks.NULL, new QueryCallback() {
            public void processRow(final ParametersCallback parameters) {
                rows++;
            }
        });
        assertEquals(0, rows);
    }


}

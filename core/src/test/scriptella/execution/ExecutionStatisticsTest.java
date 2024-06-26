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

import scriptella.DBTestCase;
import scriptella.configuration.QueryEl;
import scriptella.configuration.ScriptEl;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;


/**
 * Tests for {@link ExecutionStatistics} and {@link ExecutionStatisticsBuilder}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ExecutionStatisticsTest extends DBTestCase {
    public void test() throws EtlExecutorException {
        final EtlExecutor se = newEtlExecutor();
        final ExecutionStatistics s = se.execute();
        Map<String, Integer> cats = s.getCategoriesStatistics();
        assertEquals(2, cats.size());
        assertEquals(3, cats.get(ScriptEl.TAG_NAME).intValue());
        assertEquals(2, cats.get(QueryEl.TAG_NAME).intValue());
        assertEquals(12, s.getExecutedStatementsCount()); //4+2+2+1+3

        final Collection<ExecutionStatistics.ElementInfo> elements = s.getElements();

        for (ExecutionStatistics.ElementInfo info : elements) {
            assertTrue("Negative working time: " + info.getWorkingTime(), info.getWorkingTime() >= 0);
            if ("/etl/script[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(4, info.getStatementsCount());
            } else if ("/etl/query[1]/query[1]/script[1]".equals(
                    info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(2, info.getStatementsCount()); //1 statement executed 2 times
            } else if ("/etl/query[1]/query[1]".equals(info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(2, info.getStatementsCount());
            } else if ("/etl/query[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(1, info.getStatementsCount());
            } else if ("/etl/script[2]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(3, info.getStatementsCount());
            } else {
                fail("Unrecognized statistic element " + info.getId());
            }
        }
    }

    public void test2() throws EtlExecutorException {
        final EtlExecutor se = newEtlExecutor(
                "ExecutionStatisticsTest2.xml");
        final ExecutionStatistics s = se.execute();
        Map<String, Integer> cats = s.getCategoriesStatistics();
        assertEquals(2, cats.size());
        assertEquals(4, cats.get("script").intValue());
        assertEquals(1, cats.get("query").intValue());

        final Collection<ExecutionStatistics.ElementInfo> elements = s.getElements();

        for (ExecutionStatistics.ElementInfo info : elements) {
            if ("/etl/script[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/etl/query[1]/script[1]".equals(info.getId())) {
                assertEquals(0, info.getSuccessfulExecutionCount());
                assertEquals(2, info.getFailedExecutionCount());
            } else if ("/etl/query[1]/script[2]".equals(info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/etl/query[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else if ("/etl/script[2]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
            } else {
                fail("Unrecognized statistic element " + info.getId());
            }
        }
    }

    public void test3() throws EtlExecutorException {
        final EtlExecutor se = newEtlExecutor(
                "ExecutionStatisticsTest3.xml");
        final ExecutionStatistics s = se.execute();
        Map<String, Integer> cats = s.getCategoriesStatistics();
        assertEquals(2, cats.size());
        assertEquals(2, cats.get("script").intValue());
        assertEquals(2, cats.get("query").intValue());
        assertEquals(9, s.getExecutedStatementsCount()); //4+2+2+1

        final Collection<ExecutionStatistics.ElementInfo> elements = s.getElements();

        for (ExecutionStatistics.ElementInfo info : elements) {
            if ("/etl/script[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(4, info.getStatementsCount());
            } else if ("/etl/query[1]/query[1]/script[1]".equals(
                    info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(2, info.getStatementsCount()); //1 statement executed 2 times
            } else if ("/etl/query[1]/query[1]".equals(info.getId())) {
                assertEquals(2, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(2, info.getStatementsCount());
            } else if ("/etl/query[1]".equals(info.getId())) {
                assertEquals(1, info.getSuccessfulExecutionCount());
                assertEquals(0, info.getFailedExecutionCount());
                assertEquals(1, info.getStatementsCount());
            } else {
                fail("Unrecognized statistic element " + info.getId());
            }
        }
    }

    /**
     * Tests if total time is correctly printed.
     */
    public void testTotalTime() {
        DecimalFormat f = new DecimalFormat(ExecutionStatistics.DOUBLE_FORMAT_PTR);
        char sep = f.getDecimalFormatSymbols().getDecimalSeparator();

        long time = 288727350; //3d 8h 12m 7s 350ms
        StringBuilder sb = new StringBuilder();
        ExecutionStatistics.appendTotalTimeDuration(time, sb, f);
        assertEquals(" 3 days 8 hours 12 minutes 7" + sep + "35 seconds", sb.toString());
        time = 3605001; //1h 5s 1ms
        sb = new StringBuilder();
        ExecutionStatistics.appendTotalTimeDuration(time, sb, f);
        assertEquals(" 1 hour 5 seconds", sb.toString());
        time = 800; //800ms
        sb = new StringBuilder();
        ExecutionStatistics.appendTotalTimeDuration(time, sb, f);
        assertEquals(" 0" + sep + "8 second", sb.toString());
        time = 1000; //1000ms
        sb = new StringBuilder();
        ExecutionStatistics.appendTotalTimeDuration(time, sb, f);
        assertEquals(" 1 second", sb.toString());
    }

}

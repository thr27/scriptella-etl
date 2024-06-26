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
package scriptella.jdbc;

import scriptella.DBTestCase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Performance tests for {@link scriptella.jdbc.StatementCache}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class StatementCachePerfTest extends DBTestCase {
    private static final int LOOP_COUNT = 2000;

    private static class TestableStatementCache extends StatementCache {

        public TestableStatementCache(Connection connection, final int size) {
            super(connection, size, 0, 0);
        }

        @Override
        protected StatementWrapper.Simple create(final String sql) {
            return new StatementWrapper.Simple(sql) {
                public void close() {
                }
            };
        }

        @Override
        protected StatementWrapper.Prepared prepare(final String sql) {
            return new StatementWrapper.Prepared() {
                @Override
                public void setParameters(List<Object> params) {
                }

                @Override
                public void clear() {
                }

            };
        }

    }

    protected void setUp() {
        sc = new TestableStatementCache(null, 100);
    }

    StatementCache sc;

    /**
     * History:
     * 01.10.2006 - Duron 1.7Mhz - 1093 ms
     */
    public void testCacheMiss() throws SQLException {
        //Testing cache miss
        List<Object> params = new ArrayList<>();
        params.add(1);

        for (int i = 0; i < LOOP_COUNT; i++) {
            setUp();
            runStatements(sc, params);
        }

    }

    private void runStatements(StatementCache cache, List<Object> params) throws SQLException {
        StringBuilder sb = new StringBuilder(150);
        for (int j = 0; j < 150; j++) {
            sb.append('.');
            StatementWrapper s = cache.prepare(sb.toString(), Collections.emptyList());
            cache.releaseStatement(s);
            StatementWrapper s2 = cache.prepare(sb.toString(), params);
            cache.releaseStatement(s2);
        }

    }

    /**
     * History:
     * 01.10.2006 - Duron 1.7Mhz - 1032 ms
     */
    public void testCacheHit() throws SQLException {
        //Testing cache miss
        List<Object> params = new ArrayList<>();
        params.add(1);

        for (int i = 0; i < LOOP_COUNT; i++) {
            runStatements(sc, params);
        }

    }

    /**
     * History:
     * 01.10.2006 - Duron 1.7Mhz - 1032 ms
     */
    public void testCacheDisable() throws SQLException {
        //Testing disabled cache
        List<Object> params = new ArrayList<>();
        params.add(1);
        StatementCache disabled = new TestableStatementCache(null, -1);

        for (int i = 0; i < LOOP_COUNT; i++) {
            runStatements(disabled, params);
        }

    }


}

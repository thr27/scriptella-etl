/*
 * Copyright 2006-2007 The Scriptella Project Team.
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
package scriptella.expression;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents a global <code>etl</code> variable available for all ETL file elements.
 * <p>Currently it is available only in JEXL expressions, e.g. ${etl.date.now('MM-DD-YYYY')}
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class EtlVariable {
    //For now singleton, may be replaced with threadlocal in future
    private static final EtlVariable INSTANCE = new EtlVariable();
    public static final String NAME = "etl";

    private final DateUtils date = new DateUtils();

    /**
     * Returns the global <code>etl</code> variable.
     *
     * @return global <code>etl</code> variable.
     */
    public static EtlVariable get() {
        return INSTANCE;
    }

    public DateUtils getDate() {
        return date;
    }

    /**
     * Utility class for ETL file expressions
     */
    public static class DateUtils {
        /**
         * Returns a current date/time formatted according to the specified format.
         *
         * @param formatPattern format pattern as specified by {@link java.text.SimpleDateFormat}.
         * @return formatted string representation of current date/time.
         * @see java.text.SimpleDateFormat
         * @see #format(java.util.Date,String)
         */
        public String now(String formatPattern) {
            return format(now(), formatPattern);
        }

        /**
         * Formats a specified date/time according to the specified pattern.
         *
         * @param date          date to format.
         * @param formatPattern format pattern as specified by {@link java.text.SimpleDateFormat}.
         * @return formatted string representation of the specified date/time.
         */
        public String format(Date date, String formatPattern) {
            return new SimpleDateFormat(formatPattern).format(date);
        }

        public Date parse(String dateStr, String formatPattern) throws ParseException {
            return new SimpleDateFormat(formatPattern).parse(dateStr);
        }

        /**
         * Returns the current date.
         *
         * @return current date/time.
         */
        public Date now() {
            return new Date();
        }

        /**
         * Returns the formatted representation of current date.
         * <p>Time part is skipped.
         *
         * @return formatted representation of current date.
         */
        public String today() {
            return DateFormat.getDateInstance().format(now());
        }

        /**
         * A synonym for {@link #now(String)}.
         * @param formatPattern format pattern.
         * @return formatted date.
         */
        public String today(String formatPattern) {
            return now(formatPattern);
        }
    }

}

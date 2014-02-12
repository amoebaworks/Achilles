/*
 * Copyright (C) 2012-2014 DuyHai DOAN
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package info.archinnov.achilles.internal.statement.wrapper;

import info.archinnov.achilles.type.ConsistencyLevel;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;

public abstract class AbstractStatementWrapper {
	public static final String ACHILLES_DML_STATEMENT = "ACHILLES_DML_STATEMENT";
	protected static final Logger dmlLogger = LoggerFactory.getLogger(ACHILLES_DML_STATEMENT);

	protected Object[] values = new Object[] {};

	protected AbstractStatementWrapper(Object[] values) {
		if (ArrayUtils.isNotEmpty(values))
			this.values = values;
	}

	public Object[] getValues() {
		return values;
	}

	public abstract ResultSet execute(Session session);

	public abstract Statement getStatement();

	public abstract void logDMLStatement(String indentation);

	public static void writeDMLStartBatch() {
		if (dmlLogger.isDebugEnabled()) {
			dmlLogger.debug("******BATCH START******");
		}
	}

	public static void writeDMLEndBatch(ConsistencyLevel consistencyLevel) {
		if (dmlLogger.isDebugEnabled()) {
			dmlLogger.debug("******BATCH END with CONSISTENCY LEVEL [{}] ******", consistencyLevel);
		}
	}

	protected void writeDMLStatementLog(String queryType, String queryString, String consistencyLevel,
			Object... values) {

        dmlLogger.debug("{} : [{}] with CONSISTENCY LEVEL [{}]", queryType, queryString, consistencyLevel);

		if (ArrayUtils.isNotEmpty(values)) {
			dmlLogger.debug("\t bound values : {}", Arrays.asList(values));
		}
	}
}

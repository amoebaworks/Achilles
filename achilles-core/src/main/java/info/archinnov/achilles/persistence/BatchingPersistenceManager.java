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
package info.archinnov.achilles.persistence;

import info.archinnov.achilles.type.ConsistencyLevel;

public interface BatchingPersistenceManager extends PersistenceManager {

   /**
    * Start a batch session.
    */
   public void startBatch();

   /**
    * Start a batch session with read/write consistency levels
    */
   public void startBatch(ConsistencyLevel consistencyLevel);

   /**
    * End an existing batch and flush all the pending statements.
    * 
    * Do nothing if there is no pending statement
    * 
    */
   public void endBatch();

   /**
    * Cleaning all pending statements for the current batch session.
    */
   public void cleanBatch();

}

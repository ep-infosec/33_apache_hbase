/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.regionserver;

import java.io.IOException;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Implementors of this interface are the ones who needs to do some action when the
 * {@link Shipper#shipped()} is called
 */
@InterfaceAudience.Private
public interface ShipperListener {

  /**
   * The action that needs to be performed before {@link Shipper#shipped()} is performed
   */
  void beforeShipped() throws IOException;
}

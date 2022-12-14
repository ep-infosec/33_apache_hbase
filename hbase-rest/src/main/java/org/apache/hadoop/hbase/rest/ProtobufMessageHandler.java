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
package org.apache.hadoop.hbase.rest;

import java.io.IOException;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Common interface for models capable of supporting protobuf marshalling and unmarshalling. Hooks
 * up to the ProtobufMessageBodyConsumer and ProtobufMessageBodyProducer adapters.
 */
@InterfaceAudience.Private
public interface ProtobufMessageHandler {
  /** Returns the protobuf represention of the model */
  byte[] createProtobufOutput();

  /**
   * Initialize the model from a protobuf representation.
   * @param message the raw bytes of the protobuf message
   * @return reference to self for convenience
   */
  ProtobufMessageHandler getObjectFromMessage(byte[] message) throws IOException;
}

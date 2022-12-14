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
package org.apache.hadoop.hbase.util;

import org.apache.yetus.audience.InterfaceAudience;

/**
 * SMA measure the overall average execution time of a specific method.
 */
@InterfaceAudience.Private
public class SimpleMovingAverage<T> extends MovingAverage<T> {
  private double averageTime = 0.0;
  protected long count = 0;

  public SimpleMovingAverage(String label) {
    super(label);
    this.averageTime = 0.0;
    this.count = 0;
  }

  @Override
  public void updateMostRecentTime(long elapsed) {
    averageTime += (elapsed - averageTime) / ++count;
  }

  @Override
  public double getAverageTime() {
    return averageTime;
  }
}

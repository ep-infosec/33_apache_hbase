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
package org.apache.hadoop.hbase.quotas.policies;

import java.io.IOException;
import java.util.List;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.quotas.SpaceLimitingException;
import org.apache.hadoop.hbase.quotas.SpaceViolationPolicyEnforcement;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * A {@link SpaceViolationPolicyEnforcement} which can be treated as a singleton. When a quota is
 * not defined on a table or we lack quota information, we want to avoid creating a policy, keeping
 * this path fast.
 */
@InterfaceAudience.Private
public final class MissingSnapshotViolationPolicyEnforcement
  extends AbstractViolationPolicyEnforcement {
  private static final MissingSnapshotViolationPolicyEnforcement SINGLETON =
    new MissingSnapshotViolationPolicyEnforcement();

  private MissingSnapshotViolationPolicyEnforcement() {
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "MS_EXPOSE_REP",
      justification = "singleton pattern")
  public static SpaceViolationPolicyEnforcement getInstance() {
    return SINGLETON;
  }

  @Override
  public boolean shouldCheckBulkLoads() {
    return false;
  }

  @Override
  public long computeBulkLoadSize(FileSystem fs, List<String> paths) throws SpaceLimitingException {
    long size = 0;
    for (String path : paths) {
      size += getFileSize(fs, path);
    }
    return size;
  }

  @Override
  public void enable() throws IOException {
  }

  @Override
  public void disable() throws IOException {
  }

  @Override
  public void check(Mutation m) throws SpaceLimitingException {
  }

  @Override
  public String getPolicyName() {
    return "NoQuota";
  }
}

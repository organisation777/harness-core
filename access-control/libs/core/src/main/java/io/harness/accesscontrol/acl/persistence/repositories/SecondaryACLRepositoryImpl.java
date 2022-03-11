/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.acl.persistence.repositories;

import io.harness.accesscontrol.acl.persistence.ACL;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class SecondaryACLRepositoryImpl extends BaseACLRepositoryImpl implements ACLRepository {
  @Inject
  public SecondaryACLRepositoryImpl(MongoTemplate mongoTemplate) {
    super(mongoTemplate);
  }

  @Override
  protected String getCollectionName() {
    return ACL.SECONDARY_COLLECTION;
  }
}
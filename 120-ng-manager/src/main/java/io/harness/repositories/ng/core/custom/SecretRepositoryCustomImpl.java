/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.ng.core.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResourceScopeAndIdentifierDTO;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.models.Secret.SecretKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class SecretRepositoryCustomImpl implements SecretRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<Secret> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Secret> projects = mongoTemplate.find(query, Secret.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Secret.class));
  }

  @Override
  public long count(Criteria criteria) {
    return mongoTemplate.count(new Query(criteria), Secret.class);
  }

  @Override
  public List<ResourceScopeAndIdentifierDTO> findAllWithScopeAndIdentifierOnly(Criteria criteria) {
    Query query = new Query(criteria);
    query.fields()
        .include(SecretKeys.identifier)
        .include(SecretKeys.accountIdentifier)
        .include(SecretKeys.orgIdentifier)
        .include(SecretKeys.projectIdentifier);
    return mongoTemplate.find(query, Secret.class)
        .stream()
        .map(secret
            -> ResourceScopeAndIdentifierDTO.builder()
                   .accountIdentifier(secret.getAccountIdentifier())
                   .orgIdentifier(secret.getOrgIdentifier())
                   .projectIdentifier(secret.getProjectIdentifier())
                   .identifier(secret.getIdentifier())
                   .build())
        .collect(Collectors.toList());
  }
}

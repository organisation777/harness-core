package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxService;
import io.harness.template.beans.yaml.NGTemplateConfig;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.entity.TemplateEntity.TemplateEntityKeys;
import io.harness.template.events.TemplateCreateEvent;
import io.harness.template.events.TemplateDeleteEvent;
import io.harness.template.events.TemplateUpdateEvent;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(CDC)
public class NGTemplateRepositoryCustomImpl implements NGTemplateRepositoryCustom {
  private final GitAwarePersistence gitAwarePersistence;
  private final GitSyncSdkService gitSyncSdkService;
  OutboxService outboxService;

  @Override
  public TemplateEntity save(TemplateEntity templateToSave, NGTemplateConfig templateConfig) {
    Supplier<OutboxEvent> supplier = ()
        -> outboxService.save(new TemplateCreateEvent(templateToSave.getAccountIdentifier(),
            templateToSave.getOrgIdentifier(), templateToSave.getProjectIdentifier(), templateToSave));
    return gitAwarePersistence.save(
        templateToSave, templateToSave.getYaml(), ChangeType.ADD, TemplateEntity.class, supplier);
  }

  @Override
  public Optional<TemplateEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndVersionLabelAndDeletedNot(String accountId,
      String orgIdentifier, String projectIdentifier, String templateIdentifier, String versionLabel,
      boolean notDeleted) {
    return gitAwarePersistence.findOne(Criteria.where(TemplateEntityKeys.deleted)
                                           .is(!notDeleted)
                                           .and(TemplateEntityKeys.versionLabel)
                                           .is(versionLabel)
                                           .and(TemplateEntityKeys.identifier)
                                           .is(templateIdentifier)
                                           .and(TemplateEntityKeys.projectIdentifier)
                                           .is(projectIdentifier)
                                           .and(TemplateEntityKeys.orgIdentifier)
                                           .is(orgIdentifier)
                                           .and(TemplateEntityKeys.accountId)
                                           .is(accountId),
        projectIdentifier, orgIdentifier, accountId, TemplateEntity.class);
  }

  @Override
  public Optional<TemplateEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndIsStableAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String templateIdentifier, boolean notDeleted) {
    return gitAwarePersistence.findOne(Criteria.where(TemplateEntityKeys.deleted)
                                           .is(!notDeleted)
                                           .and(TemplateEntityKeys.isStableTemplate)
                                           .is(true)
                                           .and(TemplateEntityKeys.identifier)
                                           .is(templateIdentifier)
                                           .and(TemplateEntityKeys.projectIdentifier)
                                           .is(projectIdentifier)
                                           .and(TemplateEntityKeys.orgIdentifier)
                                           .is(orgIdentifier)
                                           .and(TemplateEntityKeys.accountId)
                                           .is(accountId),
        projectIdentifier, orgIdentifier, accountId, TemplateEntity.class);
  }

  @Override
  public TemplateEntity updateTemplateYaml(TemplateEntity templateToUpdate, TemplateEntity oldTemplateEntity,
      NGTemplateConfig templateConfig, ChangeType changeType) {
    Supplier<OutboxEvent> supplier = null;
    if (!gitSyncSdkService.isGitSyncEnabled(templateToUpdate.getAccountId(), templateToUpdate.getOrgIdentifier(),
            templateToUpdate.getProjectIdentifier())) {
      supplier = ()
          -> outboxService.save(
              new TemplateUpdateEvent(templateToUpdate.getAccountIdentifier(), templateToUpdate.getOrgIdentifier(),
                  templateToUpdate.getProjectIdentifier(), templateToUpdate, oldTemplateEntity));
    }
    return gitAwarePersistence.save(
        templateToUpdate, templateToUpdate.getYaml(), changeType, TemplateEntity.class, supplier);
  }

  @Override
  public TemplateEntity deleteTemplate(TemplateEntity templateToDelete, NGTemplateConfig templateConfig) {
    Optional<TemplateEntity> optionalTemplateEntity =
        findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndVersionLabelAndDeletedNot(
            templateToDelete.getAccountId(), templateToDelete.getOrgIdentifier(),
            templateToDelete.getProjectIdentifier(), templateToDelete.getIdentifier(),
            templateToDelete.getVersionLabel(), true);
    if (optionalTemplateEntity.isPresent()) {
      Supplier<OutboxEvent> supplier = ()
          -> outboxService.save(new TemplateDeleteEvent(templateToDelete.getAccountIdentifier(),
              templateToDelete.getOrgIdentifier(), templateToDelete.getProjectIdentifier(), templateToDelete));
      return gitAwarePersistence.save(
          templateToDelete, templateToDelete.getYaml(), ChangeType.DELETE, TemplateEntity.class, supplier);
    }
    throw new InvalidRequestException("No such template exists with identifier - " + templateToDelete.getIdentifier()
        + " and versionLabel - " + templateToDelete.getVersionLabel());
  }
}

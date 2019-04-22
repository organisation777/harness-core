package io.harness.functional.secrets;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.restutils.SecretsRestUtils;
import io.harness.restutils.VaultRestUtils;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.utils.SecretsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.util.List;

@Slf4j
public class SecretsFunctionalTest extends AbstractFunctionalTest {
  @Inject private SecretManagementDelegateService secretManagementDelegateService;

  VaultRestUtils vrUtils = new VaultRestUtils();
  String vaultId = null;
  VaultConfig vaultConfig;
  SecretsRestUtils srUtils = new SecretsRestUtils();
  final String VAULT_NAME = "Test Vault";
  final String QA_VAULT_URL = "https://vaultqa.harness.io";

  @Before
  public void vaultSetup() {
    vaultConfig = VaultConfig.builder()
                      .accountId(getAccount().getUuid())
                      .name(VAULT_NAME)
                      .vaultUrl(QA_VAULT_URL)
                      .authToken(new ScmSecret().decryptToString(new SecretName("qa_vault_root_token")))
                      .isDefault(true)
                      .basePath("/harness")
                      .build();

    List<VaultConfig> beforeVault = srUtils.getListConfigs(getAccount().getUuid(), bearerToken);
    if (SecretsUtils.isVaultAvailable(beforeVault, VAULT_NAME)) {
      logger.info("Vault already exists : " + VAULT_NAME);
      return;
    }

    vaultId = vrUtils.addVault(bearerToken, vaultConfig);
    assertTrue(StringUtils.isNotBlank(vaultId));
    logger.info("Vault created : " + vaultId);
    List<VaultConfig> afterVault = srUtils.getListConfigs(getAccount().getUuid(), bearerToken);
    if (SecretsUtils.isVaultAvailable(afterVault, VAULT_NAME)) {
      logger.info("Vault existence verified : " + VAULT_NAME);
    }
    logger.info("Done");
  }

  /*
   * This test is disabled as it uses delegate to setup vault from test, which is not working as of now.
   * Disbaling this temporarily until our team completes this from framework level.
   */
  @Test
  @Owner(emails = "swamy@harness.io", intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore
  public void secretsCRUDTests() {
    logger.info("Secrets test starts");
    String secretsName = "Secret-" + System.currentTimeMillis();
    String secretsNewName = "newName-" + System.currentTimeMillis();
    String secretValue = "value";

    SecretText secretText = SecretsUtils.createSecretTextObject(secretsName, secretValue);
    List<EncryptedData> encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    boolean isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertFalse(isSecretPresent);

    String secretsId = srUtils.addSecret(getAccount().getUuid(), bearerToken, secretText);
    assertTrue(StringUtils.isNotBlank(secretsId));

    encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertTrue(isSecretPresent);
    secretText.setName(secretsNewName);

    boolean isUpdationDone = srUtils.updateSecret(getAccount().getUuid(), bearerToken, secretsId, secretText);
    assertTrue(isUpdationDone);
    encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertTrue(isSecretPresent);

    // Verifying the secret decryption
    EncryptedData data = encryptedDataList.get(0);
    data.setEncryptionKey("SECRET_TEXT/" + secretsNewName);
    String decrypted = SecretsUtils.getValueFromName(secretManagementDelegateService, data, vaultConfig);
    assertEquals(secretValue, decrypted);

    boolean isDeletionDone = srUtils.deleteSecret(getAccount().getUuid(), bearerToken, secretsId);
    assertTrue(isDeletionDone);
    encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    assertTrue(encryptedDataList.size() == 0);
  }

  @After
  public void vaultCleanup() {
    assertTrue(vrUtils.deleteVault(getAccount().getUuid(), bearerToken, vaultId));
    logger.info("Vault Deleted. Test Clean up completed");
  }
}

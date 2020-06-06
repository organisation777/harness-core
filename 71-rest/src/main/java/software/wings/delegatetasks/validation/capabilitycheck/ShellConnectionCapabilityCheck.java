package software.wings.delegatetasks.validation.capabilitycheck;

import static java.time.Duration.ofSeconds;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.jcraft.jsch.JSchException;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.winrm.executors.WinRmSession;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.validation.capabilities.ShellConnectionCapability;
import software.wings.service.intfc.security.EncryptionService;

@Slf4j
public class ShellConnectionCapabilityCheck implements CapabilityCheck {
  @Inject EncryptionService encryptionService;
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    ShellConnectionCapability capability = (ShellConnectionCapability) delegateCapability;
    ShellScriptParameters parameters = capability.getShellScriptParameters();

    switch (parameters.getConnectionType()) {
      case SSH:
        return validateSshConnection(capability, parameters);
      case WINRM:
        return validateWinrmConnection(capability, parameters);
      default:
        logger.error("This should Not happen");
        return CapabilityResponse.builder().validated(false).delegateCapability(capability).build();
    }
  }

  private CapabilityResponse validateWinrmConnection(
      ShellConnectionCapability capability, ShellScriptParameters parameters) {
    try {
      int timeout = (int) ofSeconds(15L).toMillis();
      WinRmSessionConfig winrmConfig = parameters.winrmSessionConfig(encryptionService);
      winrmConfig.setTimeout(timeout);
      logger.info("Validating WinrmSession to Host: {}, Port: {}, useSsl: {}", winrmConfig.getHostname(),
          winrmConfig.getPort(), winrmConfig.isUseSSL());

      try (WinRmSession ignore = new WinRmSession(winrmConfig)) {
        return CapabilityResponse.builder().validated(true).delegateCapability(capability).build();
      }

    } catch (Exception ex) {
      logger.info("Exception in sshSession Validation", ex);
      return CapabilityResponse.builder().validated(false).delegateCapability(capability).build();
    }
  }

  private CapabilityResponse validateSshConnection(
      ShellConnectionCapability capability, ShellScriptParameters parameters) {
    try {
      int timeout = (int) ofSeconds(15L).toMillis();
      SshSessionConfig expectedSshConfig = parameters.sshSessionConfig(encryptionService);
      expectedSshConfig.setSocketConnectTimeout(timeout);
      expectedSshConfig.setSshConnectionTimeout(timeout);
      expectedSshConfig.setSshSessionTimeout(timeout);
      performTest(expectedSshConfig);
      return CapabilityResponse.builder().validated(true).delegateCapability(capability).build();
    } catch (Exception ex) {
      logger.info("Exception in sshSession Validation", ex);
      return CapabilityResponse.builder().validated(false).delegateCapability(capability).build();
    }
  }

  @VisibleForTesting
  void performTest(SshSessionConfig expectedSshConfig) throws JSchException {
    getSSHSession(expectedSshConfig).disconnect();
  }
}

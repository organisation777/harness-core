package software.wings.core.ssh.executors;

import com.google.inject.Inject;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.core.ssh.ExecutionLogs;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService;

import java.io.*;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static software.wings.beans.ErrorConstants.*;
import static software.wings.core.ssh.executors.SSHExecutor.ExecutionResult.FAILURE;
import static software.wings.core.ssh.executors.SSHExecutor.ExecutionResult.SUCCESS;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;
import static software.wings.utils.Misc.quietSleep;

/**
 * Created by anubhaw on 2/10/16.
 */

public abstract class AbstractSSHExecutor implements SSHExecutor {
  protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
  protected Session session;
  protected Channel channel;
  protected SSHSessionConfig config;
  protected OutputStream outputStream;
  protected InputStream inputStream;

  @Inject protected ExecutionLogs executionLogs;
  @Inject protected FileService fileService;

  public static String DEFAULT_SUDO_PROMPT_PATTERN = "^\\[sudo\\] password for .+: .*";

  public void init(SSHSessionConfig config) {
    if (null == config.getExecutionID() || config.getExecutionID().length() == 0) {
      throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, new Throwable("INVALID_EXECUTION_ID"));
    }

    this.config = config;
    try {
      session = getSession(config);
      channel = session.openChannel("exec");
      ((ChannelExec) channel).setPty(true);
      ((ChannelExec) channel).setErrStream(System.err, true);
      outputStream = channel.getOutputStream();
      inputStream = channel.getInputStream();
    } catch (JSchException e) {
      LOGGER.error("Failed to initialize executor");
      SSHException shEx = extractSSHException(e);
      throw new WingsException(shEx.code, shEx.msg, e.getCause());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ExecutionResult execute(String command) {
    return genericExecute(command);
  }

  private ExecutionResult genericExecute(String command) {
    try {
      ((ChannelExec) channel).setCommand(command);
      channel.connect();

      byte[] tmp = new byte[1024]; // FIXME: Improve stream reading/writing logic
      while (true) {
        while (inputStream.available() > 0) {
          int i = inputStream.read(tmp, 0, 1024);
          if (i < 0)
            break;
          String line = new String(tmp, 0, i);
          if (line.matches(DEFAULT_SUDO_PROMPT_PATTERN)) {
            outputStream.write((config.getSudoUserPassword() + "\n").getBytes());
            outputStream.flush();
          }
          executionLogs.appendLogs(config.getExecutionID(), line);
        }
        if (channel.isClosed()) {
          return channel.getExitStatus() == 0 ? SUCCESS : FAILURE;
        }
        quietSleep(1000);
      }
    } catch (JSchException e) {
      SSHException shEx = extractSSHException(e);
      LOGGER.error("Command execution failed with error " + e.getMessage());
      throw new WingsException(shEx.code, shEx.msg, e.getCause());
    } catch (IOException e) {
      LOGGER.error("Exception in reading InputStream");
      throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, e.getCause());
    } finally {
      destroy();
    }
  }

  public void destroy() {
    LOGGER.info("Disconnecting ssh session");
    if (null != channel) {
      channel.disconnect();
    }
    if (null != session) {
      session.disconnect();
    }
  }

  public void abort() {
    try {
      outputStream.write(3); // Send ^C command
      outputStream.flush();
    } catch (IOException e) {
      LOGGER.error("Abort command failed " + e.getStackTrace());
    }
  }

  public abstract Session getSession(SSHSessionConfig config) throws JSchException;

  public void postChannelConnect(){};

  protected class SSHException {
    private String code;
    private String msg;

    private SSHException(String code, String cause) {
      this.code = code;
      this.msg = cause;
    }

    public String getCode() {
      return code;
    }

    public String getMsg() {
      return msg;
    }
  }

  protected SSHException extractSSHException(JSchException jSchException) {
    String message = jSchException.getMessage();
    Throwable cause = jSchException.getCause();

    String customMessage = null;
    String customCode = null;

    if (null != cause) {
      if (cause instanceof NoRouteToHostException || cause instanceof UnknownHostException) {
        customMessage = UNKNOWN_HOST_ERROR_MSG;
        customCode = UNKNOWN_HOST_ERROR_CODE;
      } else if (cause instanceof SocketTimeoutException) {
        customMessage = UNKNOWN_HOST_ERROR_MSG;
        customCode = UNKNOWN_HOST_ERROR_CODE;
      } else if (cause instanceof SocketException) {
        customMessage = SSH_SOCKET_CONNECTION_ERROR_MSG;
        customCode = SSH_SOCKET_CONNECTION_ERROR_CODE;
      } else if (cause instanceof FileNotFoundException) {
        customMessage = INVALID_KEYPATH_ERROR_CODE;
        customCode = INVALID_KEYPATH_ERROR_MSG;
      } else {
        customMessage = UNKNOWN_ERROR_CODE;
        customCode = UNKNOWN_ERROR_MEG;
      }
    } else {
      if (message.startsWith("invalid privatekey")) {
        customMessage = INVALID_KEY_ERROR_MSG;
        customCode = INVALID_KEY_ERROR_CODE;
      } else if (message.contains("Auth fail") || message.contains("Auth cancel")
          || message.contains("USERAUTH fail")) {
        customMessage = INVALID_CREDENTIAL_ERROR_MSG;
        customCode = INVALID_CREDENTIAL_ERROR_CODE;
      } else if (message.startsWith("timeout: socket is not established")) {
        customMessage = SSH_SOCKET_CONNECTION_ERROR_MSG;
        customCode = SSH_SOCKET_CONNECTION_ERROR_CODE;
      }
    }
    return new SSHException(customCode, customMessage);
  }

  /**** SCP ****/

  public ExecutionResult transferFile(String localFilePath, String remoteFilePath) {
    FileInputStream fis = null;
    try {
      String command = "scp -t " + remoteFilePath;
      Channel channel = session.openChannel("exec");
      ((ChannelExec) channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out = channel.getOutputStream();
      InputStream in = channel.getInputStream();
      channel.connect();

      if (checkAck(in) != 0) {
        LOGGER.error("SCP connection initiation failed");
        return FAILURE;
      }
      GridFSFile fileMetaData = fileService.getGridFsFile(localFilePath, ARTIFACTS);

      // send "C0644 filesize filename", where filename should not include '/'
      long filesize = fileMetaData.getLength();
      String fileName = fileMetaData.getFilename();
      if (fileName.lastIndexOf('/') > 0) {
        fileName += fileName.substring(fileName.lastIndexOf('/') + 1);
      }
      command = "C0644 " + filesize + " " + fileName + "\n";

      out.write(command.getBytes());
      out.flush();
      if (checkAck(in) != 0) {
        return FAILURE;
      }
      fileService.downloadToStream(localFilePath, out, ARTIFACTS);
      out.write(new byte[1], 0, 1);
      out.flush();

      if (checkAck(in) != 0) {
        LOGGER.error("SCP connection initiation failed");
        return FAILURE;
      }
      out.close();
      channel.disconnect();
      session.disconnect();
    } catch (FileNotFoundException ex) {
      LOGGER.error("file [" + localFilePath + "] could not be found");
      throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, ex.getCause());
    } catch (IOException e) {
      LOGGER.error("Exception in reading InputStream");
      throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, e.getCause());
    } catch (JSchException e) {
      SSHException shEx = extractSSHException(e);
      LOGGER.error("Command execution failed with error " + e.getMessage());
      throw new WingsException(shEx.getCode(), shEx.getMsg(), e.getCause());
    }
    return SUCCESS;
  }

  int checkAck(InputStream in) throws IOException {
    int b = in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if (b == 0)
      return b;
    else if (b == -1)
      return b;
    else { // error or echoed string on session initiation from remote host
      StringBuilder sb = new StringBuilder();
      if (b > 2) {
        sb.append((char) b);
      }

      int c;
      do {
        c = in.read();
        sb.append((char) c);
      } while (c != '\n');

      if (b <= 2) {
        throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, new Throwable(sb.toString()));
      }
      LOGGER.error(sb.toString());
      return 0;
    }
  }
}

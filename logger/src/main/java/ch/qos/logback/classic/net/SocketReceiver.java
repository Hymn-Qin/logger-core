/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.classic.net;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.net.SocketFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.server.HardenedLoggingEventInputStream;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.net.AbstractSocketAppender;
import ch.qos.logback.core.net.DefaultSocketConnector;
import ch.qos.logback.core.net.SocketConnector;
import ch.qos.logback.core.util.CloseUtil;

/**
 * A component that receives serialized {@link ILoggingEvent} objects from a
 * remote appender over a {@link Socket}.
 *
 * @author Carl Harris
 */
public class SocketReceiver extends ReceiverBase
        implements Runnable, SocketConnector.ExceptionHandler {

  private static final int DEFAULT_ACCEPT_CONNECTION_DELAY = 5000;

  private String remoteHost;
  private InetAddress address;
  private int port;
  private int reconnectionDelay;
  private int acceptConnectionTimeout = DEFAULT_ACCEPT_CONNECTION_DELAY;

  private String receiverId;
  private volatile Socket socket;
  private Future<Socket> connectorTask;

  /**
   * {@inheritDoc}
   */
  protected boolean shouldStart() {
    int errorCount = 0;
    if (port == 0) {
      errorCount++;
      addError("No port was configured for receiver. "
              + "For more information, please visit http://logback.qos.ch/codes.html#receiver_no_port");
    }

    if (remoteHost == null) {
      errorCount++;
      addError("No host name or address was configured for receiver. "
              + "For more information, please visit http://logback.qos.ch/codes.html#receiver_no_host");
    }

    if (reconnectionDelay == 0) {
      reconnectionDelay = AbstractSocketAppender.DEFAULT_RECONNECTION_DELAY;
    }

    if (errorCount == 0) {
      try {
        address = InetAddress.getByName(remoteHost);
      } catch (UnknownHostException ex) {
        addError("unknown host: " + remoteHost);
        errorCount++;
      }
    }

    if (errorCount == 0) {
      receiverId = "receiver " + remoteHost + ":" + port + ": ";
    }

    return errorCount == 0;
  }

  /**
   * {@inheritDoc}
   */
  protected void onStop() {
    if (socket != null) {
      CloseUtil.closeQuietly(socket);
    }
  }

  @Override
  protected Runnable getRunnableTask() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public void run() {
    try {
      LoggerContext lc = (LoggerContext) getContext();
      while (!Thread.currentThread().isInterrupted()) {
        SocketConnector connector = createConnector(address, port, 0,
                reconnectionDelay);
        connectorTask = activateConnector(connector);
        if (connectorTask == null) {
          break;
        }
        socket = waitForConnectorToReturnASocket();
        if (socket == null)
          break;
        dispatchEvents(lc);
      }
    } catch (InterruptedException ex) {
      assert true;    // ok... we'll exit now
    }
    addInfo("shutting down");
  }

  private SocketConnector createConnector(InetAddress address, int port,
                                          int initialDelay, int retryDelay) {
    SocketConnector connector = newConnector(address, port, initialDelay,
            retryDelay);
    connector.setExceptionHandler(this);
    connector.setSocketFactory(getSocketFactory());
    return connector;
  }


  private Future<Socket> activateConnector(SocketConnector connector) {
    try {
      return getContext().getScheduledExecutorService().submit(connector);
    } catch (RejectedExecutionException ex) {
      return null;
    }
  }

  private Socket waitForConnectorToReturnASocket() throws InterruptedException {
    try {
      Socket s = connectorTask.get();
      connectorTask = null;
      return s;
    } catch (ExecutionException e) {
      return null;
    }
  }

  private void dispatchEvents(LoggerContext lc) {
    ObjectInputStream ois = null;
    try {
      socket.setSoTimeout(acceptConnectionTimeout);
      ois = new HardenedLoggingEventInputStream(socket.getInputStream());
      socket.setSoTimeout(0);
      addInfo(receiverId + "connection established");
      while (true) {
        ILoggingEvent event = (ILoggingEvent) ois.readObject();
        Logger remoteLogger = lc.getLogger(event.getLoggerName());
        if (remoteLogger.isEnabledFor(event.getLevel())) {
          remoteLogger.callAppenders(event);
        }
      }
    } catch (EOFException ex) {
      addInfo(receiverId + "end-of-stream detected");
    } catch (IOException ex) {
      addInfo(receiverId + "connection failed: " + ex);
    } catch (ClassNotFoundException ex) {
      addInfo(receiverId + "unknown event class: " + ex);
    } finally {
      CloseUtil.closeQuietly(ois);
      CloseUtil.closeQuietly(socket);
      socket = null;
      addInfo(receiverId + "connection closed");
    }
  }

  /**
   * {@inheritDoc}
   */
  public void connectionFailed(SocketConnector connector, Exception ex) {
    if (ex instanceof InterruptedException) {
      addWarn("connector interrupted", ex);
    } else if (ex instanceof ConnectException) {
      addWarn(receiverId + "connection refused", ex);
    } else {
      addWarn(receiverId + "unspecified error", ex);
    }
  }


  protected SocketConnector newConnector(InetAddress address,
                                         int port, int initialDelay, int retryDelay) {
    return new DefaultSocketConnector(address, port, initialDelay, retryDelay);
  }

  protected SocketFactory getSocketFactory() {
    return SocketFactory.getDefault();
  }

  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setReconnectionDelay(int reconnectionDelay) {
    this.reconnectionDelay = reconnectionDelay;
  }

  public void setAcceptConnectionTimeout(int acceptConnectionTimeout) {
    this.acceptConnectionTimeout = acceptConnectionTimeout;
  }

}

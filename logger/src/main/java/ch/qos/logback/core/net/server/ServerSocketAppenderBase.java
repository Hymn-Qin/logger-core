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
package ch.qos.logback.core.net.server;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;

import javax.net.ServerSocketFactory;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.net.AbstractSocketAppender;
import ch.qos.logback.core.spi.PreSerializationTransformer;

/**
 *
 * This is the base class for module specific ServerSocketAppender
 * implementations.
 *
 * @author Carl Harris
 */
public abstract class ServerSocketAppenderBase<E> extends AppenderBase<E> {

  /**
   * Default {@link ServerSocket} backlog
   */
  public static final int DEFAULT_BACKLOG = 50;

  /**
   * Default queue size used for each client
   */
  public static final int DEFAULT_CLIENT_QUEUE_SIZE = 100;

  private int port = AbstractSocketAppender.DEFAULT_PORT;
  private int backlog = DEFAULT_BACKLOG;
  private int clientQueueSize = DEFAULT_CLIENT_QUEUE_SIZE;

  private String address;

  private ServerRunner<RemoteReceiverClient> runner;

  @Override
  public void start() {
    if (isStarted()) return;
    try {
      ServerSocket socket = getServerSocketFactory().createServerSocket(
          getPort(), getBacklog(), getInetAddress());
      ServerListener<RemoteReceiverClient> listener = createServerListener(socket);

      runner = createServerRunner(listener, getContext().getScheduledExecutorService());
      runner.setContext(getContext());
      getContext().getScheduledExecutorService().execute(runner);
      super.start();
    }
    catch (Exception ex) {
      addError("server startup error: " + ex, ex);
    }
  }

  protected ServerListener<RemoteReceiverClient> createServerListener(
      ServerSocket socket) {
    return new RemoteReceiverServerListener(socket);
  }

  protected ServerRunner<RemoteReceiverClient> createServerRunner(
      ServerListener<RemoteReceiverClient> listener,
      Executor executor) {
    return new RemoteReceiverServerRunner(listener, executor,
        getClientQueueSize());
  }

  @Override
  public void stop() {
    if (!isStarted()) return;
    try {
      runner.stop();
      super.stop();
    }
    catch (IOException ex) {
      addError("server shutdown error: " + ex, ex);
    }
  }

  @Override
  protected void append(E event) {
    if (event == null) return;
    postProcessEvent(event);
    final Serializable serEvent = getPST().transform(event);
    runner.accept(new ClientVisitor<RemoteReceiverClient>() {
      public void visit(RemoteReceiverClient client) {
        client.offer(serEvent);
      }
    });
  }

  /**
   * Post process an event received via {@link #append}.
   * @param event the log event
   */
  protected abstract void postProcessEvent(E event);

  /**
   * Gets a transformer that will be used to convert a received event
   * to a {@link Serializable} form.
   * @return the pre-serialization transformer
   */
  protected abstract PreSerializationTransformer<E> getPST();

  /**
   * Gets the factory used to create {@link ServerSocket} objects.
   * <p>
   * The default implementation delegates to
   * {@link ServerSocketFactory#getDefault()}.  Subclasses may override to
   * private a different socket factory implementation.
   *
   * @return socket factory.
   * @throws Exception could not get socket factory
   */
  protected ServerSocketFactory getServerSocketFactory() throws Exception {
    return ServerSocketFactory.getDefault();
  }

  /**
   * Gets the local address for the listener.
   * @return an {@link InetAddress} representation of the local address.
   * @throws UnknownHostException could not get local address
   */
  protected InetAddress getInetAddress() throws UnknownHostException {
    if (getAddress() == null) return null;
    return InetAddress.getByName(getAddress());
  }

  /**
   * Gets the local port for the listener.
   * @return local port
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the local port for the listener.
   * @param port the local port to set
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Gets the listener queue depth.
   * <p>
   * This represents the number of connected clients whose connections
   * have not yet been accepted.
   * @return queue depth
   * @see ServerSocket
   */
  public Integer getBacklog() {
    return backlog;
  }

  /**
   * Sets the listener queue depth.
   * <p>
   * This represents the number of connected clients whose connections
   * have not yet been accepted.
   * @param backlog the queue depth to set
   * @see ServerSocket
   */
  public void setBacklog(Integer backlog) {
    this.backlog = backlog;
  }

  /**
   * Gets the local address for the listener.
   * @return a string representation of the local address
   */
  public String getAddress() {
    return address;
  }

  /**
   * Sets the local address for the listener.
   * @param address a host name or a string representation of an IP address
   */
  public void setAddress(String address) {
    this.address = address;
  }

  /**
   * Gets the event queue size used for each client connection.
   * @return queue size
   */
  public int getClientQueueSize() {
    return clientQueueSize;
  }

  /**
   * Sets the event queue size used for each client connection.
   * @param clientQueueSize the queue size to set
   */
  public void setClientQueueSize(int clientQueueSize) {
    this.clientQueueSize = clientQueueSize;
  }

}


/**
 * Copyright 2010 Molindo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.molindo.notify.dispatch;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import at.molindo.notify.INotifyService;
import at.molindo.notify.INotifyService.IErrorListener;
import at.molindo.notify.INotifyService.NotifyException;
import at.molindo.notify.INotifyService.NotifyRuntimeException;
import at.molindo.notify.channel.IPushChannel;
import at.molindo.notify.channel.IPushChannel.PushException;
import at.molindo.notify.dao.INotificationDAO;
import at.molindo.notify.dao.IPreferencesDAO;
import at.molindo.notify.message.INotificationRenderService;
import at.molindo.notify.model.Notification;
import at.molindo.notify.model.Preferences;
import at.molindo.notify.model.PushChannelPreferences;
import at.molindo.notify.model.PushChannelPreferences.Frequency;
import at.molindo.notify.model.PushState;
import at.molindo.notify.render.IRenderService.RenderException;
import at.molindo.utils.concurrent.FactoryThread;
import at.molindo.utils.concurrent.FactoryThread.FactoryThreadGroup;
import at.molindo.utils.concurrent.KeyLock;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class PollingPushDispatcher implements IPushDispatcher, InitializingBean, DisposableBean {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PollingPushDispatcher.class);

	private static final int DEFAULT_POOL_SIZE = 1;
	private static final int DEFAULT_MAX_ERROR = 3;

	private int _poolSize = DEFAULT_POOL_SIZE;

	private INotifyService _notifyService;

	private INotificationDAO _notificationDAO;
	private IPreferencesDAO _preferencesDAO;

	private Set<IPushChannel> _pushChannels = new CopyOnWriteArraySet<IPushChannel>();

	private final Object _wait = new Object();
	private final KeyLock<Long, PushResultMessage> _notificationLock = KeyLock.newKeyLock();

	private IErrorListener _errorListener;

	private int _maxErrorCount = DEFAULT_MAX_ERROR;

	private FactoryThreadGroup _threadGroup;

	private INotificationRenderService _notificationRenderService;

	private enum PushResult {
		SUCCESS, TEMPORARY_ERROR, PERSISTENT_ERROR;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (_pushChannels.size() == 0) {
			throw new IllegalStateException("no push channels configured");
		}
		if (_notificationRenderService == null) {
			throw new IllegalStateException("no notificationRenderService configured");
		}
		if (_notificationDAO == null) {
			throw new IllegalStateException("no notificationDAO configured");
		}
		if (_preferencesDAO == null) {
			throw new IllegalStateException("no preferencesDAO configured");
		}

		_threadGroup = new FactoryThread.FactoryThreadGroup(PollingPushDispatcher.class.getSimpleName(), _poolSize,
				new FactoryThread.IRunnableFactory() {

					@Override
					public Runnable newRunnable() {
						return new Polling();
					}
				}).start();

		_notifyService.addNotificationListener(this);
	}

	@Override
	public void destroy() {
		_notifyService.removeNotificationListener(this);

		_threadGroup.setInactive();
		synchronized (_wait) {
			_wait.notifyAll();
		}
		try {
			log.info("waiting for termination of running notification tasks");
			_threadGroup.join();
			log.info("all running notification tasks terminated");
		} catch (InterruptedException e1) {
			log.warn("interrupted while waiting for termination of notificaiton tasks");
		}

	}

	@Override
	public Map<String, PushChannelPreferences> newDefaultPreferences() {
		Map<String, PushChannelPreferences> map = Maps.newHashMap();
		for (IPushChannel channel : _pushChannels) {
			PushChannelPreferences cPrefs = channel.newDefaultPreferences();
			if (cPrefs != null) {
				map.put(channel.getId(), cPrefs);
			}
		}
		return map;
	}

	@Override
	public void notification(Notification notification) {
		synchronized (_wait) {
			_wait.notify();
		}
	}

	@Override
	public void dispatchNow(Notification notification) throws NotifyException {
		PushResultMessage rm = doPush(new DispatchConf(notification, true));

		if (rm.getResult() != PushResult.SUCCESS) {
			throw new NotifyException("failed to dispatch now: " + notification);
		} else {
			// only record success of dispatchNow as failed notifications must
			// not be stored for later use
			recordPushAttempt(notification, rm);
		}
	}

	@Override
	public void setErrorListener(IErrorListener errorListener) {
		_errorListener = errorListener;
	}

	/**
	 * wraps a {@link KeyLock} around {@link #doPush(DispatchConf)}
	 * 
	 * @see #doPush(DispatchConf)
	 * @return null if notification already gets pushed by another thread
	 */
	@CheckForNull
	PushResultMessage push(final DispatchConf dc) {
		try {
			return _notificationLock.withLock(dc.notification.getId(), new Callable<PushResultMessage>() {

				@Override
				public PushResultMessage call() {
					return doPush(dc);
				}
			});
		} catch (Exception e) {
			throw new NotifyRuntimeException("unexepcted exception from doPush()", e);
		}
	}

	@Nonnull
	private PushResultMessage doPush(DispatchConf dc) {
		Preferences prefs = _preferencesDAO.getPreferences(dc.notification.getUserId());
		if (prefs == null) {
			log.warn("can't push to unknown user " + dc.notification.getUserId());
			return PushResultMessage.persistent("unknown user " + dc.notification.getUserId());
		}

		Set<String> successChannels = Sets.newHashSet();
		Map<String, String> temporaryChannels = Maps.newHashMap();
		Map<String, String> persistentChannels = Maps.newHashMap();

		for (IPushChannel channel : _pushChannels) {
			PushChannelPreferences cPrefs = prefs.getChannelPrefs().get(channel.getId());
			if (cPrefs == null) {
				cPrefs = channel.newDefaultPreferences();
			}

			if (isAllowed(dc, channel, cPrefs, Frequency.INSTANT)) {
				try {
					channel.push(_notificationRenderService.render(dc.notification, prefs, cPrefs), cPrefs);
					successChannels.add(channel.getId());
				} catch (PushException e) {
					if (e.isTemporaryError()) {
						temporaryChannels.put(channel.getId(), e.getMessage());
					} else {
						persistentChannels.put(channel.getId(), e.getMessage());
					}
					if (_errorListener != null) {
						_errorListener.error(dc.notification, channel, e);
					} else {
						log.warn(
								"failed to deliver notification " + dc.notification + " on channel " + channel.getId(),
								e);
					}
				} catch (RenderException e) {
					log.error("failed to render notification " + dc.notification, e);
				}
			}
		}

		if (successChannels.size() > 0) {
			return PushResultMessage.success("channels: " + successChannels);
		} else if (temporaryChannels.size() > 0) {
			return PushResultMessage.temporary("temporary error, channels: " + temporaryChannels);
		} else if (persistentChannels.size() > 0) {
			return PushResultMessage.persistent("persistent error, channels: " + persistentChannels);
		} else {
			return PushResultMessage.temporary("no allowed channels available");
		}
	}

	boolean isAllowed(DispatchConf dc, IPushChannel channel, PushChannelPreferences cPrefs, Frequency frequency) {

		if (cPrefs == null) {
			// no preferences for this channel
			return false;
		}

		if (!dc.instant && !frequency.equals(cPrefs.getFrequency())) {
			return false;
		}

		if (!channel.isConfigured(dc.notification.getUserId(), cPrefs)) {
			// prefs not complete, e.g. recipient address missing
			return false;
		}

		if (!channel.getNotificationTypes().contains(dc.notification.getType())) {
			// channel not applicable for type
			return false;
		}

		return true;
	}

	public int getPoolSize() {
		return _poolSize;
	}

	public void setPoolSize(int poolSize) {
		_poolSize = poolSize;
	}

	public void setNotificationRenderService(INotificationRenderService notificationRenderService) {
		_notificationRenderService = notificationRenderService;
	}

	public void setNotificationDAO(INotificationDAO notificationDAO) {
		_notificationDAO = notificationDAO;
	}

	public void setPreferencesDAO(IPreferencesDAO preferencesDAO) {
		_preferencesDAO = preferencesDAO;
	}

	public void setPushChannels(Set<IPushChannel> pushChannels) {
		_pushChannels = pushChannels;
	}

	class Polling implements Runnable {
		@Override
		public void run() {
			Notification notification = _notificationDAO.getNext();
			if (notification != null) {
				PushResultMessage rm = push(new DispatchConf(notification));
				if (rm != null) {
					recordPushAttempt(notification, rm);
				}
			} else {
				// add a polling delay
				delay();
			}
		}

		/**
		 * overwrite for testing
		 */
		protected void delay() {
			synchronized (_wait) {
				try {
					_wait.wait(TimeUnit.MINUTES.toMillis(3));
				} catch (InterruptedException e) {
					log.debug("polling thread interrupted", e);
				}
			}
		}

	}

	static class DispatchConf {

		final Notification notification;
		final boolean instant;

		DispatchConf(Notification notification) {
			this(notification, false);
		}

		DispatchConf(Notification notification, boolean instant) {
			if (notification == null) {
				throw new NullPointerException("notification");
			}
			this.notification = notification;
			this.instant = instant;
		}

	}

	public void recordPushAttempt(Notification notification, @Nonnull PushResultMessage rm) {

		if (rm.getResult() == PushResult.SUCCESS) {
			notification.setPushState(PushState.PUSHED);
			notification.setPushDate(new Date());
			notification.setPushErrorMessage(rm.getMessage());
		} else {
			int errorCount = notification.recordPushError(rm.getMessage());

			if (errorCount > _maxErrorCount || rm.getResult() == PushResult.PERSISTENT_ERROR) {
				notification.setPushState(PushState.UNDELIVERABLE);
				notification.setPushDate(new Date());
			} else {
				notification.setPushState(PushState.QUEUED);
				notification.setPushDate(new Date(System.currentTimeMillis() + waitAfter(errorCount)));
			}

		}

		_notificationDAO.update(notification);
	}

	private long waitAfter(final int errorCount) {
		switch (errorCount) {
		case 0:
			return 0;
		case 1:
			return 60000; // 60 seconds
		case 2:
			return 900000; // 15 minutes
		case 3:
			return 7200000; // 2 hours
		case 4:
			return 86400000; // 1 day
		default:
			return 259200000; // 3 days
		}
	}

	public void setMaxErrorCount(int maxErrorCount) {
		_maxErrorCount = maxErrorCount;
	}

	public void setNotifyService(INotifyService notifyService) {
		_notifyService = notifyService;
	}

	private static class PushResultMessage {

		private static PushResultMessage success(String message) {
			return new PushResultMessage(message, PushResult.SUCCESS);
		}

		private static PushResultMessage persistent(String message) {
			return new PushResultMessage(message, PushResult.PERSISTENT_ERROR);
		}

		private static PushResultMessage temporary(String message) {
			return new PushResultMessage(message, PushResult.TEMPORARY_ERROR);
		}

		private final String _message;
		private final PushResult _result;

		private PushResultMessage(String message, PushResult result) {
			if (result == null) {
				throw new NullPointerException("result");
			}

			_message = message;
			_result = result;
		}

		public String getMessage() {
			return _message;
		}

		public PushResult getResult() {
			return _result;
		}

		@Override
		public String toString() {
			return _result + " (" + _message + ")";
		}

	}
}

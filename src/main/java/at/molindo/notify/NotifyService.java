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

package at.molindo.notify;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import at.molindo.notify.channel.IPushChannel;
import at.molindo.notify.channel.IPushChannel.PushException;
import at.molindo.notify.confirm.IConfirmationService;
import at.molindo.notify.dao.INotificationDAO;
import at.molindo.notify.dao.IPreferencesDAO;
import at.molindo.notify.dispatch.IPushDispatcher;
import at.molindo.notify.model.Confirmation;
import at.molindo.notify.model.IParams;
import at.molindo.notify.model.IPreferences;
import at.molindo.notify.model.Notification;
import at.molindo.notify.model.Preferences;
import at.molindo.notify.servlet.INotifyUrlFactory;

public class NotifyService implements INotifyService, INotifyService.IErrorListener {

	private IPreferencesDAO _preferencesDAO;
	private INotificationDAO _notificationDAO;

	private final Set<IErrorListener> _errorListeners = new CopyOnWriteArraySet<IErrorListener>();
	private final Set<INotificationListner> _notificationListeners = new CopyOnWriteArraySet<INotificationListner>();

	private IPushDispatcher _instantDispatcher;

	private IConfirmationService _confirmationService;

	private INotifyUrlFactory _notifyUrlFactory;

	private Preferences _defaultPreferences = new Preferences();

	@Override
	public IPreferences getPreferences(String userId) {
		return _preferencesDAO.getPreferences(userId);
	}

	@Override
	public Preferences newPreferences(String userId) {
		Preferences p = _defaultPreferences.clone();
		p.setUserId(userId);
		p.generateSecret();

		p.getChannelPrefs().putAll(_instantDispatcher.newDefaultPreferences());

		return p;
	}

	@Override
	public void setPreferences(IPreferences prefs) {
		_preferencesDAO.savePreferences(prefs);
	}

	@Override
	public void removePreferences(String userId) {
		_preferencesDAO.removePreferences(userId);
	}

	@Override
	public void notify(Notification notification) {
		_notificationDAO.save(notification);
		for (INotificationListner l : _notificationListeners) {
			l.notification(notification);
		}
	}

	@Override
	public void notifyNow(Notification notification) throws NotifyException {
		try {
			_notificationDAO.save(notification);
			_instantDispatcher.dispatchNow(notification);
		} catch (NotifyException e) {
			_notificationDAO.delete(notification);
			throw e;
		}
	}

	@Override
	public void confirm(Notification notification) {
		notification.setConfirmation(new Confirmation());
		notify(notification);
	}

	@Override
	public void confirmNow(Notification notification) throws NotifyException {
		notification.setConfirmation(new Confirmation());
		notifyNow(notification);
	}

	@Override
	public void error(Notification notification, IPushChannel channel, PushException e) {
		if (_errorListeners.size() == 0) {

		} else {
			for (IErrorListener l : _errorListeners) {
				l.error(notification, channel, e);
			}
		}
	}

	@Override
	public void addErrorListener(IErrorListener listener) {
		_errorListeners.add(listener);
	}

	@Override
	public void removeErrorListener(IErrorListener listener) {
		_errorListeners.remove(listener);
	}

	public void setNotificationListeners(Collection<? extends INotificationListner> listeners) {
		if (_notificationListeners.size() > 0) {
			throw new IllegalStateException("already listeneres registered: " + _notificationListeners);
		}
		_notificationListeners.addAll(listeners);
	}

	@Override
	public void addNotificationListener(INotificationListner listner) {
		_notificationListeners.add(listner);
	}

	@Override
	public void removeNotificationListener(INotificationListner listner) {
		_notificationListeners.remove(listner);
	}

	@Override
	public void addConfirmationListener(IConfirmationListener listener) {
		_confirmationService.addConfirmationListener(listener);
	}

	@Override
	public void removeConfirmationListener(IConfirmationListener listener) {
		_confirmationService.removeConfirmationListener(listener);
	}

	@Override
	public String toPullPath(String channelId, String userId, IParams params) {
		return _notifyUrlFactory.toPullPath(channelId, userId, params);
	}

	public void setInstantDispatcher(IPushDispatcher instantDispatcher) {
		_instantDispatcher = instantDispatcher;
	}

	public void setPreferencesDAO(IPreferencesDAO preferencesDAO) {
		_preferencesDAO = preferencesDAO;
	}

	public void setNotificationDAO(INotificationDAO notificationDAO) {
		_notificationDAO = notificationDAO;
	}

	public void setDefaultPreferences(Preferences defaultPreferences) {
		if (defaultPreferences == null) {
			throw new NullPointerException("defaultPreferences");
		}
		_defaultPreferences = defaultPreferences;
	}

	public void setConfirmationService(IConfirmationService confirmationService) {
		_confirmationService = confirmationService;
	}

	public void setNotifyUrlFactory(INotifyUrlFactory notifyUrlFactory) {
		_notifyUrlFactory = notifyUrlFactory;
	}

}

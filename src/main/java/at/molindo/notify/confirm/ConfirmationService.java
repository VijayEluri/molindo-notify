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

package at.molindo.notify.confirm;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import at.molindo.notify.INotifyService.IConfirmationListener;
import at.molindo.notify.dao.INotificationDAO;
import at.molindo.notify.model.Notification;

public class ConfirmationService implements IConfirmationService {

	private static final long DEFAULT_CONFIRMATION_MAX_AGE_MS = TimeUnit.DAYS.toMillis(14);

	private INotificationDAO _notificationDAO;

	private final List<IConfirmationListener> _confirmationListeners = new CopyOnWriteArrayList<IConfirmationListener>();

	private long _confirmationMaxAgeMs = DEFAULT_CONFIRMATION_MAX_AGE_MS;

	public void setConfirmationListeners(Collection<? extends IConfirmationListener> listeners) {
		if (_confirmationListeners.size() > 0) {
			throw new IllegalStateException("already listeneres registered: " + _confirmationListeners);
		}
		_confirmationListeners.addAll(listeners);
	}

	@Override
	public void addConfirmationListener(IConfirmationListener listener) {
		_confirmationListeners.add(listener);
	}

	@Override
	public void removeConfirmationListener(IConfirmationListener listener) {
		_confirmationListeners.removeAll(Arrays.asList(listener));
	}

	@Override
	public String confirm(String key) throws ConfirmationException {
		Notification notification = _notificationDAO.getByConfirmationKey(key);
		if (notification == null) {
			return null;
		}

		if (System.currentTimeMillis() - notification.getDate().getTime() > _confirmationMaxAgeMs) {
			return null;
		}

		for (IConfirmationListener l : _confirmationListeners) {
			String redirect = l.confirm(notification);
			if (redirect != null) {
				if (!redirect.startsWith("/")) {
					throw new IllegalArgumentException("redirect path must be absolute, was " + redirect);
				}
				return redirect;
			}
		}

		throw new ConfirmationException("unhandled notification " + notification);
	}

	@Override
	public Notification getNotificationByKey(String key) {
		return key == null ? null : _notificationDAO.getByConfirmationKey(key);
	}

	public void setNotificationDAO(INotificationDAO notificationDAO) {
		_notificationDAO = notificationDAO;
	}

	protected long getConfirmationMaxAgeMs() {
		return _confirmationMaxAgeMs;
	}

	protected void setConfirmationMaxAgeMs(long confirmationMaxAgeMs) {
		_confirmationMaxAgeMs = confirmationMaxAgeMs;
	}

}

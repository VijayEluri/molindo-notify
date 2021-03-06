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

import javax.annotation.Nonnull;

import at.molindo.notify.channel.IPushChannel;
import at.molindo.notify.channel.IPushChannel.PushException;
import at.molindo.notify.dao.IPreferencesDAO;
import at.molindo.notify.model.IParams;
import at.molindo.notify.model.IPreferences;
import at.molindo.notify.model.Notification;
import at.molindo.notify.model.Param;
import at.molindo.notify.model.Params;

public interface INotifyService {

	public static final String MAIL_CHANNEL = "mail";
	public static final String PRIVATE_FEED_CHANNEL = "private-feed";
	public static final String PUBLIC_FEED_CHANNEL = "public-feed";

	// reserved params
	public static final Param<Object> NOTIFICATION = Param.pObject("notification");
	public static final Param<Object> PREFERENCES = Param.pObject("preferences");
	public static final Param<Object> CHANNEL_PREFERENCES = Param.pObject("channelPreferences");

	/**
	 * force notifying of unknown user id
	 */
	public static final Param<String> NOTIFY_UNKNOWN = Param.pString("unknown");

	/**
	 * URL for confirmations
	 */
	public static final Param<String> CONFIRMATION_URL = Param.pString("confirmationUrl");

	public static final Param<Boolean> RENDER_MASTER_TEMPLATE = Param.pBoolean("renderMaster");

	IPreferences getPreferences(@Nonnull String userId);

	@Nonnull
	IPreferences newPreferences(@Nonnull String userId);

	/**
	 * depends on the implementation of {@link IPreferencesDAO}
	 * 
	 * @throws NotifyRuntimeException
	 *             if not implemented
	 */
	void setPreferences(@Nonnull IPreferences prefs);

	/**
	 * depends on the implementation of {@link IPreferencesDAO}
	 * 
	 * @throws NotifyRuntimeException
	 *             if not implemented
	 */
	void removePreferences(@Nonnull String userId);

	void notify(@Nonnull Notification notification);

	void notifyNow(@Nonnull Notification notification) throws NotifyException;

	void confirm(@Nonnull Notification notification);

	void confirmNow(@Nonnull Notification notification) throws NotifyException;

	void addErrorListener(@Nonnull IErrorListener listener);

	void removeErrorListener(@Nonnull IErrorListener listener);

	void addNotificationListener(@Nonnull INotificationListner listner);

	void removeNotificationListener(@Nonnull INotificationListner listner);

	void addConfirmationListener(@Nonnull IConfirmationListener listener);

	void removeConfirmationListener(@Nonnull IConfirmationListener listener);

	String toPullPath(String channelId, String userId, IParams params);

	public interface INotificationListner {
		void notification(@Nonnull Notification notification);
	}

	public interface IConfirmationListener {

		/**
		 * @param notification
		 * @return a redirect path if notification was handled or null
		 */
		String confirm(@Nonnull Notification notification);
	}

	public interface IErrorListener {

		/**
		 * called if notification can't be pushed to any channel
		 * 
		 * @param notification
		 * @param channel
		 * @param e
		 */
		void error(@Nonnull Notification notification, @Nonnull IPushChannel channel, @Nonnull PushException e);
	}

	public abstract static class Utils {
		private Utils() {
		}

		public static void mailNow(INotifyService notifyService, Notification notification) throws NotifyException {
			notifyUnknownNow(notifyService, INotifyService.MAIL_CHANNEL, notification);
		}

		public static void mail(INotifyService notifyService, Notification notification) throws NotifyException {
			notifyUnknown(notifyService, INotifyService.MAIL_CHANNEL, notification);
		}

		public static void notifyUnknownNow(INotifyService notifyService, String channelId, Notification notification)
				throws NotifyException {
			notification.getParams().set(NOTIFY_UNKNOWN, channelId);
			notifyService.notifyNow(notification);
		}

		public static void notifyUnknown(INotifyService notifyService, String channelId, Notification notification)
				throws NotifyException {
			notification.getParams().set(NOTIFY_UNKNOWN, channelId);
			notifyService.notify(notification);
		}
	}

	public interface IParamsFactory {

		/**
		 * add or remove params
		 * 
		 * @throws NotifyException
		 *             if necessary params aren't available (e.g. if
		 *             notification is obsolete)
		 */
		void params(@Nonnull Params params) throws NotifyException;
	}

	public static class NotifyException extends Exception {

		private static final long serialVersionUID = 1L;

		public NotifyException() {
			super();
		}

		public NotifyException(String message, Throwable cause) {
			super(message, cause);
		}

		public NotifyException(String message) {
			super(message);
		}

		public NotifyException(Throwable cause) {
			super(cause);
		}

	}

	public static class NotifyRuntimeException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public NotifyRuntimeException() {
			super();
		}

		public NotifyRuntimeException(String message, Throwable cause) {
			super(message, cause);
		}

		public NotifyRuntimeException(String message) {
			super(message);
		}

		public NotifyRuntimeException(Throwable cause) {
			super(cause);
		}

	}

}

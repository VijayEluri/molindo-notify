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

package at.molindo.notify.channel;

import at.molindo.notify.INotifyService.NotifyException;
import at.molindo.notify.model.IChannelPreferences;

public interface IPullChannel extends IChannel {

	String pull(String userId, IChannelPreferences cPrefs) throws PullException;

	boolean isAuthorized(String userId, IChannelPreferences cPrefs);

	public class PullException extends NotifyException {

		private static final long serialVersionUID = 1L;

		public PullException() {
			super();
		}

		public PullException(String message, Throwable cause) {
			super(message, cause);
		}

		public PullException(String message) {
			super(message);
		}

		public PullException(Throwable cause) {
			super(cause);
		}

	}

}

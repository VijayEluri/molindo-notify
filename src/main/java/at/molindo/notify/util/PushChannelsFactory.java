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

package at.molindo.notify.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.FactoryBean;

import at.molindo.notify.channel.IPushChannel;

public class PushChannelsFactory implements FactoryBean<List<IPushChannel>> {

	private final List<IPushChannel> _pushChannels = new CopyOnWriteArrayList<IPushChannel>();

	public void setPushChannels(List<IPushChannel> pushChannels) {
		_pushChannels.clear();
		_pushChannels.addAll(pushChannels);
	}

	public List<IPushChannel> getPushChannels() {
		return _pushChannels;
	}

	@Override
	public List<IPushChannel> getObject() throws Exception {
		return _pushChannels;
	}

	@Override
	public Class<?> getObjectType() {
		return _pushChannels.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}

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

package at.molindo.notify.context;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import at.molindo.notify.INotificationService;
import at.molindo.notify.INotificationService.NotifyException;
import at.molindo.notify.dao.dummy.DummyUtils;
import at.molindo.notify.model.Notification;
import at.molindo.notify.model.Param;
import at.molindo.notify.model.Params;

@ContextConfiguration({ ContextUtils.MAIN, ContextUtils.CHANNELS,
		ContextUtils.RENDER, ContextUtils.DAOS })
public class ContextTest extends AbstractJUnit4SpringContextTests {

	@Autowired
	private INotificationService _svc;

	@Test
	public void test() throws InterruptedException, NotifyException {
		_svc.notify(new Notification().setUserId(DummyUtils.USER_ID)
				.setKey(DummyUtils.KEY)
				.setParams(new Params().set(Param.pString("word"), "Test")));
		
		_svc.confirmNow(new Notification().setUserId(DummyUtils.USER_ID)
				.setKey(DummyUtils.KEY)
				.setParams(new Params().set(Param.pString("word"), "Test Now")));
	
		Thread.sleep(10000);
	}
	
}

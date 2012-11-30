/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. 
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */

package org.oscarehr.phr.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.oscarehr.common.dao.DemographicDao;
import org.oscarehr.common.dao.PropertyDao;
import org.oscarehr.common.dao.ProviderPreferenceDao;
import org.oscarehr.common.model.Demographic;
import org.oscarehr.common.model.ProviderPreference;
import org.oscarehr.myoscar.client.ws_manager.AccountManager;
import org.oscarehr.myoscar.utils.MyOscarLoggedInInfo;
import org.oscarehr.myoscar_server.ws.LoginResultTransfer2;
import org.oscarehr.util.DeamonThreadFactory;
import org.oscarehr.util.EncryptionUtils;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import org.oscarehr.util.WebUtils;

import oscar.OscarProperties;

public final class MyOscarUtils {
	private static final Logger logger = MiscUtils.getLogger();

	private static ExecutorService asyncAutoLoginThreadPool=Executors.newFixedThreadPool(4, new DeamonThreadFactory("asyncAutoLoginThreadPool", Thread.MIN_PRIORITY));

	private static boolean myOscarEnabled=initMyOscarEnabled();

	public static Demographic getDemographicByMyOscarUserName(String myOscarUserName) {
		DemographicDao demographicDao = (DemographicDao) SpringUtils.getBean("demographicDao");
		Demographic demographic = demographicDao.getDemographicByMyOscarUserName(myOscarUserName);
		return (demographic);
	}

	private static boolean initMyOscarEnabled() {
		OscarProperties properties = OscarProperties.getInstance();
		String myOscarModule = properties.getProperty("MY_OSCAR");
		if (myOscarModule != null) myOscarModule = myOscarModule.toLowerCase();
		myOscarModule = StringUtils.trimToNull(myOscarModule);
		boolean module = ("yes".equals(myOscarModule) || "true".equals(myOscarModule));

		return (module);
    }

	public static boolean isMyOscarEnabled()
	{
		return(myOscarEnabled);
	}
	
	public static String getDisabledStringForMyOscarSendButton(MyOscarLoggedInInfo myOscarLoggedInInfo, Integer demographicId) {
		boolean enabled = isMyOscarSendButtonEnabled(myOscarLoggedInInfo, demographicId);

		return (WebUtils.getDisabledString(enabled));
	}

	public static String getDisabledStringForMyOscarSendButton(MyOscarLoggedInInfo myOscarLoggedInInfo, Demographic demographic) {
		boolean enabled = isMyOscarSendButtonEnabled(myOscarLoggedInInfo, demographic);

		return (WebUtils.getDisabledString(enabled));
	}

	public static boolean isMyOscarSendButtonEnabled(MyOscarLoggedInInfo myOscarLoggedInInfo, Integer demographicId) {
		DemographicDao demographicDao = (DemographicDao) SpringUtils.getBean("demographicDao");
		Demographic demographic = demographicDao.getDemographicById(demographicId);

		return (isMyOscarSendButtonEnabled(myOscarLoggedInInfo, demographic));
	}

	public static boolean isMyOscarSendButtonEnabled(MyOscarLoggedInInfo myOscarLoggedInInfo, Demographic demographic) {
		return (myOscarLoggedInInfo != null && myOscarLoggedInInfo.isLoggedIn() && demographic != null && demographic.getMyOscarUserName() != null);
	}

	public static void attemptMyOscarAutoLoginIfNotAlreadyLoggedInAsynchronously(final LoggedInInfo loggedInInfo) {
		if (!isMyOscarEnabled()) return;
		
		HttpSession session = loggedInInfo.session;
		MyOscarLoggedInInfo myOscarLoggedInInfo=MyOscarLoggedInInfo.getLoggedInInfo(session);
		if (myOscarLoggedInInfo!=null && myOscarLoggedInInfo.isLoggedIn()) return;

		Runnable runnable=new Runnable()
		{
			@Override
			public void run()
			{
				attemptMyOscarAutoLoginIfNotAlreadyLoggedIn(loggedInInfo);
			}
		};
		
		asyncAutoLoginThreadPool.submit(runnable);
	}

	public static String getMyOscarUserNameFromOscar(String providerNo)
	{
		PropertyDao propertyDao = (PropertyDao) SpringUtils.getBean("propertyDao");
		List<org.oscarehr.common.model.Property> myOscarUserNameProperties=propertyDao.findByNameAndProvider("MyOscarId", providerNo); 
		if (myOscarUserNameProperties.size()>0) return(myOscarUserNameProperties.get(0).getValue());
		return(null);
	}
	
	public static Long getMyOscarUserIdFromOscarProviderNo(MyOscarLoggedInInfo myOscarLoggedInInfo, String providerNo)
	{
		String userName =getMyOscarUserNameFromOscar(providerNo);
		return(AccountManager.getUserId(myOscarLoggedInInfo, userName));
	}
	
	public static Long getMyOscarUserIdFromOscarDemographicId(MyOscarLoggedInInfo myOscarLoggedInInfo, Integer demographicId)
	{
		DemographicDao demographicDao = (DemographicDao) SpringUtils.getBean("demographicDao");
		Demographic demographic = demographicDao.getDemographicById(demographicId);
		return(AccountManager.getUserId(myOscarLoggedInInfo, demographic.getMyOscarUserName()));
	}
	
	public static void attemptMyOscarAutoLoginIfNotAlreadyLoggedIn(LoggedInInfo loggedInInfo) {
		HttpSession session = loggedInInfo.session;
		
		MyOscarLoggedInInfo myOscarLoggedInInfo=MyOscarLoggedInInfo.getLoggedInInfo(session);
		if (myOscarLoggedInInfo!=null && myOscarLoggedInInfo.isLoggedIn()) return;
		
		try {
			String myOscarUserName=getMyOscarUserNameFromOscar(loggedInInfo.loggedInProvider.getProviderNo());
			if (myOscarUserName==null) return;

			ProviderPreferenceDao providerPreferenceDao = (ProviderPreferenceDao) SpringUtils.getBean("providerPreferenceDao");
			ProviderPreference providerPreference = providerPreferenceDao.find(loggedInInfo.loggedInProvider.getProviderNo());

			byte[] encryptedMyOscarPassword = providerPreference.getEncryptedMyOscarPassword();
			if (encryptedMyOscarPassword == null) return;
			
			SecretKeySpec key = EncryptionUtils.getDeterministicallyMangledPasswordSecretKeyFromSession(session);
			byte[] decryptedMyOscarPasswordBytes = EncryptionUtils.decrypt(key, encryptedMyOscarPassword);
			String decryptedMyOscarPasswordString = new String(decryptedMyOscarPasswordBytes, "UTF-8");

			LoginResultTransfer2 loginResultTransfer=AccountManager.login(MyOscarLoggedInInfo.getMyOscarServerBaseUrl(), myOscarUserName, decryptedMyOscarPasswordString);
			
			if (loginResultTransfer != null) {
				myOscarLoggedInInfo=new MyOscarLoggedInInfo(loginResultTransfer.getPerson().getId(), loginResultTransfer.getSecurityTokenKey(), session.getId());
				MyOscarLoggedInInfo.setLoggedInInfo(session, myOscarLoggedInInfo);
			} else {
				// login failed, remove myoscar saved password
				providerPreference.setEncryptedMyOscarPassword(null);
				providerPreferenceDao.merge(providerPreference);
			}
		} catch (Exception e) {
			logger.error("Error attempting auto-myoscar login", e);
		}
	}
}

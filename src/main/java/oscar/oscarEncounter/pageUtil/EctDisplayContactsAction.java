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
package oscar.oscarEncounter.pageUtil;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.util.MessageResources;
import org.oscarehr.PMmodule.dao.ProviderDao;
import org.oscarehr.common.dao.ContactDao;
import org.oscarehr.common.dao.DemographicContactDao;
import org.oscarehr.common.model.DemographicContact;
import org.oscarehr.common.model.ProfessionalContact;
import org.oscarehr.common.model.Provider;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import oscar.util.StringUtils;

public class EctDisplayContactsAction extends EctDisplayAction {

    private static final String cmd = "contacts";

    DemographicContactDao demographicContactDao = SpringUtils.getBean(DemographicContactDao.class);
    ContactDao contactDao = SpringUtils.getBean(ContactDao.class);
	ProviderDao providerDao = SpringUtils.getBean(ProviderDao.class);
	
    
    public boolean getInfo(EctSessionBean bean, HttpServletRequest request, NavBarDisplayDAO Dao, MessageResources messages) {
 		try {
		    //Set left hand module heading and link
		    String winName = "contact" + bean.demographicNo;
		    String pathview, pathedit;

		    pathview = request.getContextPath() + "/demographic/procontactSearch.jsp";
		    pathedit = request.getContextPath() + "/demographic/Contact.do?method=manage&demographic_no=" + bean.demographicNo;


		    String url = "popupPage(650,900,'" + winName + "','" + pathview + "')";
		    Dao.setLeftHeading(messages.getMessage(request.getLocale(), "global.contacts"));
		    Dao.setLeftURL(url);

		    //set right hand heading link
		    winName = "AddContact" + bean.demographicNo;
		    url = "popupPage(800,800,'" + winName + "','" + pathedit + "'); return false;";
		    Dao.setRightURL(url);
		    Dao.setRightHeadingID(cmd);


		    List<DemographicContact> contacts = demographicContactDao.findActiveByDemographicNo(Integer.parseInt(bean.demographicNo));

		    for(DemographicContact contact:contacts) {
		    	//only show professional contacts
		    	if(contact.getCategory().equals(DemographicContact.CATEGORY_PERSONAL))
		    		continue;
		    	
		    	String name="N/A";
		    	String specialty = "";
		    	String workPhone = "";
		    	//String consent = "";
		    	
		    	if(contact.getType() == DemographicContact.TYPE_CONTACT) {
		    		ProfessionalContact c = (ProfessionalContact)contactDao.find(Integer.parseInt(contact.getContactId()));
		    		name = c.getLastName() + "," + c.getFirstName();
		    		specialty = c.getSpecialty();
		    		workPhone = c.getWorkPhone();
		    	} else  {
		    		Provider p = providerDao.getProvider(contact.getContactId());
		    		name = p.getFormattedName();
		    		specialty = p.getSpecialty();
		    		workPhone = p.getWorkPhone();
		    	}
		    	//contactDao.find(Integer.parseInt(contact.getContactId()));
		    	NavBarDisplayDAO.Item item = NavBarDisplayDAO.Item();
		    	//48.45
		    	String itemHeader = StringUtils.maxLenString(name, 20, 17, ELLIPSES) +
		    			((specialty.length()>0)?StringUtils.maxLenString("  "+ specialty, 14, 11, ELLIPSES):"") +
		    			((workPhone.length()>0)?StringUtils.maxLenString("  "+workPhone, 17, 14, ELLIPSES):"");
		        item.setTitle((contact.isConsentToContact()?"":"*") + itemHeader);
		        String consent = contact.isConsentToContact()?"Ok to contact":"Do not contact";
		        item.setLinkTitle(name + " " + specialty + " " + workPhone + " " + consent);
		        
		        //item.setDate(contact.getUpdateDate());
		        int hash = Math.abs(winName.hashCode());
		        if(contact.getType() == DemographicContact.TYPE_CONTACT) {
		        	url = "popupPage(500,900,'" + hash + "','" + request.getContextPath() + "/demographic/Contact.do?method=editProContact&pcontact.id="+ contact.getId() +"'); return false;";
		        } else {
		        	String roles =(String) request.getSession().getAttribute("userrole");
		        	if(roles.indexOf("admin") != -1)
		        		url = "popupPage(500,900,'" + hash + "','" + request.getContextPath() + "/admin/providerupdateprovider.jsp?keyword="+ contact.getContactId() +"'); return false;";
		        	else
		        		url = "alert('Cannot Edit');return false;";
		        }
		        item.setURL(url);
		        Dao.addItem(item);
		    }

		 }catch( Exception e ) {
		     MiscUtils.getLogger().error("Error", e);
		     return false;
		 }
		return true;
    	
    }

    public String getCmd() {
    	return cmd;
    }
}

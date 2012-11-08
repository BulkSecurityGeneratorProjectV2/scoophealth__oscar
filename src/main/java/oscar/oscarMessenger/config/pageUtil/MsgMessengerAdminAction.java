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


package oscar.oscarMessenger.config.pageUtil;


import java.io.IOException;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.oscarehr.common.dao.GroupMembersDao;
import org.oscarehr.common.dao.GroupsDao;
import org.oscarehr.common.model.GroupMembers;
import org.oscarehr.common.model.Groups;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import oscar.oscarDB.DBHandler;
import oscar.oscarMessenger.data.MsgAddressBookMaker;

public class MsgMessengerAdminAction extends Action {
	
	private GroupsDao groupsDao = SpringUtils.getBean(GroupsDao.class);
	private GroupMembersDao groupMembersDao = (GroupMembersDao)SpringUtils.getBean(GroupMembersDao.class);

	

 public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response)
	throws IOException, ServletException {

        String[] providers = ((MsgMessengerAdminForm)form).getProviders();
        String grpNo = ((MsgMessengerAdminForm)form).getGrpNo();
        String update = ((MsgMessengerAdminForm)form).getUpdate();
        String delete = ((MsgMessengerAdminForm)form).getDelete();

        String parent = new String();
        
        ResourceBundle oscarR = ResourceBundle.getBundle("oscarResources",request.getLocale());

        if (update.equals(oscarR.getString("oscarMessenger.config.MessengerAdmin.btnUpdateGroupMembers"))){

        	for(GroupMembers g:groupMembersDao.findByGroupId(Integer.parseInt(grpNo))) {
        		groupMembersDao.remove(g.getId());
        	}
        	
           try{          
              for (int i = 0; i < providers.length ; i++){
            	  GroupMembers gm = new GroupMembers();
            	  gm.setGroupId(Integer.parseInt(grpNo));
            	  gm.setProviderNo(providers[i]);
            	  groupMembersDao.persist(gm);
            	  
              }
              
              MsgAddressBookMaker addMake = new MsgAddressBookMaker();
              boolean  res = addMake.updateAddressBook();
           }catch (java.sql.SQLException e){MiscUtils.getLogger().error("Error", e); }


         request.setAttribute("groupNo",grpNo);
        }else if(delete.equals(oscarR.getString("oscarMessenger.config.MessengerAdmin.btnDeleteThisGroup"))){

            try{
                 
                 java.sql.ResultSet rs;

                 String sql = new String("select parentID from groups_tbl where groupID = '"+grpNo+"'");
                 rs = DBHandler.GetSQL(sql);
                 if (rs.next()){
                     parent =  oscar.Misc.getString(rs, "parentID");
                 }


                 sql = new String("select * from groups_tbl where parentID = '"+grpNo+"'");
                 rs = DBHandler.GetSQL(sql);

                 if (rs.next()){
                    request.setAttribute("groupNo",grpNo);
                    request.setAttribute("fail","This Group has Children, you must delete the children groups first");
                    return (mapping.findForward("failure"));
                 }else{
                	 for(GroupMembers g:groupMembersDao.findByGroupId(Integer.parseInt(grpNo))) {
                 		groupMembersDao.remove(g.getId());
                 	}
                   
                	 Groups g = groupsDao.find(Integer.parseInt(grpNo));
                	 if(g != null) {
                		 groupsDao.remove(g.getId());
                	 }

                 }
              rs.close();
              MsgAddressBookMaker addMake = new MsgAddressBookMaker();
              boolean res = addMake.updateAddressBook();
           }catch (java.sql.SQLException e){MiscUtils.getLogger().error("Error", e); }

         request.setAttribute("groupNo",parent);
        }

      return (mapping.findForward("success"));
 }




}

package org.oscarehr.olis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;

import oscar.oscarLab.FileUploadCheck;
import oscar.oscarLab.ca.all.upload.HandlerClassFactory;
import oscar.oscarLab.ca.all.upload.handlers.OLISHL7Handler;


public class OLISAddToInboxAction extends DispatchAction {

	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {

		String uuidToAdd = request.getParameter("uuid");

		String fileLocation = System.getProperty("java.io.tmpdir") + "/olis_" + uuidToAdd + ".response";
		File file = new File(fileLocation);
		OLISHL7Handler msgHandler = (OLISHL7Handler) HandlerClassFactory.getHandler("OLIS_HL7");
		
		InputStream is = null;
		try {
			is = new FileInputStream(fileLocation);
			String provNo =  LoggedInInfo.loggedInInfo.get().loggedInProvider.getProviderNo();
			int check = FileUploadCheck.addFile(file.getName(), is, provNo);
			
			if (check != FileUploadCheck.UNSUCCESSFUL_SAVE) {
				if (msgHandler.parse("OLIS_HL7",fileLocation, check, true) != null) {
					request.setAttribute("result", "Success");					
				} else {
					request.setAttribute("result", "Error");
				}
			} else {
				request.setAttribute("result", "Already Added");
			}			

		} catch (Exception e) {
			MiscUtils.getLogger().error("Couldn't add requested OLIS lab to Inbox.", e);
			request.setAttribute("result", "Error");
		} finally {
			try {
				is.close();
			} catch(IOException e){
				//ignore
			}
		}

		return mapping.findForward("ajax");
	}
}

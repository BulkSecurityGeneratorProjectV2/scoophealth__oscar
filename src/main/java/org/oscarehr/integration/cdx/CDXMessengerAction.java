package org.oscarehr.integration.cdx;

import ca.uvic.leadlab.obibconnector.facades.datatypes.*;
import ca.uvic.leadlab.obibconnector.facades.exceptions.OBIBException;
import ca.uvic.leadlab.obibconnector.facades.receive.IDocument;
import ca.uvic.leadlab.obibconnector.facades.registry.IClinic;
import ca.uvic.leadlab.obibconnector.facades.registry.IProvider;
import ca.uvic.leadlab.obibconnector.facades.send.ISubmitDoc;
import ca.uvic.leadlab.obibconnector.impl.send.SubmitDoc;
import com.lowagie.text.*;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RandomAccessFileOrArray;
import com.lowagie.text.pdf.codec.TiffImage;
import com.lowagie.text.rtf.parser.RtfParser;
import com.sun.xml.messaging.saaj.util.ByteInputStream;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import io.woo.htmltopdf.HtmlToPdf;
import io.woo.htmltopdf.HtmlToPdfObject;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.codehaus.jackson.map.ObjectMapper;
import org.oscarehr.casemgmt.model.CaseManagementNote;
import org.oscarehr.casemgmt.model.CaseManagementNoteLink;
import org.oscarehr.casemgmt.model.Issue;
import org.oscarehr.casemgmt.service.CaseManagementManager;
import org.oscarehr.common.dao.*;
import org.oscarehr.common.model.*;
import org.oscarehr.common.printing.FontSettings;
import org.oscarehr.common.printing.PdfWriterFactory;
import org.oscarehr.integration.cdx.dao.CdxAttachmentDao;
import org.oscarehr.integration.cdx.dao.CdxMessengerDao;
import org.oscarehr.integration.cdx.dao.CdxProvenanceDao;
import org.oscarehr.integration.cdx.model.CdxAttachment;
import org.oscarehr.integration.cdx.model.CdxMessenger;
import org.oscarehr.integration.cdx.model.CdxProvenance;
import org.oscarehr.managers.DemographicManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import oscar.OscarProperties;
import oscar.dms.EDoc;
import oscar.dms.EDocUtil;
import oscar.oscarDemographic.data.EctInformation;
import oscar.oscarEncounter.data.EctProgram;
import oscar.oscarLab.ca.all.pageUtil.LabPDFCreator;
import oscar.oscarLab.ca.on.CommonLabResultData;
import oscar.oscarLab.ca.on.LabResultData;
import oscar.util.ConcatPDF;
import oscar.util.StringUtils;
import oscar.util.UtilDateUtilities;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.*;
import java.sql.Timestamp;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.oscarehr.util.SpringUtils.getBean;

public class CDXMessengerAction extends DispatchAction {
    private static final Logger logger = MiscUtils.getLogger();
    private String contentRoute;

    public ActionForward submitDocument(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // TODO security check
        HttpSession session = request.getSession();
        if (session.getAttribute("userrole") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        contentRoute = request.getSession().getServletContext().getRealPath("/");
        HashMap<String, String[]> specialistAndClinicsPrimary = new HashMap<>();
        HashMap<String, String[]> specialistAndClinicsSecondary = new HashMap<>();
        String[] pRecipients = request.getParameterValues("precipients");
        String[] sRecipients = request.getParameterValues("srecipients");

        String specialistsToStore = "";
        if (pRecipients != null && pRecipients.length > 0) {
            for (String rec : pRecipients) {
                String[] splittedSpecialistsAndClinics = rec.split("@");
                specialistsToStore = specialistsToStore + splittedSpecialistsAndClinics[0] + ", ";
                specialistAndClinicsPrimary.put(splittedSpecialistsAndClinics[0], splittedSpecialistsAndClinics[1].split(","));
            }
            specialistsToStore = specialistsToStore.substring(0, specialistsToStore.length() - 2);
        }

        if (sRecipients != null && sRecipients.length > 0) {
            for (String rec : sRecipients) {
                String[] splittedSpecialistsAndClinics = rec.split("@");
                specialistsToStore = specialistsToStore + ", " + splittedSpecialistsAndClinics[0];
                specialistAndClinicsSecondary.put(splittedSpecialistsAndClinics[0], splittedSpecialistsAndClinics[1].split(","));
            }
        }

        CdxMessenger cdxMessenger = setCdxMessage(request, specialistsToStore);

        try {
            doCdxSend(loggedInInfo, request, cdxMessenger, specialistAndClinicsPrimary, specialistAndClinicsSecondary);
            request.setAttribute("success", true);
        } catch (OBIBException e) {
            request.setAttribute("success", false);
            request.setAttribute("error", e);
            logger.error("Error sending CDX document", e);
            String additionalText = e.getObibMessage();
            if (additionalText == null || additionalText.isEmpty()) {
                additionalText = e.getMessage();
            }
            logger.error("Additional Message." + additionalText);
        }

        return mapping.findForward("success");
    }

    public ActionForward saveDraft(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // TODO security check
        HttpSession session = request.getSession();
        if (session.getAttribute("userrole") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        String patient = request.getParameter("patient");
        //String primaryrecipient = request.getParameter("precipients");
        String[] precipients = request.getParameterValues("precipients[]");
        String[] srecipients = request.getParameterValues("srecipients[]");
        String msgType = request.getParameter("msgtype");
        String documentType = request.getParameter("documenttype");
        String content = request.getParameter("content");

        String recipientsToStore = "";
        String pSpecialists = "";
        String sSpecialists = "";
        if (precipients != null && precipients.length > 0) {
            for (String rec : precipients) {
                String[] splittedSpecialistsAndClinics = rec.split("@");
                recipientsToStore = recipientsToStore + splittedSpecialistsAndClinics[0] + ", ";
                pSpecialists = pSpecialists + rec + '#';
            }
            pSpecialists = pSpecialists.substring(0, pSpecialists.length() - 1);
            recipientsToStore = recipientsToStore.substring(0, recipientsToStore.length() - 2);
        }
        if (srecipients != null && srecipients.length > 0) {
            for (String rec : srecipients) {
                String[] splittedSpecialistsAndClinics = rec.split("@");
                recipientsToStore = recipientsToStore + ", " + splittedSpecialistsAndClinics[0];
                sSpecialists = sSpecialists + rec + '#';
            }
            sSpecialists = sSpecialists.substring(0, sSpecialists.length() - 1);
        }

        CdxMessengerDao cdxMessengerDao = SpringUtils.getBean(CdxMessengerDao.class);

        if (!StringUtils.isNullOrEmpty(patient)) {
            CdxMessenger cdxMessenger = new CdxMessenger();
            cdxMessenger.setPatient(patient);
            cdxMessenger.setRecipients(recipientsToStore);
            cdxMessenger.setPrimaryRecipient(pSpecialists);
            cdxMessenger.setSecondaryRecipient(sSpecialists);
            cdxMessenger.setCategory(msgType);
            cdxMessenger.setContent(content);
            cdxMessenger.setDocumentType(documentType);
            cdxMessenger.setAuthor(session.getAttribute("userfirstname") + "," + session.getAttribute("userlastname"));
            cdxMessenger.setTimeStamp(new Timestamp(new Date().getTime()));
            cdxMessenger.setDeliveryStatus("Not Sent");
            cdxMessenger.setDraft("Y");
            try {
                cdxMessengerDao.persist(cdxMessenger);
                response.setStatus(HttpServletResponse.SC_OK); // return OK response
            } catch (Exception ex) {
                logger.error("Got exception saving messenger Information " + ex.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, request.getRequestURI());
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST); // invalid request info
        }
        return null;
    }

    public ActionForward searchPatient(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // TODO security check
        HttpSession session = request.getSession();
        if (session.getAttribute("userrole") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        String patient = request.getParameter("patient");
        if (StringUtils.isNullOrEmpty(patient)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST); // invalid request info
            return null;
        }

        JSONArray patientsJSONArray = new JSONArray(); // to return the data

        DemographicDao demographicDao = (DemographicDao) SpringUtils.getBean("demographicDao");
        List<Demographic> demographicList = demographicDao.searchDemographicByFullName(patient);
        for (Demographic demographic : demographicList) {
            JSONObject patientJSONObject = new JSONObject();
            patientJSONObject.put("id", demographic.getId());
            patientJSONObject.put("fullName", demographic.getFullName());
            patientsJSONArray.add(patientJSONObject);
        }

        // Return a json array with the patients info
        response.setContentType("application/json");
        response.getWriter().write(patientsJSONArray.toString());
        response.setStatus(HttpServletResponse.SC_OK);
        return null;
    }

    public ActionForward fetchPatientInfo(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // TODO security check
        HttpSession session = request.getSession();
        if (session.getAttribute("userrole") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        String demographyId = request.getParameter("demographyId");
        String type = request.getParameter("type");
        if (StringUtils.isNullOrEmpty(demographyId) || StringUtils.isNullOrEmpty(type)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST); // invalid request info
            return null;
        }

        String providerNo = (String) session.getAttribute("user");
        ArrayList<String> users = (ArrayList<String>) session.getServletContext().getAttribute("CaseMgmtUsers");
        boolean useNewCmgmt = false;
        WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(session.getServletContext());
        CaseManagementManager cmgmtMgr = null;
        if (users != null && users.size() > 0 && (users.get(0).equalsIgnoreCase("all") || Collections.binarySearch(users, providerNo) >= 0)) {
            useNewCmgmt = true;
            cmgmtMgr = (CaseManagementManager) ctx.getBean("caseManagementManager");
        }

        UserPropertyDAO userPropertyDAO = (UserPropertyDAO) ctx.getBean("UserPropertyDAO");
        UserProperty fmtProperty = userPropertyDAO.getProp(providerNo, UserProperty.CONSULTATION_REQ_PASTE_FMT);
        String pasteFmt = fmtProperty != null ? fmtProperty.getValue() : null;

        // to get allergies, family history at cdxmessenger.
        String value = "";
        String cleanString = "";
        PatientOtherInfo otherInfo = PatientOtherInfo.valueOf(type);
        switch (otherInfo) {
            case FamilyHistory:
                if (OscarProperties.getInstance().getBooleanProperty("caisi", "on")) {
                    EctInformation EctInfo = new EctInformation(LoggedInInfo.getLoggedInInfoFromSession(request), demographyId);
                    value = EctInfo.getFamilyHistory();
                } else {
                    if (useNewCmgmt) {
                        value = listNotes(cmgmtMgr, "FamHistory", providerNo, demographyId);
                    } else {
                        EctInformation EctInfo = new EctInformation(LoggedInInfo.getLoggedInInfoFromSession(request), demographyId);
                        value = EctInfo.getFamilyHistory();
                    }
                }
                if (pasteFmt == null || pasteFmt.equalsIgnoreCase("single")) {
                    value = StringUtils.lineBreaks(value);
                }
                value = org.apache.commons.lang.StringEscapeUtils.escapeJavaScript(value);

                cleanString = StringEscapeUtils.unescapeJava(value);
                break;
            case MedicalHistory:
                if (useNewCmgmt) {
                    value = listNotes(cmgmtMgr, "MedHistory", providerNo, demographyId);
                } else {
                    EctInformation EctInfo = new EctInformation(LoggedInInfo.getLoggedInInfoFromSession(request), demographyId);
                    value = EctInfo.getMedicalHistory();
                }
                if (pasteFmt == null || pasteFmt.equalsIgnoreCase("single")) {
                    value = StringUtils.lineBreaks(value);
                }
                value = org.apache.commons.lang.StringEscapeUtils.escapeJavaScript(value);

                cleanString = StringEscapeUtils.unescapeJava(value);
                break;
            case ongoingConcerns:
                if (useNewCmgmt) {
                    value = listNotes(cmgmtMgr, "Concerns", providerNo, demographyId);
                } else {
                    EctInformation EctInfo = new EctInformation(LoggedInInfo.getLoggedInInfoFromSession(request), demographyId);
                    value = EctInfo.getOngoingConcerns();
                }
                if (pasteFmt == null || pasteFmt.equalsIgnoreCase("single")) {
                    value = StringUtils.lineBreaks(value);
                }
                value = org.apache.commons.lang.StringEscapeUtils.escapeJavaScript(value);

                cleanString = StringEscapeUtils.unescapeJava(value);
                break;
            case SocialHistory:
                if (useNewCmgmt) {
                    value = listNotes(cmgmtMgr, "SocHistory", providerNo, demographyId);
                } else {
                    EctInformation EctInfo = new EctInformation(LoggedInInfo.getLoggedInInfoFromSession(request), demographyId);
                    value = EctInfo.getSocialHistory();
                }
                if (pasteFmt == null || pasteFmt.equalsIgnoreCase("single")) {
                    value = StringUtils.lineBreaks(value);
                }
                value = org.apache.commons.lang.StringEscapeUtils.escapeJavaScript(value);

                cleanString = StringEscapeUtils.unescapeJava(value);
                break;
            case OtherMeds:
                if (OscarProperties.getInstance().getBooleanProperty("caisi", "on")) {
                    value = "";
                } else {
                    if (useNewCmgmt) {
                        value = listNotes(cmgmtMgr, "OMeds", providerNo, demographyId);
                    } else {
                        //family history was used as bucket for Other Meds in old encounter
                        EctInformation EctInfo = new EctInformation(LoggedInInfo.getLoggedInInfoFromSession(request), demographyId);
                        value = EctInfo.getFamilyHistory();
                    }
                }
                if (pasteFmt == null || pasteFmt.equalsIgnoreCase("single")) {
                    value = StringUtils.lineBreaks(value);
                }
                value = org.apache.commons.lang.StringEscapeUtils.escapeJavaScript(value);

                cleanString = StringEscapeUtils.unescapeJava(value);
                break;
            case Reminders:
                if (useNewCmgmt) {
                    value = listNotes(cmgmtMgr, "Reminders", providerNo, demographyId);
                } else {
                    EctInformation EctInfo = new EctInformation(LoggedInInfo.getLoggedInInfoFromSession(request), demographyId);
                    value = EctInfo.getReminders();
                }
                if (pasteFmt == null || pasteFmt.equalsIgnoreCase("single")) {
                    value = StringUtils.lineBreaks(value);
                }
                value = org.apache.commons.lang.StringEscapeUtils.escapeJavaScript(value);

                cleanString = StringEscapeUtils.unescapeJava(value);
                break;
        }

        // return the data as plain text
        response.setContentType("text/plain");
        response.getWriter().write(cleanString);
        response.setStatus(HttpServletResponse.SC_OK);
        return null;
    }

    private String listNotes(CaseManagementManager cmgmtMgr, String code, String providerNo, String demoNo) {
        // filter the notes by the checked issues
        List<Issue> issues = cmgmtMgr.getIssueInfoByCode(providerNo, code);

        String[] issueIds = new String[issues.size()];
        int idx = 0;
        for (Issue issue : issues) {
            issueIds[idx] = String.valueOf(issue.getId());
        }

        // need to apply issue filter
        List<CaseManagementNote> notes = cmgmtMgr.getNotes(demoNo, issueIds);
        StringBuilder noteStr = new StringBuilder();
        for (CaseManagementNote n : notes) {
            if (!n.isLocked() && !n.isArchived()) {
                noteStr.append(n.getNote()).append("\n");
            }
        }
        return noteStr.toString();
    }

    public ActionForward searchRecipient(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // TODO security check
        HttpSession session = request.getSession();
        if (session.getAttribute("userrole") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        OscarProperties props = OscarProperties.getInstance();
        boolean showCdx = "bc".equalsIgnoreCase(props.getProperty("billregion"));
        if (!showCdx) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN); // TODO ?
            return null;
        }

        String searchString = request.getParameter("recipient");
        if (StringUtils.isNullOrEmpty(searchString)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST); // invalid request info
            return null;
        }

        JSONArray providersJSONArray = new JSONArray(); // to return the data

        CDXSpecialist cdxSpecialist = new CDXSpecialist();
        List<IProvider> providers = cdxSpecialist.findCdxSpecialistByName(searchString);
        if (providers != null && !providers.isEmpty()) {
            for (IProvider provider : providers) {
                JSONObject providerJSONObject = new JSONObject();
                providerJSONObject.put("id", provider.getID());
                providerJSONObject.put("fullName", provider.getLastName() + " " + provider.getFirstName());

                JSONArray clinicsJSONArray = new JSONArray();

                List<IClinic> clinics = provider.getClinics();
                if (clinics != null && !clinics.isEmpty()) {
                    for (IClinic clinic : clinics) {
                        JSONObject clinicJSONObject = new JSONObject();
                        clinicJSONObject.put("id", clinic.getID());
                        clinicJSONObject.put("name", clinic.getName());
                        clinicJSONObject.put("address", clinic.getStreetAddress() + " " + clinic.getCity() + " " + clinic.getProvince());
                        clinicsJSONArray.add(clinicJSONObject);
                    }
                    providerJSONObject.put("clinics", clinicsJSONArray);
                }


                providersJSONArray.add(providerJSONObject);
            }
        }

        // Return a json array with the providers info
        response.setContentType("application/json");
        response.getWriter().write(providersJSONArray.toString());
        response.setStatus(HttpServletResponse.SC_OK);
        return null;
    }

    public ActionForward fetchAttachments(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // TODO security check
        HttpSession session = request.getSession();
        if (session.getAttribute("userrole") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String demoNo = request.getParameter("demoNo");
        String requestId = request.getParameter("requestId");

        if (StringUtils.isNullOrEmpty(demoNo)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST); // invalid request info
            return null;
        }

        // Store Docs' descriptions in a JSON array
        JSONArray docs = new JSONArray();
        List<EDoc> attachedDocs = EDocUtil.listDocsForCdxMessenger(loggedInInfo, demoNo, requestId, EDocUtil.ATTACHED);
        for (EDoc eDoc : attachedDocs) {
            docs.add(StringUtils.maxLenString(eDoc.getDescription(), 19, 16, "..."));
        }

        // Store Labs' descriptions in a JSON array
        JSONArray labs = new JSONArray();
        CommonLabResultData resultData = new CommonLabResultData();
        ArrayList<LabResultData> labResults = resultData.populateLabResultsDataCdxMessenger(loggedInInfo, demoNo, requestId, CommonLabResultData.ATTACHED);
        for (LabResultData labResult : labResults) {
            labs.add(labResult.getDiscipline() + " " + labResult.getDateTime());
        }

        JSONObject attachments = new JSONObject();
        attachments.put("docs", docs);
        attachments.put("labs", labs);

        // return a json object with the attachments info
        response.setContentType("application/json");
        response.getWriter().write(attachments.toString());
        response.setStatus(HttpServletResponse.SC_OK);
        return null;
    }

    private CdxMessenger setCdxMessage(HttpServletRequest request, String specialists) {
        CdxMessenger cdxMessenger = new CdxMessenger();
        String patient = request.getParameter("patientsearch");
        String messagetype = request.getParameter("messagetype");
        String documenttype = request.getParameter("documenttype");
        String contentmessage = request.getParameter("contentmessage");
        if (patient != null && patient.length() != 0) {
            cdxMessenger.setPatient(patient);
            cdxMessenger.setRecipients(specialists);
            cdxMessenger.setCategory(messagetype);
            cdxMessenger.setContent(contentmessage);
            cdxMessenger.setDocumentType(documenttype);
            cdxMessenger.setAuthor(request.getSession().getAttribute("userfirstname") + "," + request.getSession().getAttribute("userlastname"));
            cdxMessenger.setTimeStamp(new Timestamp(new Date().getTime()));
            cdxMessenger.setDeliveryStatus("Unknown");
        }
        return cdxMessenger;
    }

    private void doCdxSend(LoggedInInfo loggedInInfo, HttpServletRequest request, CdxMessenger cdxMessenger,
                           HashMap<String, String[]> specialistAndClinicsPrimary,
                           HashMap<String, String[]> specialistAndClinicsSecondary) throws OBIBException {

        DemographicDao demographicDao = (DemographicDao) SpringUtils.getBean("demographicDao");
        CdxProvenanceDao provenanceDao = SpringUtils.getBean(CdxProvenanceDao.class);
        List<Demographic> demoList = null;
        String patient = request.getParameter("patientsearch");
        demoList = demographicDao.searchDemographic(patient);

        Provider sendingProvider = loggedInInfo.getLoggedInProvider();
        DemographicManager demographicManager = SpringUtils.getBean(DemographicManager.class);
        Demographic demographic = demographicManager.getDemographic(loggedInInfo, demoList.get(0).getDemographicNo());
        String patientId = demographic.getHin();
        if (patientId == null || patientId.isEmpty()) {
            patientId = demographic.getDemographicNo().toString();
        }
        String authorId = sendingProvider.getOhipNo();
        if (authorId == null || authorId.isEmpty()) {
            authorId = sendingProvider.getProviderNo();
        }

        ClinicDAO clinicDAO = (ClinicDAO) SpringUtils.getBean("clinicDAO");
        Clinic clinic = clinicDAO.getClinic();

        // Add pdf attachments (scanned images, lab reports and PDF files)
        String filename = null;
        byte[] newBytes = null;
        ByteOutputStream bos = createPdfForAttachments(loggedInInfo, "" + demographic.getDemographicNo(), "" + null);
        if (bos != null) {
            newBytes = bos.toByteArray();
            filename = "ConsultationRequestAttachedPDF-" + demographic.getLastName() + "," + demographic.getFirstName() + "-" + UtilDateUtilities.getToday("yyyy-MM-dd_HHmmss") + ".pdf";
            MiscUtils.getLogger().debug("File: " + filename + ", Size: " + newBytes.length);
        }

        IDocument response;
        CDXConfiguration cdxConfig = new CDXConfiguration();
        SubmitDoc submitDoc = new SubmitDoc(cdxConfig);

        ISubmitDoc doc;

        doc = submitDoc.newDoc();
        if (cdxMessenger.getDocumentType().equalsIgnoreCase("Information Request")) {
            doc.documentType(DocumentType.INFO_REQUEST);
        } else if (cdxMessenger.getDocumentType().equalsIgnoreCase("Progress Note")) {
            doc.documentType(DocumentType.PROGRESS_NOTE);
        } else if (cdxMessenger.getDocumentType().equalsIgnoreCase("Consult Note")) {
            doc.documentType(DocumentType.CONSULT_NOTE);
        } else if (cdxMessenger.getDocumentType().equalsIgnoreCase("Referral Note")) {
            doc.documentType(DocumentType.REFERRAL_NOTE);
        } else if (cdxMessenger.getDocumentType().equalsIgnoreCase("Patient Summary")) {
            doc.documentType(DocumentType.PATIENT_SUMMARY);
        } else if (cdxMessenger.getDocumentType().equalsIgnoreCase("Discharge Summary")) {
            doc.documentType(DocumentType.DISCHARGE_SUMMARY);
        } else if (cdxMessenger.getDocumentType().equalsIgnoreCase("Care Plan")) {
            doc.documentType(DocumentType.CARE_PLAN);
        } else if (cdxMessenger.getDocumentType().equalsIgnoreCase("General Purpose Note")) {
            doc.documentType(DocumentType.NOTE);
        }
        // doc.documentType(DocumentType.ADVICE_REQUEST);

        String otherInfo = request.getParameter("otherinfo");
        String content = cdxMessenger.getContent();

        if (otherInfo != null && !otherInfo.isEmpty()) {
            content = content + System.lineSeparator() + otherInfo;
        }

        ISubmitDoc iDoc = doc.content(content)
                .patient()
                .id(patientId)
                .name(NameType.LEGAL, demographic.getFirstName(), demographic.getLastName())
                .address(AddressType.HOME, demographic.getAddress(), demographic.getCity(), demographic.getProvince(), demographic.getPostal(), "CA")
                .phone(TelcoType.HOME, demographic.getPhone())
                .birthday(demographic.getYearOfBirth(), demographic.getMonthOfBirth(), demographic.getDateOfBirth())
                .gender("M".equalsIgnoreCase(demographic.getSex()) ? Gender.MALE : Gender.FEMALE)
                .and().author()
                .id(authorId)
                .time(new Date())
                .name(NameType.LEGAL, sendingProvider.getFirstName(), sendingProvider.getLastName())
                .address(AddressType.WORK, clinic.getAddress(), clinic.getCity(), clinic.getProvince(), clinic.getPostal(), "CA")
                .phone(TelcoType.WORK, clinic.getPhone())
                .and();

        HashSet<String> allUniqueClinics = new HashSet<String>();
        ISubmitDoc iDocReturnedPrimary = addRecipient(iDoc, specialistAndClinicsPrimary, allUniqueClinics, "primary");

        if (specialistAndClinicsSecondary != null) {
            addRecipient(iDocReturnedPrimary, specialistAndClinicsSecondary, allUniqueClinics, "secondary");
        }

        for (String cl : allUniqueClinics) {
            //adding all clinics to the document as a receiver.
            iDoc.receiverId(cl);
        }
        if (newBytes != null) {
            doc = doc.attach(AttachmentType.PDF, filename, newBytes);
        }

        if (cdxMessenger.getCategory() != null && !cdxMessenger.getCategory().equalsIgnoreCase("New")) {
            CdxProvenance infulfilmentOfDoc = provenanceDao.getCdxProvenance(Integer.parseInt(cdxMessenger.getCategory().split(":")[1]));
            doc.inFulfillmentOf()
                    .id(infulfilmentOfDoc.getDocumentId())
                    .statusCode(OrderStatus.ACTIVE).and();
        }

        response = doc.submit();

        boolean debug = false;
        if (debug) {
            logResponse(response);
        }
        MiscUtils.getLogger().debug("Attempting to save document using logSentAction");
        CdxProvenanceDao cdxProvenanceDao = SpringUtils.getBean(CdxProvenanceDao.class);
        cdxProvenanceDao.logSentAction(response);
        CdxMessengerDao cdxMessengerDao = SpringUtils.getBean(CdxMessengerDao.class);

        //remove draft

        String draftId = request.getParameter("draftId");

        if (!draftId.equalsIgnoreCase("null") && !draftId.isEmpty()) {
            CdxMessenger draft = cdxMessengerDao.getCdxMessenger(Integer.parseInt(draftId));
            try {
                if (draft != null) {
                    cdxMessengerDao.deleteDraftById(draft.getId());
                }
            } catch (Exception ex) {
                MiscUtils.getLogger().error("Got exception while deleting draft " + ex.getMessage());
            }
        }

        try {
            cdxMessenger.setDocumentId(response.getDocumentID());
            cdxMessenger.setDraft("N");
            cdxMessengerDao.persist(cdxMessenger);
        } catch (Exception ex) {
            MiscUtils.getLogger().error("Got exception saving messenger Information " + ex.getMessage());
        }

        //Now we have request id for the cdx messenger, we update the request id for the attachments.
        CommonLabResultData consultLabs = new CommonLabResultData();
        ArrayList<EDoc> attachmentList = EDocUtil.listDocsForCdxMessenger(loggedInInfo, "" + demographic.getDemographicNo(), "" + null, EDocUtil.ATTACHED);
        ArrayList<LabResultData> labs = consultLabs.populateLabResultsDataCdxMessenger(loggedInInfo, "" + demographic.getDemographicNo(), null, CommonLabResultData.ATTACHED);
        for (EDoc attachment : attachmentList) {
            EDocUtil.updateAttachCdxDoc(attachment.getDocId(), "" + cdxMessenger.getId(), "" + demographic.getDemographicNo());
            EDocUtil.detachCdxDoc(attachment.getDocId(), "" + cdxMessenger.getId(), "" + demographic.getDemographicNo());
        }

        for (LabResultData lab : labs) {
            EDocUtil.updateAttachCdxDoc(lab.labPatientId, "" + cdxMessenger.getId(), "" + demographic.getDemographicNo());
            EDocUtil.detachCdxDoc(lab.labPatientId, "" + cdxMessenger.getId(), "" + demographic.getDemographicNo());
        }

        // Try to update the document distribution status
        CDXDistribution cdxDistribution = new CDXDistribution();
        cdxDistribution.updateDistributionStatus(response.getDocumentID());

        // Code to add sent "Information request" and "Progress note" in toilet roll(notes) patient e-chart.
        if (cdxMessenger.getDocumentType().equalsIgnoreCase("Information Request")
                || cdxMessenger.getDocumentType().equalsIgnoreCase("Progress Note")) {
            IProvider auth = response.getAuthor();
            try {
                // add to document table
                DocumentDao docDao = getBean(DocumentDao.class);

                org.oscarehr.common.model.Document docEntity = new org.oscarehr.common.model.Document();
                IProvider p;
                // CDXImport cdximport=new CDXImport();
                docEntity.setDoctype(response.getLoincCodeDisplayName());
                docEntity.setDocdesc(response.getLoincCodeDisplayName());
                docEntity.setDocfilename("N/A");
                docEntity.setDoccreator(response.getCustodianName());
                docEntity.setNumberofpages(0);

                p = response.getOrderingProvider();

                if (p != null) {
                    docEntity.setResponsible(p.getFirstName() + " " + p.getLastName());
                } else {
                    docEntity.setResponsible("");
                }

                docEntity.setAbnormal(0);

                docEntity.setDocClass(response.getLoincCodeDisplayName());
                docEntity.setDocxml("stored in CDX provenance table");
                docEntity.setDocfilename("CDX");
                docEntity.setContenttype("CDX");
                docEntity.setRestrictToProgram(false); // need to confirm semantics

                docEntity.setSource(auth != null ? auth.getLastName() : "");
                docEntity.setUpdatedatetime(response.getAuthoringTime());
                docEntity.setStatus(org.oscarehr.common.model.Document.STATUS_ACTIVE);
                docEntity.setReportStatus(response.getStatusCode().code);

                if (response.getObservationDate() != null) {
                    docEntity.setObservationdate(response.getObservationDate());
                } else if (response.getAuthoringTime() != null) {
                    docEntity.setObservationdate(response.getAuthoringTime());
                } else if (response.getEffectiveTime() != null) {
                    docEntity.setObservationdate(response.getEffectiveTime());
                } else {
                    docEntity.setObservationdate(new Date());
                }

                if (response.getCustodianName() != null) {
                    docEntity.setSourceFacility(response.getCustodianName());
                }

                docEntity.setContentdatetime(response.getAuthoringTime());
                docDao.persist(docEntity);

                //Add document number in the CDX provence table for Sent documents.
                CdxProvenance cdxProvenance = cdxProvenanceDao.findByDocumentIdAndAction(response.getDocumentID(), "SEND");
                if (cdxProvenance != null) {
                    cdxProvenance.setDocumentNo(docEntity.getDocumentNo());
                    cdxProvenanceDao.merge(cdxProvenance);
                }
            } catch (Exception ex) {
                MiscUtils.getLogger().error("Got exception while Saving document " + ex.getMessage());
            }

            try {
                //Code to add Information in NOTE Section in patient chart.
                Date now = EDocUtil.getDmsDateTimeAsDate();

                String docDesc = EDocUtil.getLastDocumentDesc();

                CaseManagementNote cmn = new CaseManagementNote();
                cmn.setUpdate_date(now);

                //java.sql.Date od1 = MyDateFormat.getSysDate(newDoc.getObservationDate());

                cmn.setObservation_date(now);
                cmn.setDemographic_no(demographic.getDemographicNo().toString());
                HttpSession se = request.getSession();
                String user_no = (String) se.getAttribute("user");
                String prog_no = new EctProgram(se).getProgram(user_no);
                WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(se.getServletContext());
                CaseManagementManager cmm = (CaseManagementManager) ctx.getBean("caseManagementManager");
                cmn.setProviderNo("-1");// set the provider no to be -1 so the editor appear as 'System'.

                String strNote = "Document" + " " + docDesc + " " + "created at " + now + " by " + auth.getFirstName() + " " + auth.getLastName() + ".";

                cmn.setNote(strNote);
                cmn.setSigned(true);
                cmn.setSigning_provider_no("-1");
                cmn.setProgram_no(prog_no);

                SecRoleDao secRoleDao = (SecRoleDao) SpringUtils.getBean("secRoleDao");
                SecRole doctorRole = secRoleDao.findByName("doctor");
                cmn.setReporter_caisi_role(doctorRole.getId().toString());

                cmn.setReporter_program_team("0");
                cmn.setPassword("NULL");
                cmn.setLocked(false);
                cmn.setHistory(strNote);
                cmn.setPosition(0);

                Long note_id = cmm.saveNoteSimpleReturnID(cmn);

                // Add a noteLink to casemgmt_note_link
                CaseManagementNoteLink cmnl = new CaseManagementNoteLink();
                cmnl.setTableName(CaseManagementNoteLink.DOCUMENT);
                cmnl.setTableId(Long.parseLong(EDocUtil.getLastDocumentNo()));
                cmnl.setNoteId(note_id);
                EDocUtil.addCaseMgmtNoteLink(cmnl);
            } catch (Exception ex) {
                MiscUtils.getLogger().error("Got exception  while Saving Note in casemgmt_note" + ex.getMessage());
            }
        }
    }

    private boolean logResponse(IDocument doc) {
        boolean result = false;
        ObjectMapper mapper = new ObjectMapper();
        try {
            String docStr = mapper.writeValueAsString(doc);
            MiscUtils.getLogger().info(docStr);
            result = true;
        } catch (IOException e) {
            MiscUtils.getLogger().error(e.getMessage());
        }
        return result;
    }

    public ISubmitDoc addRecipient(ISubmitDoc iDoc, HashMap<String, String[]> specialistAndClinics,
                                   HashSet<String> allUniqueClinics, String typeofRecipient) {
        List<IProvider> providers = null;
        CDXSpecialist cdxSpecialist = new CDXSpecialist();
        //HashMap<String,String> clinicInfo=(HashMap<String,String>)request.getSession().getAttribute("clinicInfo");

        //Getting all the selected primary recipients.
        for (String s : specialistAndClinics.keySet()) {
            String[] lastAndFirstName = s.split(" ", 2);
            providers = cdxSpecialist.findCdxSpecialistByName(lastAndFirstName[0].trim());

            for (IProvider provider : providers) {
                if (provider.getID() != null && !provider.getID().isEmpty()
                        && provider.getFirstName() != null && !provider.getFirstName().isEmpty()) {
                    if (provider.getLastName().equalsIgnoreCase(lastAndFirstName[0].trim())
                            && provider.getFirstName().equalsIgnoreCase(lastAndFirstName[1].trim())) {
                        //Adding Multiple Recipients to the document.
                        if (typeofRecipient.equalsIgnoreCase("primary")) {
                            iDoc = iDoc.recipient().primary()
                                    .id(provider.getID())
                                    .name(NameType.LEGAL, provider.getFirstName(), provider.getLastName())
                                    .address(AddressType.WORK, provider.getStreetAddress(), provider.getCity(), provider.getProvince(), provider.getPostalCode(), "CA")
                                    .phone(TelcoType.WORK, String.valueOf(provider.getPhones()))
                                    .and();
                        } else if (typeofRecipient.equalsIgnoreCase("secondary")) {
                            iDoc = iDoc.recipient()
                                    .id(provider.getID())
                                    .name(NameType.LEGAL, provider.getFirstName(), provider.getLastName())
                                    .address(AddressType.WORK, provider.getStreetAddress(), provider.getCity(), provider.getProvince(), provider.getPostalCode(), "CA")
                                    .phone(TelcoType.WORK, String.valueOf(provider.getPhones()))
                                    .and();
                        }

                        String[] clinics = specialistAndClinics.get(s);
                        for (String c : clinics) {
                            if (c != null && !c.isEmpty()) {
                                //Adding all the clinics associated with all the primary recipients.
                                allUniqueClinics.add(c.trim());
                            }
                        }
                    }
                }
            }
        }
        return iDoc;
    }

    private ByteOutputStream createPdfForAttachments(LoggedInInfo loggedInInfo, String demoNo, String reqId) throws OBIBException {
        ArrayList<EDoc> docs = EDocUtil.listDocsForCdxMessenger(loggedInInfo, demoNo, reqId, EDocUtil.ATTACHED);
        String path = OscarProperties.getInstance().getProperty("DOCUMENT_DIR");
        ArrayList<Object> alist = new ArrayList<Object>();
        byte[] buffer;
        ByteInputStream bis;
        ByteOutputStream bos = null;
        CommonLabResultData consultLabs = new CommonLabResultData();
        ArrayList<InputStream> streams = new ArrayList<InputStream>();

        ArrayList<LabResultData> labs = consultLabs.populateLabResultsDataCdxMessenger(loggedInInfo, demoNo, reqId, CommonLabResultData.ATTACHED);
        String error = "";
        Exception exception = null;
        try {
            boolean success = false;
            for (EDoc doc : docs) {
                if (doc.isPrintable()) {
                    if (doc.isImage()) {
                        success = false;
                        bos = new ByteOutputStream();
                        String imagePath = path + doc.getFileName();
                        String imageTitle = doc.getDescription();
                        try {
                            imageToPdf(imagePath, imageTitle, bos);
                            success = true;
                        } catch (DocumentException e) {
                            MiscUtils.getLogger().error(e.getMessage());
                        }
                        if (success) {
                            buffer = bos.getBytes();
                            bis = new ByteInputStream(buffer, bos.getCount());
                            bos.close();
                            streams.add(bis);
                            alist.add(bis);
                        }
                    } else if (doc.isPDF()) {
                        alist.add(path + doc.getFileName());
                    } else if (doc.isCDX()) {
                        success = false;
                        bos = new ByteOutputStream();
                        try {
                            cdxToPdf(doc, bos);
                            success = true;
                        } catch (OBIBException e) {
                            MiscUtils.getLogger().error(e.getMessage());
                            throw e;
                        }
                        if (success) {
                            buffer = bos.getBytes();
                            bis = new ByteInputStream(buffer, bos.getCount());
                            bos.close();
                            streams.add(bis);
                            alist.add(bis);
                        }
                    } else {
                        logger.error("CDXMessengerAction " + doc.getType() + " is marked as printable but no means have been established to print it.");
                    }
                }
            }

            // Iterating over requested labs.
            for (int i = 0; labs != null && i < labs.size(); i++) {
                // Storing the lab in PDF format inside a byte stream.
                bos = new ByteOutputStream();
                LabPDFCreator lpdfc = new LabPDFCreator(bos, labs.get(i).segmentID, loggedInInfo.getLoggedInProviderNo());
                lpdfc.printPdf();

                // Transferring PDF to an input stream to be concatenated with
                // the rest of the documents.
                buffer = bos.getBytes();
                bis = new ByteInputStream(buffer, bos.getCount());
                bos.close();
                streams.add(bis);
                alist.add(bis);
            }
            if (alist.size() > 0) {
                bos = new ByteOutputStream();
                ConcatPDF.concat(alist, bos);
            }
        } catch (DocumentException de) {
            error = "DocumentException";
            exception = de;
        } catch (IOException ioe) {
            error = "IOException";
            exception = ioe;
        } finally {
            // Cleaning up InputStreams created for concatenation.
            for (InputStream is : streams) {
                try {
                    is.close();
                } catch (IOException e) {
                    error = "IOException";
                }
            }
        }
        if (!error.equals("")) {
            logger.error(error + " occured insided createPDF", exception);
        }
        return bos;
    }

    /**
     * Converts attached CDX document in the consultation request to PDF.
     *
     * @param os the output stream where the PDF will be written
     * @throws IOException       when an error with the output stream occurs
     * @throws DocumentException when an error in document construction occurs
     */
    private void cdxToPdf(EDoc doc, ByteOutputStream os) throws OBIBException {
        CdxProvenanceDao provenanceDao = SpringUtils.getBean(CdxProvenanceDao.class);
        CdxProvenance provDoc = provenanceDao.findByDocumentNo(Integer.parseInt(doc.getDocId()));
        CdxAttachmentDao attachmentDao = SpringUtils.getBean(CdxAttachmentDao.class);

        ArrayList<Object> streamList = new ArrayList<>();

        // transform main document from XML to HTML
        try {
            StringReader cdaReader = new StringReader(provDoc.getPayload());
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(new StreamSource(contentRoute + "/share/xslt/CDA_to_HTML.xsl"));

            ByteOutputStream bos_html = new ByteOutputStream();
            transformer.transform(new StreamSource(cdaReader), new StreamResult(bos_html));

            String html = new String(bos_html.getBytes(), UTF_8);

            // transform main document from HTML to PDF

            HtmlToPdf htmlToPdf = HtmlToPdf.create().object(HtmlToPdfObject.forHtml(html));

            InputStream mainDoc = htmlToPdf.convert();
            streamList.add(mainDoc);
            mainDoc.close();

            // transform attachments to CDX document

            for (CdxAttachment att : attachmentDao.findByDocNo(provDoc.getId())) {
                InputStream attDoc = null;
                if (att.getAttachmentType().equals(AttachmentType.PDF.mediaType)) {
                    attDoc = new ByteArrayInputStream(att.getContent());
                } else if (att.getAttachmentType().equals(AttachmentType.JPEG.mediaType)
                        || att.getAttachmentType().equals(AttachmentType.PNG.mediaType)
                        || att.getAttachmentType().equals(AttachmentType.TIFF.mediaType)) {
                    ByteOutputStream aos = new ByteOutputStream();
                    imageToPdf(att.getContent(), aos);
                    aos.close();
                    byte[] buffer = aos.getBytes();
                    attDoc = new ByteArrayInputStream(buffer);
                } else if (att.getAttachmentType().equals(AttachmentType.RTF.mediaType)) {
                    Document document = new Document();
                    ByteOutputStream aos = new ByteOutputStream();
                    PdfWriter writer = PdfWriter.getInstance(document, aos);
                    document.open();
                    RtfParser parser = new RtfParser(null);
                    parser.convertRtfDocument(new ByteArrayInputStream(att.getContent()), document);
                    document.close();
                    aos.close();
                    byte[] buffer = aos.getBytes();
                    attDoc = new ByteArrayInputStream(buffer);
                } else
                    throw new OBIBException("Unknown attachment type of CDX document (" + att.getAttachmentType() + ")");
                streamList.add(attDoc);
            }
            ConcatPDF.concat(streamList, os);
        } catch (Exception e) {
            throw new OBIBException("Attachment document to PDF automatically. The document has *not* been sent.");
        }
    }

    /**
     * Converts attached image in the consultation request to PDF.
     *
     * @param os the output stream where the PDF will be written
     * @throws IOException       when an error with the output stream occurs
     * @throws DocumentException when an error in document construction occurs
     */
    private void imageToPdf(String imagePath, String imageTitle, OutputStream os) throws IOException, DocumentException {
        Image image;
        try {
            image = Image.getInstance(imagePath);
        } catch (Exception e) {
            logger.error("Unexpected error:", e);
            throw new DocumentException(e);
        }

        // Create the document we are going to write to
        Document document = new Document();
        // PdfWriter writer = PdfWriter.getInstance(document, os);
        PdfWriter writer = PdfWriterFactory.newInstance(document, os, FontSettings.HELVETICA_6PT);

        document.setPageSize(PageSize.LETTER);
        document.addCreator("OSCAR");
        document.open();

        int type = image.getOriginalType();
        if (type == Image.ORIGINAL_TIFF) {
            // The following is composed of code from com.lowagie.tools.plugins.Tiff2Pdf modified to create the
            // PDF in memory instead of on disk
            RandomAccessFileOrArray ra = new RandomAccessFileOrArray(imagePath);
            int comps = TiffImage.getNumberOfPages(ra);
            boolean adjustSize = false;
            PdfContentByte cb = writer.getDirectContent();
            for (int c = 0; c < comps; ++c) {
                Image img = TiffImage.getTiffImage(ra, c + 1);
                if (img != null) {
                    if (adjustSize) {
                        document.setPageSize(new Rectangle(img.getScaledWidth(), img.getScaledHeight()));
                        document.newPage();
                        img.setAbsolutePosition(0, 0);
                    } else {
                        if (img.getScaledWidth() > 500 || img.getScaledHeight() > 700) {
                            img.scaleToFit(500, 700);
                        }
                        img.setAbsolutePosition(20, 20);
                        document.newPage();
                        document.add(new Paragraph(imageTitle + " - page " + (c + 1)));
                    }
                    cb.addImage(img);
                }
            }
            ra.close();
        } else {
            PdfContentByte cb = writer.getDirectContent();
            if (image.getScaledWidth() > 500 || image.getScaledHeight() > 700) {
                image.scaleToFit(500, 700);
            }
            image.setAbsolutePosition(20, 20);
            cb.addImage(image);
        }
        document.close();
    }

    /**
     * Converts attached image in the consultation request to PDF.
     *
     * @param os the output stream where the PDF will be written
     * @throws IOException       when an error with the output stream occurs
     * @throws DocumentException when an error in document construction occurs
     */
    private void imageToPdf(byte[] content, OutputStream os) throws IOException, DocumentException {
        Image image = Image.getInstance(content);

        // Create the document we are going to write to
        Document document = new Document();
        // PdfWriter writer = PdfWriter.getInstance(document, os);
        PdfWriter writer = PdfWriterFactory.newInstance(document, os, FontSettings.HELVETICA_6PT);

        document.setPageSize(PageSize.LETTER);
        document.addCreator("OSCAR");
        document.open();

        PdfContentByte cb = writer.getDirectContent();
        if (image.getScaledWidth() > 500 || image.getScaledHeight() > 700) {
            image.scaleToFit(500, 700);
        }
        image.setAbsolutePosition(20, 20);
        cb.addImage(image);
        document.close();
    }
}
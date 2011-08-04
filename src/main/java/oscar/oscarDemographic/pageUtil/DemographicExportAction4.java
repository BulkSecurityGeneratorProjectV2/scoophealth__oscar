/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved. *
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA. *
 *
 * Jay Gallagher
 *
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada   Creates a new instance of DemographicExportAction
 *
 *
 * DemographicExportAction3.java
 *
 * Created on Nov 4, 2008
 */

package oscar.oscarDemographic.pageUtil;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.xmlbeans.XmlOptions;
import org.oscarehr.casemgmt.model.CaseManagementIssue;
import org.oscarehr.casemgmt.model.CaseManagementNote;
import org.oscarehr.casemgmt.model.CaseManagementNoteExt;
import org.oscarehr.casemgmt.model.CaseManagementNoteLink;
import org.oscarehr.casemgmt.service.CaseManagementManager;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import org.oscarehr.util.WebUtils;

import oscar.OscarProperties;
import oscar.appt.ApptData;
import oscar.appt.ApptStatusData;
import oscar.dms.EDoc;
import oscar.dms.EDocUtil;
import oscar.oscarClinic.ClinicData;
import oscar.oscarDemographic.data.DemographicData;
import oscar.oscarDemographic.data.DemographicExt;
import oscar.oscarEncounter.oscarMeasurements.data.ImportExportMeasurements;
import oscar.oscarEncounter.oscarMeasurements.data.LabMeasurements;
import oscar.oscarEncounter.oscarMeasurements.data.Measurements;
import oscar.oscarLab.LabRequestReportLink;
import oscar.oscarLab.ca.all.upload.ProviderLabRouting;
import oscar.oscarPrevention.PreventionData;
import oscar.oscarPrevention.PreventionDisplayConfig;
import oscar.oscarProvider.data.ProviderData;
import oscar.oscarReport.data.DemographicSets;
import oscar.oscarReport.data.RptDemographicQueryBuilder;
import oscar.oscarReport.data.RptDemographicQueryLoader;
import oscar.oscarReport.pageUtil.RptDemographicReportForm;
import oscar.oscarRx.data.RxPatientData;
import oscar.oscarRx.data.RxPrescriptionData;
import oscar.service.OscarSuperManager;
import oscar.util.StringUtils;
import oscar.util.UtilDateUtilities;
import cds.OmdCdsDocument;
import cds.AlertsAndSpecialNeedsDocument.AlertsAndSpecialNeeds;
import cds.AllergiesAndAdverseReactionsDocument.AllergiesAndAdverseReactions;
import cds.AppointmentsDocument.Appointments;
import cds.CareElementsDocument.CareElements;
import cds.ClinicalNotesDocument.ClinicalNotes;
import cds.DemographicsDocument.Demographics;
import cds.FamilyHistoryDocument.FamilyHistory;
import cds.ImmunizationsDocument.Immunizations;
import cds.LaboratoryResultsDocument.LaboratoryResults;
import cds.MedicationsAndTreatmentsDocument.MedicationsAndTreatments;
import cds.PastHealthDocument.PastHealth;
import cds.PatientRecordDocument.PatientRecord;
import cds.ProblemListDocument.ProblemList;
import cds.ReportsReceivedDocument.ReportsReceived;
import cds.RiskFactorsDocument.RiskFactors;
import java.util.Calendar;
import java.util.HashMap;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.oscarehr.common.dao.DemographicArchiveDao;
import org.oscarehr.common.dao.DemographicContactDao;
import org.oscarehr.common.model.DemographicArchive;
import org.oscarehr.common.model.DemographicContact;
import org.oscarehr.hospitalReportManager.dao.HRMDocumentCommentDao;
import org.oscarehr.hospitalReportManager.dao.HRMDocumentDao;
import org.oscarehr.hospitalReportManager.dao.HRMDocumentToDemographicDao;
import org.oscarehr.hospitalReportManager.model.HRMDocument;
import org.oscarehr.hospitalReportManager.model.HRMDocumentComment;
import org.oscarehr.hospitalReportManager.model.HRMDocumentToDemographic;
import oscar.oscarRx.data.RxAllergyData.Allergy;

/**
 *
 * @author Ronnie Cheng
 */
public class DemographicExportAction4 extends Action {

        private static final Logger logger = MiscUtils.getLogger();
        private static final String PATIENTID = "Patient";
        private static final String ALERT = "Alert";
        private static final String ALLERGY = "Allergy";
        private static final String APPOINTMENT = "Appointment";
        private static final String CAREELEMENTS = "Care";
        private static final String CLINICALNOTE = "Clinical";
        private static final String PERSONALHISTORY = "Personal";
        private static final String FAMILYHISTORY = "Family";
        private static final String IMMUNIZATION = "Immunization";
        private static final String LABS = "Labs";
        private static final String MEDICATION = "Medication";
        private static final String PASTHEALTH = "Past";
        private static final String PROBLEMLIST = "Problem";
        private static final String REPORTBINARY = "Binary";
        private static final String REPORTTEXT = "Text";
        private static final String RISKFACTOR = "Risk";

        HashMap<String, Integer> entries = new HashMap<String, Integer>();
        Integer exportNo = 0;

@Override
    @SuppressWarnings("static-access")
public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    OscarProperties oscarp = OscarProperties.getInstance();
    String strEditable = oscarp.getProperty("ENABLE_EDIT_APPT_STATUS");
    
    DemographicExportForm defrm = (DemographicExportForm)form;
    String demographicNo = defrm.getDemographicNo();
    String setName = defrm.getPatientSet();
    String pgpReady = defrm.getPgpReady();
    boolean exFamilyHistory = WebUtils.isChecked(request, "exFamilyHistory");
    boolean exPastHealth = WebUtils.isChecked(request, "exPastHealth");
    boolean exProblemList = WebUtils.isChecked(request, "exProblemList");
    boolean exRiskFactors = WebUtils.isChecked(request, "exRiskFactors");
    boolean exAllergiesAndAdverseReactions = WebUtils.isChecked(request, "exAllergiesAndAdverseReactions");
    boolean exMedicationsAndTreatments = WebUtils.isChecked(request, "exMedicationsAndTreatments");
    boolean exImmunizations = WebUtils.isChecked(request, "exImmunizations");
    boolean exLaboratoryResults = WebUtils.isChecked(request, "exLaboratoryResults");
    boolean exAppointments = WebUtils.isChecked(request, "exAppointments");
    boolean exClinicalNotes = WebUtils.isChecked(request, "exClinicalNotes");
    boolean exReportsReceived = WebUtils.isChecked(request, "exReportsReceived");
    boolean exAlertsAndSpecialNeeds = WebUtils.isChecked(request, "exAlertsAndSpecialNeeds");
    boolean exCareElements = WebUtils.isChecked(request, "exCareElements");

    ArrayList<String> list = new ArrayList<String>();
    if (demographicNo==null) {
	list = new DemographicSets().getDemographicSet(setName);
	if (list.isEmpty()) {
	    Date asofDate = UtilDateUtilities.Today();
	    RptDemographicReportForm frm = new RptDemographicReportForm ();
	    frm.setSavedQuery(setName);
	    RptDemographicQueryLoader demoL = new RptDemographicQueryLoader();
	    frm = demoL.queryLoader(frm);
	    frm.addDemoIfNotPresent();
	    frm.setAsofDate(UtilDateUtilities.DateToString(asofDate));
	    RptDemographicQueryBuilder demoQ = new RptDemographicQueryBuilder();
	    ArrayList<ArrayList> list2 = demoQ.buildQuery(frm,UtilDateUtilities.DateToString(asofDate));
            for (ArrayList<String> listDemo : list2) {
                list.add(listDemo.get(0));
            }
	}
    } else {
	list.add(demographicNo);
    }    
    
    String ffwd = "fail";
    String tmpDir = oscarp.getProperty("TMP_DIR");
    if (!Util.checkDir(tmpDir)) {
        logger.debug("Error! Cannot write to TMP_DIR - Check oscar.properties or dir permissions.");
    } else {
	XmlOptions options = new XmlOptions();
	options.put( XmlOptions.SAVE_PRETTY_PRINT );
	options.put( XmlOptions.SAVE_PRETTY_PRINT_INDENT, 3 );
	options.put( XmlOptions.SAVE_AGGRESSIVE_NAMESPACES );

        HashMap<String,String> suggestedPrefix = new HashMap<String,String>();
        suggestedPrefix.put("cds_dt","cdsd");
        options.setSaveSuggestedPrefixes(suggestedPrefix);

	options.setSaveOuter();
        
        ArrayList<String> err = new ArrayList<String>();
        ArrayList<File> files = new ArrayList<File>();
	String data="";
        for (String demoNo : list) {
	    if (StringUtils.empty(demoNo)) {
		err.add("Error! No Demographic Number");
                continue;
            }

            // DEMOGRAPHICS
            DemographicArchiveDao demoArchiveDao = (DemographicArchiveDao)SpringUtils.getBean("demographicArchiveDao");
            DemographicData d = new DemographicData();
            DemographicExt ext = new DemographicExt();

            DemographicData.Demographic demographic = d.getDemographic(demoNo);

            if (demographic.getPatientStatus()!=null && demographic.getPatientStatus().equals("Contact-only")) continue;

            HashMap<String,String> demoExt = new HashMap<String,String>();
            demoExt.putAll(ext.getAllValuesForDemo(demoNo));

            OmdCdsDocument omdCdsDoc = OmdCdsDocument.Factory.newInstance();
            OmdCdsDocument.OmdCds omdCds = omdCdsDoc.addNewOmdCds();
            PatientRecord patientRec = omdCds.addNewPatientRecord();
            Demographics demo = patientRec.addNewDemographics();

            demo.setUniqueVendorIdSequence(demoNo);
            entries.put(PATIENTID+exportNo, Integer.valueOf(demoNo));

            cdsDt.PersonNameStandard personName = demo.addNewNames();
            cdsDt.PersonNameStandard.LegalName legalName = personName.addNewLegalName();
            cdsDt.PersonNameStandard.LegalName.FirstName firstName = legalName.addNewFirstName();
            cdsDt.PersonNameStandard.LegalName.LastName  lastName  = legalName.addNewLastName();
            legalName.setNamePurpose(cdsDt.PersonNamePurposeCode.L);

            data = StringUtils.noNull(demographic.getFirstName());
            if (StringUtils.filled(data)) {
                firstName.setPart(data);
                firstName.setPartType(cdsDt.PersonNamePartTypeCode.GIV);
                firstName.setPartQualifier(cdsDt.PersonNamePartQualifierCode.BR);
            } else {
                err.add("Error! No First Name for Patient "+demoNo);
            }
            data = StringUtils.noNull(demographic.getLastName());
            if (StringUtils.filled(data)) {
                lastName.setPart(data);
                lastName.setPartType(cdsDt.PersonNamePartTypeCode.FAMC);
                lastName.setPartQualifier(cdsDt.PersonNamePartQualifierCode.BR);
            } else {
                err.add("Error! No Last Name for Patient "+demoNo);
            }

            data = demographic.getTitle();
            if (StringUtils.filled(data)) {
                if (data.equalsIgnoreCase("MISS")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.MISS);
                if (data.equalsIgnoreCase("MR")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.MR);
                if (data.equalsIgnoreCase("MRS")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.MRS);
                if (data.equalsIgnoreCase("MS")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.MS);
                if (data.equalsIgnoreCase("MSSR")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.MSSR);
                if (data.equalsIgnoreCase("PROF")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.PROF);
                if (data.equalsIgnoreCase("REEVE")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.REEVE);
                if (data.equalsIgnoreCase("REV")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.REV);
                if (data.equalsIgnoreCase("RT_HON")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.RT_HON);
                if (data.equalsIgnoreCase("SEN")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.SEN);
                if (data.equalsIgnoreCase("SGT")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.SGT);
                if (data.equalsIgnoreCase("SR")) personName.setNamePrefix(cdsDt.PersonNamePrefixCode.SR);
            } else {
                err.add("Error! No Name Prefix for Patient "+demoNo);
            }

            data = demographic.getOfficialLang();
            if (StringUtils.filled(data)) {
                if (data.equalsIgnoreCase("English"))     demo.setPreferredOfficialLanguage(cdsDt.OfficialSpokenLanguageCode.ENG);
                else if (data.equalsIgnoreCase("French")) demo.setPreferredOfficialLanguage(cdsDt.OfficialSpokenLanguageCode.FRE);
            } else {
                err.add("Error! No Preferred Official Language for Patient "+demoNo);
            }

            data = demographic.getSpokenLang();
            if (StringUtils.filled(data) && Util.convertLanguageToCode(data) != null) {
                demo.setPreferredSpokenLanguage(Util.convertLanguageToCode(data));
            }

            data = demographic.getSex();
            if (StringUtils.filled(data)) {
                demo.setGender(cdsDt.Gender.Enum.forString(data));
            } else {
                err.add("Error! No Gender for Patient "+demoNo);
            }

            data = demographic.getSin();
            if (StringUtils.filled(data) && data.length()==9) {
                demo.setSIN(data);
            }

            //Enrolment Status (Roster Status)
            Demographics.Enrolment enrolment;
            String rosterStatus = demographic.getRosterStatus();
            if (StringUtils.filled(rosterStatus)) {
                rosterStatus = rosterStatus.equalsIgnoreCase("RO") ? "1" : "0";
                enrolment = demo.addNewEnrolment();
                enrolment.setEnrollmentStatus(cdsDt.EnrollmentStatus.Enum.forString(rosterStatus));
                if (rosterStatus.equals("1")) {
                    data = demographic.getRosterDate();
                    if (UtilDateUtilities.StringToDate(data)!=null) {
                        enrolment.setEnrollmentDate(Util.calDate(data));
                    }
                } else {
                    data = demographic.getRosterTerminationDate();
                    if (UtilDateUtilities.StringToDate(data)!=null) {
                        enrolment.setEnrollmentTerminationDate(Util.calDate(data));
                    }
                }
            }
            
            //Enrolment Status history
            List<DemographicArchive> DAs = demoArchiveDao.findRosterStatusHistoryByDemographicNo(Integer.valueOf(demoNo));
            for (int i=0; i<DAs.size(); i++) {
                String historyRS = DAs.get(i).getRosterStatus();
                if (StringUtils.empty(historyRS)) continue;

                historyRS = historyRS.equalsIgnoreCase("RO") ? "1" : "0";
                if (i==0 && historyRS.equals(rosterStatus)) continue;

                enrolment = demo.addNewEnrolment();
                enrolment.setEnrollmentStatus(cdsDt.EnrollmentStatus.Enum.forString(historyRS));
                if (historyRS.equals("1")) {
                    data = demographic.getRosterDate();
                    if (UtilDateUtilities.StringToDate(data)!=null) {
                        enrolment.setEnrollmentDate(Util.calDate(data));
                    }
                } else {
                    Date terminationDate = DAs.get(i).getRosterTerminationDate();
                    if (terminationDate!=null) {
                        enrolment.setEnrollmentTerminationDate(Util.calDate(terminationDate));
                    }
                }
            }

            //Person Status (Patient Status)
            data = StringUtils.noNull(demographic.getPatientStatus());
            Demographics.PersonStatusCode personStatusCode = demo.addNewPersonStatusCode();
            if (StringUtils.empty(data)) {
                data = "";
                err.add("Error! No Person Status Code for Patient "+demoNo);
            }
            if (data.equalsIgnoreCase("AC")) personStatusCode.setPersonStatusAsEnum(cdsDt.PersonStatus.A);
            else if (data.equalsIgnoreCase("IN")) personStatusCode.setPersonStatusAsEnum(cdsDt.PersonStatus.I);
            else if (data.equalsIgnoreCase("DE")) personStatusCode.setPersonStatusAsEnum(cdsDt.PersonStatus.D);
            else {
                if ("MO".equalsIgnoreCase(data)) data = "Moved";
                else if ("FI".equalsIgnoreCase(data)) data = "Fired";
                personStatusCode.setPersonStatusAsPlainText(data);
            }

            data = demographic.getPatientStatusDate();
            if (StringUtils.filled(data)) demo.setPersonStatusDate(Util.calDate(data));

            //patient notes
            data = d.getDemographicNotes(demoNo);
            if (StringUtils.filled(data)) demo.setNoteAboutPatient(data);

            data = StringUtils.noNull(demographic.getDob("-"));
            demo.setDateOfBirth(Util.calDate(data));
            if (UtilDateUtilities.StringToDate(data)==null) {
                err.add("Error! No Date Of Birth for Patient "+demoNo);
            } else if (UtilDateUtilities.StringToDate(data)==null) {
                err.add("Note: Not exporting invalid Date of Birth for Patient "+demoNo);
            }
            data = demographic.getChartNo();
            if (StringUtils.filled(data)) demo.setChartNumber(data);

            data = demographic.getEmail();
            if (StringUtils.filled(data)) demo.setEmail(data);

            String providerNo = demographic.getProviderNo();
            if (StringUtils.filled(providerNo)) {
                Demographics.PrimaryPhysician pph = demo.addNewPrimaryPhysician();
                ProviderData prvd = new ProviderData(providerNo);
                if (StringUtils.noNull(prvd.getOhip_no()).length()<=6) pph.setOHIPPhysicianId(prvd.getOhip_no());
                Util.writeNameSimple(pph.addNewName(), prvd.getFirst_name(), prvd.getLast_name());
                String cpso = prvd.getPractitionerNo();
                if (cpso!=null && cpso.length()==5) pph.setPrimaryPhysicianCPSO(cpso);
            }
            
            if(StringUtils.filled(demographic.getSin())) {
            	demo.setSIN(demographic.getSin());
            }
            
            if (StringUtils.filled(demographic.getJustHIN())) {
                cdsDt.HealthCard healthCard = demo.addNewHealthCard();

                healthCard.setNumber(demographic.getJustHIN());
                if (Util.setProvinceCode(demographic.getHCType())!=null) healthCard.setProvinceCode(Util.setProvinceCode(demographic.getHCType()));
                else healthCard.setProvinceCode(cdsDt.HealthCardProvinceCode.X_70); //Asked, unknown
                if (healthCard.getProvinceCode()==null) {
                    err.add("Error! No Health Card Province Code for Patient "+demoNo);
                }
                if (StringUtils.filled(demographic.getVersionCode())) healthCard.setVersion(demographic.getVersionCode());
                data = demographic.getHCRenewDate();
                if (UtilDateUtilities.StringToDate(data)!=null) {
                    healthCard.setExpirydate(Util.calDate(data));
                }
            }
            if (StringUtils.filled(demographic.getAddress())) {
                cdsDt.Address addr = demo.addNewAddress();
                cdsDt.AddressStructured address = addr.addNewStructured();

                addr.setAddressType(cdsDt.AddressType.R);
                address.setLine1(demographic.getAddress());
                if (StringUtils.filled(demographic.getCity()) || StringUtils.filled(demographic.getProvince()) || StringUtils.filled(demographic.getPostal())) {
                    address.setCity(StringUtils.noNull(demographic.getCity()));
                    address.setCountrySubdivisionCode(Util.setCountrySubDivCode(demographic.getProvince()));
                    address.addNewPostalZipCode().setPostalCode(StringUtils.noNull(demographic.getPostal()).replace(" ",""));
                }
            }
            String phoneNo = Util.onlyNum(demographic.getPhone());
            if (StringUtils.filled(phoneNo) && phoneNo.length()>=7) {
                cdsDt.PhoneNumber phoneResident = demo.addNewPhoneNumber();
                phoneResident.setPhoneNumberType(cdsDt.PhoneNumberType.R);
                phoneResident.setPhoneNumber(phoneNo);
                data = demoExt.get("hPhoneExt");
                if (data!=null) {
                    if (data.length()>5) {
                        data = data.substring(0,5);
                        err.add("Note: Home phone extension too long - trimmed for Patient "+demoNo);
                    }
                    phoneResident.setExtension(data);
                }
            }
            phoneNo = Util.onlyNum(demographic.getPhone2());
            if (StringUtils.filled(phoneNo) && phoneNo.length()>=7) {
                cdsDt.PhoneNumber phoneWork = demo.addNewPhoneNumber();
                phoneWork.setPhoneNumberType(cdsDt.PhoneNumberType.W);
                phoneWork.setPhoneNumber(phoneNo);
                data = demoExt.get("wPhoneExt");
                if (data!=null) {
                    if (data.length()>5) {
                        data = data.substring(0,5);
                        err.add("Note: Work phone extension too long, export trimmed for Patient "+demoNo);
                    }
                    phoneWork.setExtension(data);
                }
            }
            phoneNo = Util.onlyNum(demoExt.get("demo_cell"));
            if (StringUtils.filled(phoneNo) && phoneNo.length()>=7) {
                cdsDt.PhoneNumber phoneCell = demo.addNewPhoneNumber();
                phoneCell.setPhoneNumberType(cdsDt.PhoneNumberType.C);
                phoneCell.setPhoneNumber(phoneNo);
            }
            demoExt = null;

            DemographicContactDao contactDao = (DemographicContactDao) SpringUtils.getBean("demographicContactDao");
            List<DemographicContact> demoContacts = contactDao.findByDemographicNo(Integer.valueOf(demoNo));


//            DemographicRelationship demoRel = new DemographicRelationship();
//            ArrayList<HashMap> demoR = demoRel.getDemographicRelationships(demoNo);
            for (int j=0; j<demoContacts.size(); j++) {
//                HashMap<String,String> r = new HashMap<String,String>();
//                r.putAll(demoR.get(j));
//                data = r.get("demographic_no");
                DemographicContact demoContact = demoContacts.get(j);
                data = demoContact.getContactId();
                if (StringUtils.filled(data)) {
                    DemographicData.Demographic relDemo = d.getDemographic(data);
                    HashMap<String,String> relDemoExt = new HashMap<String,String>();
                    relDemoExt.putAll(ext.getAllValuesForDemo(data));

                    Demographics.Contact contact = demo.addNewContact();
                    Util.writeNameSimple(contact.addNewName(), relDemo.getFirstName(), relDemo.getLastName());
                    if (StringUtils.empty(relDemo.getFirstName())) {
                        err.add("Error! No First Name for contact ("+j+") for Patient "+demoNo);
                    }
                    if (StringUtils.empty(relDemo.getLastName())) {
                        err.add("Error! No Last Name for contact ("+j+") for Patient "+demoNo);
                    }

                    String ec = demoContact.getEc();
                    String sdm = demoContact.getSdm();
                    String rel = demoContact.getRole();

                    if (ec.equals("true")) {
                        contact.addNewContactPurpose().setPurposeAsEnum(cdsDt.PurposeEnumOrPlainText.PurposeAsEnum.EC);
                    }
                    if (sdm.equals("true")) {
                        contact.addNewContactPurpose().setPurposeAsEnum(cdsDt.PurposeEnumOrPlainText.PurposeAsEnum.SDM);
                    }
                    if (StringUtils.filled(rel)) {
                        cdsDt.PurposeEnumOrPlainText contactPurpose = contact.addNewContactPurpose();
                        if (rel.equals("Next of Kin")) contactPurpose.setPurposeAsEnum(cdsDt.PurposeEnumOrPlainText.PurposeAsEnum.NK);
                        else if (rel.equals("Administrative Staff")) contactPurpose.setPurposeAsEnum(cdsDt.PurposeEnumOrPlainText.PurposeAsEnum.AS);
                        else if (rel.equals("Care Giver")) contactPurpose.setPurposeAsEnum(cdsDt.PurposeEnumOrPlainText.PurposeAsEnum.CG);
                        else if (rel.equals("Power of Attorney")) contactPurpose.setPurposeAsEnum(cdsDt.PurposeEnumOrPlainText.PurposeAsEnum.PA);
                        else if (rel.equals("Insurance")) contactPurpose.setPurposeAsEnum(cdsDt.PurposeEnumOrPlainText.PurposeAsEnum.IN);
                        else if (rel.equals("Guarantor")) contactPurpose.setPurposeAsEnum(cdsDt.PurposeEnumOrPlainText.PurposeAsEnum.GT);
                        else contactPurpose.setPurposeAsPlainText(rel);
                    }
                    if (StringUtils.filled(relDemo.getEmail())) contact.setEmailAddress(relDemo.getEmail());

                    phoneNo = Util.onlyNum(relDemo.getPhone());
                    if (StringUtils.filled(phoneNo) && phoneNo.length()>=7) {
                        cdsDt.PhoneNumber phoneRes = contact.addNewPhoneNumber();
                        phoneRes.setPhoneNumberType(cdsDt.PhoneNumberType.R);
                        phoneRes.setPhoneNumber(phoneNo);
                        data = relDemoExt.get("hPhoneExt");
                        if (StringUtils.filled(data)) {
                            if (data.length()>5) {
                                data = data.substring(0,5);
                                err.add("Note: Home phone extension too long, export trimmed for contact ("+(j+1)+") of Patient "+demoNo);
                            }
                            phoneRes.setExtension(data);
                        }
                    }
                    phoneNo = Util.onlyNum(relDemo.getPhone2());
                    if (StringUtils.filled(phoneNo) && phoneNo.length()>=7) {
                        cdsDt.PhoneNumber phoneW = contact.addNewPhoneNumber();
                        phoneW.setPhoneNumberType(cdsDt.PhoneNumberType.W);
                        phoneW.setPhoneNumber(phoneNo);
                        data = relDemoExt.get("wPhoneExt");
                        if (StringUtils.filled(data)) {
                            if (data.length()>5) {
                                data = data.substring(0,5);
                                err.add("Note: Work phone extension too long, export trimmed for contact ("+(j+1)+") of Patient "+demoNo);
                            }
                            phoneW.setExtension(data);
                        }
                    }
                    phoneNo = Util.onlyNum(relDemoExt.get("demo_cell"));
                    if (StringUtils.filled(phoneNo) && phoneNo.length()>=7) {
                        cdsDt.PhoneNumber phoneCell = contact.addNewPhoneNumber();
                        phoneCell.setPhoneNumberType(cdsDt.PhoneNumberType.C);
                        phoneCell.setPhoneNumber(phoneNo);
                    }
                    relDemoExt = null;
                }
            }

            CaseManagementManager cmm = (CaseManagementManager) SpringUtils.getBean("caseManagementManager");
            List<CaseManagementNote> lcmn = cmm.getNotes(demoNo);
            for (CaseManagementNote cmn : lcmn) {
                String famHist="", socHist="", medHist="", concerns="", reminders="", riskFactors="", encounter="", annotation="", summary="";
                Set<CaseManagementIssue> sisu = cmn.getIssues();
                boolean systemIssue = false;
                for (CaseManagementIssue isu : sisu) {
                    String _issue = isu.getIssue()!=null ? isu.getIssue().getCode() : "";
                    if (_issue.equals("SocHistory")) {
                        systemIssue = true;
                        socHist = cmn.getNote();
                        break;
                    } else if (_issue.equals("FamHistory")) {
                        systemIssue = true;
                        famHist = cmn.getNote();
                        break;
                    } else if (_issue.equals("MedHistory")) {
                        systemIssue = true;
                        medHist = cmn.getNote();
                        break;
                    } else if (_issue.equals("Concerns")) {
                        systemIssue = true;
                        concerns = cmn.getNote();
                        break;
                    } else if (_issue.equals("Reminders")) {
                        systemIssue = true;
                        reminders = cmn.getNote();
                        break;
                    } else if (_issue.equals("RiskFactors")) {
                        systemIssue = true;
                        riskFactors = cmn.getNote();
                        break;
                    } else continue;
                }
                if (!systemIssue && cmm.getLinkByNote(cmn.getId()).isEmpty()) { //this is not an annotation
                        encounter = cmn.getNote();
                }
                CaseManagementNoteLink cml = cmm.getLatestLinkByTableId(CaseManagementNoteLink.CASEMGMTNOTE, cmn.getId());
                if (cml!=null) {
                    CaseManagementNote n = cmm.getNote(cml.getNoteId().toString());
                    if (n.getNote()!=null && !n.getNote().startsWith("imported.cms4.2011.06")) //not from dumpsite
                        annotation = n.getNote();
                }
                List<CaseManagementNoteExt> cmeList = cmm.getExtByNote(cmn.getId());

                    // PERSONAL HISTORY (SocHistory)
                    if (StringUtils.filled(socHist)) {
                        summary = Util.addSummary("Personal History", socHist);
                        for (CaseManagementIssue isu : sisu) {
                            String codeSystem = isu.getIssue().getType();
                            if (!codeSystem.equals("system")) {
                                summary = Util.addSummary(summary, "Diagnosis", isu.getIssue().getDescription());
                            }
                        }
                        addOneEntry(PERSONALHISTORY);
                        boolean bSTARTDATE=false, bRESOLUTIONDATE=false;
                        for (CaseManagementNoteExt cme : cmeList) {
                            if (cme.getKeyVal().equals(CaseManagementNoteExt.STARTDATE)) {
                                if (bSTARTDATE) continue;
                                if (cme.getDateValue()!=null) {
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.STARTDATE, Util.readPartialDate(cme));
                                }
                                bSTARTDATE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.RESOLUTIONDATE)) {
                                if (bRESOLUTIONDATE) continue;
                                if (cme.getDateValue()!=null) {
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.RESOLUTIONDATE, Util.readPartialDate(cme));
                                }
                                bRESOLUTIONDATE = true;
                            }
                        }
                        summary = Util.addSummary(summary, "Notes", annotation);
                        patientRec.addNewPersonalHistory().setCategorySummaryLine(summary);
                    }
                if (exFamilyHistory) {
                    // FAMILY HISTORY (FamHistory)
                    if (StringUtils.filled(famHist)) {
                        FamilyHistory fHist = patientRec.addNewFamilyHistory();
                        fHist.setProblemDiagnosisProcedureDescription(famHist);
                        summary = Util.addSummary("Problem Description", famHist);

                        boolean diagnosisAssigned = false;
                        for (CaseManagementIssue isu : sisu) {
                            String codeSystem = isu.getIssue().getType();
                            if (!codeSystem.equals("system")) {
                                if (diagnosisAssigned) {
                                    summary = Util.addSummary(summary, "Diagnosis", isu.getIssue().getDescription());
                                } else {
                                    cdsDt.StandardCoding diagnosis = fHist.addNewDiagnosisProcedureCode();
                                    diagnosis.setStandardCodingSystem(codeSystem);
                                    String code = codeSystem.equalsIgnoreCase("icd9") ? Util.formatIcd9(isu.getIssue().getCode()) : isu.getIssue().getCode();
                                    diagnosis.setStandardCode(code);
                                    diagnosis.setStandardCodeDescription(isu.getIssue().getDescription());
                                    summary = Util.addSummary(summary, "Diagnosis", diagnosis.getStandardCodeDescription());
                                    diagnosisAssigned = true;
                                }
                            }
                        }
                        addOneEntry(FAMILYHISTORY);
                        boolean bSTARTDATE=false, bTREATMENT=false, bAGEATONSET=false, bRELATIONSHIP=false, bLIFESTAGE=false;
                        for (CaseManagementNoteExt cme : cmeList) {
                            if (cme.getKeyVal().equals(CaseManagementNoteExt.STARTDATE)) {
                                if (bSTARTDATE) continue;
                                if (cme.getDateValue()!=null) {
                                    Util.putPartialDate(fHist.addNewStartDate(), cme);
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.STARTDATE, UtilDateUtilities.DateToString(cme.getDateValue(), "yyyy-MM-dd"));
                                }
                                bSTARTDATE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.TREATMENT)) {
                                if (bTREATMENT) continue;
                                if (StringUtils.filled(cme.getValue())) {
                                    fHist.setTreatment(cme.getValue());
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.TREATMENT, cme.getValue());
                                }
                                bTREATMENT = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.AGEATONSET)) {
                                if (bAGEATONSET) continue;
                                if (StringUtils.filled(cme.getValue())) {
                                    fHist.setAgeAtOnset(BigInteger.valueOf(Long.valueOf(cme.getValue())));
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.AGEATONSET, cme.getValue());
                                }
                                bAGEATONSET = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.RELATIONSHIP)) {
                                if (bRELATIONSHIP) continue;
                                if (StringUtils.filled(cme.getValue())) {
                                    fHist.setRelationship(cme.getValue());
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.RELATIONSHIP, cme.getValue());
                                }
                                bRELATIONSHIP = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.LIFESTAGE)) {
                                if (bLIFESTAGE) continue;
                                if (StringUtils.filled(cme.getValue()) && "NICTA".contains(cme.getValue())) {
                                    fHist.setLifeStage(cdsDt.LifeStage.Enum.forString(cme.getValue()));
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.LIFESTAGE, cme.getValue());
                                }
                                bLIFESTAGE = true;
                            }
                        }
                        if (StringUtils.filled(annotation)) {
                            fHist.setNotes(annotation);
                            summary = Util.addSummary(summary, "Notes", annotation);
                        }
                        fHist.setCategorySummaryLine(summary);
                    }
                }
                if (exPastHealth) {
                    // PAST HEALTH (MedHistory)
                    if (StringUtils.filled(medHist)) {
                        PastHealth pHealth = patientRec.addNewPastHealth();
                        summary = Util.addSummary("Problem Description", medHist);

                        boolean diagnosisAssigned = false;
                        for (CaseManagementIssue isu : sisu) {
                            String codeSystem = isu.getIssue().getType();
                            if (!codeSystem.equals("system")) {
                                if (diagnosisAssigned) {
                                    summary = Util.addSummary(summary, "Diagnosis", isu.getIssue().getDescription());
                                } else {
                                    cdsDt.StandardCoding diagnosis = pHealth.addNewDiagnosisProcedureCode();
                                    
                                    diagnosis.setStandardCodingSystem(codeSystem);
                                    String code = codeSystem.equalsIgnoreCase("icd9") ? Util.formatIcd9(isu.getIssue().getCode()) : isu.getIssue().getCode();
                                    diagnosis.setStandardCode(code);
                                    diagnosis.setStandardCodeDescription(isu.getIssue().getDescription());
                                    summary = Util.addSummary(summary, "Diagnosis", diagnosis.getStandardCodeDescription());
                                    diagnosisAssigned = true;
                                }
                            }
                        }
                        addOneEntry(PASTHEALTH);
                        boolean bSTARTDATE=false, bRESOLUTIONDATE=false, bPROCEDUREDATE=false, bLIFESTAGE=false;;
                        for (CaseManagementNoteExt cme : cmeList) {
                            if (cme.getKeyVal().equals(CaseManagementNoteExt.STARTDATE)) {
                                if (bSTARTDATE) continue;
                                if (cme.getDateValue()!=null) {
                                    Util.putPartialDate(pHealth.addNewOnsetOrEventDate(), cme);
                                    summary = Util.addSummary(summary, "Onset/Event Date", UtilDateUtilities.DateToString(cme.getDateValue(), "yyyy-MM-dd"));
                                }
                                bSTARTDATE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.RESOLUTIONDATE)) {
                                if (bRESOLUTIONDATE) continue;
                                if (cme.getDateValue()!=null) {
                                    Util.putPartialDate(pHealth.addNewResolvedDate(), cme);
                                    summary = Util.addSummary(summary, "Resolved Date", UtilDateUtilities.DateToString(cme.getDateValue(), "yyyy-MM-dd"));
                                }
                                bRESOLUTIONDATE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.PROCEDUREDATE)) {
                                if (bPROCEDUREDATE) continue;
                                if (cme.getDateValue()!=null) {
                                    Util.putPartialDate(pHealth.addNewProcedureDate(), cme);
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.PROCEDUREDATE, UtilDateUtilities.DateToString(cme.getDateValue(), "yyyy-MM-dd"));
                                }
                                bPROCEDUREDATE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.LIFESTAGE)) {
                                if (bLIFESTAGE) continue;
                                    if (StringUtils.filled(cme.getValue()) && "NICTA".contains(cme.getValue())) {
                                    pHealth.setLifeStage(cdsDt.LifeStage.Enum.forString(cme.getValue()));
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.LIFESTAGE, cme.getValue());
                                }
                                bLIFESTAGE = true;
                            }
                        }
                        pHealth.setPastHealthProblemDescriptionOrProcedures(medHist);
                        if (StringUtils.filled(annotation)) {
                            pHealth.setNotes(annotation);
                            summary = Util.addSummary(summary, "Notes", annotation);
                        }
                        pHealth.setCategorySummaryLine(summary);
                    }
                }
                if (exProblemList) {
                    // PROBLEM LIST (Concerns)
                    if (StringUtils.filled(concerns)) {
                        ProblemList pList = patientRec.addNewProblemList();
                        pList.setProblemDiagnosisDescription(concerns);
                        summary = Util.addSummary("Problem Diagnosis", concerns);

                        boolean diagnosisAssigned = false;
                        for (CaseManagementIssue isu : sisu) {
                            String codeSystem = isu.getIssue().getType();
                            if (!codeSystem.equals("system")) {
                                if (diagnosisAssigned) {
                                    summary = Util.addSummary(summary, "Diagnosis", isu.getIssue().getDescription());
                                } else {
                                    cdsDt.StandardCoding diagnosis = pList.addNewDiagnosisCode();
                                    diagnosis.setStandardCodingSystem(codeSystem);
                                    String code = codeSystem.equalsIgnoreCase("icd9") ? Util.formatIcd9(isu.getIssue().getCode()) : isu.getIssue().getCode();
                                    diagnosis.setStandardCode(code);
                                    diagnosis.setStandardCodeDescription(isu.getIssue().getDescription());
                                    summary = Util.addSummary(summary, "Diagnosis", diagnosis.getStandardCodeDescription());
                                    diagnosisAssigned = true;
                                }
                            }
                        }
                        addOneEntry(PROBLEMLIST);
                        boolean bPROBLEMDESC=false, bSTARTDATE=false, bRESOLUTIONDATE=false, bPROBLEMSTATUS=false, bLIFESTAGE=false;
                        for (CaseManagementNoteExt cme : cmeList) {
                            if (cme.getKeyVal().equals(CaseManagementNoteExt.PROBLEMDESC)) {
                                if (bPROBLEMDESC) continue;
                                if (StringUtils.filled(cme.getValue())) {
                                    pList.setProblemDescription(cme.getValue());
                                    summary = Util.addSummary(summary, "Problem Description", cme.getValue());
                                }
                                bPROBLEMDESC = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.STARTDATE)) {
                                if (bSTARTDATE) continue;
                                Util.putPartialDate(pList.addNewOnsetDate(), cme);
                                summary = Util.addSummary(summary, "Onset Date", UtilDateUtilities.DateToString(cme.getDateValue(), "yyyy-MM-dd"));
                                if (cme.getDateValue()==null) {
                                    err.add("Error! No Onset Date for Problem List for Patient "+demoNo);
                                }
                                bSTARTDATE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.RESOLUTIONDATE)) {
                                if (bRESOLUTIONDATE) continue;
                                if (cme.getDateValue()!=null) {
                                    Util.putPartialDate(pList.addNewResolutionDate(), cme);
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.RESOLUTIONDATE, UtilDateUtilities.DateToString(cme.getDateValue(), "yyyy-MM-dd"));
                                }
                                bRESOLUTIONDATE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.LIFESTAGE)) {
                                if (bLIFESTAGE) continue;
                                if (StringUtils.filled(cme.getValue()) && "NICTA".contains(cme.getValue())) {
                                    pList.setLifeStage(cdsDt.LifeStage.Enum.forString(cme.getValue()));
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.LIFESTAGE, cme.getValue());
                                }
                                bLIFESTAGE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.PROBLEMSTATUS)) {
                                if (bPROBLEMSTATUS) continue;
                                if (StringUtils.filled(cme.getValue())) {
                                    pList.setProblemStatus(cme.getValue());
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.PROBLEMSTATUS, cme.getValue());
                                }
                                bPROBLEMSTATUS = true;
                            }
                        }

                        if (StringUtils.filled(annotation)) {
                            pList.setNotes(annotation);
                            summary = Util.addSummary(summary, "Notes", annotation);
                        }
                        pList.setCategorySummaryLine(summary);
                    }
                }
                if (exRiskFactors) {
                    // RISK FACTORS
                    if (StringUtils.filled(riskFactors)) {
                        RiskFactors rFact = patientRec.addNewRiskFactors();
                        rFact.setRiskFactor(riskFactors);
                        summary = Util.addSummary("Risk Factor", riskFactors);
                        addOneEntry(RISKFACTOR);

                        boolean bSTARTDATE=false, bRESOLUTIONDATE=false, bAGEATONSET=false, bEXPOSUREDETAIL=false, bLIFESTAGE=false;
                        for (CaseManagementNoteExt cme : cmeList) {
                            if (cme.getKeyVal().equals(CaseManagementNoteExt.STARTDATE)) {
                                if (bSTARTDATE) continue;
                                if (cme.getDateValue()!=null) {
                                    Util.putPartialDate(rFact.addNewStartDate(), cme);
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.STARTDATE, UtilDateUtilities.DateToString(cme.getDateValue(), "yyyy-MM-dd"));
                                }
                                bSTARTDATE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.RESOLUTIONDATE)) {
                                if (bRESOLUTIONDATE) continue;
                                if (cme.getDateValue()!=null) {
                                    Util.putPartialDate(rFact.addNewEndDate(), cme);
                                    summary = Util.addSummary(summary, "End Date", UtilDateUtilities.DateToString(cme.getDateValue(), "yyyy-MM-dd"));
                                }
                                bRESOLUTIONDATE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.AGEATONSET)) {
                                if (bAGEATONSET) continue;
                                if (StringUtils.filled(cme.getValue())) {
                                    rFact.setAgeOfOnset(BigInteger.valueOf(Long.valueOf(cme.getValue())));
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.AGEATONSET, cme.getValue());
                                }
                                bAGEATONSET = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.LIFESTAGE)) {
                                if (bLIFESTAGE) continue;
                                if (StringUtils.filled(cme.getValue()) && "NICTA".contains(cme.getValue())) {
                                    rFact.setLifeStage(cdsDt.LifeStage.Enum.forString(cme.getValue()));
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.LIFESTAGE, cme.getValue());
                                }
                                bLIFESTAGE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.EXPOSUREDETAIL)) {
                                if (bEXPOSUREDETAIL) continue;
                                if (StringUtils.filled(cme.getValue())) {
                                    rFact.setExposureDetails(cme.getValue());
                                    summary = Util.addSummary(summary, CaseManagementNoteExt.EXPOSUREDETAIL, cme.getValue());
                                }
                                bEXPOSUREDETAIL = true;
                            }
                        }
                        if (StringUtils.filled(annotation)) {
                            rFact.setNotes(annotation);
                            summary = Util.addSummary(summary, "Notes", annotation);
                        }
                        for (CaseManagementIssue isu : sisu) {
                            String codeSystem = isu.getIssue().getType();
                            if (!codeSystem.equals("system")) {
                                summary = Util.addSummary(summary, "Diagnosis", isu.getIssue().getDescription());
                            }
                        }
                        rFact.setCategorySummaryLine(summary);
                    }
                }

                if (exClinicalNotes) {
                    // CLINCAL NOTES
                    if (StringUtils.filled(encounter)) {
                        ClinicalNotes cNote = patientRec.addNewClinicalNotes();
                        for (CaseManagementIssue isu : sisu) {
                            String codeSystem = isu.getIssue().getType();
                            if (!codeSystem.equals("system")) {
                                encounter = Util.addLine(encounter, "Diagnosis: ", isu.getIssue().getDescription());
                            }
                        }
                        cNote.setMyClinicalNotesContent(encounter);
                        addOneEntry(CLINICALNOTE);

                        if (cmn.getUpdate_date()!=null) {
                            cNote.addNewEnteredDateTime().setFullDateTime(Util.calDate(cmn.getUpdate_date()));
                        }
                        if (cmn.getObservation_date()!=null) {
                            cNote.addNewEventDateTime().setFullDateTime(Util.calDate(cmn.getObservation_date()));
                        }
                        if (StringUtils.filled(cmn.getProviderNo())) {
                            ClinicalNotes.ParticipatingProviders pProvider = cNote.addNewParticipatingProviders();
                            ProviderData prvd = new ProviderData(cmn.getProviderNo());
                            Util.writeNameSimple(pProvider.addNewName(), StringUtils.noNull(prvd.getFirst_name()), StringUtils.noNull(prvd.getLast_name()));

                            if (StringUtils.noNull(prvd.getOhip_no()).length()<=6) pProvider.setOHIPPhysicianId(prvd.getOhip_no());
                            cdsDt.DateTimeFullOrPartial noteCreatedDateTime = pProvider.addNewDateTimeNoteCreated();
                            if (cmn.getCreate_date()!=null) noteCreatedDateTime.setFullDate(Util.calDate(cmn.getCreate_date()));
                            else noteCreatedDateTime.setFullDate(Util.calDate(new Date()));
                        }
                        if (StringUtils.filled(cmn.getSigning_provider_no())) {
                            ProviderData prvd = new ProviderData(cmn.getSigning_provider_no());
                            ClinicalNotes.NoteReviewer noteReviewer = cNote.addNewNoteReviewer();
                            Util.writeNameSimple(noteReviewer.addNewName(), prvd.getFirst_name(), prvd.getLast_name());
                            if (cmn.getUpdate_date()!=null) noteReviewer.addNewDateTimeNoteReviewed().setFullDate(Util.calDate(cmn.getUpdate_date()));
                            else noteReviewer.addNewDateTimeNoteReviewed().setFullDateTime(Util.calDate(new Date()));
                            if (StringUtils.noNull(prvd.getOhip_no()).length()<=6) noteReviewer.setOHIPPhysicianId(prvd.getOhip_no());
                        }
                    }
                }

                if (exAlertsAndSpecialNeeds) {
                    // ALERTS AND SPECIAL NEEDS (Reminders)
                    if (StringUtils.filled(reminders)) {
                        AlertsAndSpecialNeeds alerts = patientRec.addNewAlertsAndSpecialNeeds();
                        alerts.setAlertDescription(reminders);
                        addOneEntry(ALERT);

                        boolean bSTARTDATE=false, bRESOLUTIONDATE=false;
                        for (CaseManagementNoteExt cme : cmeList) {
                            if (cme.getKeyVal().equals(CaseManagementNoteExt.STARTDATE)) {
                                if (bSTARTDATE) continue;
                                if (cme.getDateValue()!=null) {
                                    Util.putPartialDate(alerts.addNewDateActive(), cme);
                                    reminders = Util.addLine(reminders, "Date Active: ", UtilDateUtilities.DateToString(cme.getDateValue()));
                                }
                                bSTARTDATE = true;
                            } else if (cme.getKeyVal().equals(CaseManagementNoteExt.RESOLUTIONDATE)) {
                                if (bRESOLUTIONDATE) continue;
                                if (cme.getDateValue()!=null) {
                                    Util.putPartialDate(alerts.addNewEndDate(), cme);
                                    reminders = Util.addLine(reminders, "End Date: ", UtilDateUtilities.DateToString(cme.getDateValue()));
                                }
                                bRESOLUTIONDATE = true;
                            }
                        }
                        if (StringUtils.filled(annotation)) {
                            alerts.setNotes(annotation);
                            reminders = Util.addLine(reminders, "Notes: ", annotation);
                        }
                        alerts.setCategorySummaryLine(reminders);
                    }
                }
            }

            if (exAllergiesAndAdverseReactions) {
                // ALLERGIES & ADVERSE REACTIONS
                RxPatientData.Patient.Allergy[] allergies = RxPatientData.getPatient(demoNo).getAllergies();
                for (int j=0; j<allergies.length; j++) {
                    AllergiesAndAdverseReactions alr = patientRec.addNewAllergiesAndAdverseReactions();
                    Allergy allergy = allergies[j].getAllergy();
                    String aSummary = "";
                    addOneEntry(ALLERGY);

                    data = allergy.getDESCRIPTION();
                    if (StringUtils.filled(data)) {
                        alr.setOffendingAgentDescription(data);
                        aSummary = Util.addSummary("Offending Agent Description", data);
                    }
                    data = allergy.getRegionalIdentifier();
                    if (StringUtils.filled(data) && !data.trim().equalsIgnoreCase("null")) {
                        cdsDt.DrugCode drugCode = alr.addNewCode();
                        drugCode.setCodeType("DIN");
                        drugCode.setCodeValue(data);
                        aSummary = Util.addSummary(aSummary, "DIN", data);
                    }
                    data = String.valueOf(allergy.getTYPECODE());
                    if (StringUtils.filled(data)) {
                        if (data.equals("0")) {
                            //alr.setReactionType(cdsDt.AdverseReactionType.AL);
                            alr.setPropertyOfOffendingAgent(cdsDt.PropertyOfOffendingAgent.ND);
                        } else {
                            //alr.setReactionType(cdsDt.AdverseReactionType.AR);
                            if (data.equals("13")) {
                                alr.setPropertyOfOffendingAgent(cdsDt.PropertyOfOffendingAgent.DR);
                            } else {
                                alr.setPropertyOfOffendingAgent(cdsDt.PropertyOfOffendingAgent.UK);
                            }
                        }
                        aSummary = Util.addSummary(aSummary,"Property of Offending Agent",alr.getPropertyOfOffendingAgent().toString());
                    }
                    data = allergy.getReaction();
                    if (StringUtils.filled(data)) {
                        alr.setReaction(data);
                        aSummary = Util.addSummary(aSummary, "Reaction", data);
                    }
                    data = allergy.getSeverityOfReaction();
                    if (StringUtils.filled(data)) {
                        if (data.equals("1")) {
                            alr.setSeverity(cdsDt.AdverseReactionSeverity.MI);
                        } else if (data.equals("2")) {
                            alr.setSeverity(cdsDt.AdverseReactionSeverity.MO);
                        } else if (data.equals("3")) {
                            alr.setSeverity(cdsDt.AdverseReactionSeverity.LT);
                        } else { //SeverityOfReaction==0
                            alr.setSeverity(cdsDt.AdverseReactionSeverity.MI);
                            err.add("Note: Severity Of Allergy Reaction [Unknown] exported as [Mild] for Patient "+demoNo+" ("+(j+1)+")");
                        }
                        aSummary = Util.addSummary(aSummary,"Adverse Reaction Severity",alr.getSeverity().toString());
                    }
                    if (allergy.getStartDate()!=null) {
                        alr.addNewStartDate().setFullDate(Util.calDate(allergy.getStartDate()));
                        aSummary = Util.addSummary(aSummary,"Start Date",UtilDateUtilities.DateToString(allergy.getStartDate()));
                    }
                    if (StringUtils.filled(allergy.getLifeStage()) && "NICTA".contains(allergy.getLifeStage())) {
                        alr.setLifeStage(cdsDt.LifeStage.Enum.forString(allergy.getLifeStage()));
                        aSummary = Util.addSummary(aSummary,"Life Stage at Onset", allergy.getLifeStageDesc());
                    }

                    if (allergies[j].getEntryDate()!=null) {
                        alr.addNewRecordedDate().setFullDate(Util.calDate(allergies[j].getEntryDate()));
                        aSummary = Util.addSummary(aSummary,"Recorded Date",UtilDateUtilities.DateToString(allergies[j].getEntryDate(),"yyyy-MM-dd"));
                    }
                    CaseManagementNoteLink cml = cmm.getLatestLinkByTableId(CaseManagementNoteLink.ALLERGIES, (long)allergies[j].getAllergyId());
                    if (cml!=null) {
                        CaseManagementNote n = cmm.getNote(cml.getNoteId().toString());
                        if (n.getNote()!=null && !n.getNote().startsWith("imported.cms4.2011.06")) {//not from dumpsite
                            alr.setNotes(StringUtils.noNull(n.getNote()));
                            aSummary = Util.addSummary(aSummary, "Notes", n.getNote());
                        }
                    }

                    if (StringUtils.empty(aSummary)) {
                        err.add("Error! No Category Summary Line (Allergies & Adverse Reactions) for Patient "+demoNo+" ("+(j+1)+")");
                    }
                    alr.setCategorySummaryLine(aSummary);
                }
            }

            if (exImmunizations) {
                // IMMUNIZATIONS
                ArrayList<String> inject = new ArrayList<String>();
                HashMap<String,String> prevTypes = new HashMap<String,String>();
                PreventionDisplayConfig pdc = PreventionDisplayConfig.getInstance();
                ArrayList<HashMap<String,String>> prevList = pdc.getPreventions();
                for (int k =0 ; k < prevList.size(); k++){
                    HashMap<String,String> a = new HashMap<String,String>();
                    a.putAll(prevList.get(k));
                    if (a != null && a.get("layout") != null &&  a.get("layout").equals("injection")){
                        inject.add(a.get("name"));
                        prevTypes.put(a.get("name"), a.get("healthCanadaType"));
                    }
                }
                ArrayList<Map<String,Object>> prevList2 = PreventionData.getPreventionData(demoNo);
                for (int k =0 ; k < prevList2.size(); k++){
                    HashMap<String,Object> a = new HashMap<String,Object>();
                    a.putAll(prevList2.get(k));
                    if (a != null && inject.contains(a.get("type")) ){
                        Immunizations immu = patientRec.addNewImmunizations();
                        HashMap<String,Object> extraData = new HashMap<String,Object>();
                        extraData.putAll(PreventionData.getPreventionById((String) a.get("id")));
                        if (StringUtils.filled((String)extraData.get("manufacture"))) immu.setManufacturer((String)extraData.get("manufacture"));
                        if (StringUtils.filled((String)extraData.get("lot"))) immu.setLotNumber((String)extraData.get("lot"));
                        if (StringUtils.filled((String)extraData.get("route"))) immu.setRoute((String)extraData.get("route"));
                        if (StringUtils.filled((String)extraData.get("location"))) immu.setSite((String)extraData.get("location"));
                        if (StringUtils.filled((String)extraData.get("dose"))) immu.setDose((String)extraData.get("dose"));
                        if (StringUtils.filled((String)extraData.get("comments"))) immu.setNotes((String)extraData.get("comments"));

                        String prevType = prevTypes.get(a.get("type"));
                        if (prevType!=null) {
                            immu.setImmunizationType(cdsDt.ImmunizationType.Enum.forString(prevType));
                        }

                        if (StringUtils.filled((String)extraData.get("name"))) immu.setImmunizationName((String)extraData.get("name"));
                        else
                        {
                            err.add("Error! No Immunization Name for Patient "+demoNo+" ("+(k+1)+")");
                            immu.setImmunizationName(prevType);
                        }
                        addOneEntry(IMMUNIZATION);
                        String imSummary = Util.addSummary("Immunization Name",data);

                        data = (String) a.get("refused");
                        if (StringUtils.empty(data)) {
                            immu.addNewRefusedFlag();
                            err.add("Error! No Refused Flag for Patient "+demoNo+" ("+(k+1)+")");
                        } else {
                            immu.addNewRefusedFlag().setBoolean(Util.convert10toboolean(data));
                            imSummary = Util.addSummary(imSummary, "Refused Flag", Util.convert10toboolean(data)?"Y":"N");
                        }

                        data = (String) a.get("prevention_date");
                        if (UtilDateUtilities.StringToDate(data)!=null) {
                            immu.addNewDate().setFullDate(Util.calDate(data));
                            imSummary = Util.addSummary(imSummary, "Date", data);
                        }

                        imSummary = Util.addSummary(imSummary, "Manufacturer", immu.getManufacturer());
                        imSummary = Util.addSummary(imSummary, "Lot No", immu.getLotNumber());
                        imSummary = Util.addSummary(imSummary, "Route", immu.getRoute());
                        imSummary = Util.addSummary(imSummary, "Site", immu.getSite());
                        imSummary = Util.addSummary(imSummary, "Dose", immu.getDose());
                        imSummary = Util.addSummary(imSummary, "Notes", immu.getNotes());

                        if (StringUtils.empty(imSummary)) {
                            err.add("Error! No Category Summary Line (Immunization) for Patient "+demoNo+" ("+(k+1)+")");
                        }
                        immu.setCategorySummaryLine(StringUtils.noNull(imSummary));
                    }
                }
            }

            if (exMedicationsAndTreatments) {
                // MEDICATIONS & TREATMENTS
                RxPrescriptionData prescriptData = new RxPrescriptionData();
                RxPrescriptionData.Prescription[] arr = null;
                arr = prescriptData.getUniquePrescriptionsByPatient(Integer.parseInt(demoNo));
                for (int p = 0; p < arr.length; p++){
                    MedicationsAndTreatments medi = patientRec.addNewMedicationsAndTreatments();
                    String mSummary = "";
                    if (arr[p].getWrittenDate()!=null) {
                        medi.addNewPrescriptionWrittenDate().setFullDate(Util.calDate(arr[p].getWrittenDate()));
                        mSummary = Util.addSummary("Prescription Written Date", UtilDateUtilities.DateToString(arr[p].getWrittenDate(),"yyyy-MM-dd"));
                    }
                    if (arr[p].getRxDate()!=null) {
                        medi.addNewStartDate().setFullDate(Util.calDate(arr[p].getRxDate()));
                        mSummary = Util.addSummary(mSummary,"Start Date",UtilDateUtilities.DateToString(arr[p].getRxDate(),"yyyy-MM-dd"));
                    }
                    data = arr[p].getRegionalIdentifier();
                    if (StringUtils.filled(data)) {
                        medi.setDrugIdentificationNumber(data);
                        mSummary = Util.addSummary(mSummary, "DIN", data);
                    }
                    String drugName = StringUtils.noNull(arr[p].getBrandName());
                    medi.setDrugName(drugName);
                    addOneEntry(MEDICATION);
                    mSummary = Util.addSummary(mSummary, "Drug Name", drugName);

                    /* no need:
                    DrugReasonDao drugReasonDao = (DrugReasonDao) SpringUtils.getBean("drugReasonDao");
                    List<DrugReason> drugReasons = drugReasonDao.getReasonsForDrugID(arr[p].getDrugId(), true);
                    if (drugReasons.size()>0 && StringUtils.filled(drugReasons.get(0).getCode()))
                        medi.setProblemCode(drugReasons.get(0).getCode());
                     *
                     */

                    if (StringUtils.filled(arr[p].getDosage())) {
                        String strength0 = arr[p].getDosage();
                        int sep = strength0.indexOf("/");

                        String strength = sep<0 ? Util.sleadingNum(strength0) : strength0.substring(0,sep);
                        if (sep>=0) {
                            err.add("Note: Multiple components exist for Drug "+drugName+" for Patient "+demoNo+". Exporting 1st one as Strength.");
                            if (sep<strength0.length()) strength0 = strength0.substring(sep+1);
                        }
                        cdsDt.DrugMeasure drugM = medi.addNewStrength();
                        drugM.setAmount(strength);
                        drugM.setUnitOfMeasure(Util.trailingTxt(strength0));
                        if (StringUtils.empty(drugM.getUnitOfMeasure())) drugM.setUnitOfMeasure("unit");
                        
                        mSummary = Util.addSummary(mSummary, "Strength", arr[p].getGenericName()+" "+strength);
                    }
                    if (StringUtils.filled(arr[p].getDosageDisplay())) {
                        medi.setDosage(arr[p].getDosageDisplay());
                        medi.setDosageUnitOfMeasure(StringUtils.noNull(arr[p].getUnit()));
                        mSummary = Util.addSummary(mSummary, "Dosage", arr[p].getDosageDisplay()+" "+StringUtils.noNull(arr[p].getUnit()));
                    }
                    if (StringUtils.filled(arr[p].getRoute())) {
                        medi.setRoute(arr[p].getRoute());
                        mSummary = Util.addSummary(mSummary, "Route", arr[p].getRoute());
                    }
                    if (StringUtils.filled(arr[p].getDrugForm())) {
                        medi.setForm(arr[p].getDrugForm());
                        mSummary = Util.addSummary(mSummary, "Form", arr[p].getDrugForm());
                    }
                    if (StringUtils.filled(arr[p].getCustomName())) {
                        if (StringUtils.empty(drugName)) medi.setDrugName(arr[p].getCustomName());
                        medi.setDrugDescription(arr[p].getCustomName());
                        mSummary = Util.addSummary(mSummary, "Drug Description", arr[p].getCustomName());
                    }
                    if (StringUtils.filled(arr[p].getFreqDisplay())) {
                        medi.setFrequency(arr[p].getFreqDisplay());
                        mSummary = Util.addSummary(mSummary, "Frequency", arr[p].getFreqDisplay());
                    }
                    data = arr[p].getDuration();
                    if (StringUtils.filled(data)) {
                        String durunit = StringUtils.noNull(arr[p].getDurationUnit());
                        Integer fctr = 1;
                        if (durunit.equals("W")) fctr = 7;
                        else if (durunit.equals("M")) fctr = 30;

                        if (NumberUtils.isDigits(data)) {
                            data = String.valueOf(Integer.parseInt(data)*fctr);
                            medi.setDuration(data);
                            mSummary = Util.addSummary(mSummary, "Duration", data+" Day(s)");
                        }
                    }
                    if (StringUtils.filled(arr[p].getQuantity())) {
                        medi.setQuantity(arr[p].getQuantity());
                        mSummary = Util.addSummary(mSummary, "Quantity", arr[p].getQuantity());
                    }
                    /* no need:
                    if (arr[p].getNosubs()) medi.setSubstitutionNotAllowed("Y");
                    else medi.setSubstitutionNotAllowed("N");
                    mSummary = Util.addSummary(mSummary, "Substitution not Allowed", arr[p].getNosubs()?"Yes":"No");
                     *
                     */

                    if (StringUtils.filled(medi.getDrugName()) || StringUtils.filled(medi.getDrugIdentificationNumber())) {
                        medi.setNumberOfRefills(String.valueOf(arr[p].getRepeat()));
                        mSummary = Util.addSummary(mSummary, "Number of Refills", String.valueOf(arr[p].getRepeat()));
                    }
                    if (StringUtils.filled(arr[p].getETreatmentType())) {
                        medi.setTreatmentType(arr[p].getETreatmentType());
                        mSummary = Util.addSummary(mSummary, "Treatment Type", arr[p].getETreatmentType());
                    }
                    /* no need: 
                    if (StringUtils.filled(arr[p].getRxStatus())) {
                        medi.setPrescriptionStatus(arr[p].getRxStatus());
                        mSummary = Util.addSummary(mSummary, "Prescription Status", arr[p].getRxStatus());
                    }
                    /* no need:
                    if (arr[p].getDispenseInterval()!=null) {
                        medi.setDispenseInterval(String.valueOf(arr[p].getDispenseInterval()));
                        mSummary = Util.addLine(mSummary, "Dispense Interval", arr[p].getDispenseInterval().toString());
                    }
                     *
                     */
                    if (arr[p].getRefillDuration()!=null) {
                        medi.setRefillDuration(String.valueOf(arr[p].getRefillDuration()));
                        mSummary = Util.addSummary(mSummary, "Refill Duration", arr[p].getRefillDuration().toString());
                    }
                    if (arr[p].getRefillQuantity()!=null) {
                        medi.setRefillQuantity(String.valueOf(arr[p].getRefillQuantity()));
                        mSummary = Util.addSummary(mSummary, "Refill Quantity", arr[p].getRefillQuantity().toString());
                    }

                    medi.addNewLongTermMedication().setBoolean(arr[p].getLongTerm());
                    mSummary = Util.addSummary(mSummary, "Long Term Medication",arr[p].getLongTerm()?"Yes":"No");

                    medi.addNewPastMedications().setBoolean(arr[p].getPastMed());
                    mSummary = Util.addSummary(mSummary, "Past Medcation",arr[p].getPastMed()?"Yes":"No");

                    cdsDt.YnIndicatorAndBlank pc = medi.addNewPatientCompliance();
                    if (arr[p].getPatientCompliance()==null) {
                        pc.setBlank(cdsDt.Blank.X);
                    } else {
                        String patientCompliance = arr[p].getPatientCompliance() ? "Yes" : "No";
                        pc.setBoolean(arr[p].getPatientCompliance());
                        mSummary = Util.addSummary(mSummary, "Patient Compliance", patientCompliance);
                    }

                    data = arr[p].getOutsideProviderName();
                    if (StringUtils.filled(data)) {
                        MedicationsAndTreatments.PrescribedBy pcb = medi.addNewPrescribedBy();
                        String ohip = arr[p].getOutsideProviderOhip();
                        if (ohip!=null && ohip.trim().length()<=6)
                            pcb.setOHIPPhysicianId(ohip.trim());
                        Util.writeNameSimple(pcb.addNewName(), data);
                        mSummary = Util.addSummary(mSummary, "Prescribed by", StringUtils.noNull(data));
                    } else {
                        data = arr[p].getProviderNo();
                        if (StringUtils.filled(data)) {
                            MedicationsAndTreatments.PrescribedBy pcb = medi.addNewPrescribedBy();
                            ProviderData prvd = new ProviderData(data);
                            String ohip = prvd.getOhip_no();
                            if (ohip!=null && ohip.trim().length()<=6)
                                pcb.setOHIPPhysicianId(ohip.trim());
                            Util.writeNameSimple(pcb.addNewName(), prvd.getFirst_name(), prvd.getLast_name());
                            mSummary = Util.addSummary(mSummary, "Prescribed by", StringUtils.noNull(prvd.getFirst_name())+" "+StringUtils.noNull(prvd.getLast_name()));
                        }
                    }

                    /* no need:
                    data = arr[p].getSpecial();
                    if (StringUtils.filled(data)) {
                        data = Util.extractDrugInstr(data);
                        medi.setPrescriptionInstructions(data);
                        mSummary = Util.addSummary(mSummary, "Prescription Instructions", data);
                    }

                    /* no need:
                    data = arr[p].isNonAuthoritative() ? "Y" : "N";
                    medi.setNonAuthoritativeIndicator(data);
                    mSummary = Util.addSummary(mSummary, "Non-Authoritative", data);

                    /* no need:
                    medi.setPrescriptionIdentifier(String.valueOf(arr[p].getDrugId()));
                    mSummary = Util.addSummary(mSummary, "Prescription Identifier", medi.getPrescriptionIdentifier());
                     *
                     */

                    CaseManagementNoteLink cml = cmm.getLatestLinkByTableId(CaseManagementNoteLink.DRUGS, (long)arr[p].getDrugId());
                    if (cml!=null) {
                        CaseManagementNote n = cmm.getNote(cml.getNoteId().toString());
                        if (n.getNote()!=null && !n.getNote().startsWith("imported.cms4.2011.06")) {//not from dumpsite
                            medi.setNotes(StringUtils.noNull(n.getNote()));
                            mSummary = Util.addSummary(mSummary, "Notes", n.getNote());
                        }
                    }

                    if (StringUtils.empty(mSummary)) err.add("Error! No Category Summary Line (Medications & Treatments) for Patient "+demoNo+" ("+(p+1)+")");
                    medi.setCategorySummaryLine(mSummary);
                }
                arr = null;
            }

            if (exLaboratoryResults) {
                // LABORATORY RESULTS
                List<LabMeasurements> labMeaList = ImportExportMeasurements.getLabMeasurements(demoNo);
                for (LabMeasurements labMea : labMeaList) {
                    LaboratoryResults labResults = patientRec.addNewLaboratoryResults();

                    //lab test code, test name, test name reported by lab
                    labResults.setLabTestCode(StringUtils.noNull(labMea.getExtVal("identifier")));
                    labResults.setTestName(StringUtils.noNull(labMea.getExtVal("name")));
                    labResults.setTestNameReportedByLab(StringUtils.noNull(labMea.getExtVal("name")));

                    //laboratory name
                    labResults.setLaboratoryName(StringUtils.noNull(labMea.getExtVal("labname")));
                    addOneEntry(LABS);
                    if (StringUtils.empty(labResults.getLaboratoryName())) {
                        err.add("Error! No Laboratory Name for Lab Test "+labResults.getLabTestCode()+" for Patient "+demoNo);
                    }

                    // lab collection datetime
                    cdsDt.DateTimeFullOrPartial collDate = labResults.addNewCollectionDateTime();
                    Date dateTime = labMea.getMeasure().getDateObserved();
                    String sDateTime = labMea.getExtVal("datetime");
                    if (dateTime!=null) collDate.setFullDateTime(Util.calDate(dateTime));
                    else collDate.setFullDateTime(Util.calDate(sDateTime));

                    if (dateTime==null && sDateTime==null) {
                        err.add("Error! No Collection Datetime for Lab Test "+labResults.getLabTestCode()+" for Patient "+demoNo);
                        collDate.setFullDateTime(Util.calDate(new Date()));
                    }

                    //lab normal/abnormal flag
                    labResults.setResultNormalAbnormalFlag(cdsDt.ResultNormalAbnormalFlag.U);
                    data = StringUtils.noNull(labMea.getExtVal("abnormal"));
                    if (data.equals("A")) labResults.setResultNormalAbnormalFlag(cdsDt.ResultNormalAbnormalFlag.Y);
                    if (data.equals("N")) labResults.setResultNormalAbnormalFlag(cdsDt.ResultNormalAbnormalFlag.N);

                    //lab unit of measure
                    data = StringUtils.noNull(labMea.getMeasure().getDataField());
                    if (StringUtils.filled(data)) {
                        LaboratoryResults.Result result = labResults.addNewResult();
                        result.setValue(data);
                        data = labMea.getExtVal("unit");
                        if (StringUtils.filled(data)) result.setUnitOfMeasure(data);
                    }

                    //lab accession number
                    data = StringUtils.noNull(labMea.getExtVal("accession"));
                    if (StringUtils.filled(data)) {
                        labResults.setAccessionNumber(data);
                    }

                    //notes from lab
                    data = StringUtils.noNull(labMea.getExtVal("comments"));
                    if (StringUtils.filled(data)) {
                        labResults.setNotesFromLab(data);
                    }

                    //lab reference range
                    String range = StringUtils.noNull(labMea.getExtVal("range"));
                    String min = StringUtils.noNull(labMea.getExtVal("minimum"));
                    String max = StringUtils.noNull(labMea.getExtVal("maximum"));
                    LaboratoryResults.ReferenceRange refRange = labResults.addNewReferenceRange();
                    if (StringUtils.filled(range)) refRange.setReferenceRangeText(range);
                    else {
                        if (StringUtils.filled(min)) refRange.setLowLimit(min);
                        if (StringUtils.filled(max)) refRange.setHighLimit(max);
                    }

                    //OLIS test result status
                    String olis_status = labMea.getExtVal("olis_status");
                    if (StringUtils.filled(olis_status)) labResults.setOLISTestResultStatus(olis_status);

                    String lab_no = StringUtils.noNull(labMea.getExtVal("lab_no"));
                    if (StringUtils.filled(lab_no)) {

                        //lab annotation
                        String other_id = StringUtils.noNull(labMea.getExtVal("other_id"));
                        CaseManagementNoteLink cml = cmm.getLatestLinkByTableId(CaseManagementNoteLink.LABTEST, Long.valueOf(lab_no), other_id);
                        if (cml!=null) {
                            CaseManagementNote n = cmm.getNote(cml.getNoteId().toString());
                            if (StringUtils.filled(n.getNote()) && !n.getNote().startsWith("imported.cms4.2011.06")) //not from dumpsite
                                labResults.setPhysiciansNotes(n.getNote());
                        }

                        //lab reviewer
                        HashMap<String,String> labRoutingInfo = new HashMap<String,String>();
                        labRoutingInfo.putAll(ProviderLabRouting.getInfo(lab_no));

                        String timestamp = labRoutingInfo.get("timestamp");
                        if (UtilDateUtilities.StringToDate(timestamp,"yyyy-MM-dd HH:mm:ss")!=null) {
                            LaboratoryResults.ResultReviewer reviewer = labResults.addNewResultReviewer();
                            reviewer.addNewDateTimeResultReviewed().setFullDateTime(Util.calDate(timestamp));
                            String lab_provider_no = labRoutingInfo.get("provider_no");
                            if (!"0".equals(lab_provider_no)) {
                                ProviderData pvd = new ProviderData(lab_provider_no);
                                Util.writeNameSimple(reviewer.addNewName(), pvd.getFirst_name(), pvd.getLast_name());
                                if (StringUtils.noNull(pvd.getOhip_no()).length()<=6) reviewer.setOHIPPhysicianId(pvd.getOhip_no());
                            }
                        }
//                      String info = labRoutingInfo.get("comment"); <--for whole report, may refer to >1 lab results

                        HashMap<String,Date> link = new HashMap<String,Date>();
                        link.putAll(LabRequestReportLink.getLinkByReport("hl7TextMessage", Long.valueOf(lab_no)));
                        Date reqDate = link.get("request_date");
                        if (reqDate!=null) labResults.addNewLabRequisitionDateTime().setFullDateTime(Util.calDate(reqDate));
                    }
                }
            }

            if (exAppointments) {
                // APPOINTMENTS
                OscarSuperManager oscarSuperManager = (OscarSuperManager)SpringUtils.getBean("oscarSuperManager");
                List appts = oscarSuperManager.populate("appointmentDao", "export_appt", new String[] {demoNo});
                ApptData ap = null;
                for (int j=0; j<appts.size(); j++) {
                    ap = (ApptData)appts.get(j);
                    Appointments aptm = patientRec.addNewAppointments();

                    cdsDt.DateFullOrPartial apDate = aptm.addNewAppointmentDate();
                    apDate.setFullDate(Util.calDate(ap.getAppointment_date()));
                    if (ap.getAppointment_date()==null) {
                        err.add("Error! No Appointment Date ("+j+") for Patient "+demoNo);
                    }

                    String startTime = ap.getStart_time();
                    aptm.setAppointmentTime(Util.calDate(ap.getStart_time()));
                    addOneEntry(APPOINTMENT);
                    if (UtilDateUtilities.StringToDate(startTime,"HH:mm:ss")==null) {
                        err.add("Error! No Appointment Time ("+(j+1)+") for Patient "+demoNo);
                    }

                    long dLong = (ap.getDateEndTime().getTime()-ap.getDateStartTime().getTime())/60000+1;
                    BigInteger duration = BigInteger.valueOf(dLong); //duration in minutes
                    aptm.setDuration(duration);

                    if (StringUtils.filled(ap.getStatus())) {
                        ApptStatusData asd = new ApptStatusData();
                        asd.setApptStatus(ap.getStatus());
                        String msg = null;
                        if (strEditable!=null&&strEditable.equalsIgnoreCase("yes"))
                            msg = asd.getTitle();
                        else
                            msg = getResources(request).getMessage(asd.getTitle());
                        if (StringUtils.filled(msg)) {
                            aptm.setAppointmentStatus(msg);
                        } else {
                            err.add("Error! No matching message for appointment status code: " + data);
                        }
                    }
                    if (StringUtils.filled(ap.getReason())) {
                        aptm.setAppointmentPurpose(ap.getReason());
                    }
                    if (StringUtils.filled(ap.getProviderNo())) {
                        Appointments.Provider prov = aptm.addNewProvider();

                        ProviderData appd = new ProviderData(ap.getProviderNo());
                        if (StringUtils.noNull(appd.getOhip_no()).length()<=6) prov.setOHIPPhysicianId(appd.getOhip_no());
                        Util.writeNameSimple(prov.addNewName(), appd.getFirst_name(), appd.getLast_name());
                    }
                    if (StringUtils.filled(ap.getNotes())) {
                        aptm.setAppointmentNotes(ap.getNotes());
                    }
                }
            }

            if (exReportsReceived) {
                // REPORTS RECEIVED
                ArrayList edoc_list = new EDocUtil().listDemoDocs(demoNo);
                for (int j=0; j<edoc_list.size(); j++) {
                    EDoc edoc = (EDoc)edoc_list.get(j);
                    ReportsReceived rpr = patientRec.addNewReportsReceived();
                    rpr.setFormat(cdsDt.ReportFormat.TEXT);

                    File f = new File(edoc.getFilePath());
                    if (!f.exists()) {
                        err.add("Error! Document \""+f.getName()+"\" does not exist!");
                    } else if (f.length()>Runtime.getRuntime().freeMemory()) {
                        err.add("Error! Document \""+f.getName()+"\" too big to be exported. Not enough memory!");
                    } else {
                        cdsDt.ReportContent rpc = rpr.addNewContent();
                        InputStream in = new FileInputStream(f);
                        byte[] b = new byte[(int)f.length()];

                        int offset=0, numRead=0;
                        while ((numRead=in.read(b,offset,b.length-offset)) >= 0
                               && offset < b.length) offset += numRead;

                        if (offset < b.length) throw new IOException("Could not completely read file " + f.getName());
                        in.close();
                        if (edoc.getContentType()!=null && edoc.getContentType().startsWith("text")) {
                            String str = new String(b);
                            rpc.setTextContent(str);
                            rpr.setFormat(cdsDt.ReportFormat.TEXT);
                            addOneEntry(REPORTTEXT);
                        } else {
                            rpc.setMedia(b);
                            rpr.setFormat(cdsDt.ReportFormat.BINARY);
                            addOneEntry(REPORTBINARY);
                        }

                        data = Util.mimeToExt(edoc.getContentType());
                        if (StringUtils.empty(data)) data = cutExt(edoc.getFileName());
                        if (StringUtils.empty(data)) err.add("Error! No File Extension&Version info for Document \""+edoc.getFileName()+"\"");
                        rpr.setFileExtensionAndVersion(data);

                        data = edoc.getDocClass();
                        if (StringUtils.filled(data)) {
                            rpr.setClass1(cdsDt.ReportClass.Enum.forString(data));
                        } else {
                            err.add("Error! No Class Type for Document \""+edoc.getFileName()+"\"");
                            rpr.setClass1(cdsDt.ReportClass.OTHER_LETTER);
                        }
                        data = edoc.getDocSubClass();
                        if (StringUtils.filled(data)) {
                            rpr.setSubClass(data);
                        }
                        data = edoc.getObservationDate();
                        if (UtilDateUtilities.StringToDate(data)!=null) {
                            rpr.addNewEventDateTime().setFullDate(Util.calDate(data));
                        } else {
                            err.add("Note: Not exporting invalid Event Date (Reports) for Patient "+demoNo+" ("+(j+1)+")");
                        }
                        data = edoc.getDateTimeStamp();
                        if (UtilDateUtilities.StringToDate(data,"yyyy-MM-dd HH:mm:ss")!=null) {
                            rpr.addNewReceivedDateTime().setFullDateTime(Util.calDate(data));
                        } else {
                            err.add("Note: Not exporting invalid Received DateTime (Reports) for Patient "+demoNo+" ("+(j+1)+")");
                        }
                        data = edoc.getReviewDateTime();
                        if (UtilDateUtilities.StringToDate(data,"yyyy-MM-dd HH:mm:ss")!=null) {
                            ReportsReceived.ReportReviewed reportReviewed = rpr.addNewReportReviewed();
                            reportReviewed.addNewDateTimeReportReviewed().setFullDate(Util.calDate(data));
                            Util.writeNameSimple(reportReviewed.addNewName(), edoc.getReviewerName());
                            data = StringUtils.noNull(edoc.getReviewerOhip());
                            if (data.length()<=6) reportReviewed.setReviewingOHIPPhysicianId(data);
                        }
                        Util.writeNameSimple(rpr.addNewSourceAuthorPhysician().addNewAuthorName(), edoc.getSource());

                        if (edoc.getDocId()==null) continue;
                        
                        CaseManagementNoteLink cml = cmm.getLatestLinkByTableId(CaseManagementNoteLink.DOCUMENT, Long.valueOf(edoc.getDocId()));
                        if (cml!=null) {
                            CaseManagementNote n = cmm.getNote(cml.getNoteId().toString());
                            if (n.getNote()!=null && !n.getNote().startsWith("imported.cms4.2011.06")) //not from dumpsite
                                if (n.getNote()!=null) rpr.setNotes(n.getNote());
                        }
                    }
                }

                //HRM reports
                HRMDocumentToDemographicDao hrmDocToDemographicDao = (HRMDocumentToDemographicDao) SpringUtils.getBean("HRMDocumentToDemographicDao");
                HRMDocumentDao hrmDocDao = (HRMDocumentDao) SpringUtils.getBean("HRMDocumentDao");
                HRMDocumentCommentDao hrmDocCommentDao = (HRMDocumentCommentDao) SpringUtils.getBean("HRMDocumentCommentDao");

                List<HRMDocumentToDemographic> hrmDocToDemographics = hrmDocToDemographicDao.findByDemographicNo(demoNo);
                for (HRMDocumentToDemographic hrmDocToDemographic : hrmDocToDemographics) {
                    String hrmDocumentId = hrmDocToDemographic.getHrmDocumentId();
                    List<HRMDocument> hrmDocs = hrmDocDao.findById(Integer.valueOf(hrmDocumentId));
                    for (HRMDocument hrmDoc : hrmDocs) {
                        String reportFile = hrmDoc.getReportFile();
                        if (StringUtils.empty(reportFile)) continue;

                        File hrmFile = new File(reportFile);
                        if (!hrmFile.exists()) {
                            err.add("Error! HRM report file '"+reportFile+"' not exists! HRM report not exported.");
                            continue;
                        }

                        ReadHRMFile hrm = new ReadHRMFile(reportFile);
                        for (int i=0; i<hrm.getReportsReceivedTotal(); i++) {
                            ReportsReceived rpr = patientRec.addNewReportsReceived();

                            //Message Unique ID
                            if (hrm.getTransactionMessageUniqueID(i)!=null) rpr.setMessageUniqueID(hrm.getTransactionMessageUniqueID(i));
                            
                            HashMap<String,String> reportAuthor = hrm.getReportAuthorPhysician(i);
                            HashMap<String,Object> reportContent = hrm.getReportContent(i);
                            HashMap<String,String> reportStrings = hrm.getReportStrings(i);
                            HashMap<String,Calendar> reportDates = hrm.getReportDates(i);

                            if (reportAuthor!=null) {
                                cdsDt.PersonNameSimple author = rpr.addNewSourceAuthorPhysician().addNewAuthorName();
                                author.setFirstName(reportAuthor.get("firstname"));
                                author.setLastName(reportAuthor.get("lastname"));
                            }

                            if (reportContent!=null) {
                                if (reportContent!=null) {
                                    if (reportContent.get("textcontent")!=null) {
                                        cdsDt.ReportContent content = rpr.addNewContent();
                                        content.setTextContent((String)reportContent.get("textcontent"));
                                    } else if (reportContent.get("media")!=null) {
                                        cdsDt.ReportContent content = rpr.addNewContent();
                                        content.setMedia((byte[])reportContent.get("media"));
                                    }
                                }
                            }

                            String reviewerId = null;
                            Calendar reviewDate = null;

                            if (reportStrings!=null) {
                                //Format
                                if (reportStrings.get("format")!=null) {
                                    if (reportStrings.get("format").equalsIgnoreCase("Text")) {
                                        rpr.setFormat(cdsDt.ReportFormat.TEXT);
                                    } else {
                                        rpr.setFormat(cdsDt.ReportFormat.BINARY);
                                    }
                                } else {
                                    if (rpr.getContent().getMedia()!=null) rpr.setFormat(cdsDt.ReportFormat.BINARY);
                                    else rpr.setFormat(cdsDt.ReportFormat.TEXT);
                                    err.add("Error! No Format for HRM report! Patient "+demoNo+" ("+(i+1)+")");
                                }

                                //Class
                                if (reportStrings.get("class")!=null) {
                                    rpr.setClass1(cdsDt.ReportClass.Enum.forString(reportStrings.get("class")));
                                } else {
                                    rpr.setClass1(cdsDt.ReportClass.OTHER_LETTER);
                                    err.add("Error! No Class for HRM report! Export as 'Other Letter'. Patient "+demoNo+" ("+(i+1)+")");
                                }

                                //Subclass
                                if (reportStrings.get("subclass")!=null) {
                                    rpr.setSubClass(reportStrings.get("subclass"));
                                }

                                //File extension & version
                                if (reportStrings.get("fileextension&version")!=null) {
                                    rpr.setFileExtensionAndVersion(reportStrings.get("fileextension&version"));
                                }

                                //Media
                                if (reportStrings.get("media")!=null) {
                                    rpr.setMedia(cdsDt.ReportMedia.Enum.forString(reportStrings.get("media")));
                                }

                                //HRM Result Status
                                if (reportStrings.get("resultstatus")!=null) {
                                    rpr.setHRMResultStatus(reportStrings.get("resultstatus"));
                                }

                                //Sending Facility ID
                                if (reportStrings.get("sendingfacility")!=null) {
                                    rpr.setSendingFacilityId(reportStrings.get("sendingfacility"));
                                }

                                //Sending Facility Report Number
                                if (reportStrings.get("sendingfacilityreportnumber")!=null) {
                                    rpr.setSendingFacilityReport(reportStrings.get("sendingfacilityreportnumber"));
                                }

                                //Reviewing OHIP Physician ID
                                reviewerId = reportStrings.get("reviewingohipphysicianid");
                            }

                            //report dates
                            if (reportDates!=null) {
                                if (reportDates.get("eventdatetime")!=null) {
                                    rpr.addNewEventDateTime().setFullDateTime(reportDates.get("eventdatetime"));
                                }
                                if (reportDates.get("receiveddatetime")!=null) {
                                    rpr.addNewReceivedDateTime().setFullDateTime(reportDates.get("receiveddatetime"));
                                }
                                reviewDate = reportDates.get("revieweddatetime");
                            }

                            //Source Facility
                            if (hrmDoc.getSourceFacility()!=null) {
                                rpr.setSourceFacility(hrmDoc.getSourceFacility());
                            }

                            //reviewing info
                            if (reviewerId!=null && reviewDate!=null) {
                                ReportsReceived.ReportReviewed reviewed = rpr.addNewReportReviewed();
                                reviewed.addNewName();
                                reviewed.setReviewingOHIPPhysicianId(reviewerId);
                                reviewed.addNewDateTimeReportReviewed().setFullDate(reviewDate);
                            }

                            //Notes
                            List<HRMDocumentComment> comments = hrmDocCommentDao.getCommentsForDocument(hrmDocumentId);
                            String notes = null;
                            for (HRMDocumentComment comment : comments) {
                                notes = Util.addLine(notes, comment.getComment());
                            }
                            if (StringUtils.filled(notes)) rpr.setNotes(notes);

                            //OBR Content
                            for (int j=0; j<hrm.getReportOBRContentTotal(i); j++) {
                                HashMap<String,String> obrStrings = hrm.getReportOBRStrings(i, j);
                                Calendar obrObservationDateTime = hrm.getReportOBRObservationDateTime(i, j);

                                ReportsReceived.OBRContent obrContent = rpr.addNewOBRContent();

                                if (obrStrings!=null) {

                                    //Accompanying Description
                                    if (obrStrings.get("accompanyingdescription")!=null) {
                                        obrContent.setAccompanyingDescription(obrStrings.get("accompanyingdescription"));
                                    }

                                    //Accompanying Mnemonic
                                    if (obrStrings.get("accompanyingmnemonic")!=null) {
                                        obrContent.setAccompanyingMnemonic(obrStrings.get("accompanyingmnemonic"));
                                    }

                                    //Accompanying Subclass
                                    if (obrStrings.get("accompanyingsubclass")!=null) {
                                        obrContent.setAccompanyingSubClass(obrStrings.get("accompanyingsubclass"));
                                    }
                                }

                                //OBR Observation Datetime
                                if (obrObservationDateTime!=null) {
                                    obrContent.addNewObservationDateTime().setFullDateTime(obrObservationDateTime);
                                }
                            }
                        }
                    }
                }
            }

            if (exCareElements) {
                //CARE ELEMENTS
                List<Measurements> measList = ImportExportMeasurements.getMeasurements(demoNo);
                for (Measurements meas : measList) {
                    if (meas.getType().equals("HT")) { //Height in cm
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.Height height = careElm.addNewHeight();
                        height.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Height (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        height.setHeight(meas.getDataField());
                        height.setHeightUnit(cdsDt.Height.HeightUnit.CM);
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("WT") && meas.getMeasuringInstruction().equalsIgnoreCase("in kg")) { //Weight in kg
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.Weight weight = careElm.addNewWeight();
                        weight.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Weight (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        weight.setWeight(meas.getDataField());
                        weight.setWeightUnit(cdsDt.Weight.WeightUnit.KG);
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("WAIS") || meas.getType().equals("WC")) { //Waist Circumference in cm
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.WaistCircumference waist = careElm.addNewWaistCircumference();
                        waist.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Waist Circumference (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        waist.setWaistCircumference(meas.getDataField());
                        waist.setWaistCircumferenceUnit(cdsDt.WaistCircumference.WaistCircumferenceUnit.CM);
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("BP")) { //Blood Pressure
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.BloodPressure bloodp = careElm.addNewBloodPressure();
                        bloodp.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Blood Pressure (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        String[] sdbp = meas.getDataField().split("/");
                        bloodp.setSystolicBP(sdbp[0]);
                        bloodp.setDiastolicBP(sdbp[1]);
                        bloodp.setBPUnit(cdsDt.BloodPressure.BPUnit.MM_HG);
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("POSK")) { //Packs of Cigarettes per day
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.SmokingPacks smokp = careElm.addNewSmokingPacks();
                        smokp.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Smoking Packs (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        smokp.setPerDay(new BigDecimal(meas.getDataField()));
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("SKST")) { //Smoking Status
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.SmokingStatus smoks = careElm.addNewSmokingStatus();
                        smoks.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Smoking Status (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        smoks.setStatus(Util.yn(meas.getDataField()));
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("SMBG")) { //Self Monitoring Blood Glucose
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.SelfMonitoringBloodGlucose bloodg = careElm.addNewSelfMonitoringBloodGlucose();
                        bloodg.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Self-monitoring Blood Glucose (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        bloodg.setSelfMonitoring(Util.yn(meas.getDataField()));
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("DMME")) { //Diabetes Education
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.DiabetesEducationalSelfManagement des = careElm.addNewDiabetesEducationalSelfManagement();
                        des.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Diabetes Educational Self-management (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        des.setEducationalTrainingPerformed(Util.yn(meas.getDataField()));
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("SMCD")) { //Self Management Challenges
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.DiabetesSelfManagementChallenges dsc = careElm.addNewDiabetesSelfManagementChallenges();
                        dsc.setCodeValue(cdsDt.DiabetesSelfManagementChallenges.CodeValue.X_44941_3);
                        dsc.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Diabetes Self-management Challenges (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        dsc.setChallengesIdentified(Util.yn(meas.getDataField()));
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("MCCN")) { //Motivation Counseling Completed Nutrition
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.DiabetesMotivationalCounselling dmc = careElm.addNewDiabetesMotivationalCounselling();
                        dmc.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Diabetes Motivational Counselling on Nutrition (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        dmc.setCounsellingPerformed(cdsDt.DiabetesMotivationalCounselling.CounsellingPerformed.NUTRITION);
                        if (Util.yn(meas.getDataField())==cdsDt.YnIndicatorsimple.N) {
                            err.add("Note: Patient "+demoNo+" didn't do Diabetes Counselling (Nutrition) on "+UtilDateUtilities.DateToString(meas.getDateObserved(),"yyyy-MM-dd"));
                        }
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("MCCE")) { //Motivation Counseling Completed Exercise
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.DiabetesMotivationalCounselling dmc = careElm.addNewDiabetesMotivationalCounselling();
                        dmc.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Diabetes Motivational Counselling on Exercise (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        dmc.setCounsellingPerformed(cdsDt.DiabetesMotivationalCounselling.CounsellingPerformed.EXERCISE);
                        if (Util.yn(meas.getDataField())==cdsDt.YnIndicatorsimple.N) {
                            err.add("Note: Patient "+demoNo+" didn't do Diabetes Counselling (Exercise) on "+UtilDateUtilities.DateToString(meas.getDateObserved(),"yyyy-MM-dd"));
                        }
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("MCCS")) { //Motivation Counseling Completed Smoking Cessation
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.DiabetesMotivationalCounselling dmc = careElm.addNewDiabetesMotivationalCounselling();
                        dmc.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Diabetes Motivational Counselling on Smoking Cessation (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        dmc.setCounsellingPerformed(cdsDt.DiabetesMotivationalCounselling.CounsellingPerformed.SMOKING_CESSATION);
                        if (Util.yn(meas.getDataField())==cdsDt.YnIndicatorsimple.N) {
                            err.add("Note: Patient "+demoNo+" didn't do Diabetes Counselling (Smoking Cessation) on "+UtilDateUtilities.DateToString(meas.getDateObserved(),"yyyy-MM-dd"));
                        }
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("MCCO")) { //Motivation Counseling Completed Other
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.DiabetesMotivationalCounselling dmc = careElm.addNewDiabetesMotivationalCounselling();
                        dmc.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Diabetes Motivational Counselling on Other Matters (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        dmc.setCounsellingPerformed(cdsDt.DiabetesMotivationalCounselling.CounsellingPerformed.OTHER);
                        if (Util.yn(meas.getDataField())==cdsDt.YnIndicatorsimple.N) {
                            err.add("Note: Patient "+demoNo+" didn't do Diabetes Counselling (Other) on "+UtilDateUtilities.DateToString(meas.getDateObserved(),"yyyy-MM-dd"));
                        }
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("EYEE")) { //Dilated Eye Exam
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.DiabetesComplicationScreening dcs = careElm.addNewDiabetesComplicationsScreening();
                        dcs.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Diabetes Complication Screening on Eye Exam (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        dcs.setExamCode(cdsDt.DiabetesComplicationScreening.ExamCode.X_32468_1);
                        if (Util.yn(meas.getDataField())==cdsDt.YnIndicatorsimple.N) {
                            err.add("Note: Patient "+demoNo+" didn't do Diabetes Complications Screening (Retinal Exam) on "+UtilDateUtilities.DateToString(meas.getDateObserved(),"yyyy-MM-dd"));
                        }
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("FTE")) { //Foot Exam
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.DiabetesComplicationScreening dcs = careElm.addNewDiabetesComplicationsScreening();
                        dcs.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Diabetes Complication Screening on Foot Exam (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        dcs.setExamCode(cdsDt.DiabetesComplicationScreening.ExamCode.X_11397_7);
                        if (Util.yn(meas.getDataField())==cdsDt.YnIndicatorsimple.N) {
                            err.add("Note: Patient "+demoNo+" didn't do Diabetes Complications Screening (Foot Exam) on "+UtilDateUtilities.DateToString(meas.getDateObserved(),"yyyy-MM-dd"));
                        }
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("FTLS")) { // Foot Exam Test Loss of Sensation (Neurological Exam)
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.DiabetesComplicationScreening dcs = careElm.addNewDiabetesComplicationsScreening();
                        dcs.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Diabetes Complication Screening on Neurological Exam (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        dcs.setExamCode(cdsDt.DiabetesComplicationScreening.ExamCode.NEUROLOGICAL_EXAM);
                        if (Util.yn(meas.getDataField())==cdsDt.YnIndicatorsimple.N) {
                            err.add("Note: Patient "+demoNo+" didn't do Diabetes Complications Screening (Neurological Exam) on "+UtilDateUtilities.DateToString(meas.getDateObserved(),"yyyy-MM-dd"));
                        }
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("CGSD")) { //Collaborative Goal Setting
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.DiabetesSelfManagementCollaborative dsco = careElm.addNewDiabetesSelfManagementCollaborative();
                        dsco.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Diabetes Self-management Collaborative Goal Setting (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        dsco.setCodeValue(cdsDt.DiabetesSelfManagementCollaborative.CodeValue.X_44943_9);
                        dsco.setDocumentedGoals(meas.getDataField());
                        addOneEntry(CAREELEMENTS);
                    } else if (meas.getType().equals("HYPE")) { //Hypoglycemic Episodes
                        CareElements careElm = patientRec.addNewCareElements();
                        cdsDt.HypoglycemicEpisodes he = careElm.addNewHypoglycemicEpisodes();
                        he.setDate(Util.calDate(meas.getDateObserved()));
                        if (meas.getDateObserved()==null) {
                            err.add("Error! No Date for Hypoglycemic Episodes (id="+meas.getId()+") for Patient "+demoNo);
                        }
                        he.setNumOfReportedEpisodes(new BigInteger(meas.getDataField()));
                        addOneEntry(CAREELEMENTS);
                    }
                }
            }
            exportNo++;


            //export file to temp directory
            try{
                File directory = new File(tmpDir);
                if(!directory.exists()){
                    throw new Exception("Temporary Export Directory does not exist!");
                }

                //Standard format for xml exported file : PatientFN_PatientLN_PatientUniqueID_DOB (DOB: ddmmyyyy)
                String expFile = demographic.getFirstName()+"_"+demographic.getLastName();
                expFile += "_"+demoNo;
                expFile += "_"+demographic.getDateOfBirth()+demographic.getMonthOfBirth()+demographic.getYearOfBirth();
                files.add(new File(directory, expFile+".xml"));
            }catch(Exception e){
                logger.error("Error", e);
            }
            try {
                    omdCdsDoc.save(files.get(files.size()-1), options);
            } catch (IOException ex) {logger.error("Error", ex);
                    throw new Exception("Cannot write .xml file(s) to export directory.\n Please check directory permissions.");
	    }
	}
	
	//create ReadMe.txt & ExportEvent.log
        files.add(makeReadMe(files, err));
        files.add(makeExportLog(files.get(0).getParentFile(), err));
	
	//zip all export files
        String zipName = files.get(0).getName().replace(".xml", ".zip");
	if (setName!=null) zipName = "export_"+setName.replace(" ","")+"_"+UtilDateUtilities.getToday("yyyyMMddHHmmss")+".zip";
//	if (setName!=null) zipName = "export_"+setName.replace(" ","")+"_"+UtilDateUtilities.getToday("yyyyMMddHHmmss")+".pgp";
	if (!Util.zipFiles(files, zipName, tmpDir)) {
            logger.debug("Error! Failed to zip export files");
	}

//To be un-commented after CMS4
        if (pgpReady.equals("Yes")) {
            //PGP encrypt zip file
            PGPEncrypt pgp = new PGPEncrypt();
            if (pgp.encrypt(zipName, tmpDir)) {
                Util.downloadFile(zipName+".pgp", tmpDir, response);
                Util.cleanFile(zipName+".pgp", tmpDir);
                ffwd = "success";
            } else {
                request.getSession().setAttribute("pgp_ready", "No");
            }
        } else {
            logger.debug("Warning: PGP Encryption NOT available - unencrypted file exported!");
            Util.downloadFile(zipName, tmpDir, response);
            ffwd = "success";
        }
//To be removed after CMS4
//        Util.downloadFile(zipName, tmpDir, response);
//        ffwd = "success";
//To be removed after CMS4
        

        //Remove zip & export files from temp dir
        Util.cleanFile(zipName, tmpDir);
        Util.cleanFiles(files);
    }

    return mapping.findForward(ffwd);
}

    File makeReadMe(ArrayList<File> fs, ArrayList error) throws IOException {
        OscarProperties oscarp = oscar.OscarProperties.getInstance();
	File readMe = new File(fs.get(0).getParentFile(), "ReadMe.txt");
	BufferedWriter out = new BufferedWriter(new FileWriter(readMe));
	out.write("Physician Group                    : ");
	out.write(new ClinicData().getClinicName());
	out.newLine();
	out.write("CMS Vendor, Product & Version      : ");
	String vendor = oscarp.getProperty("Vendor_Product");
	if (StringUtils.empty(vendor)) {
	    error.add("Error! Vendor_Product not defined in oscar.properties");
	} else {
	    out.write(vendor);
	}
	out.newLine();
	out.write("Application Support Contact        : ");
	String support = oscarp.getProperty("Support_Contact");
	if (StringUtils.empty(support)) {
	    error.add("Error! Support_Contact not defined in oscar.properties");
	} else {
	    out.write(support);
	}
	out.newLine();
	out.write("Date and Time stamp                : ");
	out.write(UtilDateUtilities.getToday("yyyy-MM-dd hh:mm:ss aa"));
	out.newLine();
	out.write("Total patients files extracted     : ");
	out.write(String.valueOf(fs.size()));
	out.newLine();
	out.write("Number of errors                   : ");
	out.write(String.valueOf(error.size()));
	if (error.size()>0) out.write(" (See ExportEvent.log for detail)");
	out.newLine();
	out.write("Patient ID range                   : ");
	out.write(getIDInExportFilename(fs.get(0).getName()));
	out.write("-");
	out.write(getIDInExportFilename(fs.get(fs.size()-1).getName()));
	out.newLine();
	out.close();
	
	return readMe;
    }

    File makeExportLog(File dir, ArrayList<String> error) throws IOException {
            String[][] keyword = new String[2][16];
            keyword[0][0] = PATIENTID;
            keyword[1][0] = "ID";
            keyword[0][1] = " "+PERSONALHISTORY;
            keyword[1][1] = " History";
            keyword[0][2] = " "+FAMILYHISTORY;
            keyword[1][2] = " History";
            keyword[0][3] = " "+PASTHEALTH;
            keyword[1][3] = " Health";
            keyword[0][4] = " "+PROBLEMLIST;
            keyword[1][4] = " List";
            keyword[0][5] = " "+RISKFACTOR;
            keyword[1][5] = " Factor";
            keyword[0][6] = " "+ALLERGY;
            keyword[0][7] = " "+MEDICATION;
            keyword[0][8] = " "+IMMUNIZATION;
            keyword[0][9] = " "+LABS;
            keyword[0][10] = " "+APPOINTMENT;
            keyword[0][11] = " "+CLINICALNOTE;
            keyword[1][11] = " Note";
            keyword[0][12] = "    Report    ";
            keyword[1][12] = " "+REPORTTEXT;
            keyword[1][13] = " "+REPORTBINARY;
            keyword[0][14] = " "+CAREELEMENTS;
            keyword[1][14] = " Elements";
            keyword[0][15] = " "+ALERT;

            for (int i=0; i<keyword[0].length; i++) {
                if (keyword[0][i].contains("Report")) {
                    keyword[0][i+1] = "Report2";
                    i++;
                    continue;
                }
                if (keyword[1][i]==null) keyword[1][i] = " ";
                if (keyword[0][i].length()>keyword[1][i].length()) keyword[1][i] = fillUp(keyword[1][i], ' ', keyword[0][i].length());
                if (keyword[0][i].length()<keyword[1][i].length()) keyword[0][i] = fillUp(keyword[0][i], ' ', keyword[1][i].length());
            }

            File exportLog = new File(dir, "ExportEvent.log");
            BufferedWriter out = new BufferedWriter(new FileWriter(exportLog));
            int tableWidth = 0;
            for (int i=0; i<keyword.length; i++) {
                for (int j=0; j<keyword[i].length; j++) {
                    out.write(keyword[i][j]+" |");
                    if (keyword[i][j].trim().equals("Report")) j++;
                    if (i==1) tableWidth += keyword[i][j].length()+2;
                }
                out.newLine();
            }
            out.write(fillUp("",'-',tableWidth)); out.newLine();

            //general log data
            if (exportNo==0) exportNo = 1;
            for (int i=0; i<exportNo; i++) {

                for (int j=0; j<keyword[0].length; j++) {
                    String category = keyword[0][j].trim();
                    if (category.contains("Report")) category = keyword[1][j].trim();
                    Integer occurs = entries.get(category+i);
                    if (occurs==null) occurs = 0;
                    out.write(fillUp(occurs.toString(), ' ', keyword[1][j].length()));
                    out.write(" |");
                }
                out.newLine();
                out.write(fillUp("",'-',tableWidth)); out.newLine();
            }
            out.newLine();
            out.newLine();
            out.newLine();

            //error log
            out.write("Errors/Notes");
            out.newLine();
            out.write(fillUp("",'-',tableWidth)); out.newLine();
            
            //write any error that has occurred
            if (error.size()>0) {
                out.write(error.get(0));
                out.newLine();
                for (int j=1; j<error.size(); j++) {
                    out.write("     ");
                    out.write(error.get(j));
                    out.newLine();
                }
                out.write(fillUp("",'-',tableWidth)); out.newLine();
            }
            out.write(fillUp("",'-',tableWidth)); out.newLine();

            out.close();
            exportNo = 0;
            return exportLog;
    }

/*
    File makeExportLog(ArrayList<File> fs, ArrayList<String> error) throws IOException {
	String[] keyword = new String[13];
	keyword[0] = "Demographics";
	keyword[1] = "PersonalHistory";
	keyword[2] = "FamilyHistory";
	keyword[3] = "PastHealth";
	keyword[4] = "ProblemList";
	keyword[5] = "RiskFactors";
	keyword[6] = "AllergiesAndAdverseReactions";
	keyword[7] = "MedicationsAndTreatments";
	keyword[8] = "Immunizations";
	keyword[9] = "LaboratoryResults";
	keyword[10] = "Appointments";
	keyword[11] = "ClinicalNotes";
	keyword[12] = "ReportsReceived";
	int[] content = new int[keyword.length];
	String patientID = "Patient ID";
	String totalByte = "Total Bytes";
	String field = null;
	File exportLog = new File(fs.get(0).getParentFile(), "ExportEvent.log");
	BufferedWriter out = new BufferedWriter(new FileWriter(exportLog));
	
	int tableWidth = patientID.length() + totalByte.length() + 5; //add 3+2 for left & right + PatientID delimiters
	for (int i=0; i<keyword.length; i++) tableWidth += keyword[i].length()+2; //add 3 for delimitors
	out.newLine();
	out.write(fillUp("",'-',tableWidth));
	out.newLine();
	out.write("|"+patientID+" |");
	for (int i=0; i<keyword.length; i++) out.write(keyword[i]+" |");
	out.write(totalByte+" |");
	out.newLine();
	out.write(fillUp("",'-',tableWidth));
	out.newLine();

        for (File f : fs) {
	    field = getIDInExportFilename(f.getName()); //field=PatientID
            if (field==null) continue;

	    content = countByte(f, keyword);
	    out.write("|");
	    out.write(fillUp(field,' ',patientID.length()));
	    out.write(" |");
	    int total=0;
	    for (int j=0; j<content.length; j++) {
		field = "" + content[j];   //field = data size matching each keyword
		total += Integer.parseInt(field);
		out.write(fillUp(field,' ',keyword[j].length()));
		out.write(" |");
	    }

	    out.write(fillUp(String.valueOf(total),' ',totalByte.length()));
	    out.write(" |");
	    out.newLine();
	}
	out.write(fillUp("",'-',tableWidth));
	out.newLine();
	
	//write any error that has occurred
	for (int i=0; i<error.size(); i++) {
	    out.newLine();
	    out.write(error.get(i));
	}
	out.newLine();
	out.close();
	
	return exportLog;
    }
 *
 */


    //------------------------------------------------------------
    
    private String getIDInExportFilename(String filename) {
        if (filename==null) return null;

        //PatientFN_PatientLN_PatientUniqueID_DOB
        String[] sects = filename.split("_");
        if (sects.length==4) return sects[2];

        return null;
    }

    private int[] countByte(File fin, String[] kwd) throws FileNotFoundException, IOException {
	int[] cat_cnt = new int[kwd.length];
	String[] tag = new String[kwd.length];
	
	FileInputStream fis = new FileInputStream(fin);
	BufferedInputStream bis = new BufferedInputStream(fis);
	DataInputStream dis = new DataInputStream(bis);
	
	int cnt=0, tag_in_list=0;
	boolean tag_fnd=false;
	
	while (dis.available()!=0) {
	    if (!tag_fnd) {   //looking for a start tag
		if ((char)dis.read()=='<') {   //a possible tag
		    boolean whole_tag=false;
		    
		    //retrieve the whole tag word
		    String tag_word = "";
		    while (dis.available()!=0 && !whole_tag) {
			String tmp = "" + (char)dis.read();
			if (tmp.equals(">")) {
			    whole_tag = true;
			} else {
			    tag_word += tmp;
			}
		    }
		    
		    //compare the tag word with the list
		    for (int i=0; i<kwd.length; i++) {
			if (tag_word.equals("cds:"+kwd[i])) {
			    tag_in_list = i;
			    tag_fnd = true;
			    cnt = kwd[i].length() +1 +4 +1;   //byte count +"<" +"cds:" +">"
			}
		    }
		}
	    } else {   //a start tag was found, counting...
		//look for an end tag
		if ((char)dis.read()=='<') {   //a possible tag
		    if ((char)dis.read()=='/') {   //a possible end tag
			boolean whole_tag=false;

			//retrieve the whole tag word
			String tag_word = "";
			while (dis.available()!=0 & !whole_tag) {
			    String tmp = "" + (char)dis.read();
			    if (tmp.equals(">")) {
				whole_tag = true;
			    } else {
				tag_word += tmp;
			    }
			    cnt++;
			}
			
			//compare tag word with the start tag - if matched, stop counting
			if (tag_word.equals("cds:"+kwd[tag_in_list])) {
			    tag_fnd = false;
			    cat_cnt[tag_in_list] += cnt;
			    cnt = 0;
			}
		    }
		    cnt++;
		}
		cnt++;
	    }
	}
	fis.close();
	bis.close();
	dis.close();
	
	return cat_cnt;
    }
    
    private String cutExt(String filename) {
	if (StringUtils.empty(filename)) return "";
	String[] parts = filename.split(".");
	if (parts.length>1) return "."+parts[parts.length-1];
	else return "";
    }
    
    private String fillUp(String tobefilled, char c, int size) {
	if (size>=tobefilled.length()) {
	    int fill = size-tobefilled.length();
	    for (int i=0; i<fill; i++) tobefilled += c;
	}
	return tobefilled;
    }
    
    private void addOneEntry(String category) {
        if (StringUtils.empty(category)) return;

        Integer n = entries.get(category+exportNo);
        n = n==null ? 1 : n+1;
        entries.put(category+exportNo, n);
    }

}

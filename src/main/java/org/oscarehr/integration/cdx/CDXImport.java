/**
 * Copyright (c) 2013-2015. Department of Computer Science, University of Victoria. All Rights Reserved.
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
 * Department of Computer Science
 * LeadLab
 * University of Victoria
 * Victoria, Canada
 */

package org.oscarehr.integration.cdx;

import ca.uvic.leadlab.obibconnector.facades.support.ISupport;
import ca.uvic.leadlab.obibconnector.facades.receive.*;
import ca.uvic.leadlab.obibconnector.facades.registry.IProvider;
import ca.uvic.leadlab.obibconnector.impl.receive.ReceiveDoc;
import ca.uvic.leadlab.obibconnector.impl.receive.SearchDoc;
import ca.uvic.leadlab.obibconnector.impl.support.Support;
import org.oscarehr.PMmodule.dao.ProviderDao;
import org.oscarehr.common.dao.*;
import org.oscarehr.common.model.*;
import org.oscarehr.integration.cdx.dao.CdxAttachmentDao;
import org.oscarehr.integration.cdx.dao.CdxPendingDocsDao;
import org.oscarehr.integration.cdx.dao.CdxProvenanceDao;
import org.oscarehr.integration.cdx.model.CdxPendingDoc;
import org.oscarehr.integration.cdx.model.CdxProvenance;
import org.oscarehr.util.MiscUtils;
import oscar.oscarLab.ca.all.upload.ProviderLabRouting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.oscarehr.util.SpringUtils.getBean;

public class CDXImport {

    private IReceiveDoc receiver;
    private ISearchDoc docSearcher;
    private CDXConfiguration cdxConfig;
    private ISupport support;
    private CdxPendingDocsDao pendDocDao;
    private DocumentDao     docDao;
    private ProviderDao providerDao;
    private CdxProvenanceDao provDao;
    private ProviderLabRoutingDao plrDao;
    private CdxAttachmentDao atDao;
    private DemographicDao demoDao;
    private CtlDocumentDao ctlDocDao;
    private PatientLabRoutingDao patientLabRoutingDao;

    public CDXImport() {

        cdxConfig = new CDXConfiguration();

        receiver = new ReceiveDoc(cdxConfig);
        docSearcher = new SearchDoc(cdxConfig);

        support = new Support(cdxConfig);
        pendDocDao = getBean(CdxPendingDocsDao.class);
        docDao = getBean(DocumentDao.class);
        providerDao = getBean(ProviderDao.class);
        provDao = getBean(CdxProvenanceDao.class);
        plrDao = getBean(ProviderLabRoutingDao.class);
        atDao = getBean(CdxAttachmentDao.class);
        demoDao = getBean(DemographicDao.class);
        ctlDocDao = getBean(CtlDocumentDao.class);
        patientLabRoutingDao = getBean(PatientLabRoutingDao.class);


    }


    public void importNewDocs() throws Exception {

        List<String> docIds;

        docIds = receiver.pollNewDocIDs();

        MiscUtils.getLogger().info("CDX Import: " + docIds.size() + " new messages to import" );

        importDocuments(docIds);

    }


    public void importDocuments(List<String> msgIds) {

        for (String id : msgIds)
            try {

                MiscUtils.getLogger().info("CDX Import: importing message " + id );
                IDocument doc = receiver.retrieveDocument(id);
                MiscUtils.getLogger().info("     with " + doc.getAttachments().size() + " attachments");
                storeDocument(doc, id);

            } catch (Exception e) {
                MiscUtils.getLogger().error("Error importing CDX message " + id, e);

                //undo import

                List<CdxProvenance> provs = provDao.findByMsgId(id);

                if (!provs.isEmpty()) {
                    CdxProvenance prov = provs.get(0);
                    int docNo = prov.getDocumentNo();

                    docDao.deleteDocument(docNo);
                    ctlDocDao.deleteDocument(docNo);
                    atDao.deleteAttachments(prov.getId());
                    provDao.merge(prov);
                    provDao.removeProv(id);
                }

                if (pendDocDao.findPendingDocs(id).isEmpty()) {
                    CdxPendingDoc pd = new CdxPendingDoc();
                    pd.setDocId(id);
                    pd.setReasonCode(CdxPendingDoc.error);
                    pd.setExplanation(e.toString());
                    pd.setTimestamp(new Date());
                    pendDocDao.persist(pd);
                }

                try {
                    support.notifyError("Error importing CDX document", e.toString());
                } catch (Exception e2) {
                    MiscUtils.getLogger().error("Could not communicate CDX Error to OBIB support channel", e2);
                }
            }
    }


    public IDocument retrieveDocument(String msgId) {
        IDocument result = null;
        try {

            MiscUtils.getLogger().info("Retrieving CDX document " + msgId );

            result = receiver.retrieveDocument(msgId);

        } catch (Exception e) {
            MiscUtils.getLogger().error("Error retrieving CDX message " + msgId, e);
        }
        return result;
    }

    public void storeDocument(IDocument doc, String msgId) {
        CdxProvenance prov = new CdxProvenance();
        Document    inboxDoc = null;
        String  warnings = "";

        List<CdxProvenance> versions = provDao.findReceivedVersionsOrderDesc(doc.getSetId());

        prov.populate(doc);

        if (versions.isEmpty()) // brand new document
            inboxDoc = createInboxData(doc);
        else { // another version of existing document
            CdxProvenance newestExistingVersion = versions.get(0);
            if (newestExistingVersion.getEffectiveTime().before(doc.getEffectiveTime())) {
                inboxDoc = reviseInboxData(doc, newestExistingVersion.getDocumentNo());
//                if (prov.getVersion() <= newestExistingVersion.getVersion())
//                    prov.setVersion(newestExistingVersion.getVersion() + 1);
            }

        }


        if (inboxDoc != null) {
            warnings = generateWarningsIfDemographicInconsistency(inboxDoc, doc);
            prov.setDocumentNo(inboxDoc.getDocumentNo());
        }
        prov.setWarnings(warnings);
        prov.setAction("import");
        prov.setMsgId(msgId);
        provDao.persist(prov);
        atDao.saveAttachments(doc, prov);

        List<CdxPendingDoc> pendingDocs = pendDocDao.findPendingDocs(msgId);

        if (!pendingDocs.isEmpty()) {
            pendDocDao.removePendDoc(pendingDocs.get(0).getDocId());
        }

        provDao.merge(prov);
    }


    private String generateWarningsIfDemographicInconsistency (Document inboxDoc, IDocument doc) {

        String warnings = "";

        try {
            PatientLabRouting patientLabRouting = patientLabRoutingDao.findByLabNo(inboxDoc.getDocumentNo());
            if (patientLabRouting != null) { // was the patient successfully matched?
                Demographic d = demoDao.getDemographic(Integer.toString(patientLabRouting.getDemographicNo()));
                IPatient p = doc.getPatient();

                String dAddress = (d.getAddress() == null ? "" : d.getAddress());
                String pAddress = (p.getStreetAddress() == null ? "" : p.getStreetAddress());

                if (!dAddress.toUpperCase().equals(pAddress.toUpperCase())) warnings = "<p>The <strong>street address</strong> in the patient's master file does not agree with the one in this document.</p>";

                String dPostal = (d.getPostal() == null ? "" : d.getPostal());
                String pPostal = (p.getPostalCode() == null ? "" : p.getPostalCode());

                if (!dPostal.toUpperCase().equals(pPostal.toUpperCase())) warnings += "<p>The <strong>postal code</strong> in the patient's master file does not agree with the one in this document.</p>";

                String dCity = (d.getCity() == null ? "" : d.getCity());
                String pCity = (p.getCity() == null ? "" : p.getCity());

                if (!dCity.toUpperCase().equals(pCity.toUpperCase())) warnings += "<p>The <strong>city</strong> in " +
                        "the patient's master file does not agree with the one in this document.</p>";

                String dFirstName = (d.getFirstName() == null ? "" : d.getFirstName());
                String pFirstName = (p.getFirstName() == null ? "" : p.getFirstName());

                if (!dFirstName.toUpperCase().equals(pFirstName.toUpperCase())) warnings += "<p>The <strong>first " +
                        "name</strong> in the patient's master file does not agree with the one in this document.</p>";

                Boolean newTelco = false;

                for (ITelco t : p.getPhones()) {
                    if (!(t.getAddress().equals(d.getPhone()) || t.getAddress().equals(d.getPhone2()))) newTelco = true;
                }

                if (newTelco) warnings += "<p>The CDX document contains <strong>phone numbers</strong> for the patient that are not in the database.</p>";

                newTelco = false;

                for (ITelco t : p.getEmails()) {
                    if (!t.getAddress().equals(d.getEmail())) newTelco = true;
                }

                if (newTelco) warnings += "<p>The CDX document contains <strong>email addresses</strong> for the patient that are not in the database.</p>";
            }

        } catch (Exception e) {
            MiscUtils.getLogger().error("Demographics consistency check failed (not fatal)", e);
        };
        return warnings;
    }


    private Document reviseInboxData(IDocument doc, int inboxDocId) {

        Document        existingDocEntity = docDao.getDocument(Integer.toString(inboxDocId));
        Document        newDocEntity = new Document();

        populateInboxDocument(doc, newDocEntity);
        copyPreviousRoutingAndResetStati(newDocEntity, existingDocEntity);
        existingDocEntity.setStatus(Document.STATUS_DELETED);
        docDao.merge(existingDocEntity);
        return newDocEntity;
    }

    private void copyPreviousRoutingAndResetStati(Document newDocEntity, Document existingDocEntity) {
        for (ProviderLabRoutingModel plr : plrDao.getProviderLabRoutingForLabAndType(existingDocEntity.getDocumentNo(), "DOC")) {
            ProviderLabRoutingModel plrNew = new ProviderLabRoutingModel();
            plrNew.setProviderNo(plr.getProviderNo());
            plrNew.setStatus("N");
            plrNew.setLabType(plr.getLabType());
            plrNew.setLabNo(newDocEntity.getDocumentNo());
        }
    }





    private Document createInboxData(IDocument doc) {
        Document        docEntity = new Document();
        populateInboxDocument(doc, docEntity);

        return docEntity;
    }

    private void populateInboxDocument(IDocument doc, Document docEntity) {
        IProvider p;
        docEntity.setDoctype(translateCdxCodeToDocType(doc.getLoincCodeDisplayName()));
        docEntity.setDocdesc(doc.getLoincCodeDisplayName());
        docEntity.setDocfilename("N/A");
        docEntity.setDoccreator(doc.getCustodianName());
        docEntity.setNumberofpages(0);


        p = doc.getOrderingProvider();

        if (p != null)
            docEntity.setResponsible(p.getFirstName() + " " + p.getLastName());
        else docEntity.setResponsible("");

        docEntity.setAbnormal(0);


        docEntity.setDocClass(doc.getLoincCodeDisplayName());
        docEntity.setDocxml("stored in CDX provenance table");
        docEntity.setDocfilename("CDX");
        docEntity.setContenttype("CDX");
        docEntity.setRestrictToProgram(false); // need to confirm semantics

        IProvider auth = doc.getAuthor();

        docEntity.setSource(auth != null ? auth.getLastName() : "");
        docEntity.setUpdatedatetime(doc.getAuthoringTime());
        docEntity.setStatus(Document.STATUS_ACTIVE);
        docEntity.setReportStatus(doc.getStatusCode().code);

        if (doc.getObservationDate() != null) {
            docEntity.setObservationdate(doc.getObservationDate());
        } else if (doc.getAuthoringTime() != null) {
            docEntity.setObservationdate(doc.getAuthoringTime());
        } else if (doc.getEffectiveTime() != null) {
            docEntity.setObservationdate(doc.getEffectiveTime());
        } else {
            docEntity.setObservationdate(new Date());

        }

        if (doc.getCustodianName() != null)
            docEntity.setSourceFacility(doc.getCustodianName());

        docEntity.setContentdatetime(doc.getAuthoringTime());
        docDao.persist(docEntity);

        addPatient(docEntity, doc.getPatient());

        for (IProvider q : doc.getPrimaryRecipients()) {
            addProviderRouting(docEntity, q);
        }

        for (IProvider q : doc.getSecondaryRecipients()) {
            addProviderRouting(docEntity, q);
        }

        if (!routed(docEntity)) { // even if none of the recipients appears to work at our clinic, we will route to the default provider
            addDefaultProviderRouting(docEntity);
        }
    }

    private String translateCdxCodeToDocType(String loincCodeDisplayName) {

        switch (loincCodeDisplayName.toLowerCase()) {
            case "consult note" :
            case "mental health consult note" :
            case "cardiovascular disease consult note":
            case "nephrology consult note" :
            case "psychiatry consult note":
                return "consult";
            case "general lab report" :
            case "microbiology lab report":
            case "transfusion medicine lab report":
            case "ekg":
                return "lab";
            case "diagnostic imaging study" :
            case "breast mammogram":
                return "radiology";
            case "anatomic pathology report" :
                return "pathology";
            case "discharge summary" :
                return "discharge summary";
            case "admission history and physical note" :
            case "procedure note" :
            case "progress note" :
            case "history and physical note" :
            case "outpatient surgical operation note" :
            case "surgical operation note" :
            case "emergency department note":
            case "telephone encounter note":
            case "outpatient note":
            case "orthopedics note":
            case "respiratory therapy note":
                return "note";
            case "ereferral note" :
                return "e-referral note";
            case "hospital admission notification note" :
                return "general purpose notification";
            case "labour and delivery summary":
                return "patient summary";
            default :
                return "others";
        }
    }

    private boolean routed(Document docEntity) {
        ProviderLabRoutingModel plr = plrDao.findByLabNoAndLabType(docEntity.getDocumentNo(), "DOC");
        return plr != null;
    }


    private void addPatient(Document docEntity, IPatient patient) {

        try {

            Demographic matchedDemo = null;


            // implement 4 point matching as required by CDX conformance spec


            List<Demographic> demos = demoDao.getDemographicsByHealthNum(patient.getID());

            if (demos.size() == 1) {
                Demographic demo = demos.get(0);
                if (demo.getLastName().toUpperCase().equals(patient.getLastName().toUpperCase())) {

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Date d = sdf.parse(demo.getFormattedDob());

                        Date d2 = patient.getBirthdate();

                        if (sameDates(d, d2) && (patient.getGender() != null)) {

                            if (patient.getGender().label.equals(demo.getSex())) {

                                matchedDemo = demo; // we found the patient
                            }
                        }
                    } catch (ParseException e) {
                        MiscUtils.getLogger().error("Error", e);
                    }
                }
            }


            CtlDocument ctlDoc = new CtlDocument();

            ctlDoc.getId().setDocumentNo(docEntity.getDocumentNo());
            ctlDoc.getId().setModule("demographic");
            ctlDoc.getId().setModuleId((matchedDemo == null ? -1 : matchedDemo.getId()));
            ctlDoc.setStatus("A");
            ctlDocDao.persist(ctlDoc);

            if (matchedDemo != null) {

                PatientLabRouting patientLabRouting = new PatientLabRouting();

                patientLabRouting.setLabNo(docEntity.getDocumentNo());
                patientLabRouting.setLabType("DOC");
                patientLabRouting.setDemographicNo(matchedDemo.getDemographicNo());
                patientLabRoutingDao.persist(patientLabRouting);
            }

            // route to MRP if existent
            if (matchedDemo != null && matchedDemo.getProvider() != null) {
                ProviderLabRoutingModel plr = new ProviderLabRoutingModel();
                plr.setLabNo(docEntity.getDocumentNo());
                plr.setStatus("N");
                plr.setLabType("DOC");
                plr.setProviderNo(matchedDemo.getProvider().getProviderNo());
                plrDao.persist(plr);
            }
        } catch (Exception e) {
            MiscUtils.getLogger().error("Patient linking with CDX document failed (not fatal)", e);
        }
    }

    private void addProviderRouting(Document docEntity, IProvider prov) {

        Provider provEntity = null;

        try {
            provEntity = providerDao.getProviderByOhipNo(prov.getID());
        } catch (Exception e) {
            MiscUtils.getLogger().info("Provider in CDX document does not have valid ID");
        }

        if (provEntity != null) {
            ProviderLabRouting router = new ProviderLabRouting();
            router.routeMagic(docEntity.getDocumentNo(), provEntity.getProviderNo(), "DOC");
        }
    }

    private void addDefaultProviderRouting(Document docEntity) {

        ProviderLabRoutingDao plrDao = getBean(ProviderLabRoutingDao.class);
        ProviderLabRoutingModel plr = new ProviderLabRoutingModel();

        plr.setLabNo(docEntity.getDocumentNo());
        plr.setStatus("N");
        plr.setLabType("DOC");
        plr.setProviderNo("0");
        plrDao.persist(plr);
    }




    public static boolean sameDates(Date a, Date b) {
        if (a.getDate() == b.getDate() &&
                a.getMonth() == b.getMonth() &&
                a.getYear() == b.getYear())
            return true;
        else return false;
    }
}
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
package org.oscarehr.PMmodule.dao;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.oscarehr.PMmodule.wlmatch.CriteriaBO;
import org.oscarehr.PMmodule.wlmatch.CriteriasBO;
import org.oscarehr.PMmodule.wlmatch.MatchBO;
import org.oscarehr.PMmodule.wlmatch.VacancyDisplayBO;
import org.oscarehr.common.model.EFormData;
import org.oscarehr.match.client.ClientData;
import org.oscarehr.match.vacancy.VacancyData;
import org.oscarehr.match.vacancy.VacancyTemplateData;
import org.springframework.stereotype.Repository;

@Repository
public class WaitlistDao {

	@PersistenceContext
	protected EntityManager entityManager = null;
	
  
	public List<MatchBO> getClientMatches(int vacancyId) {
		String sql = "SELECT client_id, first_name, last_name, DATEDIFF(CURDATE(), e.form_date) days_in_waitlist, " +
				"DATEDIFF(CURDATE(), last_contact_date) last_contact_days, form_id, match_percent "
				+ " FROM vacancy_client_match m, demographic  d, eform_data e WHERE vacancy_id = ?1  " +
						"and d.demographic_no = m.client_id and m.form_id = e.fdid"
				+ " order by match_percent desc";
		
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, vacancyId);
		
		List<MatchBO> list = new ArrayList<MatchBO>();
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();
		for (Object[] cols : results) {
			MatchBO out = new MatchBO();
			out.setClientID((Integer)cols[0]);
			out.setClientName((String)cols[1]);
			out.setClientName(out.getClientName() + " " + (String)cols[2]);
			
			out.setDaysInWaitList(((Long)cols[3]).intValue());
			out.setDaysSinceLastContact(((Long)cols[4]).intValue());
			out.setFormDataID((Integer)cols[5]);
			out.setPercentageMatch((Double)cols[6]);
			list.add(out);
		}
		return list;
	}
	
	public Collection<EFormData> searchForMatchingEforms(CriteriasBO crits){
		Map<Integer,EFormData> forms=new HashMap<Integer,EFormData>();
		
		String sql="SELECT fdid, demographic_no, form_date FROM eform_data ef WHERE 1=1\n";
		
		for(CriteriaBO crit:crits.crits){
			if(crit.value!=null){
				sql=sql+" AND ef.fdid IN (select fdid from eform_values where var_name=?1 and var_value=?2)\n";
			}else if(crit.rangeStart!=null){
				sql=sql+" AND ef.fdid IN (select fdid from eform_values where var_name=?1 and (var_value BETWEEN ?2 and ?3))\n";
			}else if(crit.values!=null){
				sql=sql+" AND ef.fdid IN (select fdid from eform_values where var_name=?1 and (";
				for(int i=0;i<crit.values.length;i++){
					if(i>0) sql=sql+" OR ";
					sql=sql+"(CONCAT(',',var_value,',') LIKE CONCAT('%,',?" + (i+2)+",',%'))";
				}
				sql=sql+"))\n";
			}
		}
		Query query = entityManager.createNativeQuery(sql);
		int c=1;
		for(CriteriaBO crit:crits.crits){
			query.setParameter(c++, crit.field);
			if(crit.value!=null){
				query.setParameter(c++, crit.value);
			}else if(crit.rangeStart!=null){
				query.setParameter(c++, crit.rangeStart);
				query.setParameter(c++, crit.rangeEnd);
			}else if(crit.values!=null){
				for(int i=0;i<crit.values.length;i++){
					query.setParameter(c++, crit.values[i]);
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		List<Object[]> results = query.getResultList();
		for (Object[] cols : results) {
			EFormData f = new EFormData();
			f.setId((Integer)cols[0]);
			f.setDemographicId((Integer)cols[1]);
			f.setFormDate((Timestamp)cols[2]);
			EFormData prior=forms.get(f.getId());
			if(prior==null || prior.getFormDate().getTime()<f.getFormDate().getTime()){
				forms.put(f.getId(), f);
			}
		}
		return forms.values();
	}

	
	public List<VacancyDisplayBO> listDisplayVacanciesForWaitListProgram(int programID) {
		List<VacancyDisplayBO> bos = new ArrayList<VacancyDisplayBO>();
		String queryString = "SELECT v.id, t.NAME, v.dateCreated FROM vacancy v JOIN vacancy_template t ON " 
			+ "v.templateId=t.TEMPLATE_ID WHERE t.WL_PROGRAM_ID=?1 and v.status=?2";
		
		Query query = entityManager.createNativeQuery(queryString);
		query.setParameter(1, programID);
		query.setParameter(2, "active");
		
		@SuppressWarnings("unchecked")
		List<Object[]> results = query.getResultList();
		for (Object[] cols : results) {
			VacancyDisplayBO bo = new VacancyDisplayBO();
			bo.setVacancyID((Integer)cols[0]);
			bo.setVacancyTemplateName((String)cols[1]);
			bo.setCreated((Date)cols[2]);
			bos.add(bo);
        }
		return bos;
	}
	
	 
	public List<VacancyDisplayBO> listDisplayVacanciesForAllWaitListPrograms() {
		List<VacancyDisplayBO> bos = new ArrayList<VacancyDisplayBO>();
		String queryString = "SELECT v.id, t.NAME, v.dateCreated FROM vacancy v JOIN vacancy_template t ON " + 
				"v.templateId=t.TEMPLATE_ID WHERE  v.status=?1 order by v.id asc";
		
		Query query = entityManager.createNativeQuery(queryString);
		query.setParameter(1, "active");
		
		@SuppressWarnings("unchecked")
		List<Object[]> results = query.getResultList();
		for (Object[] cols : results) {
			VacancyDisplayBO bo = new VacancyDisplayBO();
			bo.setVacancyID((Integer)cols[0]);
			bo.setVacancyTemplateName((String)cols[1]);
			bo.setCreated((Date)cols[2]);
			bos.add(bo);
        }
		
		return bos;
	}

	public List<VacancyDisplayBO> getDisplayVacanciesForAgencyProgram(int programID) {
		List<VacancyDisplayBO> bos = new ArrayList<VacancyDisplayBO>();
		String queryString = "SELECT v.id, t.NAME, v.dateCreated FROM vacancy v JOIN vacancy_template t ON" + 
			" v.templateId=t.TEMPLATE_ID WHERE t.PROGRAM_ID=?1 and v.status=?2";
		
		Query query = entityManager.createNativeQuery(queryString);
		query.setParameter(1, programID);
		query.setParameter(2, "active");
		
		@SuppressWarnings("unchecked")
		List<Object[]> results = query.getResultList();
		for (Object[] cols : results) {
			VacancyDisplayBO bo = new VacancyDisplayBO();
			bo.setVacancyID((Integer)cols[0]);
			bo.setVacancyTemplateName((String)cols[1]);
			bo.setCreated((Date)cols[2]);
			bos.add(bo);
        }
		
		return bos;
	}

	public VacancyDisplayBO getDisplayVacancy(int vacancyID) {
		VacancyDisplayBO bo = new VacancyDisplayBO();
		String queryString = "SELECT v.vacancyName, t.NAME, " +
				"v.dateCreated FROM vacancy v JOIN vacancy_template t ON v.templateId=t.TEMPLATE_ID WHERE v.id=?1";
		
		Query query = entityManager.createNativeQuery(queryString);
		query.setParameter(1, vacancyID);
		
		@SuppressWarnings("unchecked")
		List<Object[]> rows = query.getResultList();
		if(rows.size()==0) return null;
		Object[] cols = rows.get(0);
		if(cols != null){
			bo.setVacancyName((String)cols[0]);
			bo.setVacancyTemplateName((String)cols[1]);
			bo.setCreated((Date)cols[2]);
		}
		loadStats(bo);
		return bo;
	}

	public void loadStats(VacancyDisplayBO bo) {
		String queryStr = "SELECT " + "(SELECT COUNT(*) FROM vacancy_client_match WHERE vacancy_id=?1 and status='REJECTED') " +
				"as rejected, " + "(SELECT COUNT(*) FROM vacancy_client_match WHERE vacancy_id=?2 and status='ACCEPTED') as accepted, " 
				+ "(SELECT COUNT(*) FROM vacancy_client_match WHERE vacancy_id=?3 and status='PENDING') as pending";
		Query query = entityManager.createNativeQuery(queryStr);
		query.setParameter(1, bo.getVacancyID());
		query.setParameter(2, bo.getVacancyID());
		query.setParameter(3, bo.getVacancyID());
		
		@SuppressWarnings("unchecked")
		List<Object[]> rows = query.getResultList();
		if(rows.size()>0) {
			Object[] result = rows.get(0);
			if(result != null){
				bo.setRejectedCount(((BigInteger)result[0]).intValue());
				bo.setAcceptedCount(((BigInteger)result[1]).intValue());
				bo.setPendingCount(((BigInteger)result[2]).intValue());
			}
		}
	}

	public Integer getProgramIdByVacancyId(int vacancyId) {
		List<Integer> programIds = new ArrayList<Integer>();
		String queryString = "SELECT t.PROGRAM_ID FROM vacancy v JOIN vacancy_template t ON " + "v.templateId=t.TEMPLATE_ID WHERE v.id=?1";
		Query query = entityManager.createNativeQuery(queryString);
		query.setParameter(1, vacancyId);
		
		@SuppressWarnings("unchecked")
		List<Integer> rows = query.getResultList();
		if(rows.size()==0) return null;
		Integer result = rows.get(0);
		
		if(result != null){
			programIds.add(result);
		}
		
		if (!programIds.isEmpty()) {
			return programIds.get(0);
		} else {
			return null;
		}
	}
	    


	public List<VacancyDisplayBO> listNoOfVacanciesForWaitListProgram() {
		final List<VacancyDisplayBO> bos = new ArrayList<VacancyDisplayBO>();
		String queryStr = "SELECT p.id , COUNT(*) vacncyCnt,v.vacancyName,v.dateCreated,v.id FROM vacancy v JOIN program p ON " 
			+ "v.wlProgramId=p.id where v.status=?1 GROUP by v.wlProgramId";
		Query query = entityManager.createNativeQuery(queryStr);
		query.setParameter(1, "active");
		
		@SuppressWarnings("unchecked")
		List<Object[]> results = query.getResultList();
		for (Object[] cols : results) {
            VacancyDisplayBO bo = new VacancyDisplayBO();
            bo.setProgramId((Integer)cols[0]);
            bo.setNoOfVacancy(((BigInteger)cols[1]).intValue());
            bo.setVacancyName((String)cols[2]);
            bo.setCreated((java.util.Date)cols[3]);
            bo.setVacancyID((Integer)cols[4]);
			bos.add(bo);
        }
		
		return bos;
	}

	private static final String QUERY_ALL_CLIENT_DATA = "SELECT demographic_no, fdid, var_name, var_value "
			+ "FROM eform_values LEFT JOIN client_referral cr ON cr.client_id=demographic_no, " 
			+ "(SELECT demographic_no AS dmb,MAX(fdid) AS ffdid FROM eform_values GROUP BY demographic_no) xyz " 
			+ "WHERE cr.referral_id IS NULL AND " + "demographic_no= xyz.dmb AND fdid=xyz.ffdid";

		
	private static final String QUERY_GET_CLIENT_DATA = "SELECT demographic_no, fdid, var_name, var_value "
			+ "FROM eform_values LEFT JOIN client_referral cr ON cr.client_id=demographic_no, "
			+ "(SELECT demographic_no AS dmb,MAX(fdid) AS ffdid FROM eform_values WHERE demographic_no=? "
			+ "GROUP BY demographic_no) xyz WHERE cr.referral_id IS NULL AND demographic_no= ?1 AND fdid=xyz.ffdid";


	
		
	public List<ClientData> getAllClientsData() {
		Map<Integer, ClientData> clientsDataList = new HashMap<Integer, ClientData>();
		
		Query query = entityManager.createNativeQuery(QUERY_ALL_CLIENT_DATA);
		
		@SuppressWarnings("unchecked")
		List<Object[]>result = query.getResultList();
		for (Object[] cols : result) {
			Integer demographicId = (Integer)cols[0];
			Integer formId = (Integer)cols[1];
			String paramName = (String)cols[2];
			String paramValue = (String)cols[3];
			if(demographicId == null) {
				demographicId = 0;
			}
			ClientData clientData = clientsDataList.get(demographicId);
			if(clientData == null){
				clientData = new ClientData();
				clientData.setClientId(demographicId);
				clientData.setFormId(formId);
				clientsDataList.put(demographicId, clientData);
			}
			clientData.getClientData().put(paramName, paramValue);
        }
		return new ArrayList<ClientData>(clientsDataList.values());
	}
	
	
	public ClientData getClientData(int clientId) {
		ClientData clientData = new ClientData();
		
		Query query = entityManager.createNativeQuery(QUERY_GET_CLIENT_DATA);
		query.setParameter(1, clientId);
		
		@SuppressWarnings("unchecked")
		List<Object[]> rows = query.getResultList();
		if(rows.size()==0) return null;
		Object[] cols = rows.get(0);
		Integer demographicId = (Integer)cols[0];
		Integer formId = (Integer)cols[1];
		String paramName = (String)cols[2];
		String paramValue = (String)cols[3];
		if(demographicId == null) {
			demographicId = 0;
		}
		clientData.setClientId(demographicId);
		clientData.setFormId(formId);
		clientData.getClientData().put(paramName, paramValue);
		
		return clientData;
	}

	
	
	private static final String QUERY_VACANCY_DATA = "SELECT v.id, v.wlProgramId, ct.field_name,ct.field_type,"
			+ "c.criteria_value,cso.option_value,c.range_start_value,c.range_end_value "
			+ "FROM criteria c JOIN criteria_type ct ON c.CRITERIA_TYPE_ID=ct.CRITERIA_TYPE_ID "
			+ "LEFT JOIN criteria_selection_option cso ON cso.CRITERIA_ID=c.CRITERIA_ID "
			+ "JOIN vacancy v ON v.id=c.VACANCY_ID WHERE v.id=?1";
	

	private static final String field_type_multiple = "select_multiple";
	private static final String field_type_range = "select_one_range";
	private static final String field_type_one = "select_one";
	private static final String field_type_number = "number";
	
	
	public VacancyData loadVacancyData(final int vacancyId) {
		VacancyData vacancyData  = new VacancyData();
		
		Query query = entityManager.createNativeQuery(QUERY_VACANCY_DATA);
		query.setParameter(1, vacancyId);
		
		@SuppressWarnings("unchecked")
		List<Object[]>results = query.getResultList();
		for (Object[] cols : results) {
			VacancyTemplateData vtData = new VacancyTemplateData();
			int vId = (Integer)cols[0];
			int programId = (Integer)cols[1];
			String fieldName = (String)cols[2];
			String fieldType = (String)cols[3];
			String critValue = (String)cols[4];
			String optionValue = (String)cols[5];
			Integer rangeStart = (Integer)cols[6];
			Integer rangeEnd = (Integer)cols[7];
			vacancyData.setVacancy_id(vId);
			vacancyData.setProgram_id(programId);
			if (field_type_range.equals(fieldType)) {
				vtData.setRange(true);
				vtData.addRange(rangeStart, rangeEnd);
			} else {
				if (field_type_multiple.equals(fieldType)) {
					vtData = vacancyData.getVacancyData().get(fieldName);
					if (vtData == null) {
						vtData = new VacancyTemplateData();
					}
					vtData.setParam(fieldName);
				} else if (field_type_one.equals(fieldType)) {
					vtData = new VacancyTemplateData();
					vtData.setParam(fieldName);
				} 
				if (critValue != null) {
					vtData.getValues().add(critValue);
				} else {
					vtData.getValues().add(optionValue);
				}
			}
			vacancyData.getVacancyData().put(vtData.getParam(), vtData);
        }
		
		return vacancyData;
	}


}

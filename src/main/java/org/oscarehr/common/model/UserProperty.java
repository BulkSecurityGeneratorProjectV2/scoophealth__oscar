/*
 *
 * Copyright (c) 2001-2002. Centre for Research on Inner City Health, St. Michael's Hospital, Toronto. All Rights Reserved. *
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
 * <OSCAR TEAM>
 *
 * UserProperty.java
 *
 * Created on December 19, 2007, 4:30 PM
 *
 *
 *
 */

package org.oscarehr.common.model;

import java.io.Serializable;

/**
 *
 * @author rjonasz
 */
public class UserProperty implements Serializable {

    public final static String MYOSCAR_ID="MyOscarId";
    public final static String STALE_NOTEDATE = "cme_note_date";
    public final static String MYDRUGREF_ID = "mydrugref_id";
    public final static String ONTARIO_MD_USERNAME = "ontario_md_username";
    public final static String ONTARIO_MD_PASSWORD = "ontario_md_password";
    public final static String CONSULTATION_TIME_PERIOD_WARNING = "consultation_time_period_warning";
    public final static String CONSULTATION_TEAM_WARNING = "consultation_team_warning";
    public final static String WORKLOAD_MANAGEMENT = "workload_management";
    public final static String CONSULTATION_REQ_PASTE_FMT = "consultation_req_paste_fmt";
    public final static String RX_PAGE_SIZE = "rx_page_size";
    public final static String RX_DEFAULT_QUANTITY = "rx_default_quantity";
    public final static String RX_PROFILE_VIEW = "rx_profile_view";
    public final static String RX_USE_RX3 = "rx_use_rx3";
    public final static String USE_MYMEDS = "use_mymeds";
    public final static String DMFLOW_SHEET_VIEW = "DMFlowsheet_view";
    public final static String DOC_DEFAULT_QUEUE="doc_default_queue";
    public final static String HC_TYPE= "HC_Type";
    public final static String DEFAULT_SEX= "default_sex";

    public final static String EFORM_FAVOURITE_GROUP = "favourite_eform_group";
    public final static String RX_SHOW_PATIENT_DOB="rx_show_patient_dob";
    
    //added to user properties with new interface
    public final static String FAX = "fax";
    public final static String SIGNATURE = "signature";
    public final static String COLOUR = "colour";
    public final static String SEX = "sex";
    public final static String SCHEDULE_START_HOUR = "schedule.start_hour";
    public final static String SCHEDULE_END_HOUR = "schedule.end_hour";
    public final static String SCHEDULE_PERIOD = "schedule.period";
    public final static String MYGROUP_NO = "mygroup_no";
    public final static String NEW_CME = "new_cme";
    public final static String ENCOUNTER_FORM_LENGTH = "encounter.form_length";
    public final static String ENCOUNTER_FORM_NAME = "encounter.form_name";
    public final static String EFORM_NAME = "encounter.eform_name";
    public final static String RX_SHOW_QR_CODE = "rx_show_qr_code";
    public final static String MYMEDS = "mymeds";
    public final static String NEW_TICKLER_WARNING_WINDOW = "new_tickler_warning_window";
    public final static String CAISI_DEFAULT_PMM = "caisi.default_pmm";
    public final static String CAISI_PREV_BILLING = "caisi.prev_billing";
    public final static String DEFAULT_BILLING_FORM = "default_billing_form";
    public final static String DEFAULT_REFERRAL_TYPE = "default_referral_type";
    public final static String DEFAULT_PAYEE = "default_payee";
    public final static String DEFAULT_DX_CODE = "default_dx_code";
    public final static String CPP_SINGLE_LINE="cpp_single_line";
    
    private long id;
    private String name;
    private String value;
    private String providerNo;
    private String[] valueArray;
    private boolean checked;

    public boolean isChecked(){

        return this.checked;
    }
    public void setChecked(boolean checked){

        this.checked=checked;
    }

    public String[] getValueArray(){
        return this.valueArray;
    }

    public void setValueArray(String[] va){
       this.valueArray=va;
    }
    public String getProviderNo() {
        return this.providerNo;
    }

    public void setProviderNo(String providerNo) {
        this.providerNo = providerNo;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }


    /** Creates a new instance of UserProperty */
    public UserProperty() {
    }

}

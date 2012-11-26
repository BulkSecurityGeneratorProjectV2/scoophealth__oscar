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
package org.oscarehr.common.dao;

import java.util.List;

import javax.persistence.Query;

import org.oscarehr.common.model.BillingONCHeader1;
import org.oscarehr.common.model.BillingONItem;
import org.springframework.stereotype.Repository;

@Repository
public class BillingONItemDao extends AbstractDao<BillingONItem>{

	
	public BillingONItemDao() {
		super(BillingONItem.class);
	}

    public List<BillingONItem> getBillingItemByCh1Id(Integer ch1_id) {
        String queryStr = "FROM BillingONItem b WHERE b.ch1Id = ?1";
        Query q = entityManager.createQuery(queryStr);
        q.setParameter(1, ch1_id);
        
        @SuppressWarnings("unchecked")
        List<BillingONItem> rs = q.getResultList();

        return rs;
    }
        
    public List<BillingONItem> getActiveBillingItemByCh1Id(Integer ch1_id) {
        String queryStr = "FROM BillingONItem b WHERE b.ch1Id = ?1 AND b.status <> ?2";
        Query q = entityManager.createQuery(queryStr);
        q.setParameter(1, ch1_id);
        q.setParameter(2, "D");
        
        @SuppressWarnings("unchecked")
        List<BillingONItem> rs = q.getResultList();

        return rs;
    }

    public List<BillingONCHeader1> getCh1ByDemographicNo(Integer demographic_no) {
        String queryStr = "FROM BillingONCHeader1 b WHERE b.demographicNo = ?1";
        Query q = entityManager.createQuery(queryStr);
        q.setParameter(1, demographic_no);
        
        @SuppressWarnings("unchecked")
        List<BillingONCHeader1> rs = q.getResultList();

        return rs;
    }
}

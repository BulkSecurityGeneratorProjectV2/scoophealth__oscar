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

import ca.uvic.leadlab.obibconnector.facades.exceptions.OBIBException;
import ca.uvic.leadlab.obibconnector.facades.registry.IProvider;
import ca.uvic.leadlab.obibconnector.facades.registry.ISearchProviders;
import ca.uvic.leadlab.obibconnector.impl.registry.SearchProviders;
import org.junit.Assert;
import org.junit.Test;
import org.oscarehr.util.MiscUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchProvidersTest extends FacadesBaseTest {
    private String result = null;

    @Test
    public void testFindByProviderName() {
        ISearchProviders searchProviders = new SearchProviders(configClinicA);
        List<IProvider> providers = null;
        String expectedErrorMsg = "Error finding providers by name.";
        String notNullProviders = "Providers not null";
        List<String> expectedResults = new ArrayList<String>(Arrays.asList(notNullProviders,expectedErrorMsg));
        result = null;
        try {
            providers = searchProviders.findByName("pli");
            if (providers != null && !providers.isEmpty()) {
                for (IProvider p: providers) {
                    MiscUtils.getLogger().info("CDX provider: " + p.getLastName() + "," +p.getFirstName() + "," +
                            p.getClinicName() +"," + p.getClinicID() + "," + p.getCity());
                }
            }
        } catch (OBIBException e) {
            result = e.getMessage();
            MiscUtils.getLogger().warn(result);
        } catch (Exception e) {
            result = e.getMessage(); //unexpected outcome
            MiscUtils.getLogger().error(e.getStackTrace());
        }
        if (providers != null) {
            result = notNullProviders;
            MiscUtils.getLogger().debug("Num of CDX providers found in search by name:" + providers.size());
            for (IProvider p: providers) {
                MiscUtils.getLogger().debug("Found: [" + p.getLastName()+","+p.getFirstName()+"],"+p.getID());
            }
        } else {
            MiscUtils.getLogger().debug("CDX providers is null for search by name");
        }

        Assert.assertTrue("The list of expected outcomes does not contain the value " + result, expectedResults.contains(result));
    }

    @Test
    public void testFindByProviderId() {

        ISearchProviders searchProviders = new SearchProviders(configClinicA);
        List<IProvider> providers = null;
        String expectedErrorMsg = "Error finding providers by id.";
        String notNullProviders = "Providers not null";
        List<String> expectedResults = new ArrayList<String>(Arrays.asList(notNullProviders,expectedErrorMsg));
        result = null;
        try {
            providers = searchProviders.findByProviderID("93188");
        } catch (OBIBException e) {
            result = e.getMessage();
            MiscUtils.getLogger().warn(result);
        } catch (Exception e) {
            result = e.getMessage();  //unexpected outcome
            MiscUtils.getLogger().error(e.getStackTrace());
        }
        if (providers != null) {
            result = notNullProviders;
            MiscUtils.getLogger().debug("Num of CDX providers found in search by id: " + providers.size());
            for (IProvider p: providers) {
                MiscUtils.getLogger().debug("Found: [" + p.getLastName()+","+p.getFirstName()+"],"+p.getID());
            }
        } else {
            MiscUtils.getLogger().debug("CDX providers is null for search by id");
        }

        Assert.assertTrue("The list of expected outcomes does not contain the value " + result, expectedResults.contains(result));
    }

    @Test(expected = OBIBException.class) /* CDX return: "Provider clinic ID cannot be the only parameter. Please use Clinic Search instead." */
    public void testFindByClinicID() throws Exception {
        ISearchProviders searchProviders = new SearchProviders(configClinicA);

        List<IProvider> providers = searchProviders.findByClinicID(clinicIdA);

        Assert.assertNotNull(providers);
    }


    @Test(expected = OBIBException.class)
    public void testFindByProviderIdError() throws Exception {

        ISearchProviders searchProviders = new SearchProviders(configClinicA);
        List<IProvider> providers = searchProviders.findByProviderID("__Wrong_ID");
        Assert.assertNull(providers);
    }
}

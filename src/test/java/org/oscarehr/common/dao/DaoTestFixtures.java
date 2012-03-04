package org.oscarehr.common.dao;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.oscarehr.common.dao.utils.ConfigUtils;
import org.oscarehr.common.dao.utils.SchemaUtils;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public abstract class DaoTestFixtures
{
	@BeforeClass
	public static void classSetUp() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException
	{
		Logger.getRootLogger().setLevel(Level.WARN);

		long start = System.currentTimeMillis();
		if(!SchemaUtils.inited) {
			MiscUtils.getLogger().info("dropAndRecreateDatabase");
			SchemaUtils.dropAndRecreateDatabase();
		}
		long end = System.currentTimeMillis();
		long secsTaken = (end-start)/1000;
		MiscUtils.getLogger().info("Setting up db took " + secsTaken + " seconds.");

		start = System.currentTimeMillis();
		if(SpringUtils.beanFactory==null) {
			oscar.OscarProperties p = oscar.OscarProperties.getInstance();
			p.setProperty("db_name", ConfigUtils.getProperty("db_schema"));
			p.setProperty("db_username", ConfigUtils.getProperty("db_user"));
			p.setProperty("db_password", ConfigUtils.getProperty("db_password"));
			p.setProperty("db_uri", ConfigUtils.getProperty("db_url_prefix"));
			p.setProperty("db_driver", ConfigUtils.getProperty("db_driver"));
			ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("/applicationContext.xml");
			SpringUtils.beanFactory = context;
		}
		end = System.currentTimeMillis();
		secsTaken = (end-start)/1000;
		MiscUtils.getLogger().info("Setting up spring took " + secsTaken + " seconds.");

	}

}
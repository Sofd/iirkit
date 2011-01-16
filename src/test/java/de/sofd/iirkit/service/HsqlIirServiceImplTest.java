package de.sofd.iirkit.service;

import java.util.Arrays;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 *
 * @author olaf
 */
public class HsqlIirServiceImplTest extends IirServiceImplTest {
    SimpleJdbcTemplate svcJdbcTempl;

    public HsqlIirServiceImplTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
		ApplicationContext ctx = new ClassPathXmlApplicationContext("/test-spring-beans.xml");
        svcJdbcTempl = (SimpleJdbcTemplate) ctx.getBean("jdbcTemplate");
        svcJdbcTempl.batchUpdate("insert into user(name,password,roles) values(?,?,?)",
                                 Arrays.asList(new Object[]{"olaf","olafpw","admin"},
                                               new Object[]{"hans","hanspw","user;admin"}));
        svcJdbcTempl.batchUpdate("insert into iircase(userName, caseNr, hangingProtocol, result) values(?,?,?,?)",
                                 Arrays.asList(new Object[]{"olaf",1,"c1hp","c1res"},
                                               new Object[]{"olaf",2,"c2hp","c2res"},
                                               new Object[]{"olaf",3,"c3hp",""},
                                               new Object[]{"olaf",4,"c4hp",""},
                                               new Object[]{"olaf",5,"c5hp",""},
                                               new Object[]{"olaf",6,"c6hp",""},
                                               new Object[]{"hans",1,"hc1hp","hc1res"},
                                               new Object[]{"hans",2,"hc2hp","hc2res"},
                                               new Object[]{"hans",3,"hc3hp","hc3res"},
                                               new Object[]{"hans",4,"hc4hp",""},
                                               new Object[]{"hans",5,"hc5hp",""}));
        svc = (IirService) ctx.getBean("iirService");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        svcJdbcTempl.update("drop table iircase");
        svcJdbcTempl.update("drop table user");
    }

}

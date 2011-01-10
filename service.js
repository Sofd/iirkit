importPackage(java.io);
importPackage(org.springframework.context);
importPackage(org.springframework.context.support);

dbUrl = "jdbc:hsqldb:file:/home/olaf/hieronymusr/iirkit-test/textdb/dbfiles/db";
dbUser = "olaf";
dbPW = "";

conn = java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPW);
stmt = conn.createStatement();
//rs = stmt.executeQuery("select * FROM iircase");


//ApplicationContext ctx = new ClassPathXmlApplicationContext("/test-spring-beans.xml");
//ctx = new ClassPathXmlApplicationContext("/spring-beans.xml");
//tpl = ctx.getBean("jdbcTemplate");
//l = tpl.queryForList("select * FROM iircase where userName = ? and result is null order by caseNr asc limit 1", "olaf");

package de.sofd.iirkit.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author olaf
 */
public class HsqlIirServiceImpl implements IirService {

    private final SimpleJdbcTemplate jdbcTemplate;

    public HsqlIirServiceImpl() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("/spring-beans.xml");
        DataSource ds =  (DataSource) ctx.getBean("dataSource");
        jdbcTemplate = new SimpleJdbcTemplate(ds);
    }

    //TODO: the tx manager apparently isn't used -- because we're not creating the service from the spring XML?
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<User> getAllUsers() {
        return jdbcTemplate.query("select name, password, roles from user", new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int i) throws SQLException {
                return new User(rs.getString("name"), rs.getString("password"), rs.getString("roles").split(","));
            }
        });
    }

}

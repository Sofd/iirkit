package de.sofd.iirkit.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author olaf
 */
public class HsqlIirServiceImpl implements IirService /*, DatabasePopulator*/ {

    protected static Logger logger = Logger.getLogger(HsqlIirServiceImpl.class);

    private final SimpleJdbcTemplate jdbcTemplate;

    public HsqlIirServiceImpl(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    protected class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int i) throws SQLException {
            return new User(rs.getString("name"), rs.getString("password"), rs.getString("roles").split(","));
        }
    }

    protected class CaseRowMapper implements RowMapper<Case> {
        @Override
        public Case mapRow(ResultSet rs, int i) throws SQLException {
            return new Case(rs.getInt("caseNr"), rs.getString("hangingProtocol"), rs.getString("result"));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<User> getAllUsers() {
        return jdbcTemplate.query("select name, password, roles from user", new UserRowMapper());
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    @Override
    public User getUser(String name) {
        return jdbcTemplate.queryForObject("select name, password, roles from user where name=?", new UserRowMapper(), name);
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    @Override
    public User authUser(String name, String password) {
        User u = getUser(name);
        if (u == null) {
            throw new RuntimeException("unknown user " + name);
        }
        if (!(password.equals(u.getPassword()))) {
            throw new RuntimeException("wrong password for user " + name);
        }
        return u;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<Case> getAllCases() {
        return jdbcTemplate.query("select userName, caseNr, hangingProtocol, result from iircase", new CaseRowMapper());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<Case> getCasesOf(User user) {
        return jdbcTemplate.query("select userName, caseNr, hangingProtocol, result from iircase where userName = ?", new CaseRowMapper(), user.getName());
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    @Override
    public Case getNextCaseOf(User user) {
        return jdbcTemplate.queryForObject("select * FROM iircase where userName=? and result is null order by caseNr asc limit 1", new CaseRowMapper(), user.getName());
    }

    public void update(Case c) {

    }

    /*
    @Override
    public void populate(Connection cnctn) throws SQLException {
        try {
            cnctn.createStatement().execute("CREATE TEXT TABLE user(name varchar(20), password varchar(20), roles varchar(100000))");
            cnctn.createStatement().execute("SET TABLE user SOURCE \"tablefiles/user;quoted=true\"");
        } catch (SQLException e) {
            logger.debug("error on create user table", e);
        }
        try {
            cnctn.createStatement().execute("CREATE TEXT TABLE iircase(userName varchar(20), caseNr integer, hangingProtocol varchar(10000), result varchar(100000))");
            cnctn.createStatement().execute("SET TABLE iircase SOURCE \"tablefiles/iircase;quoted=true\"");
            //cnctn.createStatement().execute("INSERT INTO iircase(userName, caseNr, hangingProtocol, result) values('olaf', '1', 'foohp', 'barresult')");
        } catch (SQLException e) {
            logger.debug("error on create iircase table", e);
        }
        //these should work now no matter what
        cnctn.createStatement().execute("select count(*) from user");
        cnctn.createStatement().execute("select count(*) from iircase");
    }
    */

}

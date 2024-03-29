package de.sofd.iirkit.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author olaf
 */
public class HsqlIirServiceImpl implements IirService /*, DatabasePopulator*/ {
    //TODO: untested with the new Case API/attributes. Doesn't store the arbitrary Case#getAllAttributes.

    protected static Logger logger = Logger.getLogger(HsqlIirServiceImpl.class);

    private final SimpleJdbcTemplate jdbcTemplate;

    public HsqlIirServiceImpl(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    protected class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int i) throws SQLException {
            return new User(rs.getString("name"), rs.getString("password"), rs.getString("roles").split(";"));
        }
    }

    protected class CaseRowMapper implements RowMapper<Case> {
        @Override
        public Case mapRow(ResultSet rs, int i) throws SQLException {
            String res = rs.getString("result");
            if ("".equals(res)) {
                res = null;
            }
            return new Case(rs.getInt("caseNr"), Arrays.asList(rs.getString("seriesGroupUrls").split(",")), rs.getString("ecrfUrl"), res);
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
        return queryForZeroOrOneObject(jdbcTemplate, "select name, password, roles from user where name=?", new UserRowMapper(), name);
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
        return jdbcTemplate.query("select userName, caseNr, hangingProtocol, result from iircase order by caseNr asc", new CaseRowMapper());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<Case> getCasesOf(User user) {
        return jdbcTemplate.query("select userName, caseNr, hangingProtocol, result from iircase where userName = ?", new CaseRowMapper(), user.getName());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public int getNumberOfCasesOf(User user) {
        return jdbcTemplate.queryForInt("select count(*) from iircase where userName = ?", user.getName());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public int getNumberOfDoneCasesOf(User user) {
        return jdbcTemplate.queryForInt("select count(*) from iircase where userName = ? and not (result='' or result is null)", user.getName());
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    @Override
    public Case getNextCaseOf(User user) {
        //"result is null" works in jdbc and with SimpleJdbcTemplate from the js console, but not in the unit test..?? WTF??
        Case result = queryForZeroOrOneObject(jdbcTemplate, "select * FROM iircase where userName = ? and (result='' or result is null) order by caseNr asc limit 1", new CaseRowMapper(), user.getName());
        if (result != null) {
            result.setUser(user);
        }
        return result;
    }

    @Override
    public Case getCaseOf(User user, int caseNr) {
        //TODO: UNTESTED
        Case result = queryForZeroOrOneObject(jdbcTemplate, "select * FROM iircase where userName = ? and caseNr = ?", new CaseRowMapper(), user.getName(), caseNr);
        if (result != null) {
            result.setUser(user);
        }
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    @Override
    public int update(Case c) {
        return jdbcTemplate.update("update iircase set seriesGroupUrls=?, ecrfUrl=?, result=? where userName=? and caseNr=?", c.getSeriesGroupUrls(), c.getEcrfUrl(), c.getResult(), c.getUser().getName(), c.getNumber());
    }

    public static <T> T queryForZeroOrOneObject(SimpleJdbcTemplate templ, String sql, RowMapper<T> rm, Object... args) throws DataAccessException {
        List<T> results = templ.query(sql, rm, args);
        if (results.isEmpty()) {
            return null;
        }
        return results.iterator().next();
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

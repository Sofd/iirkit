package de.sofd.iirkit.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
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

    public HsqlIirServiceImpl(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

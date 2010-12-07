package de.sofd.iirkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author sofd
 */
@Component
public class SecurityContext {

    public enum AuthenticationResult {

        WRONG_USER, WRONG_PASSWORD, EMPTY_USER_PASSWORD, LOCKED, OK;
    }

    public enum Authority {

        Reader, Manager, User
    }
    static final Logger log4jLogger = Logger.getLogger(SecurityContext.class);
    static final String SUPERADMIN_USERNAME = "superadmin";
    static final String SUPERADMIN_PASSWORD = "all4digima!";
    @Autowired
    private UserService userService;
    private User user;
    private Authority authority = Authority.User;
    List<String> readerIdList;
    List<String> managerIdList;
    Map<String, Integer> authenticationAttempts = new HashMap<String, Integer>();

    public AuthenticationResult create(String username, String password) {
        log4jLogger.info("create: " + username + ", " + password);
        user = null;
        authority = Authority.User;
        username = StringUtils.trimToEmpty(username);
        password = StringUtils.trimToEmpty(password);
        if (isLocked(username)) {
            log4jLogger.info("create: " + AuthenticationResult.LOCKED);
            return AuthenticationResult.LOCKED;
        }
        authenticationAttempts.put(username, authenticationAttempts.get(username) == null ? 1 : authenticationAttempts.get(username) + 1);
        log4jLogger.info("create - authenticationAttempts: " + authenticationAttempts.toString());
        if (authenticationAttempts.get(username) == 6) {
            lock(username);
            log4jLogger.info("create: " + AuthenticationResult.LOCKED);
            return AuthenticationResult.LOCKED;
        }
        if (username.equals("") || password.equals("")) {
            log4jLogger.info("create: " + AuthenticationResult.EMPTY_USER_PASSWORD);
            return AuthenticationResult.EMPTY_USER_PASSWORD;
        }
        User userToAuthenticate = userService.getUserByUsername(StringUtils.trimToNull(username));
        if (userToAuthenticate == null) {
            log4jLogger.info("create: " + AuthenticationResult.WRONG_USER);
            return AuthenticationResult.WRONG_USER;
        }
        if (StringUtils.trimToEmpty(userToAuthenticate.getPassword()).equals(password)) {
            user = userToAuthenticate;
            String[] readerIdArray = StringUtils.splitByWholeSeparator(Context.getApplicationProperties().getProperty("readerIdList"), ",");
            log4jLogger.info("create - readerIdArray: " + readerIdArray);
            readerIdList = Arrays.asList(readerIdArray);
            String[] managerIdArray = StringUtils.splitByWholeSeparator(Context.getApplicationProperties().getProperty("managerIdList"), ",");
            log4jLogger.info("create - managerIdArray: " + readerIdArray);
            managerIdList = Arrays.asList(managerIdArray);
            for (String strId : readerIdList) {
                if (strId.equals(user.getId().toString())) {
                    authority = Authority.Reader;
                    break;
                }
            }
            if (authority.equals(Authority.User)) {
                for (String strId : managerIdList) {
                    if (strId.equals(user.getId().toString())) {
                        authority = Authority.Manager;
                        break;
                    }
                }
            }
            log4jLogger.info("create: " + AuthenticationResult.OK + " - username: " + user.getUsername() + " - authority: " + authority);
            authenticationAttempts.clear();
            return AuthenticationResult.OK;
        }

        log4jLogger.info("create: " + AuthenticationResult.WRONG_PASSWORD);
        return AuthenticationResult.WRONG_PASSWORD;
    }

    public AuthenticationResult verify(String username, String password) {
        log4jLogger.info("verify: " + username + ", " + password);
        username = StringUtils.trimToEmpty(username);
        password = StringUtils.trimToEmpty(password);
        if (isLocked(username)) {
            log4jLogger.info("create: " + AuthenticationResult.LOCKED);
            return AuthenticationResult.LOCKED;
        }
        authenticationAttempts.put(username, authenticationAttempts.get(username) == null ? 1 : authenticationAttempts.get(username) + 1);
        log4jLogger.info("create - authenticationAttempts: " + authenticationAttempts.toString());
        if (authenticationAttempts.get(username) == 6) {
            lock(username);
            log4jLogger.info("create: " + AuthenticationResult.LOCKED);
            return AuthenticationResult.LOCKED;
        }
        if (username.equals("") || password.equals("")) {
            log4jLogger.info("verify: " + AuthenticationResult.EMPTY_USER_PASSWORD);
            return AuthenticationResult.EMPTY_USER_PASSWORD;
        }
        if (!user.getUsername().equals(username)) {
            log4jLogger.info("verify: " + AuthenticationResult.WRONG_USER);
            return AuthenticationResult.WRONG_USER;
        }
        if (!user.getPassword().equals(password)) {
            log4jLogger.info("verify: " + AuthenticationResult.WRONG_PASSWORD);
            return AuthenticationResult.WRONG_PASSWORD;
        }
        log4jLogger.info("verify: " + AuthenticationResult.OK);
        authenticationAttempts.clear();
        return AuthenticationResult.OK;
    }

    public AuthenticationResult verifySuperadmin(String username, String password) {
        log4jLogger.info("verifySuperadmin");
        username = StringUtils.trimToEmpty(username);
        password = StringUtils.trimToEmpty(password);
        if (username.equals("") || password.equals("")) {
            log4jLogger.info("verifySuperadmin: " + AuthenticationResult.EMPTY_USER_PASSWORD);
            return AuthenticationResult.EMPTY_USER_PASSWORD;
        }
        if (!SUPERADMIN_USERNAME.equals(username)) {
            log4jLogger.info("verifySuperadmin: " + AuthenticationResult.WRONG_USER);
            return AuthenticationResult.WRONG_USER;
        }
        if (!SUPERADMIN_PASSWORD.equals(password)) {
            log4jLogger.info("verifySuperadmin: " + AuthenticationResult.WRONG_PASSWORD);
            return AuthenticationResult.WRONG_PASSWORD;
        }
        log4jLogger.info("verifySuperadmin: " + AuthenticationResult.OK);
        return AuthenticationResult.OK;
    }

    public void lock(String username) {
        log4jLogger.info("lock: " + username);
        Writer fw = null;
        try {
            URL fileUrl = Context.createFileUrlFromPropertiesDir(username + ".lock");
            fw = new FileWriter(fileUrl.getFile());
            fw.write("");
        } catch (IOException ex) {
            log4jLogger.error("", ex);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ex) {
                    log4jLogger.error("", ex);
                }
            }
        }
    }

    public void unlock(String username) {
       log4jLogger.info("unlock: " + username);
       URL fileUrl = Context.createFileUrlFromPropertiesDir(username + ".lock");
       boolean success = (new File(fileUrl.getFile())).delete();
       log4jLogger.info("unlock - success: " + success);
    }

    public boolean isLocked(String username) {
        URL fileUrl = Context.createFileUrlFromPropertiesDir(username + ".lock");
        File lockFile = new File(fileUrl.getFile());
        boolean result = lockFile.exists();;
        log4jLogger.info("isLocked: " + result);
        return result;
    }

    public User getUser() {
        return user;
    }

    public Authority getAuthority() {
        return authority;
    }

    public List<String> getManagerIdList() {
        return managerIdList;
    }

    public List<String> getReaderIdList() {
        return readerIdList;
    }
}

package de.sofd.iirkit;

import de.sofd.iirkit.service.IirService;
import de.sofd.iirkit.service.User;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
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
    private IirService iirService;
    private User user;
    private Authority authority = Authority.User;
    List<String> readerIdList;
    List<String> managerIdList;
    Map<String, Integer> authenticationAttempts = new HashMap<String, Integer>();

    public IirService getIirService() {
        return iirService;
    }

    public void setIirService(IirService iirService) {
        this.iirService = iirService;
    }

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
        User userToAuthenticate = iirService.getUser(StringUtils.trimToNull(username));
        if (userToAuthenticate == null) {
            log4jLogger.info("create: " + AuthenticationResult.WRONG_USER);
            return AuthenticationResult.WRONG_USER;
        }
        if (StringUtils.trimToEmpty(userToAuthenticate.getPassword()).equals(password)) {
            user = userToAuthenticate;
            if (user.hasRole(User.ROLE_READER)) {
                authority = Authority.Reader;
            }
            if (user.hasRole(User.ROLE_ADMIN)) {
                authority = Authority.Manager;
            }
            log4jLogger.info("create: " + AuthenticationResult.OK + " - username: " + user.getName() + " - authority: " + authority);
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
        if (!user.getName().equals(username)) {
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
            URL fileUrl = createFileUrlFromPropertiesDir(username + ".lock");
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
       URL fileUrl = createFileUrlFromPropertiesDir(username + ".lock");
       boolean success = (new File(fileUrl.getFile())).delete();
       log4jLogger.info("unlock - success: " + success);
    }

    public boolean isLocked(String username) {
        URL fileUrl = createFileUrlFromPropertiesDir(username + ".lock");
        File lockFile = new File(fileUrl.getFile());
        boolean result = lockFile.exists();
        log4jLogger.info("isLocked: " + result);
        return result;
    }

    public static URL createFileUrlFromPropertiesDir(String filename) {
        File file = new File(System.getProperty("user.dir") + File.separator + "iirkit.properties" + File.separator + filename); //TODO: user project - specific name
        log4jLogger.info("createFileUrlFromPropertiesDir - file.getPath(): " + file.getPath());
        URL url = null;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException ex) {
            throw new RuntimeException("SHOULD NEVER HAPPEN");
        }
        log4jLogger.info("createFileUrlFromPropertiesDir - url: " + url);
        return url;
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

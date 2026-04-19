package service;

import java.util.prefs.Preferences;

public class UserSession {

    private static volatile UserSession instance;

    private String userName;
    private String password;
    private String privileges;

    private UserSession(String userName, String password, String privileges) {
        this.userName = userName;
        this.password = password;
        this.privileges = privileges;
        Preferences prefs = Preferences.userRoot();
        prefs.put("USERNAME", userName);
        prefs.put("PASSWORD", password);
        prefs.put("PRIVILEGES", privileges);
    }

    public static UserSession getInstance(String userName, String password, String privileges) {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession(userName, password, privileges);
                }
            }
        }
        return instance;
    }

    public static UserSession getInstance(String userName, String password) {
        return getInstance(userName, password, "USER");
    }

    public static UserSession getInstance() {
        return instance;
    }

    // Backward-compatible typo overloads
    public static UserSession getInstace(String userName, String password, String privileges) {
        return getInstance(userName, password, privileges);
    }

    public static UserSession getInstace(String userName, String password) {
        return getInstance(userName, password);
    }

    public synchronized String getUserName() {
        return userName;
    }

    public synchronized String getPassword() {
        return password;
    }

    public synchronized String getPrivileges() {
        return privileges;
    }

    public synchronized void cleanUserSession() {
        this.userName = "";
        this.password = "";
        this.privileges = "";
        instance = null;
    }

    @Override
    public String toString() {
        return "UserSession{userName='" + userName + "', privileges=" + privileges + '}';
    }
}

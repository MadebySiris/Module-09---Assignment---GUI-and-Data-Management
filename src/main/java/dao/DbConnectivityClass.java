package dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Person;
import service.MyLogger;

import java.sql.*;

public class DbConnectivityClass {

    private static final String DB_URL = "jdbc:derby:AcademicTrackDB;create=true";

    MyLogger lg = new MyLogger();
    private final ObservableList<Person> data = FXCollections.observableArrayList();

    public ObservableList<Person> getData() {
        connectToDatabase();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM users");
             ResultSet rs = ps.executeQuery()) {

            data.clear();
            while (rs.next()) {
                data.add(new Person(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("department"),
                        rs.getString("major"),
                        rs.getString("email"),
                        rs.getString("imageURL")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public boolean connectToDatabase() {
        boolean hasRegisteredUsers = false;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Create table if it does not exist (X0Y32 = table already exists in Derby)
            try {
                stmt.executeUpdate(
                        "CREATE TABLE users (" +
                        "id INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY, " +
                        "first_name VARCHAR(200) NOT NULL, " +
                        "last_name VARCHAR(200) NOT NULL, " +
                        "department VARCHAR(200), " +
                        "major VARCHAR(200), " +
                        "email VARCHAR(200) NOT NULL, " +
                        "imageURL VARCHAR(200))"
                );
                lg.makeLog("Table 'users' created.");
            } catch (SQLException e) {
                if (!e.getSQLState().equals("X0Y32")) throw e;
            }

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) > 0) hasRegisteredUsers = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return hasRegisteredUsers;
    }

    public void insertUser(Person person) {
        connectToDatabase();
        String sql = "INSERT INTO users (first_name, last_name, department, major, email, imageURL) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, person.getFirstName());
            ps.setString(2, person.getLastName());
            ps.setString(3, person.getDepartment());
            ps.setString(4, person.getMajor());
            ps.setString(5, person.getEmail());
            ps.setString(6, person.getImageURL());
            if (ps.executeUpdate() > 0) lg.makeLog("Inserted: " + person.getFirstName());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void editUser(int id, Person p) {
        connectToDatabase();
        String sql = "UPDATE users SET first_name=?, last_name=?, department=?, major=?, email=?, imageURL=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());
            ps.setString(3, p.getDepartment());
            ps.setString(4, p.getMajor());
            ps.setString(5, p.getEmail());
            ps.setString(6, p.getImageURL());
            ps.setInt(7, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteRecord(Person person) {
        connectToDatabase();
        String sql = "DELETE FROM users WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, person.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int retrieveId(Person p) {
        connectToDatabase();
        String sql = "SELECT id FROM users WHERE email=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getEmail());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    lg.makeLog("Retrieved ID: " + id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public void queryUserByLastName(String name) {
        connectToDatabase();
        String sql = "SELECT * FROM users WHERE last_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lg.makeLog("ID: " + rs.getInt("id") +
                            ", Name: " + rs.getString("first_name") + " " + rs.getString("last_name") +
                            ", Major: " + rs.getString("major") +
                            ", Dept: " + rs.getString("department"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void listAllUsers() {
        connectToDatabase();
        String sql = "SELECT * FROM users";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lg.makeLog("ID: " + rs.getInt("id") +
                        ", Name: " + rs.getString("first_name") + " " + rs.getString("last_name") +
                        ", Dept: " + rs.getString("department") +
                        ", Major: " + rs.getString("major") +
                        ", Email: " + rs.getString("email"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

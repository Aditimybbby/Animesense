public class User {
    // No existing comments should be here.
    private String username;
    private String email;

    public User(String username, String email) {
        if (username == null || email == null) {
            throw new IllegalArgumentException("Username and email cannot be null.");
        }
        this.username = username;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}
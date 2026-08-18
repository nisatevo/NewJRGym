package gym.database;

import gym.model.*;
import gym.util.Money;

import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** SQLite persistence layer for New JR Gym. */
public final class DatabaseManager {
    private static final Path DATA_DIR = Paths.get("data");
    private static final String DB_URL = "jdbc:sqlite:" + DATA_DIR.resolve("new_jr_gym.db");
    private static boolean initialized;

    private DatabaseManager() {
    }

    public static synchronized void initialize() {
        if (initialized)
            return;
        try {
            Files.createDirectories(DATA_DIR);
            Class.forName("org.sqlite.JDBC");
            try (Connection c = connection(); Statement s = c.createStatement()) {
                s.execute("PRAGMA foreign_keys = ON");
                s.execute("PRAGMA journal_mode = WAL");
                createSchema(s);
                seedDefaults(c);
            }
            initialized = true;
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize SQLite database: " + e.getMessage(), e);
        }
    }

    private static void ensureInitialized() {
        if (!initialized)
            initialize();
    }

    private static Connection connection() throws SQLException {
        Connection c = DriverManager.getConnection(DB_URL);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    private static void createSchema(Statement s) throws SQLException {
        s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS admins (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                        password_hash TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'ADMIN',
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                """);
        s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS members (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        age INTEGER NOT NULL,
                        gender TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        email TEXT NOT NULL,
                        address TEXT NOT NULL,
                        photo_path TEXT
                    )
                """);
        s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS trainers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        age INTEGER NOT NULL,
                        specialization TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        email TEXT NOT NULL,
                        monthly_pay_cents INTEGER NOT NULL,
                        photo_path TEXT
                    )
                """);
        s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS membership_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                        months INTEGER NOT NULL CHECK(months BETWEEN 1 AND 120),
                        price_cents INTEGER NOT NULL CHECK(price_cents > 0)
                    )
                """);
        s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS memberships (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        member_id INTEGER NOT NULL,
                        plan TEXT NOT NULL,
                        months INTEGER NOT NULL,
                        price_cents INTEGER NOT NULL,
                        start_date TEXT NOT NULL,
                        end_date TEXT NOT NULL,
                        FOREIGN KEY(member_id) REFERENCES members(id) ON DELETE RESTRICT
                    )
                """);
        s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_memberships_member ON memberships(member_id)");
        s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        member_id INTEGER NOT NULL,
                        membership_id INTEGER NOT NULL UNIQUE,
                        amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
                        method TEXT NOT NULL,
                        date TEXT NOT NULL,
                        status TEXT NOT NULL,
                        FOREIGN KEY(member_id) REFERENCES members(id) ON DELETE RESTRICT,
                        FOREIGN KEY(membership_id) REFERENCES memberships(id) ON DELETE RESTRICT
                    )
                """);
        s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS trainer_payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        trainer_id INTEGER NOT NULL,
                        amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
                        payment_month TEXT NOT NULL,
                        date TEXT NOT NULL,
                        method TEXT NOT NULL,
                        status TEXT NOT NULL,
                        UNIQUE(trainer_id, payment_month),
                        FOREIGN KEY(trainer_id) REFERENCES trainers(id) ON DELETE RESTRICT
                    )
                """);
    }

    private static void seedDefaults(Connection c) throws SQLException {
        try (PreparedStatement q = c.prepareStatement("SELECT COUNT(*) FROM admins"); ResultSet rs = q.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                try (PreparedStatement p = c
                        .prepareStatement("INSERT INTO admins(username,password_hash,role) VALUES(?,?,?)")) {
                    p.setString(1, "admin");
                    p.setString(2, hash("admin123"));
                    p.setString(3, "ADMIN");
                    p.executeUpdate();
                }
            }
        }
        try (PreparedStatement q = c.prepareStatement("SELECT COUNT(*) FROM membership_plans");
                ResultSet rs = q.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                try (PreparedStatement p = c
                        .prepareStatement("INSERT INTO membership_plans(name,months,price_cents) VALUES(?,?,?)")) {
                    addPlan(p, "Basic", 1, 100000);
                    addPlan(p, "Standard", 3, 270000);
                    addPlan(p, "Premium", 6, 480000);
                }
            }
        }
    }

    private static void addPlan(PreparedStatement p, String name, int months, long price) throws SQLException {
        p.setString(1, name);
        p.setInt(2, months);
        p.setLong(3, price);
        p.executeUpdate();
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest)
                out.append(String.format("%02x", b));
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static synchronized boolean login(String username, String password) throws SQLException {
        ensureInitialized();
        if (username == null || password == null || username.isBlank())
            return false;
        try (Connection c = connection();
                PreparedStatement p = c
                        .prepareStatement("SELECT 1 FROM admins WHERE username=? AND password_hash=? LIMIT 1")) {
            p.setString(1, username.trim());
            p.setString(2, hash(password));
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    public static synchronized void registerAdmin(String username, String password, String confirmPassword)
            throws SQLException {
        ensureInitialized();
        String u = username == null ? "" : username.trim();
        require(u.matches("[A-Za-z][A-Za-z0-9_.-]{3,24}"), "Username must be 4–25 characters and start with a letter.");
        require(password != null && password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$"),
                "Password must be at least 8 characters and include uppercase, lowercase, and a number.");
        require(password.equals(confirmPassword), "Passwords do not match.");
        try (Connection c = connection();
                PreparedStatement p = c
                        .prepareStatement("INSERT INTO admins(username,password_hash,role) VALUES(?,?,?)")) {
            p.setString(1, u);
            p.setString(2, hash(password));
            p.setString(3, "ADMIN");
            p.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique"))
                throw new IllegalArgumentException("That admin username already exists.");
            throw e;
        }
    }

    public static synchronized int adminCount() throws SQLException {
        ensureInitialized();
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM admins");
                ResultSet r = p.executeQuery()) {
            return r.next() ? r.getInt(1) : 0;
        }
    }

    public static synchronized int nextMemberId() throws SQLException {
        ensureInitialized();
        return nextId("members");
    }

    public static synchronized int nextTrainerId() throws SQLException {
        ensureInitialized();
        return nextId("trainers");
    }

    private static int nextId(String table) throws SQLException {
        try (Connection c = connection();
                Statement s = c.createStatement();
                ResultSet r = s.executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM " + table)) {
            return r.next() ? r.getInt(1) : 1;
        }
    }

    public static synchronized List<Member> getMembers(String search) throws SQLException {
        ensureInitialized();
        String q = search == null ? "" : search.trim();
        List<Member> out = new ArrayList<>();
        String sql = "SELECT id,name,age,gender,phone,email,address,photo_path FROM members WHERE ?='' OR lower(name) LIKE lower(?) OR phone LIKE ? OR CAST(id AS TEXT) LIKE ? ORDER BY id DESC";
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(sql)) {
            String like = "%" + q + "%";
            p.setString(1, q);
            p.setString(2, like);
            p.setString(3, like);
            p.setString(4, like);
            try (ResultSet r = p.executeQuery()) {
                while (r.next())
                    out.add(member(r));
            }
        }
        return out;
    }

    public static synchronized Member getMember(int id) throws SQLException {
        ensureInitialized();
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement("SELECT * FROM members WHERE id=?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? member(r) : null;
            }
        }
    }

    private static Member member(ResultSet r) throws SQLException {
        return new Member(r.getInt("id"), r.getString("name"), r.getInt("age"), r.getString("gender"),
                r.getString("phone"), r.getString("email"), r.getString("address"), r.getString("photo_path"));
    }

    public static synchronized void addMember(Member m) throws SQLException {
        ensureInitialized();
        require(m != null, "Member data is required.");
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(
                        "INSERT INTO members(name,age,gender,phone,email,address,photo_path) VALUES(?,?,?,?,?,?,?)")) {
            p.setString(1, m.getName());
            p.setInt(2, m.getAge());
            p.setString(3, m.getGender());
            p.setString(4, m.getPhone());
            p.setString(5, m.getEmail());
            p.setString(6, m.getAddress());
            p.setString(7, m.getPhotoPath());
            p.executeUpdate();
        }
    }

    public static synchronized void updateMember(Member m) throws SQLException {
        ensureInitialized();
        require(m != null, "Member data is required.");
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(
                        "UPDATE members SET name=?,age=?,gender=?,phone=?,email=?,address=?,photo_path=? WHERE id=?")) {
            p.setString(1, m.getName());
            p.setInt(2, m.getAge());
            p.setString(3, m.getGender());
            p.setString(4, m.getPhone());
            p.setString(5, m.getEmail());
            p.setString(6, m.getAddress());
            p.setString(7, m.getPhotoPath());
            p.setInt(8, m.getId());
            if (p.executeUpdate() == 0)
                throw new SQLException("Member not found.");
        }
    }

    public static synchronized boolean hasMemberFinancialRecords(int id) throws SQLException {
        ensureInitialized();
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(
                        "SELECT EXISTS(SELECT 1 FROM memberships WHERE member_id=? UNION ALL SELECT 1 FROM payments WHERE member_id=? )")) {
            p.setInt(1, id);
            p.setInt(2, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() && r.getInt(1) == 1;
            }
        }
    }

    public static synchronized void deleteMember(int id) throws SQLException {
        ensureInitialized();
        if (hasMemberFinancialRecords(id))
            throw new IllegalArgumentException(
                    "This member has membership/payment records and cannot be deleted. Keep the financial history intact.");
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("DELETE FROM members WHERE id=?")) {
            p.setInt(1, id);
            if (p.executeUpdate() == 0)
                throw new SQLException("Member not found.");
        }
    }

    public static synchronized List<Trainer> getTrainers() throws SQLException {
        ensureInitialized();
        List<Trainer> out = new ArrayList<>();
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement("SELECT * FROM trainers ORDER BY id DESC");
                ResultSet r = p.executeQuery()) {
            while (r.next())
                out.add(trainer(r));
        }
        return out;
    }

    private static Trainer trainer(ResultSet r) throws SQLException {
        return new Trainer(r.getInt("id"), r.getString("name"), r.getInt("age"), r.getString("specialization"),
                r.getString("phone"), r.getString("email"), r.getLong("monthly_pay_cents"), r.getString("photo_path"));
    }

    public static synchronized void addTrainer(Trainer t) throws SQLException {
        ensureInitialized();
        require(t != null, "Trainer data is required.");
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(
                        "INSERT INTO trainers(name,age,specialization,phone,email,monthly_pay_cents,photo_path) VALUES(?,?,?,?,?,?,?)")) {
            p.setString(1, t.getName());
            p.setInt(2, t.getAge());
            p.setString(3, t.getSpecialization());
            p.setString(4, t.getPhone());
            p.setString(5, t.getEmail());
            p.setLong(6, t.getMonthlyPayCents());
            p.setString(7, t.getPhotoPath());
            p.executeUpdate();
        }
    }

    public static synchronized boolean hasTrainerPayments(int id) throws SQLException {
        ensureInitialized();
        try (Connection c = connection();
                PreparedStatement p = c
                        .prepareStatement("SELECT EXISTS(SELECT 1 FROM trainer_payments WHERE trainer_id=?)")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() && r.getInt(1) == 1;
            }
        }
    }

    public static synchronized void deleteTrainer(int id) throws SQLException {
        ensureInitialized();
        if (hasTrainerPayments(id))
            throw new IllegalArgumentException("This trainer has payment history and cannot be deleted.");
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("DELETE FROM trainers WHERE id=?")) {
            p.setInt(1, id);
            if (p.executeUpdate() == 0)
                throw new SQLException("Trainer not found.");
        }
    }

    public static synchronized List<Map<String, Object>> getPlans() throws SQLException {
        ensureInitialized();
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement("SELECT * FROM membership_plans ORDER BY id");
                ResultSet r = p.executeQuery()) {
            while (r.next())
                out.add(plan(r));
        }
        return out;
    }

    public static synchronized Map<String, Object> getPlan(String name) throws SQLException {
        ensureInitialized();
        if (name == null)
            return null;
        try (Connection c = connection();
                PreparedStatement p = c
                        .prepareStatement("SELECT * FROM membership_plans WHERE name=? COLLATE NOCASE")) {
            p.setString(1, name.trim());
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? plan(r) : null;
            }
        }
    }

    private static Map<String, Object> plan(ResultSet r) throws SQLException {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getInt("id"));
        m.put("name", r.getString("name"));
        m.put("months", r.getInt("months"));
        m.put("price", r.getLong("price_cents"));
        return m;
    }

    public static synchronized void updatePlan(String name, int months, long cents) throws SQLException {
        ensureInitialized();
        require(months >= 1 && months <= 120, "Plan duration must be between 1 and 120 months.");
        require(cents > 0, "Plan price must be greater than zero.");
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(
                        "UPDATE membership_plans SET months=?,price_cents=? WHERE name=? COLLATE NOCASE")) {
            p.setInt(1, months);
            p.setLong(2, cents);
            p.setString(3, name);
            if (p.executeUpdate() == 0)
                throw new IllegalArgumentException("Membership plan does not exist.");
        }
    }

    public static synchronized void createMembership(int memberId, String plan, int months, long price, LocalDate start,
            boolean paid, String method) throws SQLException {
        ensureInitialized();
        require(getMember(memberId) != null, "Member ID does not exist.");
        require(getActiveMembership(memberId) == null,
                "This member already has an active membership. Record payment against it or wait until it expires.");
        require(start != null && !start.isBefore(LocalDate.of(2000, 1, 1)), "Membership start date is invalid.");
        Map<String, Object> selected = getPlan(plan);
        require(selected != null, "Membership plan does not exist.");
        int expectedMonths = (Integer) selected.get("months");
        long expectedPrice = (Long) selected.get("price");
        require(months == expectedMonths && price == expectedPrice,
                "Membership plan details do not match the selected plan.");
        if (paid)
            require(method != null && !method.isBlank(), "Payment method is required.");
        LocalDate end = start.plusMonths(months);

        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try {
                int membershipId;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO memberships(member_id,plan,months,price_cents,start_date,end_date) VALUES(?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    p.setInt(1, memberId);
                    p.setString(2, plan);
                    p.setInt(3, months);
                    p.setLong(4, price);
                    p.setString(5, start.toString());
                    p.setString(6, end.toString());
                    p.executeUpdate();
                    try (ResultSet k = p.getGeneratedKeys()) {
                        if (!k.next())
                            throw new SQLException("Could not create membership ID.");
                        membershipId = k.getInt(1);
                    }
                }
                if (paid) {
                    try (PreparedStatement pay = c.prepareStatement(
                            "INSERT INTO payments(member_id,membership_id,amount_cents,method,date,status) VALUES(?,?,?,?,?,?)")) {
                        pay.setInt(1, memberId);
                        pay.setInt(2, membershipId);
                        pay.setLong(3, price);
                        pay.setString(4, method.trim());
                        pay.setString(5, LocalDate.now().toString());
                        pay.setString(6, "Paid");
                        pay.executeUpdate();
                    }
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se)
                    throw se;
                throw new SQLException(e);
            }
        }
    }

    public static synchronized Membership getActiveMembership(int memberId) throws SQLException {
        ensureInitialized();
        LocalDate today = LocalDate.now();
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(
                        "SELECT m.*, COALESCE(mem.name,'Unknown') member_name FROM memberships m LEFT JOIN members mem ON mem.id=m.member_id WHERE m.member_id=? AND m.end_date>=? ORDER BY m.end_date DESC LIMIT 1")) {
            p.setInt(1, memberId);
            p.setString(2, today.toString());
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? membership(r) : null;
            }
        }
    }

    public static synchronized List<Membership> getMemberships(String search) throws SQLException {
        ensureInitialized();
        String q = search == null ? "" : "%" + search.trim() + "%";
        List<Membership> out = new ArrayList<>();
        String sql = "SELECT m.*,COALESCE(mem.name,'Unknown') member_name FROM memberships m LEFT JOIN members mem ON mem.id=m.member_id WHERE ?='' OR lower(member_name) LIKE lower(?) OR CAST(m.member_id AS TEXT) LIKE ? OR lower(m.plan) LIKE lower(?) ORDER BY m.id DESC";
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(sql)) {
            String raw = search == null ? "" : search.trim();
            p.setString(1, raw);
            p.setString(2, q);
            p.setString(3, q);
            p.setString(4, q);
            try (ResultSet r = p.executeQuery()) {
                while (r.next())
                    out.add(membership(r));
            }
        }
        return out;
    }

    private static Membership membership(ResultSet r) throws SQLException {
        String start = r.getString("start_date");
        int months = r.getInt("months");
        String end = LocalDate.parse(start).plusMonths(months).toString();
        LocalDate today = LocalDate.now();
        LocalDate endDate = LocalDate.parse(end);
        String status = endDate.isBefore(today) ? "Expired"
                : LocalDate.parse(start).isAfter(today) ? "Scheduled" : "Active";
        return new Membership(r.getInt("id"), r.getInt("member_id"), r.getString("member_name"), r.getString("plan"),
                months, r.getLong("price_cents"), start, end, status);
    }

    public static synchronized List<Payment> getPayments() throws SQLException {
        ensureInitialized();
        List<Payment> out = new ArrayList<>();
        String sql = "SELECT p.*,COALESCE(m.name,'Unknown') member_name,COALESCE(ms.plan,'Unknown') plan FROM payments p LEFT JOIN members m ON m.id=p.member_id LEFT JOIN memberships ms ON ms.id=p.membership_id ORDER BY p.id DESC";
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(sql);
                ResultSet r = p.executeQuery()) {
            while (r.next())
                out.add(new Payment(r.getInt("id"), r.getInt("member_id"), r.getInt("membership_id"),
                        r.getString("member_name"), r.getString("plan"), r.getLong("amount_cents"),
                        r.getString("method"), r.getString("date"), r.getString("status")));
        }
        return out;
    }

    public static synchronized List<Membership> getUnpaidMemberships() throws SQLException {
        ensureInitialized();
        List<Membership> out = new ArrayList<>();
        String sql = "SELECT m.*,COALESCE(mem.name,'Unknown') member_name FROM memberships m LEFT JOIN members mem ON mem.id=m.member_id LEFT JOIN payments p ON p.membership_id=m.id WHERE m.end_date>=? AND p.id IS NULL ORDER BY m.end_date";
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, LocalDate.now().toString());
            try (ResultSet r = p.executeQuery()) {
                while (r.next())
                    out.add(membership(r));
            }
        }
        return out;
    }

    public static synchronized void recordMembershipPayment(int membershipId, long amount, String method,
            LocalDate date) throws SQLException {
        ensureInitialized();
        require(date != null && !date.isAfter(LocalDate.now()), "Payment date cannot be in the future.");
        require(method != null && !method.isBlank(), "Payment method is required.");
        try (Connection c = connection();
                PreparedStatement q = c.prepareStatement("SELECT member_id,price_cents FROM memberships WHERE id=?");) {
            q.setInt(1, membershipId);
            try (ResultSet r = q.executeQuery()) {
                require(r.next(), "Membership does not exist.");
                int memberId = r.getInt(1);
                long price = r.getLong(2);
                require(amount == price,
                        "Payment must exactly match the membership price: " + Money.format(price) + ".");
                try (PreparedStatement chk = c.prepareStatement("SELECT 1 FROM payments WHERE membership_id=?")) {
                    chk.setInt(1, membershipId);
                    try (ResultSet x = chk.executeQuery()) {
                        require(!x.next(), "This membership has already been paid.");
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO payments(member_id,membership_id,amount_cents,method,date,status) VALUES(?,?,?,?,?,?)")) {
                    p.setInt(1, memberId);
                    p.setInt(2, membershipId);
                    p.setLong(3, amount);
                    p.setString(4, method.trim());
                    p.setString(5, date.toString());
                    p.setString(6, "Paid");
                    p.executeUpdate();
                }
            }
        }
    }

    public static synchronized List<TrainerPayment> getTrainerPayments() throws SQLException {
        ensureInitialized();
        List<TrainerPayment> out = new ArrayList<>();
        String sql = "SELECT p.*,COALESCE(t.name,'Unknown') trainer_name FROM trainer_payments p LEFT JOIN trainers t ON t.id=p.trainer_id ORDER BY p.id DESC";
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(sql);
                ResultSet r = p.executeQuery()) {
            while (r.next())
                out.add(new TrainerPayment(r.getInt("id"), r.getInt("trainer_id"), r.getString("trainer_name"),
                        r.getLong("amount_cents"), r.getString("payment_month"), r.getString("date"),
                        r.getString("method"), r.getString("status")));
        }
        return out;
    }

    public static synchronized void payTrainer(int trainerId, long amount, YearMonth month, LocalDate date,
            String method) throws SQLException {
        ensureInitialized();
        require(month != null, "Payment month is required.");
        require(!month.isAfter(YearMonth.now()), "Trainer payment month cannot be in the future.");
        require(date != null && !date.isAfter(LocalDate.now()), "Payment date cannot be in the future.");
        require(method != null && !method.isBlank(), "Payment method is required.");

        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try {
                long fixed;
                try (PreparedStatement q = c.prepareStatement("SELECT monthly_pay_cents FROM trainers WHERE id=?")) {
                    q.setInt(1, trainerId);
                    try (ResultSet r = q.executeQuery()) {
                        require(r.next(), "Trainer does not exist.");
                        fixed = r.getLong(1);
                    }
                }
                require(amount == fixed, "Trainer payment is fixed at " + Money.format(fixed) + ".");
                require(getAvailableBalanceCents() >= amount, "Not enough money in the gym balance.");
                try (PreparedStatement chk = c
                        .prepareStatement("SELECT 1 FROM trainer_payments WHERE trainer_id=? AND payment_month=?")) {
                    chk.setInt(1, trainerId);
                    chk.setString(2, month.toString());
                    try (ResultSet x = chk.executeQuery()) {
                        require(!x.next(), "This trainer has already been paid for " + month + ".");
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO trainer_payments(trainer_id,amount_cents,payment_month,date,method,status) VALUES(?,?,?,?,?,?)")) {
                    p.setInt(1, trainerId);
                    p.setLong(2, amount);
                    p.setString(3, month.toString());
                    p.setString(4, date.toString());
                    p.setString(5, method.trim());
                    p.setString(6, "Paid");
                    p.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se)
                    throw se;
                throw new SQLException(e);
            }
        }
    }

    public static synchronized long getTotalMemberPaymentsCents() throws SQLException {
        ensureInitialized();
        return sum("SELECT COALESCE(SUM(amount_cents),0) FROM payments WHERE lower(status)='paid'");
    }

    public static synchronized long getTotalTrainerPaymentsCents() throws SQLException {
        ensureInitialized();
        return sum("SELECT COALESCE(SUM(amount_cents),0) FROM trainer_payments WHERE lower(status)='paid'");
    }

    private static long sum(String sql) throws SQLException {
        try (Connection c = connection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            return r.next() ? r.getLong(1) : 0;
        }
    }

    public static synchronized long getAvailableBalanceCents() throws SQLException {
        return getTotalMemberPaymentsCents() - getTotalTrainerPaymentsCents();
    }

    public static synchronized int count(String table) throws SQLException {
        ensureInitialized();
        Set<String> allowed = Set.of("members", "trainers", "memberships", "payments", "trainer_payments");
        if (!allowed.contains(table))
            throw new IllegalArgumentException("Invalid table.");
        try (Connection c = connection();
                Statement s = c.createStatement();
                ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return r.next() ? r.getInt(1) : 0;
        }
    }

    public static synchronized List<Member> recentMembers(int limit) throws SQLException {
        List<Member> all = getMembers("");
        return all.subList(0, Math.min(Math.max(0, limit), all.size()));
    }

    public static synchronized int activeMemberCount() throws SQLException {
        ensureInitialized();
        String today = LocalDate.now().toString();
        try (Connection c = connection();
                PreparedStatement p = c
                        .prepareStatement("SELECT COUNT(DISTINCT member_id) FROM memberships WHERE end_date>=?")) {
            p.setString(1, today);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        }
    }

    public static synchronized int expiringSoonCount() throws SQLException {
        ensureInitialized();
        LocalDate today = LocalDate.now();
        try (Connection c = connection();
                PreparedStatement p = c
                        .prepareStatement("SELECT COUNT(*) FROM memberships WHERE end_date>=? AND end_date<=?")) {
            p.setString(1, today.toString());
            p.setString(2, today.plusDays(7).toString());
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition)
            throw new IllegalArgumentException(message);
    }
}

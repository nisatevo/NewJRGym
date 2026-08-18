package gym.model;

public class Trainer implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final int id;
    private final String name, specialization, phone, email, photoPath;
    private final int age;
    private final long monthlyPayCents;

    public Trainer(int id, String name, int age, String specialization, String phone, String email,
            long monthlyPayCents, String photoPath) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.specialization = specialization;
        this.phone = phone;
        this.email = email;
        this.monthlyPayCents = monthlyPayCents;
        this.photoPath = photoPath;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public long getMonthlyPayCents() {
        return monthlyPayCents;
    }

    public String getPhotoPath() {
        return photoPath;
    }
}

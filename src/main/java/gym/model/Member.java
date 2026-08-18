package gym.model;

public class Member implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final int id;
    private final String name, gender, phone, email, address, photoPath;
    private final int age;

    public Member(int id, String name, int age, String gender, String phone, String email, String address,
            String photoPath) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.address = address;
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

    public String getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getPhotoPath() {
        return photoPath;
    }
}

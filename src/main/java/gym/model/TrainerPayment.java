package gym.model;

public class TrainerPayment implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final int id, trainerId;
    private final String trainerName, paymentMonth, date, method, status;
    private final long amountCents;

    public TrainerPayment(int id, int trainerId, String trainerName, long amountCents, String paymentMonth, String date,
            String method, String status) {
        this.id = id;
        this.trainerId = trainerId;
        this.trainerName = trainerName;
        this.amountCents = amountCents;
        this.paymentMonth = paymentMonth;
        this.date = date;
        this.method = method;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getTrainerId() {
        return trainerId;
    }

    public String getTrainerName() {
        return trainerName;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getPaymentMonth() {
        return paymentMonth;
    }

    public String getDate() {
        return date;
    }

    public String getMethod() {
        return method;
    }

    public String getStatus() {
        return status;
    }
}

package gym.model;

public class Payment implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final int id, memberId, membershipId;
    private final String memberName, plan, method, date, status;
    private final long amountCents;

    public Payment(int id, int memberId, int membershipId, String memberName, String plan, long amountCents,
            String method, String date, String status) {
        this.id = id;
        this.memberId = memberId;
        this.membershipId = membershipId;
        this.memberName = memberName;
        this.plan = plan;
        this.amountCents = amountCents;
        this.method = method;
        this.date = date;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getMemberId() {
        return memberId;
    }

    public int getMembershipId() {
        return membershipId;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getPlan() {
        return plan;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getMethod() {
        return method;
    }

    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }
}

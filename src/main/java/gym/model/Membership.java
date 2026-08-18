package gym.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Membership implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final int id, memberId, durationMonths;
    private final String memberName, plan, startDate, endDate, status;
    private final long priceCents;

    public Membership(int id, int memberId, String memberName, String plan, int durationMonths, long priceCents, String startDate, String endDate, String status) {
        this.id=id; this.memberId=memberId; this.memberName=memberName; this.plan=plan;
        this.durationMonths=durationMonths; this.priceCents=priceCents; this.startDate=startDate;
        this.endDate=endDate; this.status=status;
    }

    public int getId(){return id;}
    public int getMemberId(){return memberId;}
    public String getMemberName(){return memberName;}
    public String getPlan(){return plan;}
    public int getDurationMonths(){return durationMonths;}
    public long getPriceCents(){return priceCents;}
    public String getStartDate(){return startDate;}

    /**
     * Always derive the end date from the stored start date and duration.
     * This repairs memberships created by older versions that may contain
     * an incorrect persisted end date.
     */
    public String getEndDate(){
        return LocalDate.parse(startDate).plusMonths(durationMonths).toString();
    }

    public String getStatus(){return status;}

    public long remainingDays(){
        LocalDate today = LocalDate.now();
        LocalDate end = LocalDate.parse(startDate).plusMonths(durationMonths);
        return Math.max(0, ChronoUnit.DAYS.between(today, end));
    }
}

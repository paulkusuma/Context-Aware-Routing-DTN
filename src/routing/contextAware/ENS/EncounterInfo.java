package routing.contextAware.ENS;

public class EncounterInfo {
    public double MeetingTime;
    public int RemainingBuffer;
    public double RemainingEnergy;
    public double connDuration;

    public EncounterInfo(double meetingTime, int remainingBuffer, double remainingEnergy, double connDuration) {
        this.connDuration = connDuration;
        RemainingBuffer = remainingBuffer;
        RemainingEnergy = remainingEnergy;
        MeetingTime = meetingTime;
    }
}

package ch.bbv.alea.bpm;

public interface WorkflowService {

	public boolean checkUploadedGame(long gameId);

	public void sendBadUploadEMail(long gameId);

	public boolean checkCustomerVip(long gameId);

	public void publishGame(long gameId);

	public void sendGamePublishedEMail(long gameId);

	public void sendGameRejectedEMail(long gameId, String reason);
}

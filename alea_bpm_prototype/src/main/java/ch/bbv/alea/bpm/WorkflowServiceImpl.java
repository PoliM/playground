package ch.bbv.alea.bpm;

public class WorkflowServiceImpl implements WorkflowService {

	@Override
	public boolean checkUploadedGame(long gameId) {
		return false;
	}

	@Override
	public void sendBadUploadEMail(long gameId) {
	}

	@Override
	public boolean checkCustomerVip(long gameId) {
		return false;
	}

	@Override
	public void publishGame(long gameId) {
	}

	@Override
	public void sendGamePublishedEMail(long gameId) {
	}

	@Override
	public void sendGameRejectedEMail(long gameId, String reason) {
	}

	@Override
	public GameUploadState checkGameUploadState(long gameId) {
		return null;
	}

}

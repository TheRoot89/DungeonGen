package mineCSweeper;

import java.util.function.BiConsumer;

import mcPluginHelpers.MsgLevel;

public class MCSGameStateHandler {
	
	private BiConsumer<MsgLevel,String> messageCallback = null;
	private boolean messageCallbackIsSet = false;
	private GameState state;
	private String lastMessage;
	private MsgLevel lastMsgLevel;
	
	public static MCSGameStateHandler initializeNewGameState(BiConsumer<MsgLevel,String> messageCallback) {
		MCSGameStateHandler gs = getNewlyInitializedGameState();
		gs.publish();
		return gs;
	}
	
	public static MCSGameStateHandler getNewlyInitializedGameState() {
		return new MCSGameStateHandler(GameState.NOT_STARTED, MsgLevel.INFO, "Game initialization successfull.");
	}
	
	public MCSGameStateHandler(GameState state, MsgLevel level, String message) {
		this.state = state;
		this.lastMessage = message;
		this.lastMsgLevel = level;
	}
	
	public void registerMessageCallback(BiConsumer<MsgLevel,String> messageCallback) {
		this.messageCallback = messageCallback;
		messageCallbackIsSet = true;
	}
	
	public void publish() {
		if (messageCallbackIsSet) {
			messageCallback.accept(lastMsgLevel,lastMsgLevel.getPrefix() + lastMessage);
		}
	}
	
	public void setState(GameState newState) {
		this.state = newState;
		setMessage("State set to " + state.toString(), MsgLevel.DEBUG);
	}
	
	public void setMessage(String message, MsgLevel level) {
		lastMessage = message;
		lastMsgLevel = level;
		publish();
	}
	
	public void setState(GameState newState, String message, MsgLevel level) {
		this.state = newState;
		setMessage(message, level);
	}
	
	public void setToError(String message) {
		this.state = GameState.ERROR;
		setMessage(message, MsgLevel.ERROR);
	}
	
	public void logErrorKeepState(String message) {
		setMessage(message, MsgLevel.ERROR);
	}
	
	public void logWarning(String message) {
		setMessage(message, MsgLevel.WARNING);
	}
	
	public void logInfo(String message) {
		setMessage(message, MsgLevel.INFO);
	}
	
	public MsgLevel getMessageLevel() {
		return lastMsgLevel;
	}
	
	public GameState getState() {
		return state;
	}
	
	public String getMessage() {
		return lastMessage;
	}
	
	public boolean isError() {
		return state == GameState.ERROR;
	}

	public void logDebug(String message) {
		setMessage(message, MsgLevel.DEBUG);
	}
	
}

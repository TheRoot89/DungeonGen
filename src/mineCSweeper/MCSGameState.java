package mineCSweeper;

import java.util.function.BiConsumer;
import java.util.function.Function;






public class MCSGameState {
	
	private BiConsumer<MsgLevel,String> messageCallback = null;
	private boolean messageCallbackIsSet = false;
	private GameState state;
	private String lastMessage;
	private MsgLevel lastMsgLevel;
	
	public MCSGameState(GameState state, MsgLevel level, String message) {
		this.state = state;
		this.lastMessage = message;
		this.lastMsgLevel = level;
	}
	
	public void registerMessageCallback(BiConsumer<MsgLevel,String> messageCallback) {
		this.messageCallback = messageCallback;
		messageCallbackIsSet = true;
	}
	
	public void setState(GameState newState) {
		this.state = newState;
		setMessage("State set to " + state.toString(), MsgLevel.DEBUG);
	}
	
	public void setMessage(String message, MsgLevel level) {
		lastMessage = message;
		lastMsgLevel = level;
		if (messageCallbackIsSet) {
			messageCallback.accept(level, message);
		}
	}
	
	public void setState(GameState newState, String message, MsgLevel level) {
		this.state = newState;
		setMessage(message, level);
	}
	
	public void setToError(String message) {
		this.state = GameState.ERROR;
		setMessage(message,MsgLevel.ERROR);
	}
	
	public void logError(String message) {
		setMessage(message,MsgLevel.ERROR);
	}
	
	public void logWarning(String message) {
		setMessage(message,MsgLevel.WARNING);
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
}

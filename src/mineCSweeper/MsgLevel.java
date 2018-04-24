package mineCSweeper;

public enum MsgLevel{
	DEBUG,
	INFO,
	WARNING,
	ERROR;
	
	public boolean isAsSeriousAs(MsgLevel otherMsgLvl) {
		return (this.compareTo(otherMsgLvl) >= 0); //TODO: verify
	}
}

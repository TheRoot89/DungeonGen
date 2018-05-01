package mineCSweeper;

import org.bukkit.ChatColor;

public enum MsgLevel{
	DEBUG(ChatColor.GRAY),
	INFO(ChatColor.WHITE),
	WARNING(ChatColor.YELLOW),
	ERROR(ChatColor.RED);
	
	private ChatColor chatColor;
	
	private MsgLevel(ChatColor color) {
		this.chatColor = color;
	}
	
	public boolean isAtLeastAsSeriousAs(MsgLevel otherMsgLvl) {
		return (this.compareTo(otherMsgLvl) >= 0); //TODO: verify
	}
	
	public ChatColor getChatColor() {
		return chatColor;
	}
	
	public String getPrefix() {
		return "[" + toString() + "] ";
	}
}

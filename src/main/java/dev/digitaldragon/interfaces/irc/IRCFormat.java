package dev.digitaldragon.interfaces.irc;

public enum IRCFormat {
    //https://modern.ircdocs.horse/formatting
    RESET("\u000F"),
    BOLD ("\u0002"),
    ITALIC ("\u001D"),
    UNDERLINE("\u001F"),
    STRIKETHROUGH("\u001E"),
    MONOSPACE("\u0011"),
    WHITE("\u000300"),
    BLACK("\u000301"),
    BLUE("\u000302"),
    GREEN("\u000303"),
    RED("\u000304"),
    BROWN("\u000305"),
    MAGENTA("\u000306"),
    ORANGE("\u000307"),
    YELLOW("\u000308"),
    LIGHT_GREEN("\u000309"),
    CYAN("\u000310"),
    LIGHT_CYAN("\u000311"),
    LIGHT_BLUE("\u000312"),
    PINK("\u000313"),
    GREY("\u000314"),
    LIGHT_GREY("\u000315");

    final String controlCode;
    IRCFormat(String controlCode) {
        this.controlCode = controlCode;
    }
    @Override
    public String toString() {
        return controlCode;
    }
}

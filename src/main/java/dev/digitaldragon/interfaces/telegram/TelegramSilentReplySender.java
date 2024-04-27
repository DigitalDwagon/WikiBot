package dev.digitaldragon.interfaces.telegram;

import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Optional;

public class TelegramSilentReplySender extends SilentSender{
    public TelegramSilentReplySender(MessageSender sender) {
        super(sender);
    }

    public Optional<Message> sendReplyMessage(String txt, long groupId, int replyToMessageId) {
        return doSendReplyMessage(txt, groupId, replyToMessageId, false);
    }

    private Optional<Message> doSendReplyMessage(String txt, long groupId, int replyToMessageId, boolean format) {
        SendMessage smsg = new SendMessage();
        smsg.setChatId(groupId);
        smsg.setText(txt);
        smsg.setReplyToMessageId(replyToMessageId);
        smsg.enableMarkdown(format);

        return execute(smsg);
  }
}

package com.tencent.qcloud.tuikit.tuiconversation.classicui.interfaces;

import android.view.View;

import com.tencent.qcloud.tuikit.timcommon.component.interfaces.ILayout;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationInfo;

public interface IConversationLayout extends ILayout {

    View getConversationList();

    void setConversationTop(ConversationInfo conversation);

    void deleteConversation(ConversationInfo conversation);

    void clearConversation(ConversationInfo conversation);

    /**
     * Fold normal conversation
     */
    void markConversationFold(ConversationInfo conversation);

    /**
     * Mark conversation fold or unfold
     */
    void markConversationFold(ConversationInfo conversation, boolean markFold);

    /**
     * Hide normal conversation
     */
    void markConversationHidden(ConversationInfo conversation);

    /**
     * Mark conversation read or unread
     */
    void markConversationUnread(ConversationInfo conversationInfo, boolean markUnread);

    /**
     * Hide folded conversation item
     */
    void setHideStatusOfFoldedItem(boolean needHide);

    /**
     * Clear unread status of fold item
     */
    void clearUnreadStatusOfFoldItem();
}

package com.tencent.qcloud.tuikit.tuiconversation.interfaces;

import com.tencent.qcloud.tuikit.timcommon.bean.TUIMessageBean;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationInfo;

public interface IConversationPresenter {

    void setConversationListener();

    void destroy();

    void setAdapter(IConversationListAdapter adapter);

    void setShowType(int showType);

    void loadMoreConversation();

    void loadMarkedConversation();

    boolean isLoadFinished();

    boolean isTopConversation(String chatId);

    void setConversationTop(final ConversationInfo conversation);

    void setConversationTop(String chatId, final boolean isTop);

    void deleteConversation(ConversationInfo conversation);

    void deleteConversation(String chatId, boolean isGroup);

    void clearConversation(ConversationInfo conversation);

    void clearConversation(String chatId, boolean isGroup);

    void markConversationRead(String chatId);

    void markConversationUnread(String chatId);

    void markConversationUnread(ConversationInfo conversationInfo, boolean markUnread);

    void markConversationHidden(ConversationInfo conversationInfo, boolean markHidden);

    void markConversationFold(ConversationInfo conversationInfo, boolean markFold);

    void cleanConversationUnreadCount(String chatId);

    void cleanAllConversationUnreadCount();

    static String getMessageDisplayString(TUIMessageBean messageBean) {
        return null;
    }
}

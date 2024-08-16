package com.tencent.qcloud.tuikit.tuiconversation.interfaces;

import com.tencent.qcloud.tuicore.interfaces.TUICallback;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationInfo;

public interface IConversationPresenter {

    void setConversationListener();

    void setAdapter(IConversationListAdapter adapter);

    void loadMoreConversation();

    void loadMarkedConversation();

    void destroy();
//
//    void setConversationListener();
//
//    void destroy();
//
//    void setAdapter(IConversationListAdapter adapter);
//
//    void setShowType(int showType);
//
//    void loadMoreConversation();
//
//    void loadMarkedConversation();
//
//    boolean isLoadFinished();
//
//    boolean isTopConversation(String conversationID);
//
//    void setConversationTop(final ConversationInfo conversation);
//
//    void setConversationTop(String id, final boolean isTop);
//
//    void deleteConversation(ConversationInfo conversation);
//
//    void deleteConversation(String id, boolean isGroup);
//
//    void clearConversation(ConversationInfo conversation);
//
//    void clearConversation(String chatId, boolean isGroup);
//
//    void markConversationHidden(ConversationInfo conversationInfo, boolean isHidden);
//
//    void markConversationUnread(ConversationInfo conversationInfo, boolean markUnread);
//
//    void cleanConversationUnreadCount(String conversationID, TUICallback callback);
//
//    void clearAllUnreadMessage();
//
//    void updateUnreadTotalByDiff(int diff);
}

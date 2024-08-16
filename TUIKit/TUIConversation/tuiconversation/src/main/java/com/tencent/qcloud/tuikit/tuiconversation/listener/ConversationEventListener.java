package com.tencent.qcloud.tuikit.tuiconversation.listener;

import com.tencent.imsdk.v2.V2TIMUserStatus;
import com.tencent.qcloud.tuikit.timcommon.bean.TUIMessageBean;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationInfo;

import java.util.List;

public interface ConversationEventListener {

    /**
     * V2TIMConversationListener
     */
    //同步服务器会话完成
    void onSyncServerFinish();

    //有会话新增
    void onNewConversation(List<ConversationInfo> conversationList);

    //有会话更新
    void onConversationChanged(List<ConversationInfo> conversationList);

    //有会话被删除
    void onConversationDeleted(List<String> conversationIDList);

    //会话未读总数变更通知
    void updateTotalUnreadMessageCount(long count);

    /**
     * V2TIMSDKListener
     */
    void onUserStatusChanged(List<V2TIMUserStatus> userStatusList);

    /**
     * ITUIService
     */
    boolean isTopConversation(String chatId);

    void setConversationTop(String chatId, boolean isTop);

    long getUnreadTotal();

    void clearFoldMarkAndDeleteConversation(String conversationId);

    /**
     * ITUINotification
     */
    void deleteConversation(String chatId, boolean isGroup);

    void clearConversation(String chatId, boolean isGroup);

    void onFriendRemarkChanged(String id, String remark);

    void onReceiveMessage(String conversationID, boolean isTypingMessage);

    void onMessageSendForHideConversation(String conversationID);

    void onConversationLastMessageBeanChanged(String conversationID, TUIMessageBean messageBean);

    void refreshUserStatusFragmentUI();
}

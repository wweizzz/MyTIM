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
    // 获取未读数
    long getUnreadTotal();

    // 清除折叠并且删除删除会话
    void clearFoldMarkAndDeleteConversation(String conversationId);

    /**
     * ITUINotification
     */
    // 收到消息
    void onReceiveMessage(String conversationID, boolean isTypingMessage);

    // 收到消息来自隐藏会话
    void onReceiveMessageSendForHideConversation(String conversationID);

    // 会话最后一条信息变更
    void onConversationLastMessageBeanChanged(String conversationID, TUIMessageBean messageBean);
}

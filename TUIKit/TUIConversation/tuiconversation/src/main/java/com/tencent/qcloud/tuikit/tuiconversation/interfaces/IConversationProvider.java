package com.tencent.qcloud.tuikit.tuiconversation.interfaces;

import com.tencent.imsdk.v2.V2TIMConversation;
import com.tencent.imsdk.v2.V2TIMConversationListFilter;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import com.tencent.qcloud.tuicore.interfaces.TUICallback;
import com.tencent.qcloud.tuikit.timcommon.component.interfaces.IUIKitCallback;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationInfo;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationUserStatusBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IConversationProvider {

    void loadMoreConversation(int loadCount, IUIKitCallback<List<ConversationInfo>> callBack);

    void loadConversation(
            long startSeq, int loadCount, final IUIKitCallback<List<ConversationInfo>> callBack);

    void loadMarkConversation(
            final V2TIMConversationListFilter filter,
            long startSeq, int loadCount, boolean fromStart, IUIKitCallback<List<ConversationInfo>> callback);

    boolean isLoadFinished();

    void getConversation(String conversationID, IUIKitCallback<ConversationInfo> callback);

    void getTotalUnreadMessageCount(IUIKitCallback<Long> callBack);

    void setConversationTop(String conversationId, boolean isTop, IUIKitCallback<Void> callBack);

    void markConversationFold(String conversationID, boolean isFold, IUIKitCallback<Void> callback);

    void markConversationHidden(String conversationID, boolean isHidden, IUIKitCallback<Void> callback);

    void markConversationRead(String conversationID, TUICallback callback);

    void markConversationUnread(String conversationID, TUICallback callback);

    void markConversationUnread(ConversationInfo conversationInfo, boolean markUnread, IUIKitCallback<Void> callback);

    void cleanConversationUnreadCount(String conversationID, TUICallback callback);

    void deleteConversation(String conversationId, IUIKitCallback<Void> callBack);

    void clearHistoryMessage(String userId, boolean isGroup, IUIKitCallback<Void> callBack);

    void getGroupMemberIconList(String groupId, int iconCount, IUIKitCallback<List<Object>> callback);

    void loadConversationUserStatus(List<ConversationInfo> dataSource, IUIKitCallback<Map<String, ConversationUserStatusBean>> callback);

    void subscribeConversationUserStatus(List<String> userIdList, IUIKitCallback<Void> callback);

    void clearAllUnreadMessage(IUIKitCallback<Void> callback);

    void getMarkUnreadConversationList(
            V2TIMConversationListFilter filter, long nextSeq, int count, boolean fromStart, V2TIMValueCallback<HashMap<String, V2TIMConversation>> callback);
}

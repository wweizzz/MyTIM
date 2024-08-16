package com.tencent.qcloud.tuikit.tuiconversation;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.auto.service.AutoService;
import com.tencent.imsdk.v2.V2TIMConversation;
import com.tencent.imsdk.v2.V2TIMConversationListener;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMSDKListener;
import com.tencent.imsdk.v2.V2TIMUserStatus;
import com.tencent.qcloud.tuicore.ServiceInitializer;
import com.tencent.qcloud.tuicore.TUIConstants;
import com.tencent.qcloud.tuicore.TUICore;
import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.TUIThemeManager;
import com.tencent.qcloud.tuicore.annotations.TUIInitializerDependency;
import com.tencent.qcloud.tuicore.annotations.TUIInitializerID;
import com.tencent.qcloud.tuicore.interfaces.ITUINotification;
import com.tencent.qcloud.tuicore.interfaces.ITUIService;
import com.tencent.qcloud.tuicore.interfaces.TUIInitializer;
import com.tencent.qcloud.tuikit.timcommon.bean.TUIMessageBean;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationGroupBean;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationInfo;
import com.tencent.qcloud.tuikit.tuiconversation.commonutil.ConversationUtils;
import com.tencent.qcloud.tuikit.tuiconversation.commonutil.TUIConversationLog;
import com.tencent.qcloud.tuikit.tuiconversation.listener.ConversationEventListener;
import com.tencent.qcloud.tuikit.tuiconversation.listener.ConversationGroupNotifyListener;
import com.tencent.qcloud.tuikit.tuiconversation.presenter.ConversationPresenter;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@AutoService(TUIInitializer.class)
@TUIInitializerDependency("TIMCommon")
@TUIInitializerID("TUIConversation")
public class TUIConversationService implements TUIInitializer, ITUIService, ITUINotification {
    public static final String TAG = TUIConversationService.class.getSimpleName();
    private static TUIConversationService instance;

    public static TUIConversationService getInstance() {
        return instance;
    }

    private boolean syncFinished = false;

    private SoftReference<ConversationEventListener> conversationEventListener;
    private final List<SoftReference<ConversationEventListener>> conversationEventListenerList = new ArrayList<>();
    private SoftReference<ConversationGroupNotifyListener> conversationGroupNotifyListener;
    private int conversationAllGroupUnreadDiff = 0;

    @Override
    public void init(Context context) {
        instance = this;
        initTheme();
        initService();
        initEvent();
        initIMListener();
    }

    private void initTheme() {
        TUIThemeManager.addLightTheme(R.style.TUIConversationLightTheme);
        TUIThemeManager.addLivelyTheme(R.style.TUIConversationLivelyTheme);
        TUIThemeManager.addSeriousTheme(R.style.TUIConversationSeriousTheme);
    }

    private void initService() {
        TUICore.registerService(TUIConstants.TUIConversation.SERVICE_NAME, this);
    }

    private void initEvent() {
        TUICore.registerEvent(TUIConstants.TUIGroup.EVENT_GROUP, TUIConstants.TUIGroup.EVENT_SUB_KEY_EXIT_GROUP, this);
        TUICore.registerEvent(TUIConstants.TUIGroup.EVENT_GROUP, TUIConstants.TUIGroup.EVENT_SUB_KEY_MEMBER_KICKED_GROUP, this);
        TUICore.registerEvent(TUIConstants.TUIGroup.EVENT_GROUP, TUIConstants.TUIGroup.EVENT_SUB_KEY_GROUP_DISMISS, this);
        TUICore.registerEvent(TUIConstants.TUIGroup.EVENT_GROUP, TUIConstants.TUIGroup.EVENT_SUB_KEY_GROUP_RECYCLE, this);
        TUICore.registerEvent(TUIConstants.TUIGroup.EVENT_GROUP, TUIConstants.TUIGroup.EVENT_SUB_KEY_CLEAR_MESSAGE, this);

        TUICore.registerEvent(TUIConstants.TUIContact.EVENT_USER, TUIConstants.TUIContact.EVENT_SUB_KEY_CLEAR_MESSAGE, this);
        TUICore.registerEvent(TUIConstants.TUIContact.EVENT_FRIEND_INFO_CHANGED, TUIConstants.TUIContact.EVENT_SUB_KEY_FRIEND_REMARK_CHANGED, this);

        TUICore.registerEvent(
                TUIConstants.TUIChat.EVENT_KEY_RECEIVE_MESSAGE, TUIConstants.TUIChat.EVENT_SUB_KEY_CONVERSATION_ID, this);

        TUICore.registerEvent(
                TUIConstants.TUIConversation.EVENT_KEY_MESSAGE_SEND_FOR_CONVERSATION, TUIConstants.TUIConversation.EVENT_SUB_KEY_MESSAGE_SEND_FOR_CONVERSATION, this);

        TUICore.registerEvent(
                TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_KEY, TUIConversationConstants.EVENT_CONVERSATION_GROUP_ADD_DATA, this);
        TUICore.registerEvent(
                TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_KEY, TUIConversationConstants.EVENT_CONVERSATION_GROUP_ADD_MARK_DATA, this);
        TUICore.registerEvent(
                TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_KEY, TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_ADD, this);
        TUICore.registerEvent(
                TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_KEY, TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_DELETE, this);
        TUICore.registerEvent(
                TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_KEY, TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_RENAME, this);
        TUICore.registerEvent(
                TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_KEY, TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_UNREAD_COUNT, this);

        TUICore.registerEvent(
                TUIConstants.TUIChat.Event.MessageDisplayString.KEY, TUIConstants.TUIChat.Event.MessageDisplayString.SUB_KEY_PROCESS_MESSAGE, this);
    }

    @Override
    public Object onCall(String method, Map<String, Object> param) {
        Bundle result = new Bundle();

        if (TextUtils.equals(method, TUIConversationConstants.CONVERSATION_ALL_GROUP_UNREAD_CHANGE_BY_DIFF)) {
            if (param == null) {
                return result;
            }
            ConversationEventListener eventListener = getConversationEventListener();
            if (eventListener == null) {
                int diff = (int) param.get(TUIConversationConstants.CONVERSATION_ALL_GROUP_UNREAD_DIFF);
                conversationAllGroupUnreadDiff += diff;
                ConversationPresenter presenter = new ConversationPresenter();
                presenter.updateUnreadTotalByDiff(conversationAllGroupUnreadDiff);
            }
        }

        ConversationEventListener conversationEventListener = getInstance().getConversationEventListener();
        if (conversationEventListener == null) {
            TUIConversationLog.e(TAG, "execute " + method + " failed , conversationEvent listener is null");
            return result;
        }
        if (TextUtils.equals(TUIConstants.TUIConversation.METHOD_IS_TOP_CONVERSATION, method)) {
            String chatId = (String) param.get(TUIConstants.TUIConversation.CHAT_ID);
            if (!TextUtils.isEmpty(chatId)) {
                boolean isTop = conversationEventListener.isTopConversation(chatId);
                result.putBoolean(TUIConstants.TUIConversation.IS_TOP, isTop);
            }
        } else if (TextUtils.equals(TUIConstants.TUIConversation.METHOD_SET_TOP_CONVERSATION, method)) {
            String chatId = (String) param.get(TUIConstants.TUIConversation.CHAT_ID);
            boolean isTop = (boolean) param.get(TUIConstants.TUIConversation.IS_SET_TOP);
            if (!TextUtils.isEmpty(chatId)) {
                conversationEventListener.setConversationTop(chatId, isTop);
            }
        } else if (TextUtils.equals(TUIConstants.TUIConversation.METHOD_GET_TOTAL_UNREAD_COUNT, method)) {
            return conversationEventListener.getUnreadTotal();
        } else if (TextUtils.equals(TUIConstants.TUIConversation.METHOD_UPDATE_TOTAL_UNREAD_COUNT, method)) {
            HashMap<String, Object> unreadMap = new HashMap<>();
            long totalUnread = conversationEventListener.getUnreadTotal();
            unreadMap.put(TUIConstants.TUIConversation.TOTAL_UNREAD_COUNT, totalUnread);
            TUICore.notifyEvent(TUIConstants.TUIConversation.EVENT_UNREAD, TUIConstants.TUIConversation.EVENT_SUB_KEY_UNREAD_CHANGED, unreadMap);
        } else if (TextUtils.equals(TUIConstants.TUIConversation.METHOD_DELETE_CONVERSATION, method)) {
            String conversationId = (String) param.get(TUIConstants.TUIConversation.CONVERSATION_ID);
            conversationEventListener.clearFoldMarkAndDeleteConversation(conversationId);

            List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
            for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                conversationEventObserver.clearFoldMarkAndDeleteConversation(conversationId);
            }
        }
        return result;
    }

    @Override
    public void onNotifyEvent(String key, String subKey, Map<String, Object> param) {
        if (TextUtils.equals(key, TUIConstants.TUIGroup.EVENT_GROUP)) {
            handleGroupEvent(subKey, param);
        } else if (key.equals(TUIConstants.TUIContact.EVENT_USER)) {
            handleContactUserEvent(subKey, param);
        } else if (key.equals(TUIConstants.TUIContact.EVENT_FRIEND_INFO_CHANGED)) {
            handleContactFriendInfoChangedEvent(subKey, param);
        } else if (key.equals(TUIConstants.TUIChat.EVENT_KEY_RECEIVE_MESSAGE)) {
            handleChatReceiveMessageEvent(subKey, param);
        } else if (TextUtils.equals(key, TUIConstants.TUIConversation.EVENT_KEY_MESSAGE_SEND_FOR_CONVERSATION)) {
            handleConversationEvent(subKey, param);
        } else if (TextUtils.equals(key, TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_KEY)) {
            handleConversationGroupChangedEvent(subKey, param);
        } else if (TextUtils.equals(key, TUIConstants.TUIChat.Event.MessageDisplayString.KEY)) {
            handleMessageBeanUpdateEvent(subKey, param);
        }
    }

    /**
     * TUIGroup.EVENT_GROUP
     */
    private void handleGroupEvent(String subKey, Map<String, Object> param) {
        if (TextUtils.equals(subKey, TUIConstants.TUIGroup.EVENT_SUB_KEY_EXIT_GROUP)
                || TextUtils.equals(subKey, TUIConstants.TUIGroup.EVENT_SUB_KEY_GROUP_DISMISS)
                || TextUtils.equals(subKey, TUIConstants.TUIGroup.EVENT_SUB_KEY_GROUP_RECYCLE)) {
            ConversationEventListener eventListener = getConversationEventListener();
            String groupId = null;
            if (param != null) {
                groupId = (String) getOrDefault(param.get(TUIConstants.TUIGroup.GROUP_ID), "");
            }
            if (eventListener != null) {
                eventListener.deleteConversation(groupId, true);
            }
            List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
            for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                conversationEventObserver.deleteConversation(groupId, true);
            }
        } else if (TextUtils.equals(subKey, TUIConstants.TUIGroup.EVENT_SUB_KEY_MEMBER_KICKED_GROUP)) {
            if (param == null) {
                return;
            }
            String groupId = (String) getOrDefault(param.get(TUIConstants.TUIGroup.GROUP_ID), "");
            ArrayList<String> memberList = (ArrayList<String>) param.get(TUIConstants.TUIGroup.GROUP_MEMBER_ID_LIST);
            if (TextUtils.isEmpty(groupId) || memberList == null || memberList.isEmpty()) {
                return;
            }
            for (String id : memberList) {
                if (TextUtils.equals(id, TUILogin.getLoginUser())) {
                    ConversationEventListener eventListener = getConversationEventListener();
                    if (eventListener != null) {
                        eventListener.deleteConversation(groupId, true);
                    }
                    List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
                    for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                        conversationEventObserver.deleteConversation(groupId, true);
                    }
                    break;
                }
            }
        } else if (TextUtils.equals(subKey, TUIConstants.TUIGroup.EVENT_SUB_KEY_CLEAR_MESSAGE)) {
            String groupId = (String) getOrDefault(param.get(TUIConstants.TUIGroup.GROUP_ID), "");
            ConversationEventListener eventListener = getConversationEventListener();
            if (eventListener != null) {
                eventListener.clearConversation(groupId, true);
            }
            List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
            for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                conversationEventObserver.clearConversation(groupId, true);
            }
        }
    }

    /**
     * TUIContact.EVENT_USER
     */
    private void handleContactUserEvent(String subKey, Map<String, Object> param) {
        if (subKey.equals(TUIConstants.TUIContact.EVENT_SUB_KEY_CLEAR_MESSAGE)) {
            if (param == null || param.isEmpty()) {
                return;
            }
            String userID = (String) getOrDefault(param.get(TUIConstants.TUIContact.FRIEND_ID), "");
            ConversationEventListener eventListener = getConversationEventListener();
            if (eventListener != null) {
                eventListener.clearConversation(userID, false);
            }
            List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
            for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                conversationEventObserver.clearConversation(userID, false);
            }
        }
    }

    /**
     * TUIContact.EVENT_FRIEND_INFO_CHANGED
     */
    private void handleContactFriendInfoChangedEvent(String subKey, Map<String, Object> param) {
        if (subKey.equals(TUIConstants.TUIContact.EVENT_SUB_KEY_FRIEND_REMARK_CHANGED)) {
            if (param == null || param.isEmpty()) {
                return;
            }
            ConversationEventListener conversationEventListener = getInstance().getConversationEventListener();
            if (conversationEventListener == null) {
                return;
            }
            String id = (String) param.get(TUIConstants.TUIContact.FRIEND_ID);
            String remark = (String) param.get(TUIConstants.TUIContact.FRIEND_REMARK);
            conversationEventListener.onFriendRemarkChanged(id, remark);

            List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
            for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                conversationEventObserver.onFriendRemarkChanged(id, remark);
            }
        }
    }

    /**
     * TUIChat.EVENT_KEY_RECEIVE_MESSAGE
     */
    private void handleChatReceiveMessageEvent(String subKey, Map<String, Object> param) {
        if (subKey.equals(TUIConstants.TUIChat.EVENT_SUB_KEY_CONVERSATION_ID)) {
            if (param == null || param.isEmpty()) {
                return;
            }
            String conversationID = (String) param.get(TUIConstants.TUIChat.CONVERSATION_ID);
            boolean isTypingMessage = false;
            if (param.containsKey(TUIConstants.TUIChat.IS_TYPING_MESSAGE)) {
                isTypingMessage = (Boolean) param.get(TUIConstants.TUIChat.IS_TYPING_MESSAGE);
            }
            ConversationEventListener conversationEventListener = getInstance().getConversationEventListener();
            if (conversationEventListener != null) {
                conversationEventListener.onReceiveMessage(conversationID, isTypingMessage);
            }
            List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
            for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                conversationEventObserver.onReceiveMessage(conversationID, isTypingMessage);
            }
        }
    }

    /**
     * TUIConversation.EVENT_KEY_MESSAGE_SEND_FOR_CONVERSATION
     */
    private void handleConversationEvent(String subKey, Map<String, Object> param) {
        if (TextUtils.equals(subKey, TUIConstants.TUIConversation.EVENT_SUB_KEY_MESSAGE_SEND_FOR_CONVERSATION)) {
            if (param == null || param.isEmpty()) {
                return;
            }
            String conversationID = (String) param.get(TUIConstants.TUIConversation.CONVERSATION_ID);
            ConversationEventListener eventListener = getConversationEventListener();
            if (eventListener != null) {
                eventListener.onMessageSendForHideConversation(conversationID);
            }
        }
    }

    /**
     * TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_KEY
     */
    private void handleConversationGroupChangedEvent(String subKey, Map<String, Object> param) {
        if (TextUtils.equals(subKey, TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_ADD)) {
            if (param == null || param.isEmpty()) {
                TUIConversationLog.e(TAG, "EVENT_CONVERSATION_GROUP_CHANGE_ADD param is null");
                return;
            }
            ConversationGroupBean bean = (ConversationGroupBean) param.get(TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_ADD);
            ConversationGroupNotifyListener eventListener = getConversationGroupNotifyListener();
            if (eventListener != null) {
                eventListener.notifyGroupAdd(bean);
            } else {
                TUIConversationLog.e(TAG, "eventListener is null");
            }
        } else if (TextUtils.equals(subKey, TUIConversationConstants.EVENT_CONVERSATION_GROUP_ADD_DATA)) {
            if (param == null || param.isEmpty()) {
                TUIConversationLog.e(TAG, "EVENT_CONVERSATION_GROUP_ADD_DATA param is null");
                return;
            }
            List<ConversationGroupBean> beans = (List<ConversationGroupBean>) param.get(TUIConversationConstants.EVENT_CONVERSATION_GROUP_ADD_DATA);
            ConversationGroupNotifyListener eventListener = getConversationGroupNotifyListener();
            if (eventListener != null) {
                eventListener.notifyGroupsAdd(beans);
            } else {
                TUIConversationLog.e(TAG, "eventListener is null");
            }
        } else if (TextUtils.equals(subKey, TUIConversationConstants.EVENT_CONVERSATION_GROUP_ADD_MARK_DATA)) {
            if (param == null || param.isEmpty()) {
                TUIConversationLog.e(TAG, "EVENT_CONVERSATION_GROUP_ADD_MARK_DATA param is null");
                return;
            }
            List<ConversationGroupBean> beans = (List<ConversationGroupBean>) param.get(TUIConversationConstants.EVENT_CONVERSATION_GROUP_ADD_MARK_DATA);
            ConversationGroupNotifyListener eventListener = getConversationGroupNotifyListener();
            if (eventListener != null) {
                eventListener.notifyMarkGroupsAdd(beans);
            } else {
                TUIConversationLog.e(TAG, "eventListener is null");
            }
        } else if (TextUtils.equals(subKey, TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_DELETE)) {
            if (param == null || param.isEmpty()) {
                TUIConversationLog.e(TAG, "EVENT_CONVERSATION_GROUP_CHANGE_DELETE param is null");
                return;
            }
            String groupName = (String) param.get(TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_DELETE);
            ConversationGroupNotifyListener eventListener = getConversationGroupNotifyListener();
            if (eventListener != null) {
                eventListener.notifyGroupDelete(groupName);
            } else {
                TUIConversationLog.e(TAG, "eventListener is null");
            }
        } else if (TextUtils.equals(subKey, TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_RENAME)) {
            if (param == null || param.isEmpty()) {
                TUIConversationLog.e(TAG, "EVENT_CONVERSATION_GROUP_CHANGE_RENAME param is null");
                return;
            }
            String oldName = (String) param.get(TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_RENAME);
            String newName = (String) param.get(TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_GROUP_NAME);
            ConversationGroupNotifyListener eventListener = getConversationGroupNotifyListener();
            if (eventListener != null) {
                eventListener.notifyGroupRename(oldName, newName);
            } else {
                TUIConversationLog.e(TAG, "eventListener is null");
            }
        } else if (TextUtils.equals(subKey, TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_UNREAD_COUNT)) {
            if (param == null || param.isEmpty()) {
                TUIConversationLog.e(TAG, "EVENT_CONVERSATION_GROUP_CHANGE_UNREAD_COUNT param is null");
                return;
            }
            long count = (long) param.get(TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_UNREAD_COUNT);
            String groupName = (String) param.get(TUIConversationConstants.EVENT_CONVERSATION_GROUP_CHANGE_GROUP_NAME);
            ConversationGroupNotifyListener eventListener = getConversationGroupNotifyListener();
            if (eventListener != null) {
                eventListener.notifyGroupUnreadMessageCountChanged(groupName, count);
            } else {
                TUIConversationLog.e(TAG, "eventListener is null");
            }
        }
    }

    /**
     * ChatMessageDisplayString
     */
    private void handleMessageBeanUpdateEvent(String subKey, Map<String, Object> param) {
        if (TextUtils.equals(TUIConstants.TUIChat.Event.MessageDisplayString.SUB_KEY_PROCESS_MESSAGE, subKey)) {
            String conversationID = (String) param.get(TUIConstants.TUIChat.Event.MessageDisplayString.CONVERSATION_ID);
            TUIMessageBean messageBean = (TUIMessageBean) param.get(TUIConstants.TUIChat.Event.MessageDisplayString.MESSAGE_BEAN);
            ConversationEventListener eventListener = getConversationEventListener();
            if (eventListener != null) {
                eventListener.onConversationLastMessageBeanChanged(conversationID, messageBean);
            }
            List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
            for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                conversationEventObserver.onConversationLastMessageBeanChanged(conversationID, messageBean);
            }
        }
    }

    private Object getOrDefault(Object value, Object defaultValue) {
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    private void initIMListener() {
        V2TIMManager.getConversationManager().addConversationListener(new V2TIMConversationListener() {

            /**
             * 同步服务器会话开始
             */
            @Override
            public void onSyncServerStart() {
                syncFinished = false;
            }

            /**
             * 同步服务器会话完成
             */
            @Override
            public void onSyncServerFinish() {
                ConversationEventListener conversationEventListener = getInstance().getConversationEventListener();
                if (conversationEventListener != null) {
                    conversationEventListener.onSyncServerFinish();
                }

                List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
                for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                    conversationEventObserver.onSyncServerFinish();
                }

                syncFinished = true;
            }

            /**
             * 同步服务器会话失败
             */
            @Override
            public void onSyncServerFailed() {
                syncFinished = false;
            }

            /**
             * 有会话新增
             */
            @Override
            public void onNewConversation(List<V2TIMConversation> conversationList) {
                ConversationEventListener conversationEventListener = getInstance().getConversationEventListener();
                List<ConversationInfo> conversationInfoList = ConversationUtils.convertV2TIMConversationList(conversationList);
                if (conversationEventListener != null) {
                    conversationEventListener.onNewConversation(conversationInfoList);
                }

                List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
                for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                    conversationEventObserver.onNewConversation(conversationInfoList);
                }
            }

            /**
             * 有会话更新
             */
            @Override
            public void onConversationChanged(List<V2TIMConversation> conversationList) {
                ConversationEventListener conversationEventListener = getInstance().getConversationEventListener();
                List<ConversationInfo> conversationInfoList = ConversationUtils.convertV2TIMConversationList(conversationList);
                if (conversationEventListener != null) {
                    conversationEventListener.onConversationChanged(conversationInfoList);
                }

                List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
                for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                    conversationEventObserver.onConversationChanged(conversationInfoList);
                }
            }

            /**
             * 有会话被删除
             */
            @Override
            public void onConversationDeleted(List<String> conversationIDList) {
                ConversationEventListener conversationEventListener = getInstance().getConversationEventListener();
                if (conversationEventListener != null) {
                    conversationEventListener.onConversationDeleted(conversationIDList);
                }
                List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
                for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                    conversationEventObserver.onConversationDeleted(conversationIDList);
                }
            }

            /**
             * 会话未读总数变更通知
             */
            @Override
            public void onTotalUnreadMessageCountChanged(long totalUnreadCount) {
                ConversationEventListener conversationEventListener = getInstance().getConversationEventListener();
                if (conversationEventListener != null) {
                    conversationEventListener.updateTotalUnreadMessageCount(totalUnreadCount);
                }
                List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
                for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                    conversationEventObserver.updateTotalUnreadMessageCount(totalUnreadCount);
                }
            }


        });

        V2TIMManager.getInstance().addIMSDKListener(new V2TIMSDKListener() {
            @Override
            public void onUserStatusChanged(List<V2TIMUserStatus> userStatusList) {
                ConversationEventListener conversationEventListener = getInstance().getConversationEventListener();
                if (conversationEventListener != null) {
                    conversationEventListener.onUserStatusChanged(userStatusList);
                }
                List<ConversationEventListener> conversationEventObserverList = getConversationEventListenerList();
                for (ConversationEventListener conversationEventObserver : conversationEventObserverList) {
                    conversationEventObserver.onUserStatusChanged(userStatusList);
                }
            }
        });
    }

    public void addConversationEventListener(ConversationEventListener conversationEventListener) {
        if (conversationEventListener == null) {
            return;
        }
        for (SoftReference<ConversationEventListener> listenerWeakReference : conversationEventListenerList) {
            if (listenerWeakReference.get() == conversationEventListener) {
                return;
            }
        }
        conversationEventListenerList.add(new SoftReference<>(conversationEventListener));
    }

    public void removeConversationEventListener(ConversationEventListener conversationEventListener) {
        if (conversationEventListener == null) {
            return;
        }
        for (SoftReference<ConversationEventListener> listenerWeakReference : conversationEventListenerList) {
            if (listenerWeakReference.get() == conversationEventListener) {
                conversationEventListenerList.remove(listenerWeakReference);
                break;
            }
        }
    }

    public List<ConversationEventListener> getConversationEventListenerList() {
        List<ConversationEventListener> listeners = new ArrayList<>();
        Iterator<SoftReference<ConversationEventListener>> iterator = conversationEventListenerList.listIterator();
        while (iterator.hasNext()) {
            SoftReference<ConversationEventListener> listenerWeakReference = iterator.next();
            ConversationEventListener listener = listenerWeakReference.get();
            if (listener == null) {
                iterator.remove();
            } else {
                listeners.add(listener);
            }
        }
        return listeners;
    }

    public void setConversationEventListener(ConversationEventListener conversationManagerKit) {
        this.conversationEventListener = new SoftReference<>(conversationManagerKit);
        if (syncFinished) {
            conversationManagerKit.onSyncServerFinish();
        }
    }

    public void setConversationEventListenerNull() {
        this.conversationEventListener = null;
    }

    public ConversationEventListener getConversationEventListener() {
        if (conversationEventListener != null) {
            return conversationEventListener.get();
        }
        return null;
    }

    public void setConversationGroupNotifyListener(ConversationGroupNotifyListener listener) {
        this.conversationGroupNotifyListener = new SoftReference<>(listener);
    }

    public ConversationGroupNotifyListener getConversationGroupNotifyListener() {
        if (conversationGroupNotifyListener != null) {
            return conversationGroupNotifyListener.get();
        }
        return null;
    }

    public void setConversationGroupNotifyListenerNull() {
        this.conversationGroupNotifyListener = null;
    }

    public void setConversationAllGroupUnreadDiff(int diff) {
        this.conversationAllGroupUnreadDiff = diff;
    }

    public static Context getAppContext() {
        return ServiceInitializer.getAppContext();
    }
}

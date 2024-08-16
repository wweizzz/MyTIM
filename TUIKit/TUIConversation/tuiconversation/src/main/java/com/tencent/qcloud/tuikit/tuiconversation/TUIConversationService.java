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
import com.tencent.qcloud.tuicore.TUIThemeManager;
import com.tencent.qcloud.tuicore.annotations.TUIInitializerDependency;
import com.tencent.qcloud.tuicore.annotations.TUIInitializerID;
import com.tencent.qcloud.tuicore.interfaces.ITUINotification;
import com.tencent.qcloud.tuicore.interfaces.ITUIService;
import com.tencent.qcloud.tuicore.interfaces.TUIInitializer;
import com.tencent.qcloud.tuikit.timcommon.bean.TUIMessageBean;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationInfo;
import com.tencent.qcloud.tuikit.tuiconversation.commonutil.ConversationUtils;
import com.tencent.qcloud.tuikit.tuiconversation.commonutil.TUIConversationLog;
import com.tencent.qcloud.tuikit.tuiconversation.listener.ConversationEventListener;
import com.tencent.qcloud.tuikit.tuiconversation.listener.ConversationGroupNotifyListener;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
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
        TUICore.registerEvent(
                TUIConstants.TUIChat.EVENT_KEY_RECEIVE_MESSAGE, TUIConstants.TUIChat.EVENT_SUB_KEY_CONVERSATION_ID, this);

        TUICore.registerEvent(
                TUIConstants.TUIConversation.EVENT_KEY_MESSAGE_SEND_FOR_CONVERSATION, TUIConstants.TUIConversation.EVENT_SUB_KEY_MESSAGE_SEND_FOR_CONVERSATION, this);

        TUICore.registerEvent(
                TUIConstants.TUIChat.Event.MessageDisplayString.KEY, TUIConstants.TUIChat.Event.MessageDisplayString.SUB_KEY_PROCESS_MESSAGE, this);
    }

    @Override
    public Object onCall(String method, Map<String, Object> param) {
        Bundle result = new Bundle();

        ConversationEventListener conversationEventListener = getInstance().getConversationEventListener();
        if (conversationEventListener == null) {
            TUIConversationLog.e(TAG, "execute " + method + " failed , conversationEvent listener is null");
            return result;
        }

        if (TextUtils.equals(TUIConstants.TUIConversation.METHOD_GET_TOTAL_UNREAD_COUNT, method)) {
            return conversationEventListener.getUnreadTotal();
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
        if (key.equals(TUIConstants.TUIChat.EVENT_KEY_RECEIVE_MESSAGE)) {
            handleChatEvent(subKey, param);
        } else if (TextUtils.equals(key, TUIConstants.TUIConversation.EVENT_KEY_MESSAGE_SEND_FOR_CONVERSATION)) {
            handleConversationEvent(subKey, param);
        } else if (TextUtils.equals(key, TUIConstants.TUIChat.Event.MessageDisplayString.KEY)) {
            handleMessageBeanUpdateEvent(subKey, param);
        }
    }

    /**
     * TUIChat.EVENT_KEY_RECEIVE_MESSAGE
     */
    private void handleChatEvent(String subKey, Map<String, Object> param) {
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
                eventListener.onReceiveMessageSendForHideConversation(conversationID);
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

    public int getConversationAllGroupUnreadDiff() {
        return conversationAllGroupUnreadDiff;
    }

    public void setConversationAllGroupUnreadDiff(int diff) {
        this.conversationAllGroupUnreadDiff = diff;
    }

    public static Context getAppContext() {
        return ServiceInitializer.getAppContext();
    }
}

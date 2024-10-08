package com.tencent.qcloud.tuikit.tuiconversation.model;

import android.text.TextUtils;

import com.tencent.imsdk.BaseConstants;
import com.tencent.imsdk.v2.V2TIMCallback;
import com.tencent.imsdk.v2.V2TIMConversation;
import com.tencent.imsdk.v2.V2TIMConversationListFilter;
import com.tencent.imsdk.v2.V2TIMConversationOperationResult;
import com.tencent.imsdk.v2.V2TIMConversationResult;
import com.tencent.imsdk.v2.V2TIMGroupMemberFullInfo;
import com.tencent.imsdk.v2.V2TIMGroupMemberInfoResult;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMUserStatus;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import com.tencent.qcloud.tuicore.TUIConstants;
import com.tencent.qcloud.tuicore.interfaces.TUICallback;
import com.tencent.qcloud.tuicore.util.ErrorMessageConverter;
import com.tencent.qcloud.tuicore.util.ToastUtil;
import com.tencent.qcloud.tuikit.timcommon.component.interfaces.IUIKitCallback;
import com.tencent.qcloud.tuikit.tuiconversation.BuildConfig;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationInfo;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationUserStatusBean;
import com.tencent.qcloud.tuikit.tuiconversation.commonutil.ConversationUtils;
import com.tencent.qcloud.tuikit.tuiconversation.commonutil.TUIConversationLog;
import com.tencent.qcloud.tuikit.tuiconversation.commonutil.TUIConversationUtils;
import com.tencent.qcloud.tuikit.tuiconversation.interfaces.IConversationProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConversationProvider implements IConversationProvider {

    private static final String TAG = ConversationProvider.class.getSimpleName();

    protected boolean isFinished = false;
    protected long nextLoadSeq = 0L;

    private final List<ConversationInfo> markConversationInfoList = new ArrayList<>();
    private final HashMap<String, V2TIMConversation> markUnreadMap = new HashMap<>();

    @Override
    public void loadMoreConversation(int loadCount, IUIKitCallback<List<ConversationInfo>> callBack) {
        if (isFinished) {
            return;
        }
        loadConversation(nextLoadSeq, loadCount, callBack);
    }

    @Override
    public void loadConversation(
            long startSeq, int loadCount, final IUIKitCallback<List<ConversationInfo>> callBack) {
        TUIConversationLog.i(TAG, "loadConversation startSeq " + startSeq + " loadCount " + loadCount);

        V2TIMManager.getConversationManager().getConversationList(startSeq, loadCount, new V2TIMValueCallback<V2TIMConversationResult>() {
            @Override
            public void onSuccess(V2TIMConversationResult v2TIMConversationResult) {
                List<V2TIMConversation> v2TIMConversationList = v2TIMConversationResult.getConversationList();
                List<ConversationInfo> conversationInfoList = ConversationUtils.convertV2TIMConversationList(v2TIMConversationList);
                TUIConversationLog.i(TAG,
                        "loadConversation getConversationList success size " + conversationInfoList.size() + " nextSeq " + v2TIMConversationResult.getNextSeq()
                                + " isFinished " + v2TIMConversationResult.isFinished());
                if (!conversationInfoList.isEmpty()) {
                    TUIConversationLog.i(TAG,
                            "loadConversation getConversationList success first " + conversationInfoList.get(0) + " last "
                                    + conversationInfoList.get(conversationInfoList.size() - 1));
                }
                isFinished = v2TIMConversationResult.isFinished();
                nextLoadSeq = v2TIMConversationResult.getNextSeq();
                TUIConversationUtils.callbackOnSuccess(callBack, conversationInfoList);
            }

            @Override
            public void onError(int code, String desc) {
                TUIConversationLog.e(
                        TAG, "loadConversation getConversationList error, code = " + code + ", desc = " + ErrorMessageConverter.convertIMError(code, desc));
                TUIConversationUtils.callbackOnError(callBack, TAG, code, desc);
            }
        });
    }

    @Override
    public void loadMarkConversation(
            final V2TIMConversationListFilter filter,
            long startSeq, int loadCount, boolean fromStart, IUIKitCallback<List<ConversationInfo>> callback) {
        TUIConversationLog.i(TAG, "loadMarkConversation startSeq " + startSeq + " loadCount " + loadCount);

        if (fromStart) {
            markConversationInfoList.clear();
        }
        V2TIMManager.getConversationManager().getConversationListByFilter(filter, startSeq, loadCount, new V2TIMValueCallback<V2TIMConversationResult>() {
            @Override
            public void onSuccess(V2TIMConversationResult v2TIMConversationResult) {
                List<V2TIMConversation> conversationList = v2TIMConversationResult.getConversationList();
                List<ConversationInfo> conversationInfoList = ConversationUtils.convertV2TIMConversationList(conversationList);
                TUIConversationLog.i(TAG,
                        "loadMarkConversation getMarkConversationList success size " + conversationInfoList.size() + " nextSeq " + v2TIMConversationResult.getNextSeq()
                                + " isFinished " + v2TIMConversationResult.isFinished());

                markConversationInfoList.addAll(conversationInfoList);

                if (!v2TIMConversationResult.isFinished()) {
                    loadMarkConversation(filter, v2TIMConversationResult.getNextSeq(), loadCount, false, callback);
                } else {
                    if (callback != null) {
                        callback.onSuccess(markConversationInfoList);
                    }
                }
            }

            @Override
            public void onError(int code, String desc) {
                TUIConversationLog.e(
                        TAG, "loadMarkConversation getConversationList error, code = " + code + ", desc = " + ErrorMessageConverter.convertIMError(code, desc));
                TUIConversationUtils.callbackOnError(callback, TAG, code, desc);
            }
        });
    }

    @Override
    public boolean isLoadFinished() {
        return isFinished;
    }

    @Override
    public void getConversation(String conversationID, IUIKitCallback<ConversationInfo> callback) {
        V2TIMManager.getConversationManager().getConversation(conversationID, new V2TIMValueCallback<V2TIMConversation>() {
            @Override
            public void onSuccess(V2TIMConversation v2TIMConversation) {
                ConversationInfo conversationInfo = ConversationUtils.convertV2TIMConversation(v2TIMConversation);
                TUIConversationUtils.callbackOnSuccess(callback, conversationInfo);
            }

            @Override
            public void onError(int code, String desc) {
                TUIConversationLog.v(TAG, "getConversation error, code = " + code + ", desc = " + ErrorMessageConverter.convertIMError(code, desc));
                TUIConversationUtils.callbackOnError(callback, TAG, code, desc);
            }
        });
    }

    @Override
    public void getTotalUnreadMessageCount(IUIKitCallback<Long> callBack) {
        V2TIMManager.getConversationManager().getTotalUnreadMessageCount(new V2TIMValueCallback<Long>() {
            @Override
            public void onSuccess(Long count) {
                TUIConversationUtils.callbackOnSuccess(callBack, count);
            }

            @Override
            public void onError(int code, String desc) {
            }
        });
    }

    @Override
    public void setConversationTop(String conversationId, boolean isTop, IUIKitCallback<Void> callBack) {
        V2TIMManager.getConversationManager().pinConversation(conversationId, isTop, new V2TIMCallback() {
            @Override
            public void onSuccess() {
                TUIConversationUtils.callbackOnSuccess(callBack, null);
            }

            @Override
            public void onError(int code, String desc) {
                TUIConversationUtils.callbackOnError(callBack, TAG, code, desc);
            }
        });
    }

    @Override
    public void deleteConversation(String conversationId, IUIKitCallback<Void> callBack) {
        V2TIMManager.getConversationManager().deleteConversation(conversationId, new V2TIMCallback() {
            @Override
            public void onError(int code, String desc) {
                TUIConversationLog.e(TAG, "deleteConversation error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
                TUIConversationUtils.callbackOnError(callBack, TAG, code, desc);
            }

            @Override
            public void onSuccess() {
                TUIConversationLog.i(TAG, "deleteConversation success");
                TUIConversationUtils.callbackOnSuccess(callBack, null);
            }
        });
    }

    @Override
    public void clearHistoryMessage(String userId, boolean isGroup, IUIKitCallback<Void> callBack) {
        if (isGroup) {
            V2TIMManager.getMessageManager().clearGroupHistoryMessage(userId, new V2TIMCallback() {
                @Override
                public void onError(int code, String desc) {
                    TUIConversationLog.e(TAG, "clearConversationMessage error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
                    TUIConversationUtils.callbackOnError(callBack, TAG, code, desc);
                }

                @Override
                public void onSuccess() {
                    TUIConversationLog.i(TAG, "clearConversationMessage success");
                    TUIConversationUtils.callbackOnSuccess(callBack, null);
                }
            });
        } else {
            V2TIMManager.getMessageManager().clearC2CHistoryMessage(userId, new V2TIMCallback() {
                @Override
                public void onError(int code, String desc) {
                    TUIConversationLog.e(TAG, "clearConversationMessage error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
                    TUIConversationUtils.callbackOnError(callBack, TAG, code, desc);
                }

                @Override
                public void onSuccess() {
                    TUIConversationLog.i(TAG, "clearConversationMessage success");
                    TUIConversationUtils.callbackOnSuccess(callBack, null);
                }
            });
        }
    }

    @Override
    public void markConversationRead(String conversationID, TUICallback callback) {
        V2TIMManager.getConversationManager().markConversation(Collections.singletonList(conversationID),
                V2TIMConversation.V2TIM_CONVERSATION_MARK_TYPE_UNREAD, false, new V2TIMValueCallback<List<V2TIMConversationOperationResult>>() {
                    @Override
                    public void onSuccess(List<V2TIMConversationOperationResult> v2TIMConversationOperationResults) {
                        if (v2TIMConversationOperationResults.isEmpty()) {
                            return;
                        }
                        V2TIMConversationOperationResult result = v2TIMConversationOperationResults.get(0);
                        if (result.getResultCode() == BaseConstants.ERR_SUCC) {
                            TUICallback.onSuccess(callback);
                        } else {
                            TUICallback.onError(callback, result.getResultCode(), result.getResultInfo());
                        }
                    }

                    @Override
                    public void onError(int code, String desc) {
                        TUIConversationLog.e(TAG, "markConversationRead error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
                        TUICallback.onError(callback, code, desc);
                    }
                });
    }

    @Override
    public void markConversationUnread(String conversationID, TUICallback callback) {
        V2TIMManager.getConversationManager().markConversation(Collections.singletonList(conversationID),
                V2TIMConversation.V2TIM_CONVERSATION_MARK_TYPE_UNREAD, true, new V2TIMValueCallback<List<V2TIMConversationOperationResult>>() {
                    @Override
                    public void onSuccess(List<V2TIMConversationOperationResult> v2TIMConversationOperationResults) {
                        if (v2TIMConversationOperationResults.isEmpty()) {
                            return;
                        }
                        V2TIMConversationOperationResult result = v2TIMConversationOperationResults.get(0);
                        if (result.getResultCode() == BaseConstants.ERR_SUCC) {
                            TUICallback.onSuccess(callback);
                        } else {
                            TUICallback.onError(callback, result.getResultCode(), result.getResultInfo());
                        }
                    }

                    @Override
                    public void onError(int code, String desc) {
                        TUIConversationLog.e(TAG, "markConversationUnread error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
                        TUICallback.onError(callback, code, desc);
                    }
                });
    }

    @Override
    public void markConversationUnread(ConversationInfo conversationInfo, boolean markUnread, IUIKitCallback<Void> callback) {
        List<String> conversationIDList = new ArrayList<>();
        if (!TextUtils.isEmpty(conversationInfo.getConversationId())) {
            conversationIDList.add(conversationInfo.getConversationId());
        }
        if (!markUnread && conversationInfo.getUnRead() > 0) {
            V2TIMManager.getConversationManager().cleanConversationUnreadMessageCount(conversationInfo.getConversationId(), 0, 0, new V2TIMCallback() {
                @Override
                public void onSuccess() {
                    TUIConversationLog.i(TAG, "markConversationUnread->cleanConversationUnreadMessageCount success");
                }

                @Override
                public void onError(int code, String desc) {
                    TUIConversationLog.e(
                            TAG, "cleanConversationUnreadMessageCount error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
                }
            });
        }

        if (markUnread != conversationInfo.isMarkUnread()) {
            V2TIMManager.getConversationManager().markConversation(conversationIDList, V2TIMConversation.V2TIM_CONVERSATION_MARK_TYPE_UNREAD, markUnread,
                    new V2TIMValueCallback<List<V2TIMConversationOperationResult>>() {
                        @Override
                        public void onSuccess(List<V2TIMConversationOperationResult> v2TIMConversationOperationResults) {
                            if (v2TIMConversationOperationResults.isEmpty()) {
                                return;
                            }
                            V2TIMConversationOperationResult result = v2TIMConversationOperationResults.get(0);
                            if (result.getResultCode() == BaseConstants.ERR_SUCC) {
                                TUIConversationUtils.callbackOnSuccess(callback, null);
                            } else {
                                TUIConversationUtils.callbackOnError(callback, TAG, result.getResultCode(), result.getResultInfo());
                            }
                        }

                        @Override
                        public void onError(int code, String desc) {
                            TUIConversationLog.e(TAG, "markConversationUnread error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
                            TUIConversationUtils.callbackOnError(callback, TAG, code, desc);
                        }
                    });
        }
    }

    @Override
    public void markConversationHidden(String conversationID, boolean isHidden, IUIKitCallback<Void> callback) {
        V2TIMManager.getConversationManager().markConversation(Collections.singletonList(conversationID), V2TIMConversation.V2TIM_CONVERSATION_MARK_TYPE_HIDE, isHidden,
                new V2TIMValueCallback<List<V2TIMConversationOperationResult>>() {
                    @Override
                    public void onSuccess(List<V2TIMConversationOperationResult> v2TIMConversationOperationResults) {
                        if (v2TIMConversationOperationResults.isEmpty()) {
                            return;
                        }
                        V2TIMConversationOperationResult result = v2TIMConversationOperationResults.get(0);
                        if (result.getResultCode() == BaseConstants.ERR_SUCC) {
                            TUIConversationUtils.callbackOnSuccess(callback, null);
                        } else {
                            TUIConversationUtils.callbackOnError(callback, TAG, result.getResultCode(), result.getResultInfo());
                        }
                    }

                    @Override
                    public void onError(int code, String desc) {
                        TUIConversationLog.e(TAG, "markConversationHidden error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
                        TUIConversationUtils.callbackOnError(callback, TAG, code, desc);
                    }
                });
    }

    @Override
    public void markConversationFold(String conversationID, boolean isFold, IUIKitCallback<Void> callback) {
        V2TIMManager.getConversationManager().markConversation(
                Collections.singletonList(conversationID), V2TIMConversation.V2TIM_CONVERSATION_MARK_TYPE_FOLD, isFold, new V2TIMValueCallback<List<V2TIMConversationOperationResult>>() {
                    @Override
                    public void onSuccess(List<V2TIMConversationOperationResult> v2TIMConversationOperationResults) {
                        if (v2TIMConversationOperationResults.isEmpty()) {
                            return;
                        }
                        V2TIMConversationOperationResult result = v2TIMConversationOperationResults.get(0);
                        if (result.getResultCode() == BaseConstants.ERR_SUCC) {
                            TUIConversationUtils.callbackOnSuccess(callback, null);
                        } else {
                            TUIConversationUtils.callbackOnError(callback, TAG, result.getResultCode(), result.getResultInfo());
                        }
                    }

                    @Override
                    public void onError(int code, String desc) {
                        TUIConversationLog.e(TAG, "markConversationFold error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
                        TUIConversationUtils.callbackOnError(callback, TAG, code, desc);
                    }
                });
    }

    @Override
    public void cleanConversationUnreadMessageCount(String conversationID, TUICallback callback) {
        V2TIMManager.getConversationManager().cleanConversationUnreadMessageCount(conversationID, 0, 0, new V2TIMCallback() {
            @Override
            public void onSuccess() {
                TUICallback.onSuccess(callback);
                TUIConversationLog.i(TAG, "cleanConversationUnreadCount success");
            }

            @Override
            public void onError(int code, String desc) {
                TUICallback.onError(callback, code, desc);
                TUIConversationLog.e(
                        TAG, "cleanConversationUnreadCount error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
            }
        });
    }

    @Override
    public void cleanAllConversationUnreadCount(IUIKitCallback<Void> callback) {
        V2TIMManager.getConversationManager().cleanConversationUnreadMessageCount("", 0, 0, new V2TIMCallback() {
            @Override
            public void onSuccess() {
                TUIConversationLog.i(TAG, "clearAllUnreadMessage success");
                TUIConversationUtils.callbackOnSuccess(callback, null);
            }

            @Override
            public void onError(int code, String desc) {
                TUIConversationLog.i(TAG, "clearAllUnreadMessage error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
                TUIConversationUtils.callbackOnError(callback, code, desc);
            }
        });

        V2TIMConversationListFilter filter = new V2TIMConversationListFilter();
        filter.setMarkType(V2TIMConversation.V2TIM_CONVERSATION_MARK_TYPE_UNREAD);
        getMarkUnreadConversationList(filter, 0, 100, true, new V2TIMValueCallback<HashMap<String, V2TIMConversation>>() {
            @Override
            public void onSuccess(HashMap<String, V2TIMConversation> stringV2TIMConversationHashMap) {
                if (stringV2TIMConversationHashMap.isEmpty()) {
                    return;
                }
                List<String> unreadConversationIDList = new ArrayList<>();
                Iterator<Map.Entry<String, V2TIMConversation>> iterator = markUnreadMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, V2TIMConversation> entry = iterator.next();
                    unreadConversationIDList.add(entry.getKey());
                }

                V2TIMManager.getConversationManager().markConversation(unreadConversationIDList, V2TIMConversation.V2TIM_CONVERSATION_MARK_TYPE_UNREAD, false,
                        new V2TIMValueCallback<List<V2TIMConversationOperationResult>>() {
                            @Override
                            public void onSuccess(List<V2TIMConversationOperationResult> v2TIMConversationOperationResults) {
                                for (V2TIMConversationOperationResult result : v2TIMConversationOperationResults) {
                                    if (result.getResultCode() == BaseConstants.ERR_SUCC) {
                                        V2TIMConversation v2TIMConversation = markUnreadMap.get(result.getConversationID());
                                        if (!(v2TIMConversation != null && v2TIMConversation.getMarkList().contains(V2TIMConversation.V2TIM_CONVERSATION_MARK_TYPE_HIDE))) {
                                            markUnreadMap.remove(result.getConversationID());
                                        }
                                    }
                                }
                                TUIConversationUtils.callbackOnSuccess(callback, null);
                            }

                            @Override
                            public void onError(int code, String desc) {
                                TUIConversationLog.e(TAG,
                                        "triggerClearAllUnreadMessage->markConversation error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
                                TUIConversationUtils.callbackOnError(callback, code, desc);
                            }
                        });
            }

            @Override
            public void onError(int code, String desc) {
                TUIConversationLog.e(TAG,
                        "triggerClearAllUnreadMessage->getMarkUnreadConversationList error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
            }
        });
    }

    @Override
    public void getMarkUnreadConversationList(
            V2TIMConversationListFilter filter, long nextSeq, int count, boolean fromStart, V2TIMValueCallback<HashMap<String, V2TIMConversation>> callback) {
        if (fromStart) {
            markUnreadMap.clear();
        }
        V2TIMManager.getConversationManager().getConversationListByFilter(filter, nextSeq, count, new V2TIMValueCallback<V2TIMConversationResult>() {
            @Override
            public void onSuccess(V2TIMConversationResult v2TIMConversationResult) {
                List<V2TIMConversation> conversationList = v2TIMConversationResult.getConversationList();
                for (V2TIMConversation conversation : conversationList) {
                    markUnreadMap.put(conversation.getConversationID(), conversation);
                }

                if (!v2TIMConversationResult.isFinished()) {
                    getMarkUnreadConversationList(filter, v2TIMConversationResult.getNextSeq(), count, false, callback);
                } else {
                    if (callback != null) {
                        callback.onSuccess(markUnreadMap);
                    }
                }
            }

            @Override
            public void onError(int code, String desc) {
                TUIConversationLog.e(TAG, "getMarkUnreadConversationList error:" + code + ", desc:" + ErrorMessageConverter.convertIMError(code, desc));
            }
        });
    }

    @Override
    public void loadConversationUserStatus(List<ConversationInfo> dataSource, IUIKitCallback<Map<String, ConversationUserStatusBean>> callback) {
        if (dataSource == null || dataSource.isEmpty()) {
            TUIConversationLog.d(TAG, "loadConversationUserStatus datasource is null");
            return;
        }

        List<String> userList = new ArrayList<>();
        for (ConversationInfo itemBean : dataSource) {
            if (itemBean.isGroup()) {
                continue;
            }
            userList.add(itemBean.getId());
        }
        if (userList.isEmpty()) {
            TUIConversationLog.d(TAG, "loadConversationUserStatus userList is empty");
            return;
        }
        V2TIMManager.getInstance().getUserStatus(userList, new V2TIMValueCallback<List<V2TIMUserStatus>>() {
            @Override
            public void onSuccess(List<V2TIMUserStatus> v2TIMUserStatuses) {
                TUIConversationLog.i(TAG, "getUserStatus success");
                Map<String, ConversationUserStatusBean> userStatusBeanMap = new HashMap<>();
                for (V2TIMUserStatus item : v2TIMUserStatuses) {
                    ConversationUserStatusBean conversationUserStatusBean = new ConversationUserStatusBean();
                    conversationUserStatusBean.setV2TIMUserStatus(item);
                    userStatusBeanMap.put(item.getUserID(), conversationUserStatusBean);
                }
                TUIConversationUtils.callbackOnSuccess(callback, userStatusBeanMap);
            }

            @Override
            public void onError(int code, String desc) {
                TUIConversationLog.e(TAG, "getUserStatus error code = " + code + ",des = " + desc);
                TUIConversationUtils.callbackOnError(callback, code, desc);
                if (code == TUIConstants.BuyingFeature.ERR_SDK_INTERFACE_NOT_SUPPORT
                        && BuildConfig.DEBUG) {
                    ToastUtil.toastLongMessage(desc);
                }
            }
        });
    }

    @Override
    public void subscribeConversationUserStatus(List<String> userIdList, IUIKitCallback<Void> callback) {
        if (userIdList == null || userIdList.isEmpty()) {
            TUIConversationLog.e(TAG, "subscribeConversationUserStatus userId is null");
            TUIConversationUtils.callbackOnError(callback, BaseConstants.ERR_INVALID_PARAMETERS, "userid list is null");
            return;
        }

        V2TIMManager.getInstance().subscribeUserStatus(userIdList, new V2TIMCallback() {
            @Override
            public void onSuccess() {
                TUIConversationUtils.callbackOnSuccess(callback, null);
            }

            @Override
            public void onError(int code, String desc) {
                TUIConversationLog.e(TAG, "subscribeConversationUserStatus error code = " + code + ",des = " + desc);
                TUIConversationUtils.callbackOnError(callback, code, desc);
            }
        });
    }

    @Override
    public void getGroupMemberIconList(String groupId, int iconCount, IUIKitCallback<List<Object>> callback) {
        V2TIMManager.getGroupManager().getGroupMemberList(
                groupId, V2TIMGroupMemberFullInfo.V2TIM_GROUP_MEMBER_FILTER_ALL, 0, new V2TIMValueCallback<V2TIMGroupMemberInfoResult>() {
                    @Override
                    public void onError(int code, String desc) {
                        TUIConversationUtils.callbackOnError(callback, code, desc);
                        TUIConversationLog.e("ConversationIconView",
                                "getGroupMemberList failed! groupID:" + groupId + "|code:" + code + "|desc: " + ErrorMessageConverter.convertIMError(code, desc));
                    }

                    @Override
                    public void onSuccess(V2TIMGroupMemberInfoResult v2TIMGroupMemberInfoResult) {
                        List<V2TIMGroupMemberFullInfo> v2TIMGroupMemberFullInfoList = v2TIMGroupMemberInfoResult.getMemberInfoList();
                        int faceSize = Math.min(v2TIMGroupMemberFullInfoList.size(), iconCount);
                        final List<Object> urlList = new ArrayList<>();
                        for (int i = 0; i < faceSize; i++) {
                            V2TIMGroupMemberFullInfo v2TIMGroupMemberFullInfo = v2TIMGroupMemberFullInfoList.get(i);
                            urlList.add(v2TIMGroupMemberFullInfo.getFaceUrl());
                        }
                        TUIConversationUtils.callbackOnSuccess(callback, urlList);
                    }
                });
    }
}
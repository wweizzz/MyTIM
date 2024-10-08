package com.tencent.qcloud.tuikit.tuiconversation.classicui.interfaces;

import android.view.View;

import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationInfo;

import java.util.List;

public interface OnConversationAdapterListener {
    void onItemClick(View view, int viewType, ConversationInfo conversationInfo);

    void onItemLongClick(View view, int viewType, ConversationInfo conversationInfo);

    void onItemClickTop(View view, int viewType, ConversationInfo conversationInfo);

    void onItemClickDelete(View view, int viewType, ConversationInfo conversationInfo);

    void onConversationChanged(List<ConversationInfo> dataSource);
}

package com.tencent.qcloud.tuikit.tuiconversation.classicui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.tencent.qcloud.tuikit.timcommon.component.TitleBarLayout;
import com.tencent.qcloud.tuikit.tuiconversation.R;
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationInfo;
import com.tencent.qcloud.tuikit.tuiconversation.classicui.interfaces.IConversationLayout;
import com.tencent.qcloud.tuikit.tuiconversation.interfaces.IConversationListAdapter;
import com.tencent.qcloud.tuikit.tuiconversation.presenter.ConversationPresenter;

public class ConversationLayout extends RelativeLayout implements IConversationLayout {
    private ConversationListLayout mConversationList;
    private ConversationPresenter presenter;

    public ConversationLayout(Context context) {
        super(context);
        init();
    }

    public ConversationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConversationLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setPresenter(ConversationPresenter presenter) {
        this.presenter = presenter;
        if (mConversationList != null) {
            mConversationList.setPresenter(presenter);
        }
    }

    private void init() {
        inflate(getContext(), R.layout.conversation_layout, this);
        mConversationList = findViewById(R.id.conversation_list);
    }

    public void initDefault() {
        final ConversationListAdapter adapter = new ConversationListAdapter();
        if (presenter != null) {
            adapter.setShowFoldedStyle(true);
        }
        mConversationList.setAdapter((IConversationListAdapter) adapter);
        if (presenter != null) {
            presenter.setAdapter(adapter);
        }
        mConversationList.loadConversation();
        mConversationList.loadMarkedConversation();
    }

    @Override
    public void setParentLayout(Object parent) {
    }

    @Override
    public ConversationListLayout getConversationList() {
        return mConversationList;
    }

    @Override
    public void setConversationTop(ConversationInfo conversation) {
        if (presenter != null) {
            presenter.setConversationTop(conversation);
        }
    }

    @Override
    public void deleteConversation(ConversationInfo conversation) {
        if (presenter != null) {
            presenter.deleteConversation(conversation);
        }
    }

    @Override
    public void clearConversation(ConversationInfo conversation) {
        if (presenter != null) {
            presenter.clearConversation(conversation);
        }
    }

    @Override
    public void markConversationFold(ConversationInfo conversation) {
        if (presenter != null) {
            presenter.markConversationFold(conversation, true);
        }
    }

    @Override
    public void markConversationFold(ConversationInfo conversation, boolean markFold) {
        if (presenter != null) {
            presenter.markConversationFold(conversation, markFold);
        }
    }

    @Override
    public void markConversationHidden(ConversationInfo conversation) {
        if (presenter != null) {
            presenter.markConversationHidden(conversation, true);
        }
    }

    @Override
    public void markConversationUnread(ConversationInfo conversationInfo, boolean markUnread) {
        if (presenter != null) {
            presenter.markConversationUnread(conversationInfo, markUnread);
        }
    }

    @Override
    public void setHideStatusOfFoldedItem(boolean needHide) {
        if (presenter != null) {
            presenter.setHideStatusOfHideFoldItem(needHide);
        }
    }

    @Override
    public void clearUnreadStatusOfFoldItem() {
        if (presenter != null) {
            presenter.setUnreadStatusOfFoldItem(false);
        }
    }

    @Override
    public TitleBarLayout getTitleBar() {
        return null;
    }
}

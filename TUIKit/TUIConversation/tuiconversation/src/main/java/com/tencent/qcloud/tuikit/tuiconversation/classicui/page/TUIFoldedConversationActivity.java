package com.tencent.qcloud.tuikit.tuiconversation.classicui.page;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.tencent.qcloud.tuikit.timcommon.component.activities.BaseLightActivity;
import com.tencent.qcloud.tuikit.tuiconversation.R;
import com.tencent.qcloud.tuikit.tuiconversation.commonutil.TUIConversationLog;

public class TUIFoldedConversationActivity extends BaseLightActivity {
    private static final String TAG = TUIForwardSelectActivity.class.getSimpleName();

    private TUIFoldedConversationFragment mTUIFoldedConversationFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.folded_activity);

        init();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        TUIConversationLog.i(TAG, "onNewIntent");
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        TUIConversationLog.i(TAG, "onResume");
        super.onResume();
    }

    private void init() {
        mTUIFoldedConversationFragment = new TUIFoldedConversationFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.empty_view, mTUIFoldedConversationFragment).commitAllowingStateLoss();
    }
}

package src.comitton.config;

import src.comitton.common.DEF;
import android.content.Context;
import android.util.AttributeSet;

public class ScalingSeekbar extends SeekBarPreference {

	public ScalingSeekbar(Context context, AttributeSet attrs) {
		super(context, attrs);
		mDefValue = DEF.DEFAULT_SCALING;
		mMaxValue = DEF.MAX_SCALING;
		super.setKey(DEF.KEY_SCALING);
	}
}

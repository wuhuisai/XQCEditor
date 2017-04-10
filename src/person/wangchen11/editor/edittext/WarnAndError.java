package person.wangchen11.editor.edittext;

import android.graphics.Color;
import person.wangchen11.xqceditor.R;

public class WarnAndError {
	public int mLevel;
	public int mColor;
	public int mLine;
	public String mMsg;
	public boolean mDrawUnderLine;
	public boolean mFullLine;
	public int mIndex;
	public int mLength;
	public static final int LEVEL_INFO  = 0;
	public static final int LEVEL_WARN  = 1;
	public static final int LEVEL_ERROR = 2;
	public static final int COLOR_INFO  = Color.BLUE;
	public static final int COLOR_WARN  = Color.rgb(0xf0, 0xb0, 0x20);
	public static final int COLOR_ERROR = Color.RED;
	public WarnAndError(int line,int level){
		this(line,level,"");
	}
	public WarnAndError(int line,int level,String msg){
		mLine  = line;
		mLevel = level;
		switch (mLevel) {
		case LEVEL_INFO:
			mColor = COLOR_INFO;
			break;
		case LEVEL_WARN:
			mColor = COLOR_WARN;
			break;
		case LEVEL_ERROR:
			mColor = COLOR_ERROR;
			break;

		default:
			break;
		}
		mMsg   = msg;
		mDrawUnderLine = true;
		mFullLine = true;
		
	}
	
	public int getTitle(){
		switch (mLevel) {
		case LEVEL_INFO:
			return R.string.tip;
		case LEVEL_WARN:
			return R.string.warn;
		case LEVEL_ERROR:
			return R.string.error;
		default:
			return R.string.tip;
		}
	}
	
	public boolean include(int line,int offset){
		if(mLine==line){
			if(mFullLine==true)
				return true;
			// TODO 
		}
		return false;
	}
}

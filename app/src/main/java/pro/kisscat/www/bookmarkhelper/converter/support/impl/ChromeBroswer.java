package pro.kisscat.www.bookmarkhelper.converter.support.impl;

import android.content.Context;

import lombok.Getter;
import lombok.Setter;
import pro.kisscat.www.bookmarkhelper.R;
import pro.kisscat.www.bookmarkhelper.converter.support.Broswer;
import pro.kisscat.www.bookmarkhelper.converter.support.pojo.Bookmark;

/**
 * Created with Android Studio.
 * Project:BookmarkHelper
 * User:ChengLiang
 * Mail:stevenchengmask@gmail.com
 * Date:2016/10/9
 * Time:15:38
 */

public class ChromeBroswer extends Broswer {
    private String packageName = "com.android.chrome";

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public void readBookmarkSum(Context context) {
        super.setBookmarkSum(11);
    }

    @Override
    public void fillDefaultShow(Context context) {
        this.setName(context.getString(R.string.broswer_name_show_chrome));
        this.setPackageName(packageName);
//        this.setIcon();
    }

    public abstract class ChromeBookmark extends Bookmark {
        @Setter
        @Getter
        private String folder;
        @Setter
        @Getter
        private int order;
    }
}

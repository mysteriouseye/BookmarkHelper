package pro.kisscat.www.bookmarkhelper.converter.support.impl.qihoo360.impl;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import java.util.LinkedList;
import java.util.List;

import pro.kisscat.www.bookmarkhelper.R;
import pro.kisscat.www.bookmarkhelper.converter.support.impl.qihoo360.Qihoo360BrowserAble;
import pro.kisscat.www.bookmarkhelper.entry.app.Bookmark;
import pro.kisscat.www.bookmarkhelper.exception.ConverterException;
import pro.kisscat.www.bookmarkhelper.util.Path;
import pro.kisscat.www.bookmarkhelper.util.context.ContextUtil;
import pro.kisscat.www.bookmarkhelper.util.json.JsonUtil;
import pro.kisscat.www.bookmarkhelper.util.log.LogHelper;
import pro.kisscat.www.bookmarkhelper.util.storage.ExternalStorageUtil;

/**
 * Created with Android Studio.
 * Project:BookmarkHelper
 * User:ChengLiang
 * Mail:stevenchengmask@gmail.com
 * Date:2016/11/10
 * Time:12:27
 * <p>
 * 360浏览器
 */

public class Qihoo360Browser extends Qihoo360BrowserAble {
    private static final String packageName = "com.qihoo.browser";
    private List<Bookmark> bookmarks;

    public Qihoo360Browser() {
        super.TAG = "360";
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public int readBookmarkSum() {
        if (bookmarks == null) {
            readBookmark();
        }
        return bookmarks.size();
    }

    @Override
    public void fillDefaultIcon(Context context) {
        this.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_360));
    }

    @Override
    public void fillDefaultAppName(Context context) {
        this.setName(context.getString(R.string.browser_name_show_360));
    }

    private static final String filePath_origin = Path.INNER_PATH_DATA + packageName + Path.FILE_SPLIT;
    private static final String filePath_cp = Path.SDCARD_ROOTPATH + Path.SDCARD_APP_ROOTPATH + Path.SDCARD_TMP_ROOTPATH + Path.FILE_SPLIT + "360" + Path.FILE_SPLIT;

    @Override
    public List<Bookmark> readBookmark() {
        if (bookmarks != null) {
            LogHelper.v(TAG + ":bookmarks cache is hit.");
            return bookmarks;
        }
        LogHelper.v(TAG + ":bookmarks cache is miss.");
        LogHelper.v(TAG + ":开始读取书签数据");
        try {
            ExternalStorageUtil.mkdir(filePath_cp, this.getName());
            List<Bookmark> bookmarksList = new LinkedList<>();
            List<Bookmark> bookmarksListPart1 = fetchBookmarksListByUserHasLogined(filePath_origin, filePath_cp);
            List<Bookmark> bookmarksListPart2 = fetchBookmarksListByNoUserLogined(filePath_origin, filePath_cp);
            LogHelper.v(TAG + ":已登录用户书签数据:" + JsonUtil.toJson(bookmarksListPart1));
            LogHelper.v(TAG + ":已登录用户书签条数:" + bookmarksListPart1.size());
            LogHelper.v(TAG + ":未登录用户书签数据:" + JsonUtil.toJson(bookmarksListPart2));
            LogHelper.v(TAG + ":未登录用户书签条数:" + bookmarksListPart2.size());
            bookmarksList.addAll(bookmarksListPart1);
            bookmarksList.addAll(bookmarksListPart2);
            LogHelper.v(TAG + ":总的书签条数:" + bookmarksList.size());
            bookmarks = new LinkedList<>();
            fetchValidBookmarks(bookmarks, bookmarksList);
        } catch (ConverterException converterException) {
            LogHelper.e(converterException);
            converterException.printStackTrace();
            throw converterException;
        } catch (Exception e) {
            LogHelper.e(e);
            e.printStackTrace();
            throw new ConverterException(ContextUtil.buildReadBookmarksErrorMessage(this.getName()));
        } finally {
            LogHelper.v(TAG + ":读取书签数据结束");
        }
        return bookmarks;
    }

    @Override
    public int appendBookmark(List<Bookmark> bookmarks) {
        return 0;
    }
}

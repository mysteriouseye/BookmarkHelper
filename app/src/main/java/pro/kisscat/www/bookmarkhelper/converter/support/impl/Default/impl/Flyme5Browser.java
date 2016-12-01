package pro.kisscat.www.bookmarkhelper.converter.support.impl.Default.impl;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import java.util.LinkedList;
import java.util.List;

import pro.kisscat.www.bookmarkhelper.R;
import pro.kisscat.www.bookmarkhelper.converter.support.impl.Default.DefaultBrowserAble;
import pro.kisscat.www.bookmarkhelper.converter.support.pojo.Bookmark;
import pro.kisscat.www.bookmarkhelper.exception.ConverterException;
import pro.kisscat.www.bookmarkhelper.util.Path;
import pro.kisscat.www.bookmarkhelper.util.context.ContextUtil;
import pro.kisscat.www.bookmarkhelper.util.log.LogHelper;
import pro.kisscat.www.bookmarkhelper.util.storage.ExternalStorageUtil;

/**
 * Created with Android Studio.
 * Project:BookmarkHelper
 * User:ChengLiang
 * Mail:stevenchengmask@gmail.com
 * Date:2016/10/9
 * Time:15:38
 */

public class Flyme5Browser extends DefaultBrowserAble {
    private String TAG = "Flyme5";
    private List<Bookmark> bookmarks;

    public Flyme5Browser() {
        super.TAG = this.TAG;
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
        this.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_flyme5));
    }

    @Override
    public void fillDefaultAppName(Context context) {
        this.setName(context.getString(R.string.browser_name_show_flyme5));
    }


    private static final String fileName_origin = "browser2.db";
    private static final String filePath_cp = Path.SDCARD_ROOTPATH + Path.SDCARD_APP_ROOTPATH + Path.SDCARD_TMP_ROOTPATH + "/Flyme5/";

    @Override
    public List<Bookmark> readBookmark() {
        if (bookmarks != null) {
            LogHelper.v(TAG + ":bookmarks cache is hit.");
            return bookmarks;
        }
        LogHelper.v(TAG + ":bookmarks cache is miss.");
        LogHelper.v(TAG + ":开始读取书签数据");
        try {
            String filePath_origin = Path.INNER_PATH_DATA + super.packageName + "/databases/";
            String originFilePathFull = filePath_origin + fileName_origin;
            LogHelper.v(TAG + ":origin file path:" + originFilePathFull);
            ExternalStorageUtil.mkdir(filePath_cp, this.getName());
            LogHelper.v(TAG + ":tmp file path:" + filePath_cp + fileName_origin);
            ExternalStorageUtil.copyFile(originFilePathFull, filePath_cp + fileName_origin, this.getName());
            List<Bookmark> bookmarksList = fetchBookmarksList(filePath_cp + fileName_origin);
            bookmarks = new LinkedList<>();
            fetchValidBookmarks(bookmarks, bookmarksList);
        } catch (ConverterException converterException) {
            converterException.printStackTrace();
            LogHelper.e(converterException.getMessage());
            throw converterException;
        } catch (Exception e) {
            e.printStackTrace();
            LogHelper.e(e.getMessage());
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
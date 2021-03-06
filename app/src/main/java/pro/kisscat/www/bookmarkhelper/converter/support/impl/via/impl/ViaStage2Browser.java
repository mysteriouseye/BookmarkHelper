package pro.kisscat.www.bookmarkhelper.converter.support.impl.via.impl;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import pro.kisscat.www.bookmarkhelper.converter.support.impl.via.ViaBrowserAble;
import pro.kisscat.www.bookmarkhelper.entry.app.Bookmark;
import pro.kisscat.www.bookmarkhelper.database.SQLite.DBHelper;
import pro.kisscat.www.bookmarkhelper.exception.ConverterException;
import pro.kisscat.www.bookmarkhelper.util.Path;
import pro.kisscat.www.bookmarkhelper.util.context.ContextUtil;
import pro.kisscat.www.bookmarkhelper.util.log.LogHelper;
import pro.kisscat.www.bookmarkhelper.util.storage.ExternalStorageUtil;
import pro.kisscat.www.bookmarkhelper.util.storage.InternalStorageUtil;

/**
 * Created with Android Studio.
 * Project:BookmarkHelper
 * User:ChengLiang
 * Mail:stevenchengmask@gmail.com
 * Date:2016/11/14
 * Time:13:18
 * <p>
 * versionName>=2.1.1  && versionCode>=20161113
 * <p>
 * stage2较stage1主要是书签存取由json txt转变为sqlite3
 */

public class ViaStage2Browser extends ViaBrowserAble {
    private static final String TAG = "ViaStage2";
    public static final long minVersionCode = 20161113L;
    private static final String fileName_origin = "via";
    private static final String databaseDirPath_origin = Path.INNER_PATH_DATA + packageName + Path.FILE_SPLIT + "databases" + Path.FILE_SPLIT;
    private static final String filesDirPath_origin = Path.INNER_PATH_DATA + packageName + Path.FILE_SPLIT + "files" + Path.FILE_SPLIT;
    private static final String filePath_cp = Path.SDCARD_ROOTPATH + Path.SDCARD_APP_ROOTPATH + Path.SDCARD_TMP_ROOTPATH + Path.FILE_SPLIT + "Via" + Path.FILE_SPLIT;

    @Override
    public List<Bookmark> readBookmark() {
        if (bookmarks != null) {
            LogHelper.v(TAG + ":bookmarks cache is hit.");
            return bookmarks;
        }
        LogHelper.v(TAG + ":bookmarks cache is miss.");
        LogHelper.v(TAG + ":开始读取书签数据");
        try {
            String originFilePath = databaseDirPath_origin + fileName_origin;
            LogHelper.v(TAG + ":origin file path:" + originFilePath);
            boolean isExist = InternalStorageUtil.isExistFile(originFilePath);
            if (!isExist) {
                throw new ConverterException(ContextUtil.buildViaBookmarksFileMiss(this.getName()));
            }
            ExternalStorageUtil.mkdir(filePath_cp, this.getName());
            String tmpFilePath = filePath_cp + fileName_origin;
            LogHelper.v(TAG + ":tmp file path:" + tmpFilePath);
            ExternalStorageUtil.copyFile(originFilePath, tmpFilePath, this.getName());
            List<Bookmark> bookmarksList = fetchBookmarksList(tmpFilePath);
            bookmarks = new LinkedList<>();
            fetchValidBookmarks(bookmarks, bookmarksList);
        } catch (ConverterException converterException) {
            converterException.printStackTrace();
            LogHelper.e(converterException);
            throw converterException;
        } catch (Exception e) {
            e.printStackTrace();
            LogHelper.e(e);
            throw new ConverterException(ContextUtil.buildReadBookmarksErrorMessage(this.getName()));
        } finally {
            LogHelper.v(TAG + ":读取书签数据结束");
        }
        return bookmarks;
    }

    private final static String[] columns = new String[]{"id", "url", "title", "folder"};
    private final static String tableName = "bookmarks";
    private Integer latestIndex = null;

    private List<Bookmark> fetchBookmarksList(String dbFilePath) {
        LogHelper.v(TAG + ":开始读取书签SQLite数据库:" + dbFilePath);
        List<Bookmark> result = new LinkedList<>();
        SQLiteDatabase sqLiteDatabase = null;
        Cursor cursor = null;
        boolean tableExist;
        latestIndex = 0;
        try {
            sqLiteDatabase = DBHelper.openReadOnlyDatabase(dbFilePath);
            tableExist = DBHelper.checkTableExist(sqLiteDatabase, tableName);
            if (!tableExist) {
                LogHelper.v(TAG + ":database table " + tableName + " not exist.");
                throw new ConverterException(ContextUtil.buildReadBookmarksTableNotExistErrorMessage(this.getName()));
            }
            cursor = sqLiteDatabase.query(false, tableName, columns, null, null, "url", null, "id asc", null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    Bookmark item = new Bookmark();
                    item.setUrl(cursor.getString(cursor.getColumnIndex("url")));
                    item.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                    item.setFolder(cursor.getString(cursor.getColumnIndex("folder")));
                    latestIndex = cursor.getInt(cursor.getColumnIndex("id"));
                    result.add(item);
                }
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (sqLiteDatabase != null) {
                sqLiteDatabase.close();
            }
            LogHelper.v(TAG + ":读取书签SQLite数据库结束");
        }
        return result;
    }

    @Override
    public int appendBookmark(List<Bookmark> appends) {
        LogHelper.v(TAG + ":开始合并书签数据，bookmarks appends size:" + appends.size());
        int successCount = 0;
        SQLiteDatabase sqLiteDatabase = null;
        try {
            List<Bookmark> exists = this.readBookmark();
            Set<Bookmark> increment = buildNoRepeat(appends, exists);
            LogHelper.v(TAG + ":bookmarks increment size:" + increment.size());
            if (increment.isEmpty()) {
                return 0;
            }
            String originFilePath = databaseDirPath_origin + fileName_origin;
            String tmpFilePath = filePath_cp + fileName_origin;
            LogHelper.v(TAG + ":tmp file path:" + tmpFilePath);
            sqLiteDatabase = DBHelper.openDatabase(tmpFilePath);
            int count = 0;
            for (Bookmark item : increment) {
                latestIndex++;
                ContentValues cv = new ContentValues();
                cv.put("id", latestIndex);
                cv.put("url", item.getUrl());
                cv.put("title", item.getTitle());
                if (item.getFolder() == null) {
                    cv.put("folder", "");
                } else {
                    cv.put("folder", item.getFolder());
                }
                cv.put("clickTimes", 0);
                long ret = sqLiteDatabase.insert(tableName, null, cv);
                if (ret <= -1) {
                    LogHelper.e("database insert error");
                    continue;
                }
                count++;
            }
            ExternalStorageUtil.copyFile(tmpFilePath, originFilePath, this.getName());
            String cleanFilePath = filesDirPath_origin + "bookmarks.html";//干掉这个缓存文件，以便via重新生成书签页面
            InternalStorageUtil.deleteFile(cleanFilePath, this.getName());
            successCount = count;
        } catch (ConverterException converterException) {
            LogHelper.e(converterException.getMessage());
            converterException.printStackTrace();
            throw converterException;
        } catch (Exception e) {
            e.printStackTrace();
            LogHelper.e(e.getMessage());
            throw new ConverterException(ContextUtil.buildAppendBookmarksErrorMessage(this.getName()));
        } finally {
            if (sqLiteDatabase != null) {
                sqLiteDatabase.close();
            }
            this.bookmarks = null;
            this.latestIndex = null;
            LogHelper.v(TAG + ":合并书签数据结束");
        }
        return successCount;
    }
}

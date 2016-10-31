package pro.kisscat.www.bookmarkhelper.converter.support.impl;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import pro.kisscat.www.bookmarkhelper.R;
import pro.kisscat.www.bookmarkhelper.common.shared.MetaData;
import pro.kisscat.www.bookmarkhelper.converter.support.BasicBroswer;
import pro.kisscat.www.bookmarkhelper.converter.support.pojo.Bookmark;
import pro.kisscat.www.bookmarkhelper.exception.ConverterException;
import pro.kisscat.www.bookmarkhelper.util.Path;
import pro.kisscat.www.bookmarkhelper.util.context.ContextUtil;
import pro.kisscat.www.bookmarkhelper.util.json.JsonUtil;
import pro.kisscat.www.bookmarkhelper.util.log.LogHelper;
import pro.kisscat.www.bookmarkhelper.util.storage.ExternalStorageUtil;
import pro.kisscat.www.bookmarkhelper.util.storage.InternalStorageUtil;

/**
 * Created with Android Studio.
 * Project:BookmarkHelper
 * User:ChengLiang
 * Mail:stevenchengmask@gmail.com
 * Date:2016/10/9
 * Time:15:38
 */

public class ViaBroswer extends BasicBroswer {
    private static final String TAG = "Via";
    private static final String packageName = "mark.via";
    private List<Bookmark> bookmarks;

    public String getPackageName() {
        return packageName;
    }

    @Override
    public int readBookmarkSum(Context context) {
        if (bookmarks == null) {
            readBookmark(context);
        }
        return bookmarks.size();
    }

    @Override
    public void fillDefaultIcon(Context context) {
        this.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_via));
    }

    @Override
    public void fillDefaultAppName(Context context) {
        this.setName(context.getString(R.string.broswer_name_show_via));
    }

    private static final String fileName_origin = "bookmarks.dat";
    private static final String filePath_origin = Path.INNER_PATH_DATA + packageName + "/files/";
    private static final String filePath_cp = Path.SDCARD_ROOTPATH + Path.SDCARD_APP_ROOTPATH + Path.SDCARD_TMP_ROOTPATH + "/Via/";

    @Override
    public List<Bookmark> readBookmark(Context context) {
        if (bookmarks != null) {
            LogHelper.v(TAG + ":bookmarks cache is hit.");
            return bookmarks;
        }
        LogHelper.v(TAG + ":bookmarks cache is miss.");
        LogHelper.v(TAG + ":开始读取书签数据");
        BufferedReader reader = null;
        try {
            String originFilePath = filePath_origin + fileName_origin;
            LogHelper.v(TAG + ":origin file path:" + originFilePath);
            boolean isExist = InternalStorageUtil.isExistFile(originFilePath);
            if (!isExist) {
                throw new ConverterException(ContextUtil.buildViaBookmarksFileMiss(context, this.getName()));
            }
            File cpPath = new File(filePath_cp);
            cpPath.deleteOnExit();
            cpPath.mkdirs();
            String tmpFilePath = filePath_cp + fileName_origin;
            LogHelper.v(TAG + ":tmp file path:" + tmpFilePath);
            File file = ExternalStorageUtil.copyFile(context, originFilePath, tmpFilePath, this.getName());
            reader = new BufferedReader(new FileReader(file));
            List<ViaBookmark> list = new ArrayList<>();
            String tempString;
            while ((tempString = reader.readLine()) != null) {
                ViaBookmark item = JsonUtil.fromJson(tempString, ViaBookmark.class);
                list.add(item);
            }
            reader.close();
            LogHelper.v("书签数据:" + JsonUtil.toJson(list));
            LogHelper.v("书签条数:" + list.size());
            bookmarks = new LinkedList<>();
            for (ViaBookmark item : list) {
                String bookmarkUrl = item.getUrl();
                String bookmarkName = item.getTitle();
                LogHelper.v("name:" + bookmarkName);
                LogHelper.v("url:" + bookmarkUrl);
                if (bookmarkUrl == null || bookmarkUrl.isEmpty()) {
                    LogHelper.v("url:" + bookmarkUrl + ",skip.");
                }
                if (bookmarkName == null || bookmarkName.isEmpty()) {
                    LogHelper.v("url:" + bookmarkName + ",set to default value.");
                    bookmarkName = MetaData.BOOKMARK_TITLE_DEFAULT;
                }
                Bookmark bookmark = new Bookmark();
                bookmark.setTitle(bookmarkName);
                bookmark.setUrl(bookmarkUrl);
                bookmarks.add(bookmark);
            }
        } catch (ConverterException converterException) {
            converterException.printStackTrace();
            LogHelper.e(MetaData.LOG_E_DEFAULT, converterException.getMessage());
            throw converterException;
        } catch (Exception e) {
            e.printStackTrace();
            LogHelper.e(MetaData.LOG_E_DEFAULT, e.getMessage());
            throw new ConverterException(ContextUtil.buildReadBookmarksErrorMessage(context, this.getName()));
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    LogHelper.e(MetaData.LOG_E_DEFAULT, e1.getMessage());
                }
            }
            LogHelper.v(TAG + ":读取书签数据结束");
        }
        return bookmarks;
    }

    @Override
    public int appendBookmark(Context context, List<Bookmark> appends) {
        LogHelper.v(TAG + ":开始合并书签数据，bookmarks appends size:" + appends.size());
        int successCount = 0;
        BufferedWriter writer = null;
        try {
            List<Bookmark> exists = this.readBookmark(context);
            Set<Bookmark> increment = buildNoRepeat(appends, exists);
            LogHelper.v(TAG + ":bookmarks increment size:" + increment.size());
            if (increment.isEmpty()) {
                return 0;
            }
            exists.addAll(increment);
            LogHelper.v(TAG + ":merge size:" + exists.size());
            String originFilePath = filePath_origin + fileName_origin;
            String tmpFilePath = filePath_cp + fileName_origin;
            LogHelper.v(TAG + ":tmp file path:" + tmpFilePath);
            writer = new BufferedWriter(new FileWriter(tmpFilePath, false));//覆盖原文件
//            int index = 0;
            for (Bookmark item : exists) {
                ViaBookmark viaBookmark = new ViaBookmark();
                viaBookmark.setTitle(item.getTitle());
                viaBookmark.setUrl(item.getUrl());
//                viaBookmark.setOrder(index);
                viaBookmark.setOrder(0);
//                index++;
                String json = JsonUtil.toJson(viaBookmark);
                if (json == null) {
                    continue;
                }
                writer.write(json);
                writer.newLine();//换行
            }
            writer.flush();
            ExternalStorageUtil.copyFile(context, tmpFilePath, originFilePath, this.getName());

            String cleanFilePath = filePath_origin + "bookmarks.html";//干掉这个缓存文件，以便via重新生成书签页面
            InternalStorageUtil.deleteFile(context, cleanFilePath, this.getName());
            successCount = increment.size();
        } catch (ConverterException converterException) {
            converterException.printStackTrace();
            LogHelper.e(MetaData.LOG_E_DEFAULT, converterException.getMessage());
            throw converterException;
        } catch (Exception e) {
            e.printStackTrace();
            LogHelper.e(MetaData.LOG_E_DEFAULT, e.getMessage());
            throw new ConverterException(ContextUtil.buildAppendBookmarksErrorMessage(context, this.getName()));
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    LogHelper.e(MetaData.LOG_E_DEFAULT, e1.getMessage());
                }
            }
            this.bookmarks = null;
            LogHelper.v(TAG + ":合并书签数据结束");
        }
        return successCount;
    }

    private static class ViaBookmark {
        @Setter
        @Getter
        @JSONField(ordinal = 1)
        String title;
        @Setter
        @Getter
        @JSONField(ordinal = 2)
        String url;
        @Setter
        @Getter
        @JSONField(ordinal = 3)
        String folder = "";
        @Setter
        @Getter
        @JSONField(ordinal = 4)
        int order = 0;
    }
}

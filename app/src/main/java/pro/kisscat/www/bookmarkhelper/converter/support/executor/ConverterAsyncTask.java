package pro.kisscat.www.bookmarkhelper.converter.support.executor;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.List;

import lombok.Setter;
import pro.kisscat.www.bookmarkhelper.converter.support.BasicBrowser;
import pro.kisscat.www.bookmarkhelper.entry.app.Bookmark;
import pro.kisscat.www.bookmarkhelper.entry.converter.Params;
import pro.kisscat.www.bookmarkhelper.entry.rule.Rule;
import pro.kisscat.www.bookmarkhelper.exception.ConverterException;
import pro.kisscat.www.bookmarkhelper.pojo.executor.Result;
import pro.kisscat.www.bookmarkhelper.util.context.ContextUtil;
import pro.kisscat.www.bookmarkhelper.util.json.JsonUtil;
import pro.kisscat.www.bookmarkhelper.util.log.LogHelper;
import pro.kisscat.www.bookmarkhelper.util.progressBar.ProgressBarUtil;
import pro.kisscat.www.bookmarkhelper.util.root.RootUtil;
import pro.kisscat.www.bookmarkhelper.util.storage.ExternalStorageUtil;
import pro.kisscat.www.bookmarkhelper.util.storage.InternalStorageUtil;

/**
 * Created with Android Studio.
 * Project:BookmarkHelper
 * User:ChengLiang
 * Mail:stevenchengmask@gmail.com
 * Date:2016/11/24
 * Time:10:59
 */

public class ConverterAsyncTask extends AsyncTask<Params, Void, Result> {
    private static final String TAG = "ConverterAsyncTask";
    private ProgressBarUtil progressBarUtil;
    @Setter
    private boolean needFC = false;
    private static final Result fcResult = new Result(true, "force close task.");
    private Handler handler;

    public ConverterAsyncTask(ProgressBarUtil progressBarUtil) {
        this.progressBarUtil = progressBarUtil;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        if (needFC) {
            this.cancel(true);
        }
        progressBarUtil.next();
    }

    @Override
    protected void onPostExecute(Result result) {
        result.setComplete(true);
        handleDialogMessage(JsonUtil.toJson(result));
        progressBarUtil.stop();
        LogHelper.write();
    }

    private void handleToastMessage(String msg) {
        handleMessage(0, msg);
    }

    private void handleToastMessage(Result result) {
        handleToastMessage(JsonUtil.toJson(result));
    }

    private void handleDialogMessage(String msg) {
        handleMessage(1, msg);
    }

    private void handleMessage(int what, String msg) {
        if (handler != null) {
            Message message = new Message();
            message.what = what;
            Bundle bundle = new Bundle();
            bundle.putString("result", msg);
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }

    @Override
    protected Result doInBackground(Params... params) {
        if (needFC) {
            return fcResult;
        }
        Params param = params[0];
        handler = param.getHandler();
        Rule rule = param.getRule();
        if (needFC) {
            return fcResult;
        }
        handlePreExecuteMessage(rule);
        long s = System.currentTimeMillis();
        LogHelper.v(TAG, "开始执行合并，规则是:" + rule.getSource().getName() + "----------->" + rule.getTarget().getName());
        LogHelper.v(TAG, "rule detail info:" + rule.toString());
        Result result = processConverter(rule);
        LogHelper.v(TAG, "完成合并，耗时：" + (System.currentTimeMillis() - s) + "ms，规则是:" + rule.getSource().getName() + "----------->" + rule.getTarget().getName());
        if (needFC) {
            return fcResult;
        }
        return result;
    }

    private void handlePreExecuteMessage(Rule rule) {
        String sourceMessage = rule.getSource().getPreExecuteConverterMessage();
        if (sourceMessage != null) {
            Result result = new Result(sourceMessage);
            handleToastMessage(JsonUtil.toJson(result));
        }
    }

    private void handleExecuteRunningMessage() {
        handleToastMessage(new Result("正在合并，约需30秒，请等待."));
    }

    private void handleExecuteCompleteMessage() {
        handleToastMessage(new Result("已完成合并."));
    }

    private void handleUpgradeRootPermissionMessage() {
        handleToastMessage(new Result("正在获取Root权限，如长时间无响应，请打开Root管理App."));
    }

    private void handleCheckReadAndWriteAbleMessage() {
        handleToastMessage(new Result("正在检查存储读写权限."));
    }

    private Result processConverter(Rule rule) {
        Result result = new Result();
        long start = System.currentTimeMillis();
        long end = -1;
        int ret = -1;
        try {
            if (needFC) {
                return fcResult;
            }
            handleUpgradeRootPermissionMessage();
            boolean isRoot = RootUtil.upgradeRootPermission();
            if (!isRoot) {
                String errorUpgrade = "获取Root权限失败，不能使用.";
                result.setErrorMsg(errorUpgrade);
                LogHelper.v(errorUpgrade);
                return result;
            } else {
                if (needFC) {
                    return fcResult;
                }
                publishProgress();
                LogHelper.v("成功获取了Root权限.");
            }
            handleCheckReadAndWriteAbleMessage();
            if (!InternalStorageUtil.remountDataDir()) {
                result.setErrorMsg(ContextUtil.getSystemNotReadOrWriteable());
                return result;
            }
            if (needFC) {
                return fcResult;
            }
            publishProgress();
            if (!ExternalStorageUtil.remountSDCardDir()) {
                result.setErrorMsg(ContextUtil.getSDCardNotReadOrWriteable());
                return result;
            }
            if (needFC) {
                return fcResult;
            }
            publishProgress();
            handleExecuteRunningMessage();
            start = System.currentTimeMillis();
            ret = execute(rule);
            end = System.currentTimeMillis();
            if (needFC) {
                return fcResult;
            }
            publishProgress();
            handleExecuteCompleteMessage();
        } catch (ConverterException e) {
            if (result.getErrorMsg() == null) {
                result.setErrorMsg(e.getMessage());
            }
            return result;
        } finally {
            if (ret > 0) {
                String s = null;
                if (end > 0) {
                    long ms = end - start;
                    if (ms > 1000) {
                        s = ((end - start) / 1000) + "s";
                    } else {
                        s = ms + "ms";
                    }
                }
                result.setSuccessCount(ret);
                result.setSuccessMsg(rule.getSource().getName() + "：" + ret + "条书签合并完成，重启" + rule.getTarget().getName() + "后见效" + (s == null ? "." : ("，耗时：" + s + ".")));
            } else if (ret == 0) {
                result.setSuccessCount(ret);
                result.setSuccessMsg(rule.getSource().getName() + "：" + "所有书签已存在，不需要合并.");
            } else {
                if (result.getErrorMsg() == null) {
                    result.setErrorMsg(rule.getSource().getName() + "：" + "合并遇到问题，请联系作者.");
                }
            }
            LogHelper.write();
        }
        publishProgress();
        return result;
    }

    private int execute(Rule rule) {
        BasicBrowser source = rule.getSource();
        List<Bookmark> sourceBookmarks = source.readBookmark();
        publishProgress();
        if (sourceBookmarks == null) {
            throw new ConverterException(ContextUtil.buildReadBookmarksErrorMessage(source.getName()));
        }
        if (sourceBookmarks.isEmpty()) {
            throw new ConverterException(ContextUtil.buildReadBookmarksEmptyMessage(source.getName()));
        }
        BasicBrowser target = rule.getTarget();
        int ret = target.appendBookmark(sourceBookmarks);
        publishProgress();
        if (ret < 0) {
            throw new ConverterException(ContextUtil.buildAppendBookmarksErrorMessage(target.getName()));
        }
        return ret;
    }
}

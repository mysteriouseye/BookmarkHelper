package pro.kisscat.www.bookmarkhelper.entry.rule;

/**
 * Created with Android Studio.
 * Project:BookmarkHelper
 * User:ChengLiang
 * Mail:stevenchengmask@gmail.com
 * Date:2016/10/10
 * Time:10:11
 */

import android.content.Context;

import lombok.Getter;
import lombok.Setter;
import pro.kisscat.www.bookmarkhelper.converter.support.BasicBrowser;

/**
 * 从source合并到target
 */
public class Rule {
    @Setter
    @Getter
    private int id;
    @Setter
    @Getter
    private BasicBrowser source;
    @Setter
    @Getter
    private BasicBrowser target;
    @Setter
    @Getter
    private boolean canUse;

    public Rule(int id, Context context, BasicBrowser source, BasicBrowser target) {
        boolean sourceInstalled = false, targetInstalled = false;
        if (source.isInstalled(context, source)) {
            sourceInstalled = true;
            source.fillVersion(context);
        } else {
            source.fillDefaultIcon(context);
        }
        if (source.getIcon() == null) {
            source.fillDefaultIcon(context);
        }
        source.fillName(context);
        if (target.isInstalled(context, target)) {
            targetInstalled = true;
            target.fillVersion(context);
        } else {
            target.fillDefaultIcon(context);
        }
        if (target.getIcon() == null) {
            target.fillDefaultIcon(context);
        }
        if (sourceInstalled && targetInstalled) {
            canUse = true;
        }
        target.fillName(context);
        this.id = id;
        this.source = source;
        this.target = target;
    }

    @Override
    public String toString() {
        return "{\"id\":" + id + ",\"source\":{" + source.toString() + "},\"target\":{" + target.toString() + "},\"canUse\":" + canUse + "}";
    }
}

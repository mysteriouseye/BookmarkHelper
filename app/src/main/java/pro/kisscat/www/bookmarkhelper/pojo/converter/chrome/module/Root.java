package pro.kisscat.www.bookmarkhelper.pojo.converter.chrome.module;

import lombok.Getter;
import lombok.Setter;

/**
 * Created with Android Studio.
 * Project:BookmarkHelper
 * User:ChengLiang
 * Mail:stevenchengmask@gmail.com
 * Date:2016/11/1
 * Time:18:08
 */

public class Root {
    @Setter
    @Getter
    private Node bookmark_bar;
    @Setter
    @Getter
    private Node other;
    @Setter
    @Getter
    private Node synced;
    @Setter
    @Getter
    private Node tablet;
}

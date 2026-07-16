// LuaLoader for plugin.webviewext (Android variant).
// Entry point: require("plugin.webviewext")
// Returns a Lua module {hideZoomControls = function() ... end}.
//
// Solar2D's Android core (CoronaWebView.java) calls
// settings.setBuiltInZoomControls(true) on every native.newWebView but never
// calls settings.setDisplayZoomControls(false). Android's default for display
// zoom controls is true, so on-screen +/- zoom buttons pop up in the bottom
// right corner whenever the user pinches or scrolls past a page edge. There is
// no Lua API to reach WebSettings, hence this plugin.
//
// hideZoomControls() walks the activity view hierarchy on the UI thread and
// disables the on-screen zoom buttons on every WebView found. Pinch-to-zoom
// keeps working. Call it right after native.newWebView(); both the WebView
// creation and this call are posted to the main looper in order, so the
// WebView is guaranteed to exist by the time the walk runs.

package plugin.webviewext;

import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;

@SuppressWarnings({"WeakerAccess", "unused"})
public class LuaLoader implements JavaFunction {

    @SuppressWarnings("unused")
    public LuaLoader() {
    }

    @Override
    public int invoke(LuaState L) {
        // Stack: [moduleName]
        L.newTable(0, 1);
        L.pushJavaFunction(new HideZoomControls());
        L.setField(-2, "hideZoomControls");
        // Stack: [moduleName, module]
        return 1;
    }

    private static class HideZoomControls implements JavaFunction {
        @Override
        public int invoke(LuaState L) {
            final CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
            if (activity == null) {
                return 0;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideOn(activity.getWindow().getDecorView());
                }
            });
            return 0;
        }

        private static void hideOn(View view) {
            if (view instanceof WebView) {
                ((WebView) view).getSettings().setDisplayZoomControls(false);
            } else if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    hideOn(group.getChildAt(i));
                }
            }
        }
    }
}

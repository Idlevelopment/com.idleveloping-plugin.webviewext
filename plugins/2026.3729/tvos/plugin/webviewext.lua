-- plugin.webviewext — non-Android no-op.
-- The on-screen +/- zoom buttons this plugin hides are an Android WebView
-- artifact (WebSettings built-in zoom controls); iOS/macOS/tvOS webviews
-- never show them, so there is nothing to do here.

local M = {}

function M.hideZoomControls()
end

return M

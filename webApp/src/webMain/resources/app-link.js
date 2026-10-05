// Shows elements marked data-app-link only on devices that can install
// Muster, and not inside the app's own web view (its Privacy policy screen).
// Dev has no App Store id, so iPhones see nothing there.
(function () {
    var ua = navigator.userAgent;
    var iosAppStoreId = "@IOS_APP_STORE_ID@";
    var iphone = /iPhone|iPod/.test(ua);
    var inAppWebView = /; wv\)/.test(ua) || (iphone && !/Safari\//.test(ua));
    var installable = /Android/.test(ua) || (iphone && iosAppStoreId !== "");
    if (!installable || inAppWebView) return;
    var elements = document.querySelectorAll("[data-app-link]");
    for (var i = 0; i < elements.length; i++) elements[i].hidden = false;
})();

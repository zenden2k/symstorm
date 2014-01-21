chrome.extension.onRequest.addListener(
    function(request, sender, sendResponse) {
        if(request.method == "getRouteInfo"){
            var url = document.location.href;
//            var sfToolbar = document.querySelector(".sf-toolbar");
            var controllerEl = document.querySelector(".sf-toolbar-info-with-next-pointer abbr");
            var controller = (controllerEl && controllerEl.title) ? controllerEl.title.replace(/.*Proxy.*\\__CG__\\/,'') : '';
            var methodEl = document.querySelector(".sf-toolbar-info-method");
            var method = methodEl ? methodEl.innerText : '';

            sendResponse({ data: {method: method, controller: controller, url: url }, method: "getRouteInfo"});

        }
    }
);
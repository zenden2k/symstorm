chrome.browserAction.onClicked.addListener(function(tab) {
    chrome.tabs.sendRequest(tab.id, {method: "getRouteInfo"}, function(response) {
        if( response && response.method=="getRouteInfo"){
            // alert(url);
            var xhr = new XMLHttpRequest();
            // xhr.onreadystatechange = handleStateChange;
            xhr.open("POST", "http://localhost:63343/", true);
            xhr.setRequestHeader("Content-Type", "text/xml");

            var req =
                '<?xml version=\"1.0\" encoding=\"UTF-8\"?>' +
                    '<methodCall>'+
                    '<methodName>symStormHandler.open</methodName>'+
                    '<params>'+
                    '<param><value>' + response.data.url + '</value></param>'+
                    '<param><value>' + response.data.controller + '</value></param>'+
                    '<param><value>' + response.data.method + '</value></param>'+
                    '</params>'+
                    '</methodCall>';

            xhr.send(req);
//            alert(req);
        } else {
            alert("Request to content script failed");
        }
    });
});
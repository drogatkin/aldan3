var CLPBD = function() {
    'available': function() {
        return (window.clipboardData && window.clipboardData.setData) || document.queryCommandSupported('copy')
    },
    canCopy: function () {
        return (window.clipboardData && window.clipboardData.setData) || document.queryCommandEnabled('copy')
    },
    
    copy: function (text) {
        if (window.clipboardData && window.clipboardData.setData) {
            return clipboardData.setData("Text", text)
        }
        var textArea = document.createElement("textarea")
        textArea.value = text;
        textArea.style.position = "fixed"
        document.body.appendChild(textArea);
        var res = false
        textArea.select();
        try {
            if (canCopy())
                res =  document.execCommand('copy');
        } catch (err) {
             
        } finally {
            document.body.removeChild(textArea)
        }
        return res
    }    
    
}
$(".rippleDarkEnable").click(function (e) {
    $(".rippleDark").remove();
    var posX = $(this).offset().left;
    var posY = $(this).offset().top;
    var buttonWidth = $(this).width();
    var buttonHeight =  $(this).height();
    $(this).prepend("<span class='rippleDark'></span>");
    if(buttonWidth >= buttonHeight) {
        buttonHeight = buttonWidth;
    } else {
        buttonWidth = buttonHeight;
    }
    var x = e.pageX - posX - buttonWidth / 2;
    var y = e.pageY - posY - buttonHeight / 2;
    $(".rippleDark").css({
        width: buttonWidth,
        height: buttonHeight,
        top: y + 'px',
        left: x + 'px'
    }).addClass("rippleEffect");
});
$(".rippleLightEnable").click(function (e) {
    $(".rippleLight").remove();
    var posX = $(this).offset().left;
    var posY = $(this).offset().top;
    var buttonWidth = $(this).width();
    var buttonHeight =  $(this).height();
    $(this).prepend("<span class='rippleLight'></span>");
    if(buttonWidth >= buttonHeight) {
        buttonHeight = buttonWidth;
    } else {
        buttonWidth = buttonHeight;
    }
    var x = e.pageX - posX - buttonWidth / 2;
    var y = e.pageY - posY - buttonHeight / 2;
    $(".rippleLight").css({
        width: buttonWidth,
        height: buttonHeight,
        top: y + 'px',
        left: x + 'px'
    }).addClass("rippleEffect");
});
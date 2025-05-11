window.onload = function () {
    Inputmask({
        mask: "+7 (999) 999-99-99",
        showMaskOnHover: false,
        showMaskOnFocus: true,
        clearIncomplete: true,
        autoUnmask: false,
        placeholder: "_"
    }).mask("#customerPhone");
};
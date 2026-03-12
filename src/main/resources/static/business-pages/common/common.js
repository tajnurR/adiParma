window.BusinessPages = window.BusinessPages || {};

window.BusinessPages.register = function (key, init) {
    window.BusinessPages[key] = { init };
};

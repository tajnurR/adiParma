window.ToastService = (function () {
    let container = null;

    function ensureContainer() {
        if (container) return container;
        container = document.createElement("div");
        container.className = "toast-container";
        document.body.appendChild(container);
        return container;
    }

    function show(message, type = "error", timeout = 2500) {
        if (!message) return;
        const host = ensureContainer();
        const toast = document.createElement("div");
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        host.appendChild(toast);

        requestAnimationFrame(() => {
            toast.classList.add("is-visible");
        });

        setTimeout(() => {
            toast.classList.remove("is-visible");
            setTimeout(() => {
                toast.remove();
            }, 200);
        }, timeout);
    }

    return { show };
})();

window.BusinessPages.register("settings", function (root) {
    const statusBadge = root.querySelector("#day-status-badge");
    const businessDateEl = root.querySelector("#day-business-date");
    const openedAtEl = root.querySelector("#day-opened-at");
    const openedByEl = root.querySelector("#day-opened-by");
    const closedAtEl = root.querySelector("#day-closed-at");
    const closedByEl = root.querySelector("#day-closed-by");
    const openBtn = root.querySelector("#day-open-btn");
    const closeBtn = root.querySelector("#day-close-btn");
    const recordsBody = root.querySelector("#day-records-body");

    if (!recordsBody) return root;

    function formatDateTime(value) {
        if (!value) return "—";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;
        return date.toLocaleString("en-GB", {
            day: "2-digit",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function formatDate(value) {
        if (!value) return "—";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;
        return date.toLocaleDateString("en-GB", {
            day: "2-digit",
            month: "short",
            year: "numeric"
        });
    }

    function formatMoney(value) {
        const numeric = Number(value || 0);
        return `৳${numeric.toFixed(2)}`;
    }

    function updateStatusUi(data) {
        const isOpen = Boolean(data?.isOpen);
        const headerDayStatus = document.getElementById("app-day-status");

        window.AppDayState = { isOpen };
        if (headerDayStatus) {
            headerDayStatus.textContent = isOpen ? "Day Open" : "Day Closed";
        }

        if (statusBadge) {
            statusBadge.textContent = isOpen ? "Day Open" : "Day Closed";
            statusBadge.classList.toggle("is-open", isOpen);
        }
        if (businessDateEl) businessDateEl.textContent = formatDate(data?.date);
        if (openedAtEl) openedAtEl.textContent = formatDateTime(data?.openedAt);
        if (openedByEl) openedByEl.textContent = data?.openedBy || "—";
        if (closedAtEl) closedAtEl.textContent = formatDateTime(data?.closedAt);
        if (closedByEl) closedByEl.textContent = data?.closedBy || "—";

        if (openBtn) openBtn.disabled = isOpen;
        if (closeBtn) closeBtn.disabled = !isOpen;
    }

    function loadStatus() {
        return fetch("/api/day/status")
            .then((response) => response.json())
            .then((data) => {
                updateStatusUi(data);
                return data;
            })
            .catch(() => updateStatusUi({ isOpen: false }));
    }

    function loadRecords() {
        fetch("/api/day/records?limit=10")
            .then((response) => response.json())
            .then((data) => {
                const items = Array.isArray(data) ? data : [];
                if (items.length === 0) {
                    recordsBody.innerHTML = `
                        <tr>
                            <td colspan="7" class="settings-empty">No day records yet.</td>
                        </tr>
                    `;
                    return;
                }
                recordsBody.innerHTML = items.map((item) => `
                    <tr>
                        <td>${formatDate(item.businessDate)}</td>
                        <td>${item.status || "—"}</td>
                        <td>${formatDateTime(item.openedAt)}</td>
                        <td>${item.openedBy || "—"}</td>
                        <td>${formatDateTime(item.closedAt)}</td>
                        <td>${item.closedBy || "—"}</td>
                        <td>${formatMoney(item.totalSales)}</td>
                    </tr>
                `).join("");
            })
            .catch(() => {
                recordsBody.innerHTML = `
                    <tr>
                        <td colspan="7" class="settings-empty">Unable to load day records.</td>
                    </tr>
                `;
            });
    }

    function runAction(endpoint, successMessage) {
        if (openBtn) openBtn.disabled = true;
        if (closeBtn) closeBtn.disabled = true;
        fetch(endpoint, { method: "POST" })
            .then((response) => response.json().then((data) => ({ ok: response.ok, data })))
            .then(({ ok, data }) => {
                if (!ok) {
                    throw new Error(data?.message || "Unable to update day status.");
                }
                updateStatusUi(data);
                loadRecords();
                if (window.ToastService && typeof window.ToastService.show === "function") {
                    window.ToastService.show(successMessage, "success");
                }
            })
            .catch((error) => {
                if (window.ToastService && typeof window.ToastService.show === "function") {
                    window.ToastService.show(error.message || "Unable to update day status.", "error");
                }
            })
            .finally(() => {
                loadStatus();
            });
    }

    if (openBtn) {
        openBtn.addEventListener("click", () => runAction("/api/day/open", "Day opened successfully."));
    }
    if (closeBtn) {
        closeBtn.addEventListener("click", () => runAction("/api/day/close", "Day closed successfully."));
    }

    loadStatus();
    loadRecords();
    return root;
});

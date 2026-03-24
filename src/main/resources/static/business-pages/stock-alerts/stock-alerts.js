window.BusinessPages.register("stockAlerts", function (root) {
    const searchInput = root.querySelector("#stock-alerts-search");
    const sortSelect = root.querySelector("#stock-alerts-sort");
    const sortDirBtn = root.querySelector("#stock-alerts-sort-dir");
    const filterSelect = root.querySelector("#stock-alerts-filter");
    const pageSizeSelect = root.querySelector("#stock-alerts-page-size");
    const resultCount = root.querySelector("#stock-alerts-result-count");
    const tableBody = root.querySelector("#stock-alerts-body");
    const paginationEl = root.querySelector("#stock-alerts-pagination");
    const lowCount = root.querySelector("#stock-summary-low");
    const expiringCount = root.querySelector("#stock-summary-expiring");
    const outCount = root.querySelector("#stock-summary-out");
    const expiringLabel = root.querySelector("#stock-summary-expiring-label");
    const tabs = Array.from(root.querySelectorAll(".stock-tab"));

    if (!tableBody) return root;

    const state = {
        query: "",
        type: "all",
        page: 0,
        size: Number(pageSizeSelect?.value || 15),
        sort: sortSelect?.value || "sku",
        dir: "asc"
    };

    let searchTimer = null;

    function updateSortDirButton() {
        if (!sortDirBtn) return;
        const icon = sortDirBtn.querySelector(".material-symbols-outlined");
        if (icon) {
            icon.textContent = state.dir === "asc" ? "north" : "south";
        }
        sortDirBtn.setAttribute(
            "aria-label",
            state.dir === "asc" ? "Sort ascending" : "Sort descending"
        );
    }

    function setLoading() {
        tableBody.innerHTML = `
            <tr>
                <td colspan="5" class="stock-empty">Loading alerts...</td>
            </tr>
        `;
    }

    function setEmpty() {
        tableBody.innerHTML = `
            <tr>
                <td colspan="5" class="stock-empty">No alerts found.</td>
            </tr>
        `;
    }

    function formatDate(value) {
        if (!value) return "—";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;
        return date.toLocaleDateString("en-GB");
    }

    function renderRows(items) {
        if (!items || items.length === 0) {
            setEmpty();
            return;
        }
        tableBody.innerHTML = items
            .map((row) => {
                const qty = row.qty ?? 0;
                const status = row.status;
                const badgeClass = status === "out_of_stock"
                    ? "stock-badge-out"
                    : status === "expiring_soon"
                        ? "stock-badge-expiring"
                        : "stock-badge-low";
                const badgeLabel = status === "out_of_stock"
                    ? "Out of Stock"
                    : status === "expiring_soon"
                        ? "Expiring Soon"
                        : "Low Stock";
                return `
                    <tr>
                        <td>${row.sku || "—"}</td>
                        <td>${row.name || "—"}</td>
                        <td class="stock-qty">${qty}</td>
                        <td>${formatDate(row.expireDate)}</td>
                        <td><span class="stock-badge ${badgeClass}">${badgeLabel}</span></td>
                    </tr>
                `;
            })
            .join("");
    }

    function updateResultCount(total, page, size) {
        if (!resultCount) return;
        const start = total === 0 ? 0 : page * size + 1;
        const end = Math.min((page + 1) * size, total);
        resultCount.textContent = `Showing ${start} to ${end} of ${total} results`;
    }

    function buildPagination(totalPages, currentPage) {
        if (!paginationEl) return;
        paginationEl.innerHTML = "";
        if (totalPages <= 1) return;

        const createButton = (label, page, isDisabled, isActive) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "stock-page-btn";
            button.textContent = label;
            if (isActive) button.classList.add("is-active");
            if (isDisabled) button.disabled = true;
            button.dataset.page = String(page);
            return button;
        };

        paginationEl.appendChild(
            createButton("Prev", Math.max(currentPage - 1, 0), currentPage === 0, false)
        );

        const maxButtons = 5;
        const half = Math.floor(maxButtons / 2);
        let start = Math.max(currentPage - half, 0);
        let end = Math.min(start + maxButtons - 1, totalPages - 1);
        if (end - start < maxButtons - 1) {
            start = Math.max(end - (maxButtons - 1), 0);
        }

        for (let i = start; i <= end; i += 1) {
            paginationEl.appendChild(
                createButton(String(i + 1), i, false, i === currentPage)
            );
        }

        paginationEl.appendChild(
            createButton("Next", Math.min(currentPage + 1, totalPages - 1), currentPage >= totalPages - 1, false)
        );
    }

    function fetchAlerts() {
        const params = new URLSearchParams();
        if (state.query) params.set("q", state.query);
        params.set("type", state.type);
        params.set("page", String(state.page));
        params.set("size", String(state.size));
        params.set("sort", state.sort);
        params.set("dir", state.dir);
        return fetch(`/api/stock-alerts?${params.toString()}`)
            .then((response) => response.json());
    }

    function loadAlerts() {
        setLoading();
        updateSortDirButton();
        fetchAlerts()
            .then((data) => {
                const items = Array.isArray(data?.items) ? data.items : [];
                renderRows(items);
                updateResultCount(Number(data?.total || 0), Number(data?.page || 0), Number(data?.size || state.size));
                buildPagination(Number(data?.totalPages || 0), Number(data?.page || 0));
            })
            .catch(() => {
                setEmpty();
                updateResultCount(0, 0, state.size);
                buildPagination(0, 0);
            });
    }

    function loadSummary() {
        fetch("/api/stock-alerts/summary")
            .then((response) => response.json())
            .then((data) => {
                if (lowCount) lowCount.textContent = data?.lowStock ?? 0;
                if (expiringCount) expiringCount.textContent = data?.expiringSoon ?? 0;
                if (outCount) outCount.textContent = data?.outOfStock ?? 0;
                if (expiringLabel) {
                    const days = data?.expiringDays ?? 30;
                    expiringLabel.textContent = `Items expiring within ${days} days`;
                }
            })
            .catch(() => {
                // Keep defaults.
            });
    }

    if (searchInput) {
        searchInput.addEventListener("input", (event) => {
            const value = event.target.value || "";
            if (searchTimer) clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                state.query = value.trim();
                state.page = 0;
                loadAlerts();
            }, 250);
        });
    }

    if (sortSelect) {
        sortSelect.addEventListener("change", () => {
            state.sort = sortSelect.value || "sku";
            state.page = 0;
            loadAlerts();
        });
    }

    if (sortDirBtn) {
        sortDirBtn.addEventListener("click", () => {
            state.dir = state.dir === "asc" ? "desc" : "asc";
            state.page = 0;
            updateSortDirButton();
            loadAlerts();
        });
    }

    if (filterSelect) {
        filterSelect.addEventListener("change", () => {
            state.type = filterSelect.value || "all";
            state.page = 0;
            updateActiveTab();
            loadAlerts();
        });
    }

    if (pageSizeSelect) {
        pageSizeSelect.addEventListener("change", () => {
            state.size = Number(pageSizeSelect.value || 15);
            state.page = 0;
            loadAlerts();
        });
    }

    function updateActiveTab() {
        tabs.forEach((tab) => {
            tab.classList.toggle("active", tab.dataset.tab === state.type);
        });
    }

    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            state.type = tab.dataset.tab;
            if (filterSelect) {
                filterSelect.value = state.type;
            }
            updateActiveTab();
            state.page = 0;
            loadAlerts();
        });
    });

    if (paginationEl) {
        paginationEl.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            const button = target.closest(".stock-page-btn");
            if (!button || button.disabled) return;
            const page = Number(button.dataset.page);
            if (Number.isNaN(page) || page === state.page) return;
            state.page = page;
            loadAlerts();
        });
    }

    updateActiveTab();
    loadSummary();
    loadAlerts();
    return root;
});

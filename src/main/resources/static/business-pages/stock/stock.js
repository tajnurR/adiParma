window.BusinessPages.register("stock", function (root) {
    const searchInput = root.querySelector("#stock-search");
    const sortSelect = root.querySelector("#stock-sort");
    const sortDirBtn = root.querySelector("#stock-sort-dir");
    const statusSelect = root.querySelector("#stock-status");
    const pageSizeSelect = root.querySelector("#stock-page-size");
    const resultCount = root.querySelector("#stock-result-count");
    const tableBody = root.querySelector("#stock-table-body");
    const paginationEl = root.querySelector("#stock-pagination");
    const totalItemsEl = root.querySelector("#stock-total-items");
    const lowItemsEl = root.querySelector("#stock-low-items");
    const expiringItemsEl = root.querySelector("#stock-expiring-items");
    const outItemsEl = root.querySelector("#stock-out-items");
    const expiringLabel = root.querySelector("#stock-expiring-label");
    const modal = root.querySelector("#stock-modal");
    const modalTitle = root.querySelector("#stock-modal-title");
    const detailCode = root.querySelector("#stock-detail-code");
    const detailCategory = root.querySelector("#stock-detail-category");
    const detailQty = root.querySelector("#stock-detail-qty");
    const detailPrice = root.querySelector("#stock-detail-price");
    const detailCost = root.querySelector("#stock-detail-cost");
    const detailExpiry = root.querySelector("#stock-detail-expiry");

    if (!tableBody) return root;

    const state = {
        query: "",
        status: "all",
        page: 0,
        size: Number(pageSizeSelect?.value || 20),
        sort: sortSelect?.value || "code",
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

    function formatMoney(value) {
        const numeric = Number(value || 0);
        return `৳${numeric.toFixed(2)}`;
    }

    function formatDate(value) {
        if (!value) return "—";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;
        return date.toLocaleDateString("en-GB");
    }

    function setLoading() {
        tableBody.innerHTML = `
            <tr>
                <td colspan="9" class="stock-empty">Loading stock...</td>
            </tr>
        `;
    }

    function setEmpty() {
        tableBody.innerHTML = `
            <tr>
                <td colspan="9" class="stock-empty">No stock found.</td>
            </tr>
        `;
    }

    function renderRows(items) {
        if (!items || items.length === 0) {
            setEmpty();
            return;
        }
        tableBody.innerHTML = items
            .map((row) => {
                const status = row.status;
                const statusLabel = status === "out_of_stock"
                    ? "Out of Stock"
                    : status === "expiring_soon"
                        ? "Expiring Soon"
                        : status === "low_stock"
                            ? "Low Stock"
                            : "In Stock";
                const statusClass = status === "out_of_stock"
                    ? "out"
                    : status === "expiring_soon"
                        ? "expiring"
                        : status === "low_stock"
                            ? "low"
                            : "normal";
                return `
                    <tr data-id="${row.id}">
                        <td>${row.code || "—"}</td>
                        <td>${row.name || "—"}</td>
                        <td>${row.category || "—"}</td>
                        <td class="stock-qty">${row.qty ?? 0}</td>
                        <td>${formatMoney(row.price)}</td>
                        <td>${formatMoney(row.costPrice)}</td>
                        <td>${formatDate(row.expireDate)}</td>
                        <td><span class="stock-status ${statusClass}">${statusLabel}</span></td>
                        <td>
                            <button class="stock-action-btn" type="button">
                                <span class="material-symbols-outlined">visibility</span>
                                View
                            </button>
                        </td>
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

    function fetchStock() {
        const params = new URLSearchParams();
        if (state.query) params.set("q", state.query);
        params.set("status", state.status);
        params.set("page", String(state.page));
        params.set("size", String(state.size));
        params.set("sort", state.sort);
        params.set("dir", state.dir);
        return fetch(`/api/stock?${params.toString()}`)
            .then((response) => response.json());
    }

    function loadStock() {
        setLoading();
        updateSortDirButton();
        fetchStock()
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
        fetch("/api/stock/summary")
            .then((response) => response.json())
            .then((data) => {
                if (totalItemsEl) totalItemsEl.textContent = data?.total ?? 0;
                if (lowItemsEl) lowItemsEl.textContent = data?.lowStock ?? 0;
                if (expiringItemsEl) expiringItemsEl.textContent = data?.expiringSoon ?? 0;
                if (outItemsEl) outItemsEl.textContent = data?.outOfStock ?? 0;
                if (expiringLabel) {
                    const days = data?.expiringDays ?? 30;
                    expiringLabel.textContent = `Within ${days} days`;
                }
            })
            .catch(() => {
                // Ignore summary errors.
            });
    }

    function openModal() {
        if (!modal) return;
        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
    }

    function closeModal() {
        if (!modal) return;
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
    }

    function loadStockDetails(id) {
        fetch(`/api/products/${id}`)
            .then((response) => response.json().then((data) => ({ ok: response.ok, data })))
            .then(({ ok, data }) => {
                if (!ok) {
                    throw new Error(data?.message || "Unable to load stock.");
                }
                if (modalTitle) modalTitle.textContent = data.name || "—";
                if (detailCode) detailCode.textContent = data.code || "—";
                if (detailCategory) detailCategory.textContent = data.category || "—";
                if (detailQty) detailQty.textContent = data.qty ?? 0;
                if (detailPrice) detailPrice.textContent = formatMoney(data.sellingPrice);
                if (detailCost) detailCost.textContent = formatMoney(data.costPrice);
                if (detailExpiry) detailExpiry.textContent = formatDate(data.expireDate);
                openModal();
            })
            .catch(() => {
                if (window.ToastService && typeof window.ToastService.show === "function") {
                    window.ToastService.show("Unable to load stock details.", "error");
                }
            });
    }

    if (tableBody) {
        tableBody.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            const row = target.closest("tr");
            if (!row) return;
            const id = row.getAttribute("data-id");
            if (!id) return;
            loadStockDetails(id);
        });
    }

    if (modal) {
        modal.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            if (target.closest("[data-action=\"close-stock-modal\"]")) {
                closeModal();
            }
        });
    }

    if (searchInput) {
        searchInput.addEventListener("input", (event) => {
            const value = event.target.value || "";
            if (searchTimer) clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                state.query = value.trim();
                state.page = 0;
                loadStock();
            }, 250);
        });
    }

    if (sortSelect) {
        sortSelect.addEventListener("change", () => {
            state.sort = sortSelect.value || "code";
            state.page = 0;
            loadStock();
        });
    }

    if (sortDirBtn) {
        sortDirBtn.addEventListener("click", () => {
            state.dir = state.dir === "asc" ? "desc" : "asc";
            state.page = 0;
            updateSortDirButton();
            loadStock();
        });
    }

    if (statusSelect) {
        statusSelect.addEventListener("change", () => {
            state.status = statusSelect.value || "all";
            state.page = 0;
            loadStock();
        });
    }

    if (pageSizeSelect) {
        pageSizeSelect.addEventListener("change", () => {
            state.size = Number(pageSizeSelect.value || 20);
            state.page = 0;
            loadStock();
        });
    }

    if (paginationEl) {
        paginationEl.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            const button = target.closest(".stock-page-btn");
            if (!button || button.disabled) return;
            const page = Number(button.dataset.page);
            if (Number.isNaN(page) || page === state.page) return;
            state.page = page;
            loadStock();
        });
    }

    loadSummary();
    loadStock();
    return root;
});

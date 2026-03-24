window.BusinessPages.register("transactions", function (root) {
    const searchInput = root.querySelector("#transactions-search");
    const startInput = root.querySelector("#transactions-start");
    const endInput = root.querySelector("#transactions-end");
    const paymentSelect = root.querySelector("#transactions-payment");
    const sortSelect = root.querySelector("#transactions-sort");
    const pageSizeSelect = root.querySelector("#transactions-page-size");
    const resultCount = root.querySelector("#transactions-result-count");
    const tableBody = root.querySelector("#transactions-body");
    const paginationEl = root.querySelector("#transactions-pagination");
    const modal = root.querySelector("#transactions-modal");
    const modalMeta = root.querySelector("#transaction-meta");
    const modalCustomer = root.querySelector("#transaction-customer");
    const modalPayment = root.querySelector("#transaction-payment");
    const modalTotal = root.querySelector("#transaction-total");
    const modalItems = root.querySelector("#transaction-items");
    const invoiceBtn = root.querySelector("#transaction-invoice-btn");

    if (!tableBody) return root;

    const state = {
        query: "",
        payment: "",
        start: "",
        end: "",
        sort: "date",
        dir: "desc",
        page: 0,
        size: Number(pageSizeSelect?.value || 15)
    };

    let searchTimer = null;
    let activeInvoiceId = null;

    function formatMoney(value) {
        const numeric = Number(value || 0);
        return `৳${numeric.toFixed(2)}`;
    }

    function formatDate(value) {
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

    function setLoading() {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="transactions-empty">Loading transactions...</td>
            </tr>
        `;
    }

    function setEmpty() {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="transactions-empty">No transactions found.</td>
            </tr>
        `;
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
            button.className = "transactions-page-btn";
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

    function fetchTransactions() {
        const params = new URLSearchParams();
        if (state.query) params.set("q", state.query);
        if (state.payment) params.set("payment", state.payment);
        if (state.start) params.set("start", state.start);
        if (state.end) params.set("end", state.end);
        params.set("page", String(state.page));
        params.set("size", String(state.size));
        params.set("sort", state.sort);
        params.set("dir", state.dir);
        return fetch(`/api/transactions?${params.toString()}`)
            .then((response) => response.json());
    }

    function renderRows(items) {
        if (!items || items.length === 0) {
            setEmpty();
            return;
        }
        tableBody.innerHTML = items
            .map((item) => {
                return `
                    <tr data-id="${item.id}">
                        <td>${item.invoiceNo || "—"}</td>
                        <td>${formatDate(item.saleDate)}</td>
                        <td>${item.customer || "—"}</td>
                        <td>${item.payment || "—"}</td>
                        <td>${formatMoney(item.totalAmount)}</td>
                        <td>
                            <button class="transactions-action-btn" type="button">
                                <span class="material-symbols-outlined">visibility</span>
                                View
                            </button>
                        </td>
                    </tr>
                `;
            })
            .join("");
    }

    function loadTransactions() {
        setLoading();
        fetchTransactions()
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

    function openModal() {
        if (!modal) return;
        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
    }

    function closeModal() {
        if (!modal) return;
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        if (modalItems) modalItems.innerHTML = "";
        activeInvoiceId = null;
    }

    function loadTransactionDetails(id) {
        fetch(`/api/transactions/${id}`)
            .then((response) => response.json().then((data) => ({ ok: response.ok, data })))
            .then(({ ok, data }) => {
                if (!ok) {
                    throw new Error(data?.message || "Unable to load transaction.");
                }
                if (modalMeta) {
                    modalMeta.textContent = `${data.invoiceNo || ""} • ${formatDate(data.saleDate)}`;
                }
                if (modalCustomer) modalCustomer.textContent = data.customer || "—";
                if (modalPayment) modalPayment.textContent = data.payment || "—";
                if (modalTotal) modalTotal.textContent = formatMoney(data.totalAmount);
                if (modalItems) {
                    const items = Array.isArray(data.items) ? data.items : [];
                    modalItems.innerHTML = items.map((row) => {
                        return `
                            <tr>
                                <td>${row.name || "—"}</td>
                                <td>${row.code || "—"}</td>
                                <td>${row.qty ?? 0}</td>
                                <td>${formatMoney(row.totalPrice)}</td>
                            </tr>
                        `;
                    }).join("");
                }
                activeInvoiceId = data.id;
                openModal();
            })
            .catch(() => {
                if (window.ToastService && typeof window.ToastService.show === "function") {
                    window.ToastService.show("Unable to load transaction.", "error");
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
            loadTransactionDetails(id);
        });
    }

    if (modal) {
        modal.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            if (target.closest("[data-action=\"close-transaction-modal\"]")) {
                closeModal();
            }
        });
    }

    if (invoiceBtn) {
        invoiceBtn.addEventListener("click", () => {
            if (!activeInvoiceId) return;
            window.open(`/api/invoices/${activeInvoiceId}/pdf`, "_blank");
        });
    }

    if (searchInput) {
        searchInput.addEventListener("input", (event) => {
            const value = event.target.value || "";
            if (searchTimer) clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                state.query = value.trim();
                state.page = 0;
                loadTransactions();
            }, 250);
        });
    }

    if (startInput) {
        startInput.addEventListener("change", () => {
            state.start = startInput.value;
            state.page = 0;
            loadTransactions();
        });
    }

    if (endInput) {
        endInput.addEventListener("change", () => {
            state.end = endInput.value;
            state.page = 0;
            loadTransactions();
        });
    }

    if (paymentSelect) {
        paymentSelect.addEventListener("change", () => {
            state.payment = paymentSelect.value;
            state.page = 0;
            loadTransactions();
        });
    }

    if (sortSelect) {
        sortSelect.addEventListener("change", () => {
            state.sort = sortSelect.value || "date";
            state.page = 0;
            loadTransactions();
        });
    }

    if (pageSizeSelect) {
        pageSizeSelect.addEventListener("change", () => {
            state.size = Number(pageSizeSelect.value || 15);
            state.page = 0;
            loadTransactions();
        });
    }

    if (paginationEl) {
        paginationEl.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            const button = target.closest(".transactions-page-btn");
            if (!button || button.disabled) return;
            const page = Number(button.dataset.page);
            if (Number.isNaN(page) || page === state.page) return;
            state.page = page;
            loadTransactions();
        });
    }

    loadTransactions();
    return root;
});

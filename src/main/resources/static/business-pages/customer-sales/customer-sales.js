window.BusinessPages.register("customerSales", function (root) {
    const subtitle = root.querySelector("#customer-sales-subtitle");
    const backButton = root.querySelector("#customer-sales-back");
    const searchInput = root.querySelector("#customer-sales-search");
    const countEl = root.querySelector("#customer-sales-count");
    const tableBody = root.querySelector("#customer-sales-table tbody");
    const emptyState = root.querySelector("#customer-sales-empty");
    const detailsMeta = root.querySelector("#customer-sales-details-meta");
    const itemsBody = root.querySelector("#customer-sales-items-table tbody");
    const itemsEmpty = root.querySelector("#customer-sales-items-empty");

    let sales = [];
    let filteredSales = [];
    let activeSaleId = null;

    function parseCustomerId() {
        const match = window.location.pathname.match(/\/customers\/(\d+)\/sales/);
        return match ? Number(match[1]) : null;
    }

    const customerId = parseCustomerId();
    if (!customerId) return root;

    function formatMoney(value) {
        const num = Number(value || 0);
        return `৳${num.toFixed(2)}`;
    }

    function formatDate(value) {
        if (!value) return "—";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;
        return date.toLocaleDateString("en-US");
    }

    function paymentLabel(code) {
        if (code === 1) return "Cash";
        if (code === 2) return "Card";
        if (code === 3) return "Mobile";
        if (code === 4) return "Other";
        return "—";
    }

    function updateCount() {
        if (countEl) {
            countEl.textContent = `${filteredSales.length} sales`;
        }
    }

    function renderSalesTable() {
        if (!tableBody) return;
        if (!filteredSales.length) {
            tableBody.innerHTML = "";
            if (emptyState) emptyState.classList.add("is-visible");
            updateCount();
            return;
        }
        if (emptyState) emptyState.classList.remove("is-visible");
        tableBody.innerHTML = filteredSales.map((sale) => {
            const isActive = sale.id === activeSaleId ? "is-active" : "";
            return `
                <tr class="customer-sales-row ${isActive}" data-id="${sale.id}">
                    <td>${sale.invoiceNo || "—"}</td>
                    <td>${formatDate(sale.saleDate)}</td>
                    <td>${paymentLabel(sale.paymentType)}</td>
                    <td>${formatMoney(sale.totalAmount)}</td>
                    <td>${formatMoney(sale.cashReceived)}</td>
                    <td>${formatMoney(sale.changeAmount)}</td>
                    <td>${sale.createdBy || "—"}</td>
                    <td class="customer-sales-download-cell">
                        <button class="customer-sales-download-btn" type="button" data-action="download" aria-label="Download invoice PDF">
                            <span class="material-symbols-outlined">download</span>
                        </button>
                    </td>
                </tr>
            `;
        }).join("");
        updateCount();
    }

    function renderItems(items) {
        if (!itemsBody || !itemsEmpty) return;
        if (!items || !items.length) {
            itemsBody.innerHTML = "";
            itemsEmpty.classList.add("is-visible");
            return;
        }
        itemsEmpty.classList.remove("is-visible");
        itemsBody.innerHTML = items.map((item) => {
            const name = item.medicineName || "Item";
            const code = item.medicineCode ? `(${item.medicineCode})` : "";
            const discountValue = Number(item.discount || 0);
            let discountLabel = "—";
            if (discountValue > 0) {
                if (String(item.discountType || "").toUpperCase() === "PERCENT") {
                    discountLabel = `${discountValue.toFixed(2).replace(/\.00$/, "")}%`;
                } else {
                    discountLabel = formatMoney(discountValue);
                }
            }
            return `
                <tr>
                    <td>${name} ${code}</td>
                    <td>${item.qty ?? 0}</td>
                    <td>${formatMoney(item.unitPrice)}</td>
                    <td>${discountLabel}</td>
                    <td>${formatMoney(item.totalPrice)}</td>
                </tr>
            `;
        }).join("");
    }

    function loadDetails(saleId) {
        activeSaleId = saleId;
        renderSalesTable();
        if (detailsMeta) {
            detailsMeta.textContent = `Loading items for sale #${saleId}...`;
        }
        fetch(`/api/customers/${customerId}/sales/${saleId}/details`)
            .then((response) => response.json())
            .then((data) => {
                renderItems(data.items || []);
                if (detailsMeta) {
                    detailsMeta.textContent = `Showing ${data.items?.length || 0} items for sale #${saleId}`;
                }
            })
            .catch(() => {
                renderItems([]);
                if (detailsMeta) {
                    detailsMeta.textContent = "Unable to load sale items.";
                }
            });
    }

    function applyFilter(value) {
        const query = String(value || "").trim().toLowerCase();
        if (!query) {
            filteredSales = [...sales];
        } else {
            filteredSales = sales.filter((sale) => {
                const invoice = String(sale.invoiceNo || "").toLowerCase();
                const payment = paymentLabel(sale.paymentType).toLowerCase();
                return invoice.includes(query) || payment.includes(query);
            });
        }
        renderSalesTable();
    }

    function loadSales() {
        fetch(`/api/customers/${customerId}/sales`)
            .then((response) => response.json())
            .then((data) => {
                sales = Array.isArray(data.sales) ? data.sales : [];
                filteredSales = [...sales];
                if (subtitle) {
                    const name = data.customerName ? `Customer: ${data.customerName}` : `Customer #${customerId}`;
                    subtitle.textContent = name;
                }
                renderSalesTable();
                if (sales.length) {
                    loadDetails(sales[0].id);
                } else {
                    if (detailsMeta) {
                        detailsMeta.textContent = "No sales available for this customer.";
                    }
                    renderItems([]);
                }
            })
            .catch(() => {
                sales = [];
                filteredSales = [];
                renderSalesTable();
                if (subtitle) {
                    subtitle.textContent = `Customer #${customerId}`;
                }
                if (detailsMeta) {
                    detailsMeta.textContent = "Unable to load sales.";
                }
                renderItems([]);
            });
    }

    if (backButton) {
        backButton.addEventListener("click", () => {
            window.location.href = "/customers";
        });
    }

    if (searchInput) {
        searchInput.addEventListener("input", (event) => {
            applyFilter(event.target.value);
        });
    }

    if (tableBody) {
        tableBody.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            const downloadBtn = target.closest("[data-action=\"download\"]");
            if (downloadBtn) {
                event.stopPropagation();
                const row = downloadBtn.closest(".customer-sales-row");
                if (!row) return;
                const saleId = Number(row.getAttribute("data-id"));
                if (!saleId) return;
                window.open(`/api/invoices/${saleId}/pdf`, "_blank");
                return;
            }
            const row = target.closest(".customer-sales-row");
            if (!row) return;
            const saleId = Number(row.getAttribute("data-id"));
            if (!saleId) return;
            loadDetails(saleId);
        });
    }

    loadSales();
    return root;
});

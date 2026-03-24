window.BusinessPages.register("reports", function (root) {
    const startInput = root.querySelector("#reports-start-date");
    const endInput = root.querySelector("#reports-end-date");
    const exportBtn = root.querySelector(".reports-export-btn");
    const totalSalesEl = root.querySelector("#reports-total-sales");
    const totalRevenueEl = root.querySelector("#reports-total-revenue");
    const avgOrderEl = root.querySelector("#reports-avg-order");
    const totalProductsEl = root.querySelector("#reports-total-products");
    const stockValueEl = root.querySelector("#reports-stock-value");
    const lowStockEl = root.querySelector("#reports-low-stock");
    const expiringEl = root.querySelector("#reports-expiring-soon");
    const topTableBody = root.querySelector("#reports-top-products");

    if (!startInput || !endInput) return root;

    function formatMoney(value) {
        const numeric = Number(value || 0);
        return `৳${numeric.toFixed(2)}`;
    }

    function formatDateInput(date) {
        return date.toISOString().slice(0, 10);
    }

    function setDefaultDates() {
        const today = new Date();
        const start = new Date();
        start.setDate(today.getDate() - 30);
        startInput.value = formatDateInput(start);
        endInput.value = formatDateInput(today);
    }

    function renderTopProducts(items) {
        if (!topTableBody) return;
        if (!items || items.length === 0) {
            topTableBody.innerHTML = `
                <tr>
                    <td colspan="3" class="reports-empty">No data available</td>
                </tr>
            `;
            return;
        }
        topTableBody.innerHTML = items
            .map((item) => {
                return `
                    <tr>
                        <td>${item.product || "—"}</td>
                        <td>${item.qty ?? 0}</td>
                        <td>${formatMoney(item.revenue)}</td>
                    </tr>
                `;
            })
            .join("");
    }

    function loadReport() {
        const start = startInput.value;
        const end = endInput.value;
        const params = new URLSearchParams({ start, end });
        fetch(`/api/reports/summary?${params.toString()}`)
            .then((response) => response.json())
            .then((data) => {
                if (totalSalesEl) totalSalesEl.textContent = data.totalSales ?? 0;
                if (totalRevenueEl) totalRevenueEl.textContent = formatMoney(data.totalRevenue);
                if (avgOrderEl) avgOrderEl.textContent = formatMoney(data.avgOrderValue);
                if (totalProductsEl) totalProductsEl.textContent = data.totalProducts ?? 0;
                if (stockValueEl) stockValueEl.textContent = formatMoney(data.stockValue);
                if (lowStockEl) lowStockEl.textContent = data.lowStock ?? 0;
                if (expiringEl) expiringEl.textContent = data.expiringSoon ?? 0;
                renderTopProducts(Array.isArray(data.topProducts) ? data.topProducts : []);
            })
            .catch(() => {
                renderTopProducts([]);
            });
    }

    startInput.addEventListener("change", loadReport);
    endInput.addEventListener("change", loadReport);

    if (exportBtn) {
        exportBtn.addEventListener("click", () => {
            const params = new URLSearchParams({
                start: startInput.value,
                end: endInput.value
            });
            window.location.href = `/api/reports/export?${params.toString()}`;
        });
    }

    setDefaultDates();
    loadReport();
    return root;
});

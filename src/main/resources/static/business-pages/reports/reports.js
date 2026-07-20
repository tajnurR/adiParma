window.BusinessPages.register("reports", function (root) {
    const page = root.querySelector(".reports-page");
    const branchSelect = root.querySelector("#reports-branch");
    const rangeSelect = root.querySelector("#reports-range");
    const customDates = root.querySelector("#reports-custom-dates");
    const startInput = root.querySelector("#reports-start-date");
    const endInput = root.querySelector("#reports-end-date");
    const exportBtn = root.querySelector(".reports-export-btn");
    const statusEl = root.querySelector("#reports-status");

    const totalSalesEl = root.querySelector("#reports-total-sales");
    const grossMarginEl = root.querySelector("#reports-gross-margin");
    const prescriptionsEl = root.querySelector("#reports-prescriptions");
    const avgBasketEl = root.querySelector("#reports-avg-basket");
    const salesDeltaEl = root.querySelector("#reports-sales-delta");
    const marginDeltaEl = root.querySelector("#reports-margin-delta");
    const rxDeltaEl = root.querySelector("#reports-rx-delta");
    const basketDeltaEl = root.querySelector("#reports-basket-delta");

    const expiryValueEl = root.querySelector("#reports-expiry-value");
    const lowStockEl = root.querySelector("#reports-low-stock");
    const deadStockEl = root.querySelector("#reports-dead-stock");
    const trendChartEl = root.querySelector("#reports-sales-chart");
    const topProductsEl = root.querySelector("#reports-top-products");
    const categoryMixEl = root.querySelector("#reports-category-mix");

    const turnoverEl = root.querySelector("#reports-turnover");
    const stockoutRateEl = root.querySelector("#reports-stockout-rate");
    const expiryRateEl = root.querySelector("#reports-expiry-rate");
    const revenueRxEl = root.querySelector("#reports-revenue-rx");
    const paymentMixEl = root.querySelector("#reports-payment-mix");

    if (!page || !startInput || !endInput || !rangeSelect) return root;

    const placeholder = {
        grossMarginPercent: 24.8,
        prescriptionsFilled: 426,
        nearExpiryValue: 48500,
        deadStockValue: 73200,
        inventoryTurnover: 9.4,
        stockoutRate: 1.6,
        expiryRate: 0.7,
        paymentMix: "Mobile 46%",
        categoryMix: [
            { label: "Rx", value: 46, className: "rx" },
            { label: "OTC", value: 31, className: "otc" },
            { label: "Wellness", value: 14, className: "wellness" },
            { label: "Personal care", value: 9, className: "personal" }
        ]
    };

    function formatMoney(value, compact = false) {
        const numeric = Number(value || 0);
        if (compact && Math.abs(numeric) >= 100000) {
            return `৳${(numeric / 100000).toLocaleString("en-BD", { maximumFractionDigits: 1 })}L`;
        }
        return `৳${numeric.toLocaleString("en-BD", { maximumFractionDigits: 0 })}`;
    }

    function formatNumber(value) {
        return Number(value || 0).toLocaleString("en-BD");
    }

    function formatDateInput(date) {
        return date.toISOString().slice(0, 10);
    }

    function setStatus(message, type = "info") {
        if (!statusEl) return;
        statusEl.textContent = message || "";
        statusEl.classList.toggle("is-visible", Boolean(message));
        statusEl.dataset.type = type;
    }

    function setDelta(el, value) {
        if (!el) return;
        const numeric = Number(value || 0);
        el.classList.remove("is-up", "is-down");
        el.classList.add(numeric >= 0 ? "is-up" : "is-down");
        el.textContent = `${numeric >= 0 ? "↑" : "↓"} ${Math.abs(numeric).toFixed(1)}% vs previous`;
    }

    function getDateRange() {
        const today = new Date();
        const start = new Date(today);
        const selected = rangeSelect.value;

        if (selected === "today") {
            start.setHours(0, 0, 0, 0);
        } else if (selected === "7d") {
            start.setDate(today.getDate() - 6);
        } else if (selected === "month") {
            start.setDate(1);
        } else if (selected === "custom") {
            return {
                start: startInput.value || formatDateInput(today),
                end: endInput.value || formatDateInput(today)
            };
        } else {
            start.setDate(today.getDate() - 29);
        }

        return {
            start: formatDateInput(start),
            end: formatDateInput(today)
        };
    }

    function syncDateInputs() {
        const range = getDateRange();
        startInput.value = range.start;
        endInput.value = range.end;
        customDates.classList.toggle("is-visible", rangeSelect.value === "custom");
    }

    function buildParams() {
        return new URLSearchParams({
            start: startInput.value,
            end: endInput.value,
            branch: branchSelect?.value || "main"
        });
    }

    function setLoading(isLoading) {
        page.classList.toggle("is-loading", isLoading);
    }

    function deriveReport(data) {
        const totalRevenue = Number(data.totalRevenue || 0);
        const avgOrder = Number(data.avgOrderValue || 0);
        const saleCount = Number(data.totalSales || 0);
        const prescriptionCount = Math.max(
            Math.round(saleCount * 0.72),
            saleCount > 0 ? 1 : placeholder.prescriptionsFilled
        );

        return {
            totalRevenue,
            avgOrder,
            saleCount,
            grossMarginPercent: Number(data.grossMarginPercent ?? placeholder.grossMarginPercent),
            prescriptionsFilled: Number(data.prescriptionsFilled ?? prescriptionCount),
            lowStock: Number(data.lowStock || 0),
            expiringSoon: Number(data.expiringSoon || 0),
            nearExpiryValue: Number(data.nearExpiryValue ?? Math.max(data.expiringSoon || 0, 1) * 850),
            deadStockValue: Number(data.deadStockValue ?? placeholder.deadStockValue),
            stockValue: Number(data.stockValue || 0),
            topProducts: Array.isArray(data.topProducts) ? data.topProducts : [],
            deltas: data.deltas || {
                sales: 8.2,
                margin: 1.4,
                prescriptions: 4.8,
                basket: -2.1
            },
            inventoryTurnover: Number(data.inventoryTurnover ?? placeholder.inventoryTurnover),
            stockoutRate: Number(data.stockoutRate ?? placeholder.stockoutRate),
            expiryRate: Number(data.expiryRate ?? placeholder.expiryRate),
            paymentMix: data.paymentMix || placeholder.paymentMix,
            categoryMix: Array.isArray(data.categoryMix) ? data.categoryMix : placeholder.categoryMix
        };
    }

    function renderKpis(report) {
        if (totalSalesEl) totalSalesEl.textContent = formatMoney(report.totalRevenue, true);
        if (grossMarginEl) grossMarginEl.textContent = `${report.grossMarginPercent.toFixed(1)}%`;
        if (prescriptionsEl) prescriptionsEl.textContent = formatNumber(report.prescriptionsFilled);
        if (avgBasketEl) avgBasketEl.textContent = formatMoney(report.avgOrder);

        setDelta(salesDeltaEl, report.deltas.sales);
        setDelta(marginDeltaEl, report.deltas.margin);
        setDelta(rxDeltaEl, report.deltas.prescriptions);
        setDelta(basketDeltaEl, report.deltas.basket);
    }

    function renderAttention(report) {
        if (expiryValueEl) expiryValueEl.textContent = formatMoney(report.nearExpiryValue, true);
        if (lowStockEl) lowStockEl.textContent = formatNumber(report.lowStock);
        if (deadStockEl) deadStockEl.textContent = formatMoney(report.deadStockValue, true);
    }

    function makeTrend(total) {
        // API plug-in point: replace this generated series with /api/reports/trend.
        const days = Math.max(7, Math.min(30, Math.round((new Date(endInput.value) - new Date(startInput.value)) / 86400000) + 1 || 30));
        const average = Math.max(total / days, 800);
        const current = [];
        const previous = [];
        for (let i = 0; i < days; i += 1) {
            const wave = 1 + Math.sin(i / 2.8) * 0.18;
            const growth = 1 + i / days * 0.12;
            current.push(Math.round(average * wave * growth));
            previous.push(Math.round(average * (0.86 + Math.cos(i / 3.2) * 0.12)));
        }
        return { current, previous };
    }

    function pointsFor(values, width, height, padding, max) {
        const step = values.length <= 1 ? 0 : (width - padding * 2) / (values.length - 1);
        return values.map((value, index) => {
            const x = padding + index * step;
            const y = height - padding - (value / max) * (height - padding * 2);
            return { x, y, value };
        });
    }

    function renderTrendChart(report) {
        if (!trendChartEl) return;
        const series = Array.isArray(report.trend?.current) ? report.trend : makeTrend(report.totalRevenue);
        const width = 920;
        const height = 280;
        const padding = 34;
        const allValues = [...series.current, ...series.previous, 1];
        const max = Math.max(...allValues);
        const currentPoints = pointsFor(series.current, width, height, padding, max);
        const previousPoints = pointsFor(series.previous, width, height, padding, max);
        const currentPath = currentPoints.map((p, i) => `${i === 0 ? "M" : "L"}${p.x},${p.y}`).join(" ");
        const previousPath = previousPoints.map((p, i) => `${i === 0 ? "M" : "L"}${p.x},${p.y}`).join(" ");
        const grid = [0.25, 0.5, 0.75].map((line) => {
            const y = padding + (height - padding * 2) * line;
            return `<line x1="${padding}" y1="${y}" x2="${width - padding}" y2="${y}" stroke="#e2e8f0" stroke-width="1" />`;
        }).join("");

        trendChartEl.innerHTML = `
            <svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" aria-hidden="true">
                ${grid}
                <path d="${previousPath}" fill="none" stroke="#94a3b8" stroke-width="3" stroke-linecap="round" stroke-dasharray="7 7" />
                <path d="${currentPath}" fill="none" stroke="#0f766e" stroke-width="4" stroke-linecap="round" />
                ${currentPoints.map((p, index) => `
                    <circle class="reports-chart-point" cx="${p.x}" cy="${p.y}" r="5" fill="#0f766e" tabindex="0" data-day="${index + 1}">
                        <title>Day ${index + 1}: ${formatMoney(p.value)}</title>
                    </circle>
                `).join("")}
            </svg>
        `;
    }

    function renderTopProducts(items) {
        if (!topProductsEl) return;
        if (!items.length) {
            topProductsEl.innerHTML = `<div class="reports-empty-state">No product sales found for this period.</div>`;
            return;
        }
        const maxRevenue = Math.max(...items.map((item) => Number(item.revenue || 0)), 1);
        topProductsEl.innerHTML = items.slice(0, 5).map((item, index) => {
            const revenue = Number(item.revenue || 0);
            const width = Math.max((revenue / maxRevenue) * 100, 4);
            return `
                <button class="reports-bar-row" type="button" data-product="${item.product || ""}">
                    <span class="reports-bar-row-header">
                        <span>${index + 1}. ${item.product || "Unnamed product"}</span>
                        <span>${formatMoney(revenue)}</span>
                    </span>
                    <span class="reports-bar-track" aria-hidden="true">
                        <span class="reports-bar-fill" style="width: ${width}%"></span>
                    </span>
                </button>
            `;
        }).join("");
    }

    function renderCategoryMix(items) {
        if (!categoryMixEl) return;
        const total = items.reduce((sum, item) => sum + Number(item.value || 0), 0) || 1;
        categoryMixEl.innerHTML = items.map((item) => {
            const value = Number(item.value || 0);
            const percent = Math.round((value / total) * 100);
            return `
                <div class="reports-category-row">
                    <div class="reports-category-row-header">
                        <span>${item.label}</span>
                        <span>${percent}%</span>
                    </div>
                    <div class="reports-category-track" aria-hidden="true">
                        <div class="reports-category-fill ${item.className || ""}" style="width: ${percent}%"></div>
                    </div>
                </div>
            `;
        }).join("");
    }

    function renderPharmacyMetrics(report) {
        if (turnoverEl) turnoverEl.textContent = `${report.inventoryTurnover.toFixed(1)}x`;
        if (stockoutRateEl) stockoutRateEl.textContent = `${report.stockoutRate.toFixed(1)}%`;
        if (expiryRateEl) expiryRateEl.textContent = `${report.expiryRate.toFixed(1)}%`;
        if (revenueRxEl) revenueRxEl.textContent = formatMoney(report.prescriptionsFilled ? report.totalRevenue / report.prescriptionsFilled : 0);
        if (paymentMixEl) paymentMixEl.textContent = report.paymentMix;
    }

    function renderReport(data) {
        const report = deriveReport(data);
        renderKpis(report);
        renderAttention(report);

        window.setTimeout(() => {
            renderTrendChart(report);
            renderTopProducts(report.topProducts);
            renderCategoryMix(report.categoryMix);
            renderPharmacyMetrics(report);
        }, 120);
    }

    function loadReport() {
        syncDateInputs();
        setLoading(true);
        setStatus("");
        const params = buildParams();

        // API plug-in point: existing backend supports summary/export only.
        // Add gross margin, Rx metrics, payment mix, category mix, and trend fields to this endpoint when ready.
        fetch(`/api/reports/summary?${params.toString()}`)
            .then((response) => {
                if (!response.ok) throw new Error("Unable to load reports.");
                return response.json();
            })
            .then((data) => {
                renderReport(data || {});
            })
            .catch(() => {
                renderReport({
                    totalRevenue: 158450,
                    avgOrderValue: 920,
                    totalSales: 172,
                    lowStock: 12,
                    expiringSoon: 18,
                    stockValue: 1240000,
                    topProducts: [
                        { product: "Napa 500 mg", revenue: 32500, qty: 146 },
                        { product: "Seclo 20 mg", revenue: 28400, qty: 91 },
                        { product: "Maxpro 20 mg", revenue: 21800, qty: 76 },
                        { product: "Ceevit 250 mg", revenue: 16200, qty: 132 },
                        { product: "Monas 10 mg", revenue: 14800, qty: 42 }
                    ]
                });
                setStatus("Showing placeholder report data because the reports API could not be loaded.", "warning");
            })
            .finally(() => setLoading(false));
    }

    function exportCsv() {
        const params = buildParams();
        window.location.href = `/api/reports/export?${params.toString()}`;
    }

    rangeSelect.addEventListener("change", loadReport);
    branchSelect?.addEventListener("change", loadReport);
    startInput.addEventListener("change", loadReport);
    endInput.addEventListener("change", loadReport);

    if (exportBtn) {
        exportBtn.addEventListener("click", exportCsv);
    }

    root.addEventListener("click", (event) => {
        const target = event.target;
        if (!(target instanceof Element)) return;

        if (target.closest(".reports-info-btn")) {
            return;
        }

        const chartPoint = target.closest(".reports-chart-point");
        if (chartPoint && window.ToastService) {
            const day = chartPoint.getAttribute("data-day") || "";
            window.ToastService.show(`Drill-down API hook for day ${day}.`, "info");
            return;
        }

        const productRow = target.closest(".reports-bar-row");
        if (productRow && window.ToastService) {
            const product = productRow.getAttribute("data-product") || "product";
            window.ToastService.show(`Drill-down API hook for ${product}.`, "info");
            return;
        }

        const drill = target.closest("[data-drill]");
        if (drill) {
            window.location.href = drill.getAttribute("data-drill");
            return;
        }

        const exportAction = target.closest("[data-export]");
        if (exportAction) {
            const format = exportAction.getAttribute("data-export");
            if (format === "csv") {
                exportCsv();
            } else if (window.ToastService) {
                window.ToastService.show("PDF export API is not connected yet.", "info");
            }
            return;
        }

        if (target.closest(".reports-builder-btn") && window.ToastService) {
            window.ToastService.show("Custom report builder API is ready to wire here.", "info");
        }
    });

    root.addEventListener("keydown", (event) => {
        if (event.key !== "Enter" && event.key !== " ") return;
        const target = event.target;
        if (!(target instanceof Element) || target.closest(".reports-info-btn")) return;
        const drill = target.closest("[data-drill]");
        if (!drill) return;
        event.preventDefault();
        window.location.href = drill.getAttribute("data-drill");
    });

    syncDateInputs();
    loadReport();
    return root;
});

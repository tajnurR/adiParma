window.BusinessPages.register("productCatalog", function (root) {
    const searchInput = root.querySelector("#product-search");
    const categorySelect = root.querySelector("#product-category");
    const sortSelect = root.querySelector("#product-sort");
    const sortDirBtn = root.querySelector("#product-sort-direction");
    const pageSizeSelect = root.querySelector("#product-page-size");
    const tableBody = root.querySelector("#product-table-body");
    const resultCount = root.querySelector("#product-result-count");
    const paginationEl = root.querySelector("#product-pagination");
    const bulkBtn = root.querySelector(".product-bulk-btn");
    const addBtn = root.querySelector(".product-add-btn");
    const modal = root.querySelector("#product-modal");
    const modalForm = root.querySelector("#product-modal-form");
    const modalTitle = root.querySelector("#product-modal-title");
    const modalSubmit = modalForm ? modalForm.querySelector(".product-modal-submit") : null;
    const pricingSection = root.querySelector("#product-pricing-section");
    const barcodeModal = root.querySelector("#product-barcode-modal");
    const barcodePreviewSvg = root.querySelector("#barcode-preview-svg");
    const barcodePreviewCode = root.querySelector("#barcode-preview-code");
    const barcodeSizeSelect = root.querySelector("#barcode-size");
    const barcodeLayoutSelect = root.querySelector("#barcode-layout");
    const barcodeQuantitySelect = root.querySelector("#barcode-quantity");
    const barcodeQuantityField = root.querySelector("#barcode-quantity-field");
    const barcodeShowCode = root.querySelector("#barcode-show-code");
    const barcodePrintBtn = root.querySelector("#barcode-print-btn");
    const barcodePdfBtn = root.querySelector("#barcode-pdf-btn");
    const pricingModal = root.querySelector("#product-pricing-modal");
    const pricingForm = root.querySelector("#product-pricing-form");
    const pricingProductName = root.querySelector("#pricing-product-name");
    const pricingProductCode = root.querySelector("#pricing-product-code");
    let activePricingMedicineId = null;

    if (!tableBody) return root;

    let activeBarcodeProduct = null;

    const state = {
        query: "",
        category: "",
        page: 0,
        size: Number(pageSizeSelect?.value || 20),
        sort: sortSelect?.value || "name",
        dir: "asc"
    };

    let searchTimer = null;
    let editingProductId = null;
    let modalOptionsPromise = null;
    let updatePricingPreview = () => {};

    function formatMoney(value) {
        const numeric = Number(value || 0);
        return `৳${numeric.toFixed(2)}`;
    }

    function getCategoryLabel(item) {
        const medicine = item?.medicine || {};
        const type = medicine.type || "";
        const dosage = medicine.dosageForm || "";
        if (type.trim()) return type.trim();
        if (dosage.trim()) return dosage.trim();
        return "Uncategorized";
    }

    function getRxLabel(item) {
        const medicine = item?.medicine || {};
        return medicine.requiresRx ? "Yes" : "No";
    }

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
                <td colspan="6" class="product-empty">Loading products...</td>
            </tr>
        `;
    }

    function setEmpty() {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="product-empty">No products found.</td>
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
            button.className = "product-page-btn";
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

    const catalogCache = new Map();

    function renderRows(items) {
        if (!items || items.length === 0) {
            setEmpty();
            return;
        }
        catalogCache.clear();
        tableBody.innerHTML = items
            .map((item) => {
                const medicine = item.medicine || {};
                const generic = medicine.generic || {};
                const manufacturer = medicine.manufacturer || {};
                const name = medicine.brandName || "—";
                const strength = medicine.strength ? ` ${medicine.strength}` : "";
                const sku = medicine.brandCode || "—";
                const genericName = generic.genericName || "—";
                const manufacturerName = manufacturer.manufacturerName || "—";
                const category = getCategoryLabel(item);
                const price = formatMoney(item.price);
                const rxLabel = getRxLabel(item);
                catalogCache.set(String(item.id), {
                    id: item.id,
                    medicineId: medicine.id,
                    name,
                    code: sku,
                    price: Number(item.price || 0)
                });
                return `
                    <tr data-id="${item.id}">
                        <td>
                            <div class="product-name">${name}${strength}</div>
                            <div class="product-meta">Generic: ${genericName}</div>
                            <div class="product-meta">Mfr: ${manufacturerName}</div>
                        </td>
                        <td><span class="product-category-badge">${category}</span></td>
                        <td class="product-sku">${sku}</td>
                        <td>${price}</td>
                        <td>${rxLabel}</td>
                        <td>
                            <div class="product-actions">
                                <button class="product-action-btn product-action-analytics" type="button" data-action="barcode" aria-label="Barcode">
                                    <span class="material-symbols-outlined">barcode</span>
                                </button>
                                <button class="product-action-btn product-action-pricing" type="button" data-action="pricing" aria-label="Pricing">
                                    <span class="material-symbols-outlined">payments</span>
                                </button>
                                <button class="product-action-btn product-action-edit" type="button" data-action="edit" aria-label="Edit">
                                    <span class="material-symbols-outlined">edit</span>
                                </button>
                            </div>
                        </td>
                    </tr>
                `;
            })
            .join("");
    }

    function ensureBarcodeLib() {
        if (window.JsBarcode) return Promise.resolve();
        return new Promise((resolve, reject) => {
            const existing = document.getElementById("barcode-lib");
            if (existing) {
                existing.addEventListener("load", () => resolve());
                existing.addEventListener("error", () => reject(new Error("Failed to load barcode library")));
                return;
            }
            const script = document.createElement("script");
            script.id = "barcode-lib";
            script.src = "https://cdn.jsdelivr.net/npm/jsbarcode@3.11.6/dist/JsBarcode.all.min.js";
            script.onload = () => resolve();
            script.onerror = () => reject(new Error("Failed to load barcode library"));
            document.body.appendChild(script);
        });
    }

    function buildBarcodeSvg(code, sizeKey) {
        const sizeMap = {
            small: { width: 1.2, height: 40 },
            medium: { width: 1.6, height: 48 },
            large: { width: 2, height: 56 },
            xlarge: { width: 2.4, height: 64 }
        };
        const size = sizeMap[sizeKey] || sizeMap.medium;
        const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        window.JsBarcode(svg, code, {
            format: "CODE128",
            displayValue: false,
            margin: 0,
            width: size.width,
            height: size.height
        });
        return svg;
    }

    function openBarcodeModal(product) {
        if (!barcodeModal) return;
        ensureBarcodeLib()
            .then(() => {
                activeBarcodeProduct = product;
                if (barcodePreviewCode) {
                    barcodePreviewCode.dataset.codeValue = product.code || "";
                    barcodePreviewCode.textContent = barcodeShowCode?.checked ? product.code : "";
                }
                if (barcodePreviewSvg) {
                    barcodePreviewSvg.innerHTML = "";
                    const svg = buildBarcodeSvg(product.code, barcodeSizeSelect?.value || "medium");
                    barcodePreviewSvg.appendChild(svg);
                }
                barcodeModal.classList.add("is-open");
                barcodeModal.setAttribute("aria-hidden", "false");
            })
            .catch(() => {
                if (window.ToastService && typeof window.ToastService.show === "function") {
                    window.ToastService.show("Unable to load barcode preview.", "error");
                }
            });
    }

    function closeBarcodeModal() {
        if (!barcodeModal) return;
        barcodeModal.classList.remove("is-open");
        barcodeModal.setAttribute("aria-hidden", "true");
        activeBarcodeProduct = null;
    }

    function updateBarcodeQuantityVisibility() {
        if (!barcodeLayoutSelect || !barcodeQuantityField) return;
        const isCustom = barcodeLayoutSelect.value === "custom";
        barcodeQuantityField.style.display = isCustom ? "block" : "none";
    }

    function updateBarcodePreviewCode() {
        if (!barcodePreviewCode || !barcodeShowCode) return;
        const activeCode = barcodeShowCode.checked ? (barcodePreviewCode.dataset.codeValue || "") : "";
        barcodePreviewCode.textContent = activeCode;
    }

    function initBarcodeQuantityOptions() {
        if (!barcodeQuantitySelect) return;
        barcodeQuantitySelect.innerHTML = "";
        const quantities = [1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 25, 30];
        quantities.forEach((qty) => {
            const option = document.createElement("option");
            option.value = String(qty);
            option.textContent = `${qty} labels`;
            if (qty === 1) option.selected = true;
            barcodeQuantitySelect.appendChild(option);
        });
    }

    function buildLabelHtml(product, sizeKey, showCode) {
        const svg = buildBarcodeSvg(product.code, sizeKey);
        const codeLine = showCode ? `<div class="label-code">${product.code}</div>` : "";
        return `
            <div class="label-card">
                <div class="label-barcode">${svg.outerHTML}</div>
                ${codeLine}
            </div>
        `;
    }

    function buildPrintLayout(layoutKey) {
        const layoutMap = {
            "a4-4": { columns: 2, rows: 2 },
            "a4-8": { columns: 4, rows: 2 },
            "a4-16": { columns: 4, rows: 4 },
            "a4-32": { columns: 4, rows: 8 }
        };
        return layoutMap[layoutKey] || layoutMap["a4-32"];
    }

    function openPrintWindow(product, action) {
        const sizeKey = barcodeSizeSelect?.value || "medium";
        const layoutKey = barcodeLayoutSelect?.value || "a4-32";
        const showCode = barcodeShowCode?.checked ?? true;
        let totalLabels = 1;
        let columns = 1;
        let rows = 1;
        if (layoutKey === "custom") {
            const parsed = Number(barcodeQuantitySelect?.value || 1);
            totalLabels = Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
        } else {
            const layout = buildPrintLayout(layoutKey);
            columns = layout.columns;
            rows = layout.rows;
            totalLabels = columns * rows;
        }

        const labelHtml = buildLabelHtml(product, sizeKey, showCode);
        const labels = Array.from({ length: totalLabels }).map(() => labelHtml).join("");

        const printWindow = window.open("", "_blank");
        if (!printWindow) {
            if (window.ToastService && typeof window.ToastService.show === "function") {
                window.ToastService.show("Popup blocked. Allow popups to print.", "error");
            }
            return;
        }

        const gridStyle = layoutKey === "custom"
            ? "display: flex; flex-wrap: wrap; gap: 12px;"
            : `display: grid; grid-template-columns: repeat(${columns}, 1fr); gap: 12px;`;

        printWindow.document.write(`
            <html>
            <head>
                <title>Print Barcode Labels</title>
                <style>
                    @page { size: A4; margin: 12mm; }
                    body { font-family: Inter, Arial, sans-serif; margin: 0; padding: 0; }
                    .label-grid { ${gridStyle} }
                    .label-card {
                        border: 1px solid #e5e7eb;
                        border-radius: 8px;
                        padding: 8px;
                        text-align: center;
                        min-height: 90px;
                    }
                    .label-name { font-size: 10px; font-weight: 600; margin-bottom: 4px; }
                    .label-code { font-size: 10px; color: #64748b; margin-top: 4px; }
                    .label-barcode svg { width: 100%; height: auto; }
                </style>
            </head>
            <body>
                <div class="label-grid">${labels}</div>
            </body>
            </html>
        `);
        printWindow.document.close();
        printWindow.focus();
        printWindow.print();
        if (action === "pdf") {
            printWindow.close();
        }
    }

    function updateBarcodeActionLabel() {
        if (!barcodePrintBtn) return;
        const layoutKey = barcodeLayoutSelect?.value || "a4-32";
        const label = layoutKey === "custom" ? "Print Label" : `Print ${layoutKey.toUpperCase()}`;
        barcodePrintBtn.innerHTML = `<span class="material-symbols-outlined">print</span>${label}`;
    }

    function fetchCatalog() {
        const params = new URLSearchParams();
        if (state.query) params.set("q", state.query);
        if (state.category) params.set("category", state.category);
        params.set("page", String(state.page));
        params.set("size", String(state.size));
        params.set("sort", state.sort);
        params.set("dir", state.dir);
        return fetch(`/api/products/catalog?${params.toString()}`)
            .then((response) => response.json());
    }

    function loadCatalog() {
        setLoading();
        updateSortDirButton();
        fetchCatalog()
            .then((data) => {
                const items = Array.isArray(data?.items) ? data.items : [];
                renderRows(items);
                updateResultCount(Number(data?.total || 0), Number(data?.page || 0), Number(data?.size || state.size));
                buildPagination(Number(data?.totalPages || 0), Number(data?.page || 0));
            })
            .catch(() => {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="6" class="product-empty">Unable to load products.</td>
                    </tr>
                `;
                updateResultCount(0, 0, state.size);
                buildPagination(0, 0);
            });
    }

    function loadCategories() {
        if (!categorySelect) return;
        fetch("/api/products/catalog/categories")
            .then((response) => response.json())
            .then((data) => {
                if (!Array.isArray(data)) return;
                const fragment = document.createDocumentFragment();
                data.forEach((category) => {
                    const option = document.createElement("option");
                    option.value = category;
                    option.textContent = category;
                    fragment.appendChild(option);
                });
                categorySelect.appendChild(fragment);
            })
            .catch(() => {
                // Keep default category option.
            });
    }

    function loadSelectOptions(endpoint, selectEl, placeholder) {
        if (!selectEl) return Promise.resolve();
        return fetch(endpoint)
            .then((response) => response.json())
            .then((data) => {
                if (!Array.isArray(data)) return;
                selectEl.innerHTML = "";
                const defaultOption = document.createElement("option");
                defaultOption.value = "";
                defaultOption.disabled = true;
                defaultOption.selected = true;
                defaultOption.textContent = placeholder;
                selectEl.appendChild(defaultOption);

                data.forEach((item) => {
                    const option = document.createElement("option");
                    option.value = String(item.id);
                    option.textContent = item.name;
                    selectEl.appendChild(option);
                });
            })
            .catch(() => {
                // Keep empty select if load fails.
            });
    }

    function openModal() {
        if (!modal) return;
        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
        const nameInput = modal.querySelector("input[name=\"name\"]");
        if (nameInput instanceof HTMLInputElement) {
            nameInput.focus();
        }
    }

    function closeModal() {
        if (!modal) return;
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        resetProductForm();
        editingProductId = null;
        setModalMode("create");
    }

    function resetProductForm() {
        if (!(modalForm instanceof HTMLFormElement)) return;
        modalForm.reset();
        const genericSelect = modalForm.querySelector("select[name=\"genericId\"]");
        const manufacturerSelect = modalForm.querySelector("select[name=\"manufacturerId\"]");
        const categorySelectModal = modalForm.querySelector("select[name=\"category\"]");
        if (genericSelect) {
            genericSelect.value = "";
            if (genericSelect.options.length > 0) genericSelect.selectedIndex = 0;
        }
        if (manufacturerSelect) {
            manufacturerSelect.value = "";
            if (manufacturerSelect.options.length > 0) manufacturerSelect.selectedIndex = 0;
        }
        if (categorySelectModal) {
            categorySelectModal.value = "";
            if (categorySelectModal.options.length > 0) categorySelectModal.selectedIndex = 0;
        }
        const requiresRxInput = modalForm.querySelector("input[name=\"requiresRx\"]");
        const trackExpiryInput = modalForm.querySelector("input[name=\"trackExpiry\"]");
        if (requiresRxInput) requiresRxInput.checked = false;
        if (trackExpiryInput) trackExpiryInput.checked = true;
        updatePricingPreview();
    }

    function ensureModalOptions() {
        if (modalOptionsPromise) return modalOptionsPromise;
        if (!modalForm) return Promise.resolve();
        const genericSelect = modalForm.querySelector("select[name=\"genericId\"]");
        const manufacturerSelect = modalForm.querySelector("select[name=\"manufacturerId\"]");
        const categorySelectModal = modalForm.querySelector("select[name=\"category\"]");
        modalOptionsPromise = Promise.all([
            loadSelectOptions("/api/products/options/generics", genericSelect, "Select generic name"),
            loadSelectOptions("/api/products/options/manufacturers", manufacturerSelect, "Select manufacturer"),
            fetch("/api/products/catalog/categories")
                .then((response) => response.json())
                .then((data) => {
                    if (!Array.isArray(data) || !categorySelectModal) return;
                    categorySelectModal.innerHTML = "";
                    const defaultOption = document.createElement("option");
                    defaultOption.value = "";
                    defaultOption.disabled = true;
                    defaultOption.selected = true;
                    defaultOption.textContent = "Select category";
                    categorySelectModal.appendChild(defaultOption);
                    data.forEach((category) => {
                        const option = document.createElement("option");
                        option.value = category;
                        option.textContent = category;
                        categorySelectModal.appendChild(option);
                    });
                })
                .catch(() => {
                    // Ignore modal category load errors.
                })
        ]);
        return modalOptionsPromise;
    }

    function setModalMode(mode) {
        if (modalTitle) {
            modalTitle.textContent = mode === "edit" ? "Edit Product" : "Add New Product";
        }
        if (modalSubmit instanceof HTMLButtonElement) {
            modalSubmit.textContent = mode === "edit" ? "Update Product" : "Create Product";
        }
        if (pricingSection) {
            pricingSection.style.display = mode === "edit" ? "none" : "";
        }
    }

    function fillProductForm(data) {
        if (!modalForm || !data) return;
        modalForm.querySelector("input[name=\"name\"]").value = data.name || "";
        modalForm.querySelector("input[name=\"code\"]").value = data.code || "";
        modalForm.querySelector("textarea[name=\"description\"]").value = data.description || "";
        modalForm.querySelector("input[name=\"sellingPrice\"]").value = data.sellingPrice ?? 0;
        modalForm.querySelector("input[name=\"costPrice\"]").value = data.costPrice ?? 0;
        modalForm.querySelector("input[name=\"qty\"]").value = data.qty ?? 0;
        modalForm.querySelector("input[name=\"requiresRx\"]").checked = Boolean(data.requiresRx);
        modalForm.querySelector("input[name=\"trackExpiry\"]").checked = Boolean(data.trackExpiry);

        const genericSelect = modalForm.querySelector("select[name=\"genericId\"]");
        const manufacturerSelect = modalForm.querySelector("select[name=\"manufacturerId\"]");
        const categorySelectModal = modalForm.querySelector("select[name=\"category\"]");
        if (genericSelect) genericSelect.value = data.genericId ? String(data.genericId) : "";
        if (manufacturerSelect) manufacturerSelect.value = data.manufacturerId ? String(data.manufacturerId) : "";
        if (categorySelectModal) categorySelectModal.value = data.category || "";
        updatePricingPreview();
    }

    function openEditModal(productId) {
        if (!productId) return;
        ensureModalOptions()
            .then(() => fetch(`/api/products/${productId}`))
            .then((response) => response.json().then((data) => ({ ok: response.ok, data })))
            .then(({ ok, data }) => {
                if (!ok) {
                    throw new Error(data?.message || "Unable to load product.");
                }
                editingProductId = productId;
                setModalMode("edit");
                fillProductForm(data);
                openModal();
            })
            .catch((error) => {
                if (window.ToastService && typeof window.ToastService.show === "function") {
                    window.ToastService.show(error.message || "Unable to load product.", "error");
                }
            });
    }

    function bindModalActions() {
        if (!modal) return;
        modal.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            if (target.closest("[data-action=\"close-product-modal\"]")) {
                closeModal();
            }
        });
    }

    function openPricingModal(product) {
        if (!pricingModal) return;
        if (pricingProductName) pricingProductName.textContent = product.name || "";
        if (pricingProductCode) pricingProductCode.textContent = product.code || "";
        activePricingMedicineId = product.medicineId || null;
        if (pricingForm instanceof HTMLFormElement) {
            pricingForm.reset();
        }
        pricingModal.classList.add("is-open");
        pricingModal.setAttribute("aria-hidden", "false");
    }

    function closePricingModal() {
        if (!pricingModal) return;
        pricingModal.classList.remove("is-open");
        pricingModal.setAttribute("aria-hidden", "true");
        if (pricingForm instanceof HTMLFormElement) {
            pricingForm.reset();
        }
        activePricingMedicineId = null;
    }

    if (searchInput) {
        searchInput.addEventListener("input", (event) => {
            const value = event.target.value || "";
            if (searchTimer) clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                state.query = value.trim();
                state.page = 0;
                loadCatalog();
            }, 250);
        });
    }

    if (categorySelect) {
        categorySelect.addEventListener("change", () => {
            state.category = categorySelect.value || "";
            state.page = 0;
            loadCatalog();
        });
    }

    if (sortSelect) {
        sortSelect.addEventListener("change", () => {
            state.sort = sortSelect.value || "name";
            state.page = 0;
            loadCatalog();
        });
    }

    if (sortDirBtn) {
        sortDirBtn.addEventListener("click", () => {
            state.dir = state.dir === "asc" ? "desc" : "asc";
            state.page = 0;
            updateSortDirButton();
            loadCatalog();
        });
    }

    if (pageSizeSelect) {
        pageSizeSelect.addEventListener("change", () => {
            state.size = Number(pageSizeSelect.value || 20);
            state.page = 0;
            loadCatalog();
        });
    }

    if (paginationEl) {
        paginationEl.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            const button = target.closest(".product-page-btn");
            if (!button || button.disabled) return;
            const page = Number(button.dataset.page);
            if (Number.isNaN(page) || page === state.page) return;
            state.page = page;
            loadCatalog();
        });
    }

    if (tableBody) {
        tableBody.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            const actionButton = target.closest(".product-action-btn");
            if (!actionButton) return;
            const action = actionButton.getAttribute("data-action");
            if (action === "barcode") {
                const row = actionButton.closest("tr");
                const productId = row?.getAttribute("data-id");
                if (productId && catalogCache.has(productId)) {
                    openBarcodeModal(catalogCache.get(productId));
                }
                return;
            }
            if (action === "pricing") {
                const row = actionButton.closest("tr");
                const productId = row?.getAttribute("data-id");
                if (productId && catalogCache.has(productId)) {
                    openPricingModal(catalogCache.get(productId));
                }
                return;
            }
            if (action === "edit") {
                const row = actionButton.closest("tr");
                const productId = row?.getAttribute("data-id");
                if (productId) {
                    openEditModal(productId);
                }
                return;
            }
            if (window.ToastService && typeof window.ToastService.show === "function") {
                window.ToastService.show("This action is coming soon.", "info");
            }
        });
    }

    if (bulkBtn) {
        bulkBtn.addEventListener("click", () => {
            if (window.ToastService && typeof window.ToastService.show === "function") {
                window.ToastService.show("Bulk import is not configured yet.", "info");
            }
        });
    }

    if (addBtn) {
        addBtn.addEventListener("click", () => {
            ensureModalOptions().then(() => {
                editingProductId = null;
                setModalMode("create");
                resetProductForm();
                openModal();
            });
        });
    }

    if (modalForm instanceof HTMLFormElement) {
        const sellingInput = modalForm.querySelector("input[name=\"sellingPrice\"]");
        const costInput = modalForm.querySelector("input[name=\"costPrice\"]");
        const sellingPreview = modalForm.querySelector("#product-selling-preview");
        const costPreview = modalForm.querySelector("#product-cost-preview");
        const profitPreview = modalForm.querySelector("#product-profit-margin");

        updatePricingPreview = function () {
            const selling = Number(sellingInput?.value || 0);
            const cost = Number(costInput?.value || 0);
            if (sellingPreview) sellingPreview.textContent = `Preview: ৳${selling.toFixed(2)}`;
            if (costPreview) costPreview.textContent = `Preview: ৳${cost.toFixed(2)}`;
            let margin = 0;
            if (selling > 0) {
                margin = ((selling - cost) / selling) * 100;
            }
            const safeMargin = Number.isFinite(margin) ? margin : 0;
            if (profitPreview) {
                profitPreview.innerHTML = `Profit Margin: <span>${safeMargin.toFixed(1)}%</span>`;
            }
        }

        if (sellingInput) {
            sellingInput.addEventListener("input", updatePricingPreview);
        }
        if (costInput) {
            costInput.addEventListener("input", updatePricingPreview);
        }

        modalForm.addEventListener("submit", (event) => {
            event.preventDefault();
            const isEdit = Boolean(editingProductId);
            const formData = new FormData(modalForm);
            const payload = {
                name: formData.get("name") || "",
                code: formData.get("code") || "",
                genericId: formData.get("genericId") ? Number(formData.get("genericId")) : null,
                manufacturerId: formData.get("manufacturerId") ? Number(formData.get("manufacturerId")) : null,
                category: formData.get("category") || "",
                description: formData.get("description") || "",
                sellingPrice: formData.get("sellingPrice") ? Number(formData.get("sellingPrice")) : null,
                costPrice: formData.get("costPrice") ? Number(formData.get("costPrice")) : null,
                qty: formData.get("qty") ? Number(formData.get("qty")) : null,
                requiresRx: formData.get("requiresRx") === "on",
                trackExpiry: formData.get("trackExpiry") === "on"
            };
            const codeValue = String(payload.code || "").trim();
            if (!/^M\d{6}$/.test(codeValue)) {
                if (window.ToastService && typeof window.ToastService.show === "function") {
                    window.ToastService.show("Code must be M followed by 6 digits (e.g., M217156).", "error");
                }
                return;
            }

            const endpoint = editingProductId ? `/api/products/${editingProductId}` : "/api/products";
            const method = editingProductId ? "PUT" : "POST";

            fetch(endpoint, {
                method,
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            })
                .then((response) => response.json().then((data) => ({ ok: response.ok, data })))
                .then(({ ok, data }) => {
                    if (!ok) {
                        throw new Error(data?.message || "Unable to create product.");
                    }
                    closeModal();
                    loadCatalog();
                    if (window.ToastService && typeof window.ToastService.show === "function") {
                        window.ToastService.show(
                            isEdit ? "Product updated successfully." : "Product created successfully.",
                            "success"
                        );
                    }
                })
                .catch((error) => {
                    if (window.ToastService && typeof window.ToastService.show === "function") {
                        window.ToastService.show(error.message || "Unable to create product.", "error");
                    }
                });
        });

        updatePricingPreview();
    }

    loadCategories();
    bindModalActions();
    if (barcodeModal) {
        initBarcodeQuantityOptions();
        updateBarcodeQuantityVisibility();
        updateBarcodeActionLabel();
        barcodeModal.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            if (target.closest("[data-action=\"close-barcode-modal\"]")) {
                closeBarcodeModal();
            }
        });
    }
    if (barcodeLayoutSelect) {
        barcodeLayoutSelect.addEventListener("change", () => {
            updateBarcodeQuantityVisibility();
            updateBarcodeActionLabel();
        });
    }
    if (barcodeSizeSelect) {
        barcodeSizeSelect.addEventListener("change", () => {
            const code = barcodePreviewCode?.dataset.codeValue || barcodePreviewCode?.textContent || "";
            if (!code) return;
            if (!barcodePreviewSvg || !window.JsBarcode) return;
            barcodePreviewSvg.innerHTML = "";
            const svg = buildBarcodeSvg(code, barcodeSizeSelect.value);
            barcodePreviewSvg.appendChild(svg);
        });
    }
    if (barcodeShowCode) {
        barcodeShowCode.addEventListener("change", updateBarcodePreviewCode);
    }
    if (barcodePrintBtn) {
        barcodePrintBtn.addEventListener("click", () => {
            if (!activeBarcodeProduct) return;
            openPrintWindow(activeBarcodeProduct, "print");
        });
    }
    if (barcodePdfBtn) {
        barcodePdfBtn.addEventListener("click", () => {
            if (!activeBarcodeProduct) return;
            openPrintWindow(activeBarcodeProduct, "pdf");
        });
    }
    if (pricingModal) {
        pricingModal.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            if (target.closest("[data-action=\"close-pricing-modal\"]")) {
                closePricingModal();
            }
        });
    }
    if (pricingForm instanceof HTMLFormElement) {
        pricingForm.addEventListener("submit", (event) => {
            event.preventDefault();
            const formData = new FormData(pricingForm);
            const sellingPrice = formData.get("sellingPrice") ? Number(formData.get("sellingPrice")) : null;
            const costPrice = formData.get("costPrice") ? Number(formData.get("costPrice")) : null;
            const qty = formData.get("qty") ? Number(formData.get("qty")) : null;
            if (!activePricingMedicineId) {
                if (window.ToastService && typeof window.ToastService.show === "function") {
                    window.ToastService.show("Unable to locate selected product.", "error");
                }
                return;
            }

            fetch(`/api/products/${activePricingMedicineId}/pricing`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ sellingPrice, costPrice, qty })
            })
                .then((response) => response.json().then((data) => ({ ok: response.ok, data })))
                .then(({ ok, data }) => {
                    if (!ok) {
                        throw new Error(data?.message || "Unable to save pricing.");
                    }
                    closePricingModal();
                    loadCatalog();
                    if (window.ToastService && typeof window.ToastService.show === "function") {
                        window.ToastService.show("Pricing saved successfully.", "success");
                    }
                })
                .catch((error) => {
                    if (window.ToastService && typeof window.ToastService.show === "function") {
                        window.ToastService.show(error.message || "Unable to save pricing.", "error");
                    }
                });
        });
    }
    loadCatalog();
    return root;
});

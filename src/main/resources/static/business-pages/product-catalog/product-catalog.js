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

    if (!tableBody) return root;

    const state = {
        query: "",
        category: "",
        page: 0,
        size: Number(pageSizeSelect?.value || 20),
        sort: sortSelect?.value || "name",
        dir: "asc"
    };

    let searchTimer = null;

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

    function renderRows(items) {
        if (!items || items.length === 0) {
            setEmpty();
            return;
        }
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
                                <button class="product-action-btn product-action-analytics" type="button" data-action="analytics" aria-label="Analytics">
                                    <span class="material-symbols-outlined">bar_chart</span>
                                </button>
                                <button class="product-action-btn product-action-edit" type="button" data-action="edit" aria-label="Edit">
                                    <span class="material-symbols-outlined">edit</span>
                                </button>
                                <button class="product-action-btn product-action-delete" type="button" data-action="delete" aria-label="Delete">
                                    <span class="material-symbols-outlined">delete</span>
                                </button>
                            </div>
                        </td>
                    </tr>
                `;
            })
            .join("");
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
        if (!selectEl) return;
        fetch(endpoint)
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
        if (modalForm instanceof HTMLFormElement) {
            modalForm.reset();
        }
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
            if (window.ToastService && typeof window.ToastService.show === "function") {
                const label = action === "delete"
                    ? "Delete action is not available yet."
                    : "This action is coming soon.";
                window.ToastService.show(label, "info");
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
            openModal();
        });
    }

    if (modalForm instanceof HTMLFormElement) {
        const sellingInput = modalForm.querySelector("input[name=\"sellingPrice\"]");
        const costInput = modalForm.querySelector("input[name=\"costPrice\"]");
        const sellingPreview = modalForm.querySelector("#product-selling-preview");
        const costPreview = modalForm.querySelector("#product-cost-preview");
        const profitPreview = modalForm.querySelector("#product-profit-margin");

        function updatePricingPreview() {
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

            fetch("/api/products", {
                method: "POST",
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
                        window.ToastService.show("Product created successfully.", "success");
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
    if (modalForm) {
        const genericSelect = modalForm.querySelector("select[name=\"genericId\"]");
        const manufacturerSelect = modalForm.querySelector("select[name=\"manufacturerId\"]");
        const categorySelectModal = modalForm.querySelector("select[name=\"category\"]");
        loadSelectOptions("/api/products/options/generics", genericSelect, "Select generic name");
        loadSelectOptions("/api/products/options/manufacturers", manufacturerSelect, "Select manufacturer");
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
            });
    }
    bindModalActions();
    loadCatalog();
    return root;
});

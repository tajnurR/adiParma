window.CustomerDropdownService = (function () {
    function loadTomSelectAssets() {
        if (window.TomSelect) return Promise.resolve();

        const cssId = "tom-select-css";
        const jsId = "tom-select-js";

        if (!document.getElementById(cssId)) {
            const link = document.createElement("link");
            link.id = cssId;
            link.rel = "stylesheet";
            link.href = "https://cdn.jsdelivr.net/npm/tom-select@2.3.1/dist/css/tom-select.css";
            document.head.appendChild(link);
        }

        return new Promise((resolve, reject) => {
            if (document.getElementById(jsId)) {
                resolve();
                return;
            }
            const script = document.createElement("script");
            script.id = jsId;
            script.src = "https://cdn.jsdelivr.net/npm/tom-select@2.3.1/dist/js/tom-select.complete.min.js";
            script.onload = () => resolve();
            script.onerror = () => reject(new Error("Failed to load Tom Select"));
            document.body.appendChild(script);
        });
    }

    function initCustomerDropdown(options) {
        const root = options.root;
        const customerSelect = options.selectEl;
        const onSelect = options.onSelect;
        const onClear = options.onClear;

        if (!customerSelect || !window.TomSelect) return null;

        let errorState = false;
        const createOptionId = "__create_customer__";
        const pageSize = 10;
        let currentQuery = "";
        let currentPage = 0;
        let hasMore = false;
        let isLoading = false;
        let createPanel = null;
        let lastTypedValue = "";

        function normalizeQuery(query) {
            return query.trim().toLowerCase();
        }

        function buildCreateOption(query) {
            return {
                id: createOptionId,
                name: query ? `Create "${query}"` : "Create New Customer",
                query: query || "",
                isCreate: true,
                $order: -1000
            };
        }

        function mapCustomer(customer) {
            return {
                id: String(customer.id),
                name: customer.name || "Unknown",
                phone: customer.phone || "",
                age: customer.age ?? "",
                address: customer.address || ""
            };
        }

        function renderCreateOption(data, escape) {
            const label = data.query ? `Create "${data.query}"` : "Create New Customer";
            const subLabel = data.query ? "Add as new customer" : "Add a new customer";
            return `
                <div class="pos-customer-create-option">
                    <div class="pos-customer-create-title">+ ${escape(label)}</div>
                    <div class="pos-customer-create-sub">${escape(subLabel)}</div>
                </div>
            `;
        }

        function getCurrentSearchText(select) {
            const inputValue = select.control_input?.value?.trim();
            if (inputValue) return inputValue;
            if (select.items.length) {
                const option = select.options[select.items[0]];
                return option?.name || "";
            }
            return "";
        }

        function syncCreateOption(select, query) {
            const nextQuery = query ?? getCurrentSearchText(select);
            const option = buildCreateOption(nextQuery);
            if (select.options[createOptionId]) {
                select.updateOption(createOptionId, option);
            } else {
                select.addOption(option);
            }
            select.refreshOptions(false);
        }

        function fetchCustomers(query, page) {
            return fetch(`/api/customers/search?q=${encodeURIComponent(query)}&page=${page}&size=${pageSize}`)
                .then((response) => response.json())
                .then((data) => {
                    const items = Array.isArray(data.items) ? data.items.map(mapCustomer) : [];
                    return {
                        items,
                        hasMore: Boolean(data.hasMore)
                    };
                });
        }

        function ensureCreatePanel(select) {
            if (createPanel) return;
            createPanel = document.createElement("div");
            createPanel.className = "pos-customer-create-panel";
            createPanel.innerHTML = `
                <div class="pos-customer-create-header">Add New Customer</div>
                <div class="pos-customer-create-body">
                    <label class="pos-customer-create-field">
                        <span>Name *</span>
                        <input type="text" name="name" required />
                    </label>
                    <label class="pos-customer-create-field">
                        <span>Phone *</span>
                        <input type="text" name="phone" required />
                    </label>
                    <label class="pos-customer-create-field">
                        <span>Age *</span>
                        <input type="text" name="age" inputmode="numeric" maxlength="3" pattern="\\d{1,3}" required />
                    </label>
                    <label class="pos-customer-create-field">
                        <span>Address *</span>
                        <input type="text" name="address" required />
                    </label>
                </div>
                <div class="pos-customer-create-actions">
                    <button type="button" class="pos-customer-create-close">Close</button>
                    <button type="button" class="pos-customer-create-submit">Submit</button>
                </div>
            `;
            const customerSearch = customerSelect.closest(".pos-customer-search");
            const container = customerSearch?.parentElement || root;
            if (customerSearch && customerSearch.nextSibling) {
                container.insertBefore(createPanel, customerSearch.nextSibling);
            } else {
                container.appendChild(createPanel);
            }

            const closeButton = createPanel.querySelector(".pos-customer-create-close");
            const submitButton = createPanel.querySelector(".pos-customer-create-submit");
            const ageInput = createPanel.querySelector("input[name=\"age\"]");
            if (closeButton) {
                closeButton.addEventListener("click", () => {
                    createPanel.classList.remove("is-open");
                });
            }
            if (ageInput) {
                ageInput.addEventListener("beforeinput", (event) => {
                    if (event.inputType === "insertText" && /\D/.test(event.data || "")) {
                        event.preventDefault();
                    }
                });
                ageInput.addEventListener("keydown", (event) => {
                    const allowed = ["Backspace", "Delete", "ArrowLeft", "ArrowRight", "Tab", "Home", "End"];
                    if (allowed.includes(event.key)) return;
                    if (!/^\d$/.test(event.key)) {
                        event.preventDefault();
                    }
                });
                ageInput.addEventListener("input", (event) => {
                    const target = event.target;
                    if (!(target instanceof HTMLInputElement)) return;
                    target.value = target.value.replace(/\D/g, "").slice(0, 3);
                });
            }
            if (submitButton) {
                submitButton.addEventListener("click", () => {
                    const nameInput = createPanel.querySelector("input[name=\"name\"]");
                    const phoneInput = createPanel.querySelector("input[name=\"phone\"]");
                    const ageInput = createPanel.querySelector("input[name=\"age\"]");
                    const addressInput = createPanel.querySelector("input[name=\"address\"]");
                    if (!(nameInput instanceof HTMLInputElement) ||
                        !(phoneInput instanceof HTMLInputElement) ||
                        !(ageInput instanceof HTMLInputElement) ||
                        !(addressInput instanceof HTMLInputElement)) {
                        return;
                    }

                    const nameValue = nameInput.value.trim();
                    const phoneValue = phoneInput.value.trim();
                    const ageValue = ageInput.value.replace(/\D/g, "").slice(0, 3).trim();
                    const addressValue = addressInput.value.trim();
                    if (!nameValue || !phoneValue || !ageValue || !addressValue) {
                        return;
                    }
                    if (!/^\d{1,3}$/.test(ageValue)) {
                        return;
                    }

                    submitButton.disabled = true;
                    fetch("/api/customers", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            name: nameValue,
                            phone: phoneValue,
                            age: ageValue,
                            address: addressValue
                        })
                    })
                        .then((response) => response.json())
                        .then((data) => {
                            const option = mapCustomer(data);
                            select.addOption(option);
                            select.setValue(option.id, true);
                            createPanel.classList.remove("is-open");
                        })
                        .finally(() => {
                            submitButton.disabled = false;
                        });
                });
            }
        }

        function openCreatePanel(select) {
            ensureCreatePanel(select);
            if (!createPanel) return;
            const nameInput = createPanel.querySelector("input[name=\"name\"]");
            const phoneInput = createPanel.querySelector("input[name=\"phone\"]");
            const ageInput = createPanel.querySelector("input[name=\"age\"]");
            const addressInput = createPanel.querySelector("input[name=\"address\"]");
            if (nameInput instanceof HTMLInputElement) {
                nameInput.value = getCurrentSearchText(select) || lastTypedValue.trim();
                nameInput.focus();
            }
            if (phoneInput instanceof HTMLInputElement) phoneInput.value = "";
            if (ageInput instanceof HTMLInputElement) ageInput.value = "";
            if (addressInput instanceof HTMLInputElement) addressInput.value = "";
            createPanel.classList.add("is-open");
        }

        function loadNextPage(select) {
            if (isLoading || !hasMore || !currentQuery) return;
            isLoading = true;
            fetchCustomers(currentQuery, currentPage)
                .then((data) => {
                    hasMore = data.hasMore;
                    currentPage += 1;
                    if (data.items.length) {
                        select.addOptions(data.items);
                        select.refreshOptions(false);
                    }
                })
                .catch(() => {
                    errorState = true;
                })
                .finally(() => {
                    isLoading = false;
                });
        }

        const select = new TomSelect(customerSelect, {
            valueField: "id",
            labelField: "name",
            searchField: [],
            maxItems: 1,
            create: false,
            preload: false,
            closeAfterSelect: false,
            loadThrottle: 280,
            shouldLoad: () => true,
            sortField: [{ field: "$order" }],
            render: {
                option: function (data, escape) {
                    if (data.id === createOptionId || data.isCreate) {
                        return renderCreateOption(data, escape);
                    }
                    const phone = data.phone ? `<div class="customer-meta">${escape(data.phone)}</div>` : "";
                    const extra = !data.phone && data.age ? `<div class="customer-meta">Age: ${escape(String(data.age))}</div>` : "";
                    return `
                        <div>
                            <div class="customer-name">${escape(data.name)}</div>
                            ${phone || extra}
                        </div>
                    `;
                },
                item: function (data, escape) {
                    return `<div class="customer-name">${escape(data.name)}</div>`;
                },
                no_results: function () {
                    return `<div class="customer-meta">${errorState ? "Unable to load customers" : "No customer found"}</div>`;
                },
                loading: function () {
                    return `<div class="customer-meta">Searching customers...</div>`;
                }
            },
            load: function (query, callback) {
                const normalized = normalizeQuery(query);
                const isNewQuery = normalized !== currentQuery;
                if (isNewQuery) {
                    currentQuery = normalized;
                    currentPage = 0;
                    hasMore = false;
                    select.clearOptions();
                }

                errorState = false;

                if (!normalized) {
                    const fallbackQuery = query || getCurrentSearchText(select);
                    callback([buildCreateOption(fallbackQuery)]);
                    return;
                }

                isLoading = true;
                fetchCustomers(normalized, currentPage)
                    .then((data) => {
                        hasMore = data.hasMore;
                        currentPage += 1;
                        const options = [buildCreateOption(query), ...data.items];
                        callback(options);
                    })
                    .catch(() => {
                        errorState = true;
                        callback([buildCreateOption(query)]);
                    })
                    .finally(() => {
                        isLoading = false;
                    });
            }
        });

        select.on("type", (value) => {
            lastTypedValue = value;
            syncCreateOption(select, value);
        });

        select.on("dropdown_open", () => {
            syncCreateOption(select);
            const dropdownContent = select.dropdown_content;
            if (!dropdownContent || dropdownContent.dataset.scrollBound) return;
            dropdownContent.dataset.scrollBound = "true";
            dropdownContent.addEventListener("scroll", () => {
                const threshold = 24;
                if (dropdownContent.scrollTop + dropdownContent.clientHeight >= dropdownContent.scrollHeight - threshold) {
                    loadNextPage(select);
                }
            });
        });

        select.on("item_add", (value) => {
            if (value === createOptionId) {
                select.removeItem(value, true);
                select.close();
                openCreatePanel(select);
                return;
            }

            const option = select.options[value];
            if (typeof onSelect === "function") {
                onSelect({
                    id: value,
                    name: option?.name || "",
                    phone: option?.phone || ""
                });
            }
            select.close();
            syncCreateOption(select);
            if (createPanel) {
                createPanel.classList.remove("is-open");
            }
        });

        select.on("clear", () => {
            if (typeof onClear === "function") {
                onClear();
            }
            syncCreateOption(select, "");
        });

        return select;
    }

    return {
        loadTomSelectAssets,
        initCustomerDropdown
    };
})();

window.BusinessPages.register("customer", function (root) {
    const tableEl = root.querySelector("#customer-table");
    const searchInput = root.querySelector("#customer-search");
    const sortSelect = root.querySelector("#customer-sort");
    const pageSizeSelect = root.querySelector("#customer-page-size");
    const resultCount = root.querySelector("#customer-result-count");
    const addButton = root.querySelector(".customer-add-btn");
    const modal = root.querySelector("#customer-modal");
    const modalForm = root.querySelector("#customer-modal-form");

    if (!tableEl) return root;

    function ensureDataTablesAssets() {
        const cssId = "datatable-css";
        const jsId = "datatable-js";

        if (!document.getElementById(cssId)) {
            const link = document.createElement("link");
            link.id = cssId;
            link.rel = "stylesheet";
            link.href = "https://cdn.datatables.net/1.13.8/css/jquery.dataTables.min.css";
            document.head.appendChild(link);
        }

        return new Promise((resolve, reject) => {
            if (document.getElementById(jsId)) {
                resolve();
                return;
            }
            const script = document.createElement("script");
            script.id = jsId;
            script.src = "https://cdn.datatables.net/1.13.8/js/jquery.dataTables.min.js";
            script.onload = () => resolve();
            script.onerror = () => reject(new Error("Failed to load DataTables"));
            document.body.appendChild(script);
        });
    }

    function formatDate(value) {
        if (!value) return "—";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;
        return date.toLocaleDateString("en-US");
    }

    function updateResultCount(settings) {
        if (!resultCount || !settings) return;
        const total = settings.fnRecordsTotal ? settings.fnRecordsTotal() : 0;
        const filtered = settings.fnRecordsDisplay ? settings.fnRecordsDisplay() : total;
        const start = filtered === 0 ? 0 : settings._iDisplayStart + 1;
        const end = Math.min(settings._iDisplayStart + settings._iDisplayLength, filtered);
        resultCount.textContent = `Showing ${start} to ${end} of ${filtered} results`;
    }

    let dataTable = null;

    function initTable() {
        dataTable = $(tableEl).DataTable({
            serverSide: true,
            processing: true,
            searching: true,
            lengthChange: false,
            pageLength: Number(pageSizeSelect?.value || 20),
            order: [[3, "desc"]],
            dom: "t<'customer-table-footer'p>",
            ajax: {
                url: "/api/customers/datatable",
                type: "GET"
            },
            columns: [
                {
                    data: "name",
                    render: function (data) {
                        const name = data || "—";
                        return `<div class="customer-name">${name}</div>`;
                    }
                },
                {
                    data: null,
                    render: function (row) {
                        const phone = row.contact || "—";
                        const age = row.age ? `Age: ${row.age}` : "";
                        return `
                            <div class="customer-meta-line">
                                <span class="material-symbols-outlined">call</span>
                                <span>${phone}</span>
                            </div>
                            ${age ? `
                                <div class="customer-meta-line">
                                    <span class="material-symbols-outlined">person</span>
                                    <span>${age}</span>
                                </div>` : ""}
                        `;
                    }
                },
                {
                    data: "address",
                    render: function (data) {
                        const address = data || "—";
                        return `
                            <div class="customer-address">
                                <span class="material-symbols-outlined">location_on</span>
                                <span>${address}</span>
                            </div>
                        `;
                    }
                },
                {
                    data: "added",
                    render: function (data) {
                        return formatDate(data);
                    }
                },
                {
                    data: null,
                    orderable: false,
                    searchable: false,
                    render: function () {
                        return `
                            <div class="customer-actions">
                                <button class="customer-action-btn customer-action-refresh" type="button" aria-label="Refresh">
                                    <span class="material-symbols-outlined">refresh</span>
                                </button>
                                <button class="customer-action-btn customer-action-edit" type="button" aria-label="Edit">
                                    <span class="material-symbols-outlined">edit</span>
                                </button>
                                <button class="customer-action-btn customer-action-delete" type="button" aria-label="Delete">
                                    <span class="material-symbols-outlined">delete</span>
                                </button>
                            </div>
                        `;
                    }
                }
            ],
            drawCallback: function (settings) {
                updateResultCount(settings);
            }
        });

        if (searchInput) {
            let searchTimer = null;
            searchInput.addEventListener("input", (event) => {
                const value = event.target.value || "";
                if (searchTimer) clearTimeout(searchTimer);
                searchTimer = setTimeout(() => {
                    dataTable.search(value).draw();
                }, 250);
            });
        }

        if (sortSelect) {
            sortSelect.addEventListener("change", () => {
                const value = sortSelect.value;
                if (value === "name_asc") {
                    dataTable.order([0, "asc"]).draw();
                } else {
                    dataTable.order([3, "desc"]).draw();
                }
            });
        }

        if (pageSizeSelect) {
            pageSizeSelect.addEventListener("change", () => {
                dataTable.page.len(Number(pageSizeSelect.value || 20)).draw();
            });
        }
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

    function bindModalEvents() {
        if (!modal) return;

        modal.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            const action = target.getAttribute("data-action");
            if (action === "close-customer-modal") {
                closeModal();
            }
        });

        if (addButton) {
            addButton.addEventListener("click", () => {
                openModal();
            });
        }

        if (modalForm) {
            modalForm.addEventListener("submit", (event) => {
                event.preventDefault();
                const formData = new FormData(modalForm);
                const payload = {
                    name: String(formData.get("name") || "").trim(),
                    phone: String(formData.get("phone") || "").trim(),
                    age: String(formData.get("age") || "").trim(),
                    address: String(formData.get("address") || "").trim()
                };

                if (!payload.name || !payload.phone || !payload.age || !payload.address) {
                    if (window.ToastService && typeof window.ToastService.show === "function") {
                        window.ToastService.show("Please fill in all required fields.", "error");
                    }
                    return;
                }

                const submitBtn = modalForm.querySelector(".customer-modal-submit");
                if (submitBtn instanceof HTMLButtonElement) {
                    submitBtn.disabled = true;
                }

                fetch("/api/customers", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                })
                    .then((response) => response.json().then((data) => ({ ok: response.ok, data })))
                    .then(({ ok, data }) => {
                        if (!ok) {
                            throw new Error(data?.message || "Failed to add customer.");
                        }
                        if (window.ToastService && typeof window.ToastService.show === "function") {
                            window.ToastService.show("Customer added successfully.", "success");
                        }
                        closeModal();
                        if (dataTable) {
                            dataTable.ajax.reload(null, false);
                        }
                    })
                    .catch((error) => {
                        if (window.ToastService && typeof window.ToastService.show === "function") {
                            window.ToastService.show(error.message || "Failed to add customer.", "error");
                        }
                    })
                    .finally(() => {
                        if (submitBtn instanceof HTMLButtonElement) {
                            submitBtn.disabled = false;
                        }
                    });
            });
        }
    }

    bindModalEvents();
    ensureDataTablesAssets().then(initTable).catch(() => {});
    return root;
});

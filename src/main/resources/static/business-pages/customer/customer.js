window.BusinessPages.register("customer", function (root) {
    const tableEl = root.querySelector("#customer-table");
    const searchInput = root.querySelector("#customer-search");
    const sortSelect = root.querySelector("#customer-sort");
    const pageSizeSelect = root.querySelector("#customer-page-size");
    const resultCount = root.querySelector("#customer-result-count");

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

    function initTable() {
        const table = $(tableEl).DataTable({
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
                    table.search(value).draw();
                }, 250);
            });
        }

        if (sortSelect) {
            sortSelect.addEventListener("change", () => {
                const value = sortSelect.value;
                if (value === "name_asc") {
                    table.order([0, "asc"]).draw();
                } else {
                    table.order([3, "desc"]).draw();
                }
            });
        }

        if (pageSizeSelect) {
            pageSizeSelect.addEventListener("change", () => {
                table.page.len(Number(pageSizeSelect.value || 20)).draw();
            });
        }
    }

    ensureDataTablesAssets().then(initTable).catch(() => {});
    return root;
});

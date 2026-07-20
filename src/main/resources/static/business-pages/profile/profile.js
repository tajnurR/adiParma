window.BusinessPages.register("profile", function (root) {
    const form = root.querySelector("#profile-settings-form");
    const logo = root.querySelector("#profile-logo");
    const logoName = root.querySelector("#profile-logo-name");
    const logoTagline = root.querySelector("#profile-logo-tagline");

    if (!form) return root;

    const invoiceSettingFields = [
        "invoiceTitle",
        "receiptTitle",
        "currencySymbol",
        "billToLabel",
        "walkInCustomerLabel",
        "customerLabel",
        "invoiceNoLabel",
        "invoiceDateLabel",
        "paymentLabel",
        "processedByLabel",
        "itemLabel",
        "qtyLabel",
        "unitPriceLabel",
        "discountLabel",
        "amountLabel",
        "subtotalLabel",
        "grandTotalLabel",
        "invoiceFooterNote",
        "receiptFooterNote"
    ];

    function applyLogo(name, tagline) {
        if (logo && name) {
            const initial = name.trim().charAt(0).toUpperCase();
            logo.textContent = initial || "A";
        }
        if (logoName && name) {
            logoName.textContent = name;
        }
        if (logoTagline && tagline) {
            logoTagline.textContent = tagline;
        }
    }

    function loadSettings() {
        fetch("/api/system-settings")
            .then((response) => response.json())
            .then((data) => {
                form.pharmacyName.value = data.pharmacyName || "";
                form.pharmacyTagline.value = data.pharmacyTagline || "";
                form.pharmacyPhone.value = data.pharmacyPhone || "";
                form.pharmacyEmail.value = data.pharmacyEmail || "";
                form.pharmacyAddress.value = data.pharmacyAddress || "";
                invoiceSettingFields.forEach((field) => {
                    if (form[field]) {
                        form[field].value = data[field] || "";
                    }
                });
                applyLogo(data.pharmacyName || "", data.pharmacyTagline || "");
            })
            .catch(() => {});
    }

    form.addEventListener("input", () => {
        applyLogo(form.pharmacyName.value, form.pharmacyTagline.value);
    });

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        const payload = {
            pharmacyName: form.pharmacyName.value || "",
            pharmacyTagline: form.pharmacyTagline.value || "",
            pharmacyPhone: form.pharmacyPhone.value || "",
            pharmacyEmail: form.pharmacyEmail.value || "",
            pharmacyAddress: form.pharmacyAddress.value || ""
        };
        invoiceSettingFields.forEach((field) => {
            payload[field] = form[field]?.value || "";
        });

        fetch("/api/system-settings", {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        })
            .then((response) => response.json().then((data) => ({ ok: response.ok, data })))
            .then(({ ok, data }) => {
                if (!ok) {
                    throw new Error(data?.message || "Unable to save settings.");
                }
                window.AppSettings = data;
                applyLogo(data.pharmacyName || "", data.pharmacyTagline || "");
                if (window.ToastService && typeof window.ToastService.show === "function") {
                    window.ToastService.show("Settings saved successfully.", "success");
                }
            })
            .catch((error) => {
                if (window.ToastService && typeof window.ToastService.show === "function") {
                    window.ToastService.show(error.message || "Unable to save settings.", "error");
                }
            });
    });

    loadSettings();
    return root;
});

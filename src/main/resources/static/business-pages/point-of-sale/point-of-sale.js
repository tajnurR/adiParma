window.BusinessPages.register("pos", function (root) {
    let products = [];

    const cartItems = [];

    const productsContainer = root.querySelector("#pos-products");
    const cartContainer = root.querySelector("#pos-cart-items");
    const cartHeader = root.querySelector(".pos-cart-header-left");
    const emptyState = root.querySelector("#pos-empty-state");
    const clearButton = root.querySelector("#pos-clear-cart");
    const totalAmount = root.querySelector(".pos-total-amount");
    const changeAmount = root.querySelector(".pos-change-amount");
    const cashInput = root.querySelector(".pos-cash-input");
    const customerSelect = root.querySelector("#pos-customer-select");
    const posState = { selectedCustomer: null };
    root.posState = posState;

    if (!productsContainer) return root;

    function formatMoney(value) {
        return `৳${value.toFixed(2)}`;
    }

    function renderProducts() {
        productsContainer.innerHTML = products
            .map((product) => {
                return `
                <article class="pos-card" data-id="${product.id}">
                    <div class="pos-card-header">
                        <div>
                            <div class="pos-card-title">${product.brandLine}</div>
                            <div class="pos-card-subtitle">${product.genericLine}</div>
                        </div>
                    </div>
                    <div class="pos-card-footer">
                        <span class="pos-price">${formatMoney(product.price)}</span>
                        <span class="pos-stock">Stock: ${product.stock}</span>
                    </div>
                </article>
            `;
            })
            .join("");
    }

    function getDiscountedLineTotal(item) {
        const subtotal = item.price * item.qty;
        const discountValue = parseFloat(item.discountValue || "0") || 0;
        let discountAmount = 0;
        if (item.discountType === "amount") {
            discountAmount = Math.min(discountValue, Math.max(subtotal - 0.01, 0));
        } else {
            const maxPercent = subtotal > 0
                ? Math.max(((subtotal - 0.01) / subtotal) * 100, 0)
                : 0;
            const percent = Math.min(Math.max(discountValue, 0), maxPercent);
            discountAmount = subtotal * (percent / 100);
        }
        return Math.max(subtotal - discountAmount, 0);
    }

    function updateTotals() {
        const total = cartItems.reduce((sum, item) => sum + getDiscountedLineTotal(item), 0);
        if (totalAmount) totalAmount.textContent = formatMoney(total);
        const cashValue = parseFloat(cashInput?.value || "0") || 0;
        const change = Math.max(cashValue - total, 0);
        if (changeAmount) changeAmount.textContent = formatMoney(change);
    }

    function renderCart(keepFocusId) {
        if (!cartContainer || !cartHeader) return;
        cartHeader.textContent = `Cart Items (${cartItems.length})`;

        if (cartItems.length === 0) {
            cartContainer.innerHTML = "";
            if (emptyState) emptyState.style.display = "flex";
        } else {
            if (emptyState) emptyState.style.display = "none";
            cartContainer.innerHTML = cartItems
                .map((item) => {
                    const rxBadge = item.rx ? "<span class=\"pos-rx-badge\">Rx</span>" : "";
                    const lineTotal = getDiscountedLineTotal(item);
                    const discountValue = item.discountValue || "";
                    const percentActive = item.discountType !== "amount" ? "active" : "";
                    const amountActive = item.discountType === "amount" ? "active" : "";
                    return `
                        <div class="pos-cart-item" data-id="${item.id}">
                            <div class="pos-cart-info">
                                <div class="pos-cart-name">${item.name} ${rxBadge}</div>
                                <div class="pos-cart-sku">${item.subtitle}</div>
                            </div>
                            <div class="pos-cart-price">${formatMoney(item.price)}</div>
                            <div class="pos-qty-control">
                                <button class="pos-qty-btn" type="button" data-action="decrease">−</button>
                                <span class="pos-qty-value">${item.qty}</span>
                                <button class="pos-qty-btn" type="button" data-action="increase">+</button>
                            </div>
                            <div class="pos-discount-control">
                                <div class="pos-discount-toggle" role="group" aria-label="Discount type">
                                    <button type="button" class="pos-discount-btn ${percentActive}" data-action="discount-percent">%</button>
                                    <button type="button" class="pos-discount-btn ${amountActive}" data-action="discount-amount">৳</button>
                                </div>
                                <input
                                    class="pos-discount-input"
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    inputmode="decimal"
                                    value="${discountValue}"
                                    placeholder="0"
                                    data-action="discount-input"
                                    aria-label="Item discount"
                                />
                            </div>
                            <div class="pos-cart-total">${formatMoney(lineTotal)}</div>
                            <button class="pos-cart-remove" type="button" data-action="remove">🗑️</button>
                        </div>
                    `;
                })
                .join("");

            if (keepFocusId) {
                const input = cartContainer.querySelector(`.pos-cart-item[data-id="${keepFocusId}"] .pos-discount-input`);
                if (input instanceof HTMLInputElement) {
                    const length = input.value.length;
                    input.focus();
                    input.setSelectionRange(length, length);
                }
            }
        }

        updateTotals();
    }

    function addToCart(productId) {
        const product = products.find((item) => item.id === productId);
        if (!product) return;
        const existing = cartItems.find((item) => item.id === productId);
        if (existing) {
            existing.qty += 1;
        } else {
            cartItems.push({
                id: product.id,
                name: product.brandLine,
                subtitle: product.genericLine,
                price: product.price,
                qty: 1,
                discountType: "percent",
                discountValue: ""
            });
        }
        renderCart();
    }

    productsContainer.addEventListener("click", (event) => {
        const target = event.target;
        if (!(target instanceof HTMLElement)) return;
        const card = target.closest(".pos-card");
        if (!card) return;
        const id = parseInt(card.getAttribute("data-id"), 10);
        if (!Number.isNaN(id)) addToCart(id);
    });

    if (cartContainer) {
        cartContainer.addEventListener("click", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLElement)) return;
            if (target.closest(".pos-discount-input")) return;
            const row = target.closest(".pos-cart-item");
            if (!row) return;
            const id = parseInt(row.getAttribute("data-id"), 10);
            const item = cartItems.find((entry) => entry.id === id);
            if (!item) return;
            const action = target.getAttribute("data-action");
            if (!action) return;
            if (action === "increase") {
                item.qty += 1;
            }
            if (action === "decrease") {
                item.qty -= 1;
                if (item.qty <= 0) {
                    const index = cartItems.findIndex((entry) => entry.id === id);
                    if (index !== -1) cartItems.splice(index, 1);
                }
            }
            if (action === "remove") {
                const index = cartItems.findIndex((entry) => entry.id === id);
                if (index !== -1) cartItems.splice(index, 1);
            }
            if (action === "discount-percent") {
                item.discountType = "percent";
                renderCart(id);
                return;
            }
            if (action === "discount-amount") {
                item.discountType = "amount";
                renderCart(id);
                return;
            }
            renderCart();
        });

        cartContainer.addEventListener("input", (event) => {
            const target = event.target;
            if (!(target instanceof HTMLInputElement)) return;
            if (target.getAttribute("data-action") !== "discount-input") return;
            const row = target.closest(".pos-cart-item");
            if (!row) return;
            const id = parseInt(row.getAttribute("data-id"), 10);
            const item = cartItems.find((entry) => entry.id === id);
            if (!item) return;
            const rawValue = target.value;
            const numericValue = parseFloat(rawValue || "0") || 0;
            const subtotal = item.price * item.qty;
            let adjustedValue = numericValue;
            let validationMessage = "";
            if (item.discountType === "amount") {
                const maxAmount = Math.max(subtotal - 0.01, 0);
                adjustedValue = Math.min(Math.max(numericValue, 0), maxAmount);
                if (numericValue > maxAmount) {
                    validationMessage = "Discount amount cannot make total zero or negative.";
                }
            } else {
                const maxPercent = subtotal > 0
                    ? Math.max(((subtotal - 0.01) / subtotal) * 100, 0)
                    : 0;
                adjustedValue = Math.min(Math.max(numericValue, 0), maxPercent);
                if (numericValue > maxPercent) {
                    validationMessage = "Discount percent cannot make total zero or negative.";
                }
            }
            if (adjustedValue !== numericValue) {
                target.value = adjustedValue ? adjustedValue.toFixed(2).replace(/\.00$/, "") : "";
            }
            item.discountValue = target.value;
            const lineTotal = row.querySelector(".pos-cart-total");
            if (lineTotal) {
                lineTotal.textContent = formatMoney(getDiscountedLineTotal(item));
            }
            updateTotals();
            if (validationMessage && window.ToastService && typeof window.ToastService.show === "function") {
                window.ToastService.show(validationMessage, "error");
            }
        });
    }

    if (clearButton) {
        clearButton.addEventListener("click", () => {
            cartItems.splice(0, cartItems.length);
            renderCart();
        });
    }

    const paymentButtons = root.querySelectorAll(".pos-payment-btn");
    paymentButtons.forEach((button) => {
        button.addEventListener("click", () => {
            paymentButtons.forEach((btn) => btn.classList.remove("active"));
            button.classList.add("active");
        });
    });

    const cashButtons = root.querySelectorAll(".pos-cash-buttons button");
    cashButtons.forEach((button) => {
        button.addEventListener("click", () => {
            if (!cashInput) return;
            const value = button.textContent.trim();
            if (value.toLowerCase() === "exact") {
                const total = cartItems.reduce((sum, item) => sum + item.price * item.qty, 0);
                cashInput.value = total.toFixed(2);
            } else {
                cashInput.value = parseFloat(value).toFixed(2);
            }
            updateTotals();
        });
    });

    if (cashInput) {
        cashInput.addEventListener("input", updateTotals);
    }

    if (window.CustomerDropdownService && typeof window.CustomerDropdownService.loadTomSelectAssets === "function") {
        window.CustomerDropdownService.loadTomSelectAssets()
            .then(() => {
                if (!window.CustomerDropdownService || typeof window.CustomerDropdownService.initCustomerDropdown !== "function") {
                    return;
                }
                window.CustomerDropdownService.initCustomerDropdown({
                    root,
                    selectEl: customerSelect,
                    onSelect: (customer) => {
                        posState.selectedCustomer = customer;
                    },
                    onClear: () => {
                        posState.selectedCustomer = null;
                    }
                });
            })
            .catch(() => {
                if (customerSelect) {
                    customerSelect.outerHTML =
                        '<input type="text" placeholder="Search customer or type name to create new..." aria-label="Search customer" class="pos-customer-fallback" />';
                }
            });
    }

    const productSearchInput = root.querySelector(".pos-search-input");
    let productSearchTimer = null;

    function mapApiProduct(item) {
        const medicine = item.medicine || {};
        const generic = medicine.generic || {};
        const brandCode = medicine.brandCode || "";
        const brandName = medicine.brandName || "";
        const strength = medicine.strength ? ` ${medicine.strength}` : "";
        const genericCode = generic.genericCode || "";
        const genericName = generic.genericName || "";
        return {
            id: item.id,
            brandCode,
            brandName,
            brandLine: `[${brandCode}] - ${brandName}${strength}`,
            genericLine: `[${genericCode}] - ${genericName}`,
            price: Number(item.price || 0),
            stock: item.qty ?? 0
        };
    }

    function fetchProducts(query) {
        const url = `/api/medicine-stock-price-mappings?q=${encodeURIComponent(query)}`;
        return fetch(url)
            .then((response) => response.json())
            .then((data) => Array.isArray(data) ? data.map(mapApiProduct) : []);
    }

    function runProductSearch(query) {
        fetchProducts(query)
            .then((items) => {
                products = items;
                renderProducts();
            })
            .catch(() => {
                products = [];
                renderProducts();
            });
    }

    if (productSearchInput) {
        productSearchInput.addEventListener("input", (event) => {
            const value = event.target.value || "";
            if (productSearchTimer) {
                clearTimeout(productSearchTimer);
            }
            productSearchTimer = setTimeout(() => {
                if (value.trim().length >= 3) {
                    runProductSearch(value);
                } else {
                    products = [];
                    renderProducts();
                }
            }, 250);
        });
    }

    renderProducts();
    renderCart();

    return root;
});

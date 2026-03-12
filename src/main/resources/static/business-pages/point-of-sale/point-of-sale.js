window.BusinessPages.register("pos", function (root) {
    const products = [
        { id: 1, name: "Anas test", subtitle: "Anas", price: 200.0, stock: 554 },
        { id: 2, name: "Docopa 200 Mg", subtitle: "Doxiphylin 200 mg", price: 6.5, stock: 156, rx: true },
        { id: 3, name: "ALGIN", subtitle: "paracetamol", price: 10.0, stock: 991 },
        { id: 4, name: "Napa Extend", subtitle: "Paracetamol 665 mg", price: 8.0, stock: 312 },
        { id: 5, name: "Monas 10", subtitle: "Montelukast 10 mg", price: 12.5, stock: 208, rx: true },
        { id: 6, name: "Ceevit", subtitle: "Vitamin C 250 mg", price: 3.0, stock: 420 },
        { id: 7, name: "Seclo 20", subtitle: "Omeprazole 20 mg", price: 7.5, stock: 187, rx: true },
        { id: 8, name: "Fexo", subtitle: "Fexofenadine 120 mg", price: 15.0, stock: 90 },
        { id: 9, name: "Azyth 500", subtitle: "Azithromycin 500 mg", price: 18.0, stock: 74, rx: true }
    ];

    const cartItems = [];

    const productsContainer = root.querySelector("#pos-products");
    const cartContainer = root.querySelector("#pos-cart-items");
    const cartHeader = root.querySelector(".pos-cart-header-left");
    const emptyState = root.querySelector("#pos-empty-state");
    const clearButton = root.querySelector("#pos-clear-cart");
    const totalAmount = root.querySelector(".pos-total-amount");
    const changeAmount = root.querySelector(".pos-change-amount");
    const cashInput = root.querySelector(".pos-cash-input");

    if (!productsContainer) return root;

    function formatMoney(value) {
        return `$${value.toFixed(2)}`;
    }

    function renderProducts() {
        productsContainer.innerHTML = products
            .map((product) => {
                const rxBadge = product.rx ? "<span class=\"pos-rx-badge\">Rx</span>" : "";
                return `
                <article class="pos-card" data-id="${product.id}">
                    <div class="pos-card-header">
                        <div>
                            <div class="pos-card-title">${product.name}</div>
                            <div class="pos-card-subtitle">${product.subtitle}</div>
                        </div>
                        ${rxBadge}
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

    function updateTotals() {
        const total = cartItems.reduce((sum, item) => sum + item.price * item.qty, 0);
        if (totalAmount) totalAmount.textContent = formatMoney(total);
        const cashValue = parseFloat(cashInput?.value || "0") || 0;
        const change = Math.max(cashValue - total, 0);
        if (changeAmount) changeAmount.textContent = formatMoney(change);
    }

    function renderCart() {
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
                            <div class="pos-cart-total">${formatMoney(item.price * item.qty)}</div>
                            <button class="pos-cart-remove" type="button" data-action="remove">🗑️</button>
                        </div>
                    `;
                })
                .join("");
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
                name: product.name,
                subtitle: product.subtitle,
                price: product.price,
                qty: 1,
                rx: product.rx
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
            const row = target.closest(".pos-cart-item");
            if (!row) return;
            const id = parseInt(row.getAttribute("data-id"), 10);
            const item = cartItems.find((entry) => entry.id === id);
            if (!item) return;
            const action = target.getAttribute("data-action");
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
            renderCart();
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

    renderProducts();
    renderCart();

    return root;
});

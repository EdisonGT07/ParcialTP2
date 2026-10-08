(() => {
    const form = document.querySelector("#purchase-form");
    const grid = document.querySelector("#product-grid");
    const search = document.querySelector("#product-search");

    if (!form || !grid) {
        return;
    }

    const cards = Array.from(grid.querySelectorAll(".product-card"));
    const cartItems = document.querySelector("#cart-items");
    const cartEmpty = document.querySelector("#cart-empty");
    const cartBadge = document.querySelector("#cart-badge");
    const cartLines = document.querySelector("#cart-lines");
    const cartTotal = document.querySelector("#cart-total");
    const submitButton = document.querySelector("#submit-purchase");
    const purchaseError = document.querySelector("#purchase-error");
    const emptySearch = document.querySelector("#empty-search");
    const sort = document.querySelector("#sort-products");
    const currency = new Intl.NumberFormat("es-CO", { maximumFractionDigits: 0 });
    let activeCategory = "all";

    const formatCurrency = (amount) => `$ ${currency.format(amount)}`;

    const updateCart = () => {
        let itemCount = 0;
        let total = 0;
        let visibleLines = 0;
        cartItems.replaceChildren();

        cards.forEach((card) => {
            const quantityInput = card.querySelector(".quantity-input");
            const quantity = Number.parseInt(quantityInput.value, 10) || 0;
            const name = card.dataset.name || "Producto";
            const price = Number(card.dataset.price) || 0;
            const maxStock = Number(card.dataset.stock) || 0;
            const clampedQuantity = Math.min(Math.max(quantity, 0), maxStock);
            quantityInput.value = String(clampedQuantity);
            card.classList.toggle("is-selected", clampedQuantity > 0);

            card.querySelectorAll(".quantity-button").forEach((button) => {
                const step = Number(button.dataset.step);
                button.disabled = step > 0 ? clampedQuantity >= maxStock : clampedQuantity <= 0;
            });

            if (clampedQuantity === 0) {
                return;
            }

            itemCount += clampedQuantity;
            visibleLines += 1;
            total += price * clampedQuantity;

            const line = document.createElement("div");
            line.className = "cart-line";

            const productInfo = document.createElement("div");
            const productName = document.createElement("strong");
            productName.className = "cart-line__name";
            productName.textContent = name;
            const productQuantity = document.createElement("span");
            productQuantity.className = "cart-line__quantity";
            productQuantity.textContent = `${clampedQuantity} × ${formatCurrency(price)}`;
            productInfo.append(productName, productQuantity);

            const subtotal = document.createElement("strong");
            subtotal.className = "cart-line__subtotal";
            subtotal.textContent = formatCurrency(price * clampedQuantity);

            const remove = document.createElement("button");
            remove.className = "cart-line__remove";
            remove.type = "button";
            remove.textContent = "Quitar";
            remove.setAttribute("aria-label", `Quitar ${name} del pedido`);
            remove.addEventListener("click", () => {
                quantityInput.value = "0";
                updateCart();
            });

            line.append(productInfo, subtotal, remove);
            cartItems.append(line);
        });

        cartEmpty.hidden = visibleLines > 0;
        cartBadge.textContent = String(itemCount);
        cartLines.textContent = `${itemCount} ${itemCount === 1 ? "artículo" : "artículos"}`;
        cartTotal.textContent = formatCurrency(total);
        if (submitButton) {
            submitButton.disabled = itemCount === 0 || submitButton.dataset.profileMissing === "true";
        }
        if (purchaseError) {
            purchaseError.hidden = true;
        }
    };

    const applyFilters = () => {
        const term = search ? search.value.trim().toLocaleLowerCase("es") : "";
        let visibleCount = 0;
        cards.forEach((card) => {
            const category = (card.dataset.category || "").toLocaleLowerCase("es");
            const searchableText = `${card.dataset.name || ""} ${card.dataset.description || ""} ${card.dataset.category || ""}`.toLocaleLowerCase("es");
            const matchesCategory = activeCategory === "all" || category === activeCategory.toLocaleLowerCase("es");
            const matchesSearch = searchableText.includes(term);
            card.hidden = !matchesCategory || !matchesSearch;
            if (!card.hidden) {
                visibleCount += 1;
            }
        });
        if (emptySearch) {
            emptySearch.hidden = cards.length === 0 || visibleCount > 0;
        }
    };

    cards.forEach((card) => {
        const quantityInput = card.querySelector(".quantity-input");
        quantityInput.addEventListener("input", updateCart);
        quantityInput.addEventListener("change", updateCart);
        card.querySelectorAll(".quantity-button").forEach((button) => {
            button.addEventListener("click", () => {
                const current = Number.parseInt(quantityInput.value, 10) || 0;
                quantityInput.value = String(current + Number(button.dataset.step));
                updateCart();
            });
        });
    });

    document.querySelectorAll(".category-chip").forEach((button) => {
        button.addEventListener("click", () => {
            activeCategory = button.dataset.category || "all";
            document.querySelectorAll(".category-chip").forEach((chip) => {
                chip.classList.toggle("is-active", chip === button);
                chip.setAttribute("aria-pressed", String(chip === button));
            });
            applyFilters();
        });
        button.setAttribute("aria-pressed", String(button.classList.contains("is-active")));
    });

    if (search) {
        search.addEventListener("input", applyFilters);
        search.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                search.value = "";
                applyFilters();
            }
        });
    }

    if (sort) {
        sort.addEventListener("change", () => {
            const sorted = [...cards].sort((first, second) => {
                if (sort.value === "name") {
                    return (first.dataset.name || "").localeCompare(second.dataset.name || "", "es");
                }
                if (sort.value === "price-asc") {
                    return Number(first.dataset.price) - Number(second.dataset.price);
                }
                if (sort.value === "price-desc") {
                    return Number(second.dataset.price) - Number(first.dataset.price);
                }
                return Number(first.dataset.index) - Number(second.dataset.index);
            });
            sorted.forEach((card) => grid.append(card));
        });
    }

    const clearSearch = document.querySelector("#clear-search");
    if (clearSearch) {
        clearSearch.addEventListener("click", () => {
            if (search) {
                search.value = "";
            }
            activeCategory = "all";
            document.querySelectorAll(".category-chip").forEach((chip) => {
                const active = chip.dataset.category === "all";
                chip.classList.toggle("is-active", active);
                chip.setAttribute("aria-pressed", String(active));
            });
            applyFilters();
        });
    }

    form.addEventListener("submit", (event) => {
        const hasSelection = cards.some((card) => Number.parseInt(card.querySelector(".quantity-input").value, 10) > 0);
        if (!hasSelection) {
            event.preventDefault();
            if (purchaseError) {
                purchaseError.hidden = false;
                purchaseError.scrollIntoView({ behavior: "smooth", block: "center" });
            }
        }
    });

    document.addEventListener("keydown", (event) => {
        if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
            event.preventDefault();
            if (search) {
                search.focus();
            }
        }
    });

    applyFilters();
    updateCart();
})();

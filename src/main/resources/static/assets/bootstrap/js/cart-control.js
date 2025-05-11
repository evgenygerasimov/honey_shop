document.addEventListener("DOMContentLoaded", function () {
    let cart = JSON.parse(localStorage.getItem("cart")) || [];
    const cartCount = document.getElementById("cart-count");
    const cartTotal = document.getElementById("cart-total");
    const cartItems = document.getElementById("cart-items");
    const checkoutBtn = document.getElementById("checkout-btn");

    function updateCartUI() {
        const totalCount = cart.reduce((sum, item) => sum + item.quantity, 0);
        const totalPrice = cart.reduce((sum, item) => sum + item.quantity * item.price, 0);
        cartCount.textContent = totalCount;
        cartTotal.textContent = totalPrice.toFixed(2);
        document.getElementById("total-price").textContent = totalPrice.toFixed(2);

        cartItems.innerHTML = cart.length ? "" : "<tr><td colspan='5' class='text-center'>Ваша корзина пуста</td></tr>";
        cart.forEach(item => {
            const row = document.createElement("tr");
            row.innerHTML = `
                        <td>${item.name}</td>
                        <td><input type="number" value="${item.quantity}" min="1" max="100" class="form-control quantity" data-product-id="${item.productId}"></td>
                        <td>${item.price} ₽</td>
                        <td>${(item.quantity * item.price).toFixed(2)} ₽</td>
                        <td><button class="btn btn-danger btn-sm delete-btn" data-product-id="${item.productId}">Удалить</button></td>
                    `;
            cartItems.appendChild(row);
        });

        checkoutBtn.disabled = cart.length === 0;
    }

    cartItems.addEventListener("click", function (e) {
        if (e.target.classList.contains("delete-btn")) {
            const productId = e.target.getAttribute("data-product-id");
            cart = cart.filter(item => item.productId !== productId);
            localStorage.setItem("cart", JSON.stringify(cart));
            updateCartUI();
        }
    });

    cartItems.addEventListener("change", function (e) {
        if (e.target.classList.contains("quantity")) {
            const productId = e.target.getAttribute("data-product-id");
            const quantity = parseInt(e.target.value);
            if (quantity > 0 && quantity <= 100) {
                const item = cart.find(item => item.productId === productId);
                if (item) {
                    item.quantity = quantity;
                    localStorage.setItem("cart", JSON.stringify(cart));
                    updateCartUI();
                }
            }
        }
    });

    checkoutBtn.addEventListener('click', function () {
        localStorage.setItem("checkout-cart", JSON.stringify(cart));
    });
    updateCartUI();
});

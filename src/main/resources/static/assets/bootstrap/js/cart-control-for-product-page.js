document.addEventListener("DOMContentLoaded", function () {
    let cart = JSON.parse(localStorage.getItem("cart")) || [];

    function updateCartUI() {
        let totalCount = cart.reduce((sum, item) => sum + item.quantity, 0);
        let totalPrice = cart.reduce((sum, item) => sum + item.quantity * item.price, 0);

        document.getElementById("cart-count").textContent = totalCount;
        document.getElementById("cart-total").textContent = totalPrice.toFixed(2);
    }

    function addToCart(productId, name, price, quantity) {
        let existingItem = cart.find(item => item.productId === productId);
        if (existingItem) {
            existingItem.quantity += quantity;
        } else {
            cart.push({ productId, name, price, quantity });
        }
        localStorage.setItem("cart", JSON.stringify(cart));
        updateCartUI();
    }

    // Обработчик кнопок выбора количества товара
    let quantityInput = document.getElementById('quantityInput');
    document.getElementById('decreaseBtn').addEventListener('click', function () {
        let value = parseInt(quantityInput.value);
        if (value > 1) {
            quantityInput.value = value - 1;
        }
    });

    document.getElementById('increaseBtn').addEventListener('click', function () {
        let value = parseInt(quantityInput.value);
        if (value < 100) {
            quantityInput.value = value + 1;
        }
    });

    // Обработчик кнопки "Добавить в корзину"
    document.getElementById("addToCartBtn").addEventListener("click", function () {
        let productId = this.getAttribute("data-id"); // Получаем ID товара из атрибута data-id
        let name = this.getAttribute("data-name"); // Получаем имя товара из атрибута data-name
        let price = parseFloat(this.getAttribute("data-price")); // Получаем цену товара из атрибута data-price
        let quantity = parseInt(quantityInput.value);

        addToCart(productId, name, price, quantity);
        showNotification();
    });

    // Обновляем корзину каждый раз при загрузке страницы (включая переходы назад)
    window.addEventListener("pageshow", function (event) {
        let historyTraversal = event.persisted || (typeof window.performance != "undefined" && window.performance.navigation.type === 2);
        if (historyTraversal) {
            // Если это переход через историю (кнопка "назад")
            cart = JSON.parse(localStorage.getItem("cart")) || [];  // Считываем актуальные данные из localStorage
        }

        updateCartUI();
    });

    updateCartUI();  // Обновляем UI при загрузке страницы
});

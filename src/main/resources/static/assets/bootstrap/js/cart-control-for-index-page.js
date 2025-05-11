document.addEventListener("DOMContentLoaded", function () {
    // Настройка кнопок изменения количества
    document.querySelectorAll(".d-flex").forEach(group => {
        let quantityInput = group.querySelector(".quantityInput");
        let decreaseBtn = group.querySelector(".decreaseBtn");
        let increaseBtn = group.querySelector(".increaseBtn");

        if (quantityInput && decreaseBtn && increaseBtn) {  // Проверяем, что элементы существуют
            decreaseBtn.addEventListener("click", function () {
                let value = parseInt(quantityInput.value);
                if (value > 1) {
                    quantityInput.value = value - 1;
                }
            });

            increaseBtn.addEventListener("click", function () {
                let value = parseInt(quantityInput.value);
                if (value < 100) {
                    quantityInput.value = value + 1;
                }
            });
        }
    });

    // Инициализация корзины
    let cart = JSON.parse(localStorage.getItem("cart")) || [];

    function updateCartUI() {
        let totalCount = cart.reduce((sum, item) => sum + item.quantity, 0);
        let totalPrice = cart.reduce((sum, item) => sum + item.quantity * item.price, 0);

        document.getElementById("cart-count").textContent = totalCount;
        document.getElementById("cart-total").textContent = totalPrice.toFixed(2);
    }

    function addToCart(productId, name, price, quantity, weight, width, height, length) {
        let existingItem = cart.find(item => item.productId === productId);

        if (existingItem) {
            existingItem.quantity += quantity;
        } else {
            cart.push({
                productId,
                name,
                price,
                quantity,
                weight,  // Добавляем вес
                width,   // Добавляем ширину
                height,  // Добавляем высоту
                length   // Добавляем длину
            });
        }

        localStorage.setItem("cart", JSON.stringify(cart));
        updateCartUI();
    }


    function clearCart() {
        localStorage.removeItem("cart");
        cart = [];  // Очистка локального массива
        console.log("Корзина очищена после успешного платежа!");
        updateCartUI();
    }


    // Назначаем обработчик на кнопки "Добавить в корзину"
    document.querySelectorAll(".btn-success").forEach(button => {
        button.addEventListener("click", function () {
            let card = this.closest(".card");
            let productId = card.getAttribute("data-product-id");
            let name = card.querySelector(".card-title").textContent;
            let price = parseFloat(card.querySelector(".product-price").textContent.replace("Цена: ", "").trim());
            let quantity = parseInt(card.querySelector(".quantityInput").value);
            let weight = parseFloat(card.querySelector("#product-weight").value);
            let width = parseFloat(card.querySelector("#product-width").value);
            let height = parseFloat(card.querySelector("#product-height").value);
            let length = parseFloat(card.querySelector("#product-length").value);

            addToCart(productId, name, price, quantity, weight, width, height, length);
        });
    });

    // Проверка статуса платежа при загрузке страницы
    fetch('/payments/check-payment-status')
        .then(response => response.json())
        .then(data => {
            if (data.paymentSuccess) {
                clearCart();

                fetch('/payments/clear-cart', {method: 'POST'})
                    .then(response => {
                        if (response.ok) {
                            console.log('Флаг успешной оплаты сброшен в Redis');
                        } else {
                            console.error('Ошибка при сбросе флага успешной оплаты');
                        }
                    });
            }
        })
        .catch(error => {
            console.error('Ошибка при проверке статуса платежа:', error);
        });

    updateCartUI();  // Обновляем UI при загрузке страницы
});

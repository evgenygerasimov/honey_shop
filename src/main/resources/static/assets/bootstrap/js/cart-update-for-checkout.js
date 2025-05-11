document.addEventListener("DOMContentLoaded", function () {
    let cart = JSON.parse(localStorage.getItem("cart")) || [];

    function updateCartUI() {
        let totalCount = cart.reduce((sum, item) => sum + item.quantity, 0);
        let totalPrice = cart.reduce((sum, item) => sum + item.quantity * item.price, 0);

        document.getElementById("cart-count").textContent = totalCount;
        document.getElementById("cart-total").textContent = totalPrice.toFixed(2);
    }

    updateCartUI();
});
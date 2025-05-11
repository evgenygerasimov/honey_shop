document.addEventListener("DOMContentLoaded", function () {
    const checkoutBtn = document.getElementById("checkout-btn");

    if (checkoutBtn) {
        checkoutBtn.addEventListener("click", function (event) {
            const currentCart = JSON.parse(localStorage.getItem("cart")) || [];

            if (currentCart.length === 0) {
                event.preventDefault();
                showEmptyCartNotification();
            }
        });
    }

    function showEmptyCartNotification() {
        const notification = document.getElementById('emptyCartNotification');
        notification.style.display = 'block';

        setTimeout(function () {
            notification.style.display = 'none';
        }, 3000);
    }
});
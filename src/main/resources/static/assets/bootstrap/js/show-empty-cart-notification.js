document.getElementById("cart-button").addEventListener("click", function (event) {
    const currentCart = JSON.parse(localStorage.getItem("cart")) || [];
    if (currentCart.length === 0) {
        event.preventDefault();
        showEmptyCartNotification();
    }
});

function showEmptyCartNotification() {
    let notification = document.getElementById('emptyCartNotification');
    notification.style.display = 'block';
    setTimeout(function () {
        notification.style.display = 'none';
    }, 3000);
}

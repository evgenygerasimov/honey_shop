function showNotification() {
    let notification = document.getElementById('addToCartNotification');
    notification.style.display = 'block';

    setTimeout(function () {
        notification.style.display = 'none';
    }, 3000);
}
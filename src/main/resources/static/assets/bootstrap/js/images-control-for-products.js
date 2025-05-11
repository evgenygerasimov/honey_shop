
    function deleteImage(button) {
    const imageFilename = button.getAttribute('data-filename');
    const productId = button.getAttribute('data-product-id');
    const formData = new URLSearchParams();
    formData.append('imageFilename', imageFilename); // строка
    formData.append('productId', productId); // строка

    fetch('/products/delete-image', {
    method: 'POST',
    body: formData
})
    .then(response => {
    if (response.ok) {
    location.reload();
} else {
    alert("Ошибка удаления изображения.");
}
})
    .catch(error => console.error('Ошибка:', error));
}

    document.addEventListener('DOMContentLoaded', function () {
    const imageList = document.getElementById('existing-images');

    // Инициализация Sortable
    const sortable = new Sortable(imageList, {
    handle: 'img', // Указываем, что можно перетаскивать изображения
    onEnd: function (evt) {
    updateImageOrder();
    console.log("Изображения переставлены")// Обновляем порядок после перетаскивания
}
});

    // Функция для обновления порядка изображений
    function updateImageOrder() {
    const orderedImages = Array.from(imageList.children)
    .map(item => {
    const imageNameElement = item.querySelector('span');
    return imageNameElement ? imageNameElement.textContent : '';  // Считываем имя файла
}).filter(image => image !== '');
    document.getElementById('image-order').value = orderedImages.join(',');  // Обновляем скрытое поле
}
});


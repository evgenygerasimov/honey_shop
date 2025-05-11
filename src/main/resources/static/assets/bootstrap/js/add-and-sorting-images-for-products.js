document.addEventListener('DOMContentLoaded', function () {
    const imageInput = document.getElementById('pictures');
    const imagePreview = document.getElementById('imagePreview');
    const imageOrderInput = document.getElementById('imageOrder');

    // Функция для отображения изображений
    function displayImages(files) {
        imagePreview.innerHTML = '';  // Очищаем предыдущие изображения

        Array.from(files).forEach((file, index) => {
            const reader = new FileReader();
            reader.onload = function (e) {
                const imgWrapper = document.createElement('div');
                imgWrapper.classList.add('image-wrapper');
                imgWrapper.dataset.index = index;

                const img = document.createElement('img');
                img.src = e.target.result;
                img.classList.add('img-thumbnail');
                img.style.width = '100px';  // Устанавливаем размер изображения

                const removeButton = document.createElement('button');
                removeButton.textContent = 'Удалить';
                removeButton.classList.add('btn', 'btn-danger', 'btn-sm');
                removeButton.onclick = function () {
                    imgWrapper.remove();
                    updateImageOrder();
                };

                imgWrapper.appendChild(img);
                imgWrapper.appendChild(removeButton);
                imagePreview.appendChild(imgWrapper);
            };
            reader.readAsDataURL(file);
        });

        // Обновляем порядок изображений
        updateImageOrder();
    }

    // Обработчик изменения файлов
    imageInput.addEventListener('change', function (e) {
        displayImages(e.target.files);
    });

    // Функция для обновления порядка изображений
    function updateImageOrder() {
        const images = document.querySelectorAll('.image-wrapper');
        const order = Array.from(images).map(img => img.dataset.index);
        imageOrderInput.value = order.join(',');
    }

    // Инициализация перетаскивания изображений
    new Sortable(imagePreview, {
        onEnd: function () {
            updateImageOrder();  // Обновляем порядок после перемещения
        }
    });
});
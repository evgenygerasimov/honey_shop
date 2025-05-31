function deleteImage(button) {
    const imageFilename = button.getAttribute('data-filename');
    const categoryId = button.getAttribute('data-category-id');
    const formData = new URLSearchParams();
    formData.append('imageFilename', imageFilename);
    formData.append('categoryId', categoryId);

    fetch('/categories/delete-image', {
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
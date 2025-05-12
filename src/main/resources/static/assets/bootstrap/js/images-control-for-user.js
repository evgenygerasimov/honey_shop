function deleteImage(button) {
    const imageFilename = button.getAttribute('data-filename');
    const userId = button.getAttribute('data-user-id');
    const formData = new URLSearchParams();
    formData.append('imageFilename', imageFilename);
    formData.append('userId',userId);

    fetch('/users/delete-image', {
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
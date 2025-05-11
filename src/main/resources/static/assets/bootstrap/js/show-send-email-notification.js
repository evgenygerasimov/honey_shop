document.addEventListener("DOMContentLoaded", function () {
    const emailBtn = document.getElementById("email-btn");
    const contactForm = document.getElementById("contactForm");

    if (emailBtn) {
        emailBtn.addEventListener("click", function (event) {
            if (!validateForm()) {
                event.preventDefault();
            } else {
                event.preventDefault();
                sendFormData();
            }
        });
    }

    function sendFormData() {
        const formData = new FormData(contactForm);

        fetch('/contacts/send', {
            method: 'POST',
            body: formData
        })
            .then(response => {
                if (response.ok) {
                    showNotification();
                } else {
                    alert('Ошибка при отправке сообщения');
                }
            })
            .catch(error => {
                console.error('Ошибка:', error);
                alert('Ошибка при отправке сообщения');
            });
    }

    function showNotification() {
        let notification = document.getElementById('emailNotification');
        notification.style.display = 'block';

        setTimeout(function () {
            notification.style.display = 'none';
        }, 3000);
    }
});

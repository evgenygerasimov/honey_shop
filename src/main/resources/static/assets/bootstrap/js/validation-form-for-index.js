function validateForm() {
    let isValid = true;

    // Валидация email
    const email = document.getElementById("email-1").value;
    const emailPattern = /^(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    const emailError = document.getElementById("emailError");

    if (!emailPattern.test(email)) {
        emailError.style.display = "block";
        isValid = false;
    } else {
        emailError.style.display = "none";
    }

    // Валидация телефона
    const phone = document.getElementById("phone-1").value;
    const phonePattern = /^\+7 \(\d{3}\) \d{3}-\d{2}-\d{2}$/;
    const phoneError = document.getElementById("phoneError");

    if (!phonePattern.test(phone)) {
        phoneError.style.display = "block";
        isValid = false;
    } else {
        phoneError.style.display = "none";
    }

    // Валидация сообщения
    const messageInput = document.getElementById('message-1');
    const messageError = document.getElementById('messageError');
    const messageLength = messageInput.value.length;
    if (messageLength < 3 || messageLength > 300) {
        messageError.style.display = 'block';
        isValid = false;
    } else {
        messageError.style.display = 'none';
    }

    // Валидация имени
    const nameInput = document.getElementById('name-1');
    const nameError = document.getElementById('nameError');
    const nameLength = nameInput.value.length;
    if (nameLength < 3 || nameLength > 50) {
        nameError.style.display = 'block';
        isValid = false;
    } else {
        nameError.style.display = 'none';
    }

    // Валидация чекбокса согласия
    const consentCheckbox = document.getElementById("consentCheckbox");
    const consentError = document.getElementById("consentError");
    if (!consentCheckbox.checked) {
        consentError.style.display = "block";
        isValid = false;
    } else {
        consentError.style.display = "none";
    }

    return isValid;
}

const categoriesContainer = document.getElementById('categories');
new Sortable(categoriesContainer, {
    animation: 150,
    ghostClass: 'sortable-ghost',
    handle: '.category-item',
});

document.querySelectorAll('.products').forEach(productsEl => {
    new Sortable(productsEl, {
        animation: 150,
        ghostClass: 'sortable-ghost',
        handle: '.product-item',
    });
});

document.getElementById('saveOrderBtn').addEventListener('click', () => {
    const categoryOrder = [];
    const productOrder = {};

    categoriesContainer.querySelectorAll('.category-item').forEach(catEl => {
        const catId = catEl.getAttribute('data-category-id');
        categoryOrder.push(catId);

        const products = [];
        catEl.querySelectorAll('.product-item').forEach(prodEl => {
            products.push(prodEl.getAttribute('data-product-id'));
        });
        productOrder[catId] = products;
    });

    fetch('/showcase/reorder', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({categoryOrder, productOrder})
    }).then(response => {
        if (response.ok) {
            alert('Порядок сохранён!');
        } else {
            alert('Ошибка сохранения порядка!');
        }
    }).catch(() => alert('Ошибка сети!'));
});
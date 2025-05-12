let selectedDeliveryCost = 0; // Переменная для хранения выбранной стоимости доставки

function initCDEKWidget(totalVolume, totalWeight) {
    new window.CDEKWidget({
        from: {
            country_code: 'RU',
            city: 'Димитровград',
            postal_code: 433513,
            code: 2350,
            address: 'пр-т. Автостроителей, 67, 2',
        },
        root: 'cdek-map',
        apiKey: '42be93db-f498-4812-89f4-dab05715e2fc',
        canChoose: true,
        servicePath: 'https://localhost:8443/cdek/offices',
        hideFilters: {
            have_cashless: false,
            have_cash: false,
            is_dressing_room: false,
            type: false,
        },
        hideDeliveryOptions: {
            office: false,
            door: false,
        },
        debug: false,
        goods: [
            {
                width: totalVolume.width,
                height: totalVolume.height,
                length: totalVolume.length,
                weight: totalWeight,
            },
        ],
        defaultLocation: [37.616173, 55.75628],
        lang: 'rus',
        currency: 'RUB',
        tariffs: {
            office: [234, 136, 138],
            door: [233, 137, 139],
        },
        onChoose: function (deliveryMode, tariff, address) {
            selectedDeliveryCost = tariff.delivery_sum;

            // Формирование адреса
            let fullAddress = address.country_code + ", ";
            if (deliveryMode === 'office' && address.type !== 'POSTAMAT') {
                fullAddress += address.city + ", " + address.address;
            } else if (address.type === 'POSTAMAT') {
                fullAddress += address.city + ", " + address.address;
            } else if (deliveryMode === 'door') {
                fullAddress += address.formatted;
            }

            // Тип доставки
            let deliveryType;
            if (deliveryMode === 'office' && address.type !== 'POSTAMAT') {
                deliveryType = "До ПВЗ";
            } else if (deliveryMode === 'door') {
                deliveryType = "До двери";
            } else if (address.type === 'POSTAMAT') {
                deliveryType = "До постамата";
            }

            // Установка значений в скрытые поля
            document.getElementById("deliveryAddress").value = fullAddress;
            document.getElementById("deliveryType").value = deliveryType;
            updateTotalPrice();
        },
    });
}

function updateTotalPrice() {
    let productTotal = parseFloat(document.getElementById("product-total").innerText) || 0;
    let deliveryCost = selectedDeliveryCost || 0;

    document.getElementById("delivery-cost").innerText = deliveryCost;

    let total = productTotal + deliveryCost;
    document.getElementById("total-price").innerText =
        "Общая стоимость заказа с доставкой: " + total + " рублей.";

    document.getElementById("productAmount").value = productTotal.toFixed(2);
    document.getElementById("deliveryAmount").value = deliveryCost.toFixed(2);
    document.getElementById("totalOrderAmount").value = total.toFixed(2);
}

document.addEventListener("DOMContentLoaded", function () {
    let cart = JSON.parse(localStorage.getItem("cart")) || [];

    let totalWeight = 0;
    let totalVolume = { width: 0, height: 0, length: 0 };
    let productTotal = 0;
    let orderItemsData = [];

    cart.forEach(item => {
        totalWeight += (item.weight || 0) * item.quantity;

        totalVolume.width = Math.max(totalVolume.width, item.width || 0);
        totalVolume.height += (item.height || 0) * item.quantity;
        totalVolume.length = Math.max(totalVolume.length, item.length || 0);

        productTotal += item.quantity * item.price;

        orderItemsData.push({
            order: {
                orderId: this.orderId,
            },
            product: {
                productId: item.productId
            },
            quantity: item.quantity,
            pricePerUnit: item.price
        });
    });

    document.getElementById('orderItemsInput').value = JSON.stringify(orderItemsData);
    document.getElementById("product-total").innerText = productTotal.toFixed(2);

    initCDEKWidget(totalVolume, totalWeight);
    updateTotalPrice();
});
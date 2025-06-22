const toggleBtn = document.getElementById('toggleSidebar');
const sidebar = document.getElementById('sidebar');
const backdrop = document.getElementById('sidebarBackdrop');

toggleBtn.addEventListener('click', () => {
    sidebar.classList.toggle('active');
    backdrop.classList.toggle('active');
});

backdrop.addEventListener('click', () => {
    sidebar.classList.remove('active');
    backdrop.classList.remove('active');
});
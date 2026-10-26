/* ReBook - script dùng chung */
const RB = (() => {
    function csrfToken() {
        const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
        return m ? decodeURIComponent(m[1]) : "";
    }

    /** fetch có kèm CSRF token (dùng cho các thao tác AJAX) */
    async function post(url, data) {
        const res = await fetch(url, {
            method: "POST",
            headers: {"X-XSRF-TOKEN": csrfToken(), "Content-Type": "application/x-www-form-urlencoded", "Accept": "application/json"},
            body: new URLSearchParams(data || {})
        });
        if (res.redirected && res.url.includes("/auth/login")) {
            window.location.href = "/auth/login?redirect=" + encodeURIComponent(location.pathname + location.search);
            throw new Error("login");
        }
        const json = await res.json().catch(() => ({}));
        if (!res.ok) throw new Error(json.error || "Có lỗi xảy ra, vui lòng thử lại");
        return json;
    }

    function toast(message, type = "success", link) {
        let wrap = document.querySelector(".rb-toast-wrap");
        if (!wrap) {
            wrap = document.createElement("div");
            wrap.className = "rb-toast-wrap";
            document.body.appendChild(wrap);
        }
        const el = document.createElement("div");
        const icon = type === "success" ? "check-circle-fill" : type === "info" ? "bell-fill" : "exclamation-triangle-fill";
        el.className = `toast align-items-center text-bg-${type === "info" ? "dark" : type} border-0 mb-2`;
        el.setAttribute("role", "alert");
        el.innerHTML = `<div class="d-flex"><div class="toast-body"><i class="bi bi-${icon} me-2"></i><span></span></div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
        el.querySelector("span").textContent = message;
        if (link) {
            el.querySelector(".toast-body").classList.add("cursor-pointer");
            el.querySelector(".toast-body").addEventListener("click", () => location.href = link);
        }
        wrap.appendChild(el);
        const t = new bootstrap.Toast(el, {delay: 5000});
        t.show();
        el.addEventListener("hidden.bs.toast", () => el.remove());
    }

    function setBadge(selector, value) {
        document.querySelectorAll(selector).forEach(b => {
            b.textContent = value;
            b.classList.toggle("d-none", !value || value === "0");
        });
    }

    // ===== WebSocket: thông báo & chat realtime =====
    let stomp = null;
    const chatListeners = [];

    function connectSocket() {
        if (!document.body.dataset.loggedIn || typeof SockJS === "undefined") return;
        stomp = new StompJs.Client({
            webSocketFactory: () => new SockJS("/ws"),
            reconnectDelay: 5000,
            debug: () => {}
        });
        stomp.onConnect = () => {
            stomp.subscribe("/user/queue/notify", frame => {
                const data = JSON.parse(frame.body);
                toast(data.message, "info", data.link || null);
                const badge = document.querySelector(".js-notify-badge");
                const current = parseInt(badge && badge.textContent || "0", 10) || 0;
                setBadge(".js-notify-badge", current + 1);
            });
            stomp.subscribe("/user/queue/chat", frame => {
                const msg = JSON.parse(frame.body);
                chatListeners.forEach(fn => fn(msg));
            });
        };
        stomp.activate();
    }

    function sendChat(storeId, customerId, content) {
        if (!stomp || !stomp.connected) {
            toast("Đang kết nối lại máy chủ chat...", "warning");
            return false;
        }
        stomp.publish({destination: "/app/chat.send", body: JSON.stringify({storeId, customerId, content})});
        return true;
    }

    function onChat(fn) { chatListeners.push(fn); }

    // ===== Giỏ hàng & yêu thích (AJAX) =====
    function bindActions() {
        document.addEventListener("click", async e => {
            const addBtn = e.target.closest(".js-add-cart");
            if (addBtn) {
                e.preventDefault();
                const qtyInput = addBtn.dataset.qtyInput ? document.querySelector(addBtn.dataset.qtyInput) : null;
                try {
                    const res = await post("/cart/add", {productId: addBtn.dataset.id, quantity: qtyInput ? qtyInput.value : 1});
                    setBadge(".js-cart-badge", res.cartCount);
                    toast(res.message);
                    if (addBtn.dataset.buyNow) location.href = "/cart";
                } catch (err) {
                    if (err.message !== "login") toast(err.message, "danger");
                }
            }
            const favBtn = e.target.closest(".js-favorite");
            if (favBtn) {
                e.preventDefault();
                try {
                    const res = await post("/user/favorites/toggle", {productId: favBtn.dataset.id});
                    favBtn.classList.toggle("active", res.favorite);
                    const icon = favBtn.querySelector("i");
                    if (icon) icon.className = res.favorite ? "bi bi-heart-fill text-danger" : "bi bi-heart";
                    toast(res.favorite ? "Đã thêm vào yêu thích" : "Đã bỏ yêu thích");
                } catch (err) {
                    if (err.message !== "login") toast(err.message, "danger");
                }
            }
        });

        // Xác nhận trước các thao tác quan trọng
        document.addEventListener("submit", e => {
            const msg = e.target.dataset.confirm;
            if (msg && !confirm(msg)) e.preventDefault();
        });

        // Ẩn/hiện sidebar dashboard trên mobile
        document.querySelectorAll(".js-sidebar-toggle").forEach(b =>
            b.addEventListener("click", () => document.querySelector(".rb-sidebar").classList.toggle("show")));
    }

    document.addEventListener("DOMContentLoaded", () => {
        bindActions();
        connectSocket();
        document.querySelectorAll("[data-flash]").forEach(el => toast(el.dataset.flash, el.dataset.type || "success"));
    });

    return {post, toast, sendChat, onChat, setBadge};
})();

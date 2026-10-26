package vn.hcmute.rebook.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.entity.*;
import vn.hcmute.rebook.entity.enums.*;
import vn.hcmute.rebook.repository.*;
import vn.hcmute.rebook.util.SlugUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/** Tạo dữ liệu mẫu khi database còn trống (tài khoản demo, cửa hàng, sách, đơn hàng 12 tháng) */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    public static final String DEMO_PASSWORD = "123456a";

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CarrierRepository carrierRepository;
    private final VoucherRepository voucherRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    @Value("${app.seed-data:true}")
    private boolean enabled;

    private final Random random = new Random(17);

    /** Các câu lệnh lùi ngày tạo, chạy sau khi Hibernate flush để không bị ghi đè */
    private final List<Object[]> deferredUpdates = new ArrayList<>();

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || userRepository.count() > 0) return;
        log.info("Đang tạo dữ liệu mẫu...");
        String pw = passwordEncoder.encode(DEMO_PASSWORD);

        user("Quản Trị Viên", "admin@rebook.vn", "0900000001", Role.ADMIN, pw, 400);
        user("Lê Minh Quản", "manager@rebook.vn", "0900000002", Role.MANAGER, pw, 380);
        User shipper1 = user("Phạm Văn Giao", "shipper1@rebook.vn", "0900000003", Role.SHIPPER, pw, 300);
        User shipper2 = user("Trần Thị Nhanh", "shipper2@rebook.vn", "0900000004", Role.SHIPPER, pw, 250);

        String[][] storeInfo = {
                {"ReBook Thủ Đức", "1 Võ Văn Ngân, Linh Chiểu, Thủ Đức, TP.HCM", "0281234561", "Nguyễn Thành Đạt", "vendor1@rebook.vn",
                        "Cửa hàng đầu tiên của chuỗi ReBook, gần làng đại học. Chuyên giáo trình, sách kỹ năng và văn học."},
                {"ReBook Quận 1", "45 Nguyễn Thị Minh Khai, Bến Nghé, Quận 1, TP.HCM", "0281234562", "Võ Hoàng Yến", "vendor2@rebook.vn",
                        "Không gian sách cũ giữa trung tâm thành phố, nhiều sách văn học nước ngoài và sách hiếm."},
                {"Sách Cũ Gò Vấp", "120 Quang Trung, Phường 10, Gò Vấp, TP.HCM", "0281234563", "Đặng Quốc Bảo", "vendor3@rebook.vn",
                        "Truyện tranh, sách thiếu nhi và sách ngoại ngữ giá sinh viên."},
                {"Nhà Sách Xưa Bình Thạnh", "88 Đinh Tiên Hoàng, Phường 1, Bình Thạnh, TP.HCM", "0281234564", "Huỳnh Mai Anh", "vendor4@rebook.vn",
                        "Sách lịch sử, khoa học và các ấn bản xưa được tuyển chọn kỹ."}};
        List<Store> stores = new ArrayList<>();
        for (int i = 0; i < storeInfo.length; i++) {
            String[] s = storeInfo[i];
            User owner = user(s[3], s[4], "09110000" + (10 + i), Role.VENDOR, pw, 360 - i * 40);
            Store store = storeRepository.save(Store.builder().owner(owner).name(s[0]).address(s[1]).phone(s[2])
                    .description(s[5]).status(StoreStatus.ACTIVE).commissionRate(5 + i % 2).build());
            backdate("stores", store.getId(), 355 - i * 40);
            stores.add(store);
        }
        // Một cửa hàng đang chờ duyệt để demo chức năng duyệt shop
        User pendingOwner = user("Lý Gia Hân", "vendor5@rebook.vn", "0911000099", Role.USER, pw, 5);
        Store pending = storeRepository.save(Store.builder().owner(pendingOwner).name("Góc Sách Tân Bình")
                .address("15 Cộng Hòa, Tân Bình, TP.HCM").phone("0281234565")
                .description("Sách cũ đủ thể loại cho dân văn phòng.").status(StoreStatus.PENDING).commissionRate(5).build());
        backdate("stores", pending.getId(), 2);

        List<User> customers = new ArrayList<>();
        String[][] customerInfo = {
                {"Nguyễn Văn An", "user1@rebook.vn"}, {"Trần Thị Bình", "user2@rebook.vn"}, {"Lê Hoàng Cường", "user3@rebook.vn"},
                {"Phạm Ngọc Dung", "user4@rebook.vn"}, {"Hoàng Gia Huy", "user5@rebook.vn"}, {"Vũ Thanh Lan", "user6@rebook.vn"}};
        for (int i = 0; i < customerInfo.length; i++) {
            customers.add(user(customerInfo[i][0], customerInfo[i][1], "09220000" + (10 + i), Role.USER, pw, 340 - i * 30));
        }
        String[] lastNames = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Võ", "Đặng", "Bùi", "Đỗ", "Ngô"};
        String[] firstNames = {"Minh Anh", "Gia Bảo", "Thu Hà", "Đức Huy", "Khánh Linh", "Quốc Nam", "Bảo Ngọc", "Tuấn Kiệt", "Mai Phương", "Hải Đăng"};
        for (int i = 0; i < 40; i++) {
            customers.add(user(lastNames[i % 10] + " " + firstNames[(i * 3) % 10], "khach" + (i + 1) + "@rebook.vn",
                    "093" + String.format("%07d", i + 1), Role.USER, pw, random.nextInt(360)));
        }
        String[][] addr = {{"12 Lê Văn Việt", "Hiệp Phú", "Thủ Đức", "TP. Hồ Chí Minh"}, {"34 Trần Hưng Đạo", "Phạm Ngũ Lão", "Quận 1", "TP. Hồ Chí Minh"},
                {"56 Nguyễn Trãi", "Thượng Đình", "Thanh Xuân", "Hà Nội"}, {"78 Hùng Vương", "Hải Châu 1", "Hải Châu", "Đà Nẵng"}};
        for (int i = 0; i < customers.size(); i++) {
            User c = customers.get(i);
            String[] a = addr[i % addr.length];
            addressRepository.save(Address.builder().user(c).receiverName(c.getFullName()).phone(c.getPhone())
                    .detail(a[0]).ward(a[1]).district(a[2]).province(a[3]).defaultAddress(true).build());
        }
        addressRepository.save(Address.builder().user(customers.get(0)).receiverName("Nguyễn Văn An (công ty)").phone("0922000099")
                .detail("1 Võ Văn Ngân").ward("Linh Chiểu").district("Thủ Đức").province("TP. Hồ Chí Minh").defaultAddress(false).build());

        String[][] cats = {
                {"Văn học Việt Nam", "bi-feather"}, {"Văn học nước ngoài", "bi-globe2"}, {"Kinh tế - Kỹ năng", "bi-graph-up"},
                {"Thiếu nhi", "bi-balloon"}, {"Giáo trình - Tham khảo", "bi-mortarboard"}, {"Khoa học - Lịch sử", "bi-hourglass-split"},
                {"Truyện tranh", "bi-emoji-laughing"}, {"Ngoại ngữ", "bi-translate"}};
        List<Category> categories = new ArrayList<>();
        for (String[] c : cats) {
            categories.add(categoryRepository.save(Category.builder().name(c[0]).slug(SlugUtils.toSlug(c[0])).icon(c[1])
                    .description("Sách cũ thể loại " + c[0].toLowerCase()).build()));
        }

        Object[][] books = {
                {"Dế Mèn Phiêu Lưu Ký", "Tô Hoài", "NXB Kim Đồng", 2019, 0, 60000},
                {"Số Đỏ", "Vũ Trọng Phụng", "NXB Văn Học", 2018, 0, 75000},
                {"Tắt Đèn", "Ngô Tất Tố", "NXB Văn Học", 2017, 0, 55000},
                {"Cho Tôi Xin Một Vé Đi Tuổi Thơ", "Nguyễn Nhật Ánh", "NXB Trẻ", 2020, 0, 80000},
                {"Mắt Biếc", "Nguyễn Nhật Ánh", "NXB Trẻ", 2019, 0, 110000},
                {"Tôi Thấy Hoa Vàng Trên Cỏ Xanh", "Nguyễn Nhật Ánh", "NXB Trẻ", 2018, 0, 125000},
                {"Nỗi Buồn Chiến Tranh", "Bảo Ninh", "NXB Trẻ", 2016, 0, 98000},
                {"Vang Bóng Một Thời", "Nguyễn Tuân", "NXB Văn Học", 2015, 0, 68000},
                {"Nhà Giả Kim", "Paulo Coelho", "NXB Hội Nhà Văn", 2020, 1, 79000},
                {"Rừng Na Uy", "Haruki Murakami", "NXB Hội Nhà Văn", 2018, 1, 150000},
                {"Hoàng Tử Bé", "Antoine de Saint-Exupéry", "NXB Kim Đồng", 2019, 1, 65000},
                {"Ông Già Và Biển Cả", "Ernest Hemingway", "NXB Văn Học", 2017, 1, 50000},
                {"Trăm Năm Cô Đơn", "Gabriel García Márquez", "NXB Văn Học", 2016, 1, 160000},
                {"Giết Con Chim Nhại", "Harper Lee", "NXB Văn Học", 2019, 1, 120000},
                {"1984", "George Orwell", "NXB Hội Nhà Văn", 2020, 1, 105000},
                {"Những Người Khốn Khổ (Tập 1)", "Victor Hugo", "NXB Văn Học", 2015, 1, 140000},
                {"Đắc Nhân Tâm", "Dale Carnegie", "NXB Tổng Hợp TP.HCM", 2021, 2, 86000},
                {"Nhà Đầu Tư Thông Minh", "Benjamin Graham", "NXB Lao Động", 2019, 2, 239000},
                {"Cha Giàu Cha Nghèo", "Robert T. Kiyosaki", "NXB Trẻ", 2020, 2, 95000},
                {"Tư Duy Nhanh Và Chậm", "Daniel Kahneman", "NXB Thế Giới", 2018, 2, 199000},
                {"7 Thói Quen Hiệu Quả", "Stephen R. Covey", "NXB Tổng Hợp TP.HCM", 2019, 2, 145000},
                {"Từ Tốt Đến Vĩ Đại", "Jim Collins", "NXB Trẻ", 2017, 2, 135000},
                {"Kính Vạn Hoa (Tập 1)", "Nguyễn Nhật Ánh", "NXB Kim Đồng", 2018, 3, 70000},
                {"Harry Potter Và Hòn Đá Phù Thủy", "J.K. Rowling", "NXB Trẻ", 2017, 3, 150000},
                {"Totto-chan Bên Cửa Sổ", "Kuroyanagi Tetsuko", "NXB Hội Nhà Văn", 2019, 3, 98000},
                {"Không Gia Đình", "Hector Malot", "NXB Văn Học", 2016, 3, 130000},
                {"Chuyện Con Mèo Dạy Hải Âu Bay", "Luis Sepúlveda", "NXB Hội Nhà Văn", 2020, 3, 49000},
                {"Giải Tích 1", "Nguyễn Đình Trí", "NXB Giáo Dục", 2018, 4, 72000},
                {"Cấu Trúc Dữ Liệu Và Giải Thuật", "Đỗ Xuân Lôi", "NXB ĐHQG Hà Nội", 2016, 4, 85000},
                {"Clean Code", "Robert C. Martin", "Prentice Hall", 2012, 4, 420000},
                {"Head First Java", "Kathy Sierra, Bert Bates", "O'Reilly", 2015, 4, 390000},
                {"Kinh Tế Học Vĩ Mô", "N. Gregory Mankiw", "NXB Thống Kê", 2017, 4, 180000},
                {"Lập Trình Hướng Đối Tượng Với Java", "Trần Đan Thư", "NXB KHKT", 2019, 4, 95000},
                {"Sapiens: Lược Sử Loài Người", "Yuval Noah Harari", "NXB Tri Thức", 2020, 5, 209000},
                {"Lược Sử Thời Gian", "Stephen Hawking", "NXB Trẻ", 2019, 5, 115000},
                {"Súng, Vi Trùng Và Thép", "Jared Diamond", "NXB Thế Giới", 2018, 5, 250000},
                {"Việt Nam Sử Lược", "Trần Trọng Kim", "NXB Văn Học", 2016, 5, 160000},
                {"Đại Việt Sử Ký Toàn Thư (Tập 1)", "Ngô Sĩ Liên", "NXB Khoa Học Xã Hội", 2013, 5, 220000},
                {"Doraemon (Tập 1)", "Fujiko F. Fujio", "NXB Kim Đồng", 2019, 6, 22000},
                {"Thám Tử Lừng Danh Conan (Tập 1)", "Gosho Aoyama", "NXB Kim Đồng", 2020, 6, 25000},
                {"One Piece (Tập 1)", "Eiichiro Oda", "NXB Kim Đồng", 2021, 6, 25000},
                {"Dragon Ball (Tập 1)", "Akira Toriyama", "NXB Kim Đồng", 2018, 6, 22000},
                {"Thần Đồng Đất Việt (Tập 1)", "Lê Linh", "Phan Thị", 2015, 6, 18000},
                {"English Grammar in Use", "Raymond Murphy", "Cambridge University Press", 2019, 7, 280000},
                {"Hackers TOEIC Reading", "Hackers Language Research Institute", "NXB Tổng Hợp", 2020, 7, 230000},
                {"Minna no Nihongo Sơ Cấp 1", "3A Network", "NXB Trẻ", 2018, 7, 120000},
                {"Cambridge IELTS 15", "Cambridge University Press", "Cambridge", 2020, 7, 190000},
                {"Tiếng Anh Giao Tiếp Hằng Ngày", "Phan Thu Hà", "NXB ĐHQG TP.HCM", 2017, 7, 65000}};
        BookCondition[] conditions = BookCondition.values();
        int[] conditionPercent = {75, 62, 50, 38};
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < books.length; i++) {
            Object[] b = books[i];
            Category cat = categories.get((int) b[4]);
            Store store = stores.get(chooseStore((int) b[4], i));
            BookCondition cond = conditions[(i * 7) % 4 == 3 && i % 3 != 0 ? 2 : (i * 7) % 3];
            long original = ((Number) b[5]).longValue();
            long price = Math.max(10000, Math.round(original * conditionPercent[cond.ordinal()] / 100.0 / 1000) * 1000);
            Product p = productRepository.save(Product.builder()
                    .store(store).category(cat).title((String) b[0]).author((String) b[1]).publisher((String) b[2])
                    .publishYear((Integer) b[3]).pages(120 + random.nextInt(480))
                    .isbn("978604" + String.format("%07d", 1000000 + i * 7919 % 9000000))
                    .bookCondition(cond)
                    .description(describe((String) b[0], (String) b[1], cond, cat))
                    .price(price).originalPrice(original)
                    .discountPercent(i % 5 == 0 ? 10 : (i % 7 == 0 ? 15 : 0))
                    .stock(3 + random.nextInt(25)).status(ProductStatus.ACTIVE).build());
            backdate("products", p.getId(), 330 - i * 6);
            products.add(p);
        }

        Carrier ghn = carrierRepository.save(Carrier.builder().name("Giao Hàng Nhanh").fee(30000).estimatedTime("1-2 ngày").active(true).build());
        carrierRepository.save(Carrier.builder().name("Giao Hàng Tiết Kiệm").fee(22000).estimatedTime("3-4 ngày").active(true).build());
        carrierRepository.save(Carrier.builder().name("Viettel Post").fee(25000).estimatedTime("2-4 ngày").active(true).build());
        carrierRepository.save(Carrier.builder().name("J&T Express").fee(27000).estimatedTime("2-3 ngày").active(true).build());

        LocalDateTime now = LocalDateTime.now();
        voucherRepository.save(Voucher.builder().code("REBOOK10").name("Giảm 10% toàn sàn").type(VoucherType.PRODUCT_PERCENT)
                .percent(10).maxDiscount(30000).minOrder(100000).startAt(now.minusDays(30)).endAt(now.plusMonths(3)).quantity(500).active(true).build());
        voucherRepository.save(Voucher.builder().code("FREESHIP").name("Miễn phí vận chuyển").type(VoucherType.SHIPPING_PERCENT)
                .percent(100).maxDiscount(30000).minOrder(150000).startAt(now.minusDays(10)).endAt(now.plusMonths(1)).quantity(200).active(true).build());
        voucherRepository.save(Voucher.builder().code("THUDUC20").name("Mừng sinh nhật ReBook Thủ Đức").type(VoucherType.PRODUCT_PERCENT)
                .percent(20).maxDiscount(50000).minOrder(150000).store(stores.get(0)).startAt(now.minusDays(5)).endAt(now.plusMonths(2)).quantity(100).active(true).build());
        voucherRepository.save(Voucher.builder().code("Q1SHIP50").name("Giảm 50% phí ship").type(VoucherType.SHIPPING_PERCENT)
                .percent(50).maxDiscount(15000).minOrder(0).store(stores.get(1)).startAt(now.minusDays(5)).endAt(now.plusMonths(1)).quantity(100).active(true).build());

        seedOrders(customers, stores, products, List.of(ghn, carrierRepository.findAll().get(1)), List.of(shipper1, shipper2));

        chatMessageRepository.save(ChatMessage.builder().store(stores.get(0)).customer(customers.get(0)).sender(customers.get(0))
                .content("Chào shop, cuốn Clean Code còn bản nào mới hơn không ạ?").readFlag(true).build());
        chatMessageRepository.save(ChatMessage.builder().store(stores.get(0)).customer(customers.get(0)).sender(stores.get(0).getOwner())
                .content("Chào bạn, hiện shop có bản tình trạng Tốt 85%, giấy hơi ngả màu nhưng không rách trang nhé.").readFlag(false).build());
        userRepository.flush();
        deferredUpdates.forEach(u -> jdbc.update((String) u[0], Arrays.copyOfRange(u, 1, u.length)));
        log.info("Đã tạo dữ liệu mẫu. Mật khẩu tất cả tài khoản demo: {}", DEMO_PASSWORD);
    }

    /** Phân sách về cửa hàng theo chuyên môn của từng shop */
    private int chooseStore(int category, int index) {
        return switch (category) {
            case 0, 2, 4 -> index % 2 == 0 ? 0 : (category == 0 ? 1 : 3);
            case 1 -> 1;
            case 3, 6, 7 -> 2;
            default -> 3;
        };
    }

    private void seedOrders(List<User> customers, List<Store> stores, List<Product> products, List<Carrier> carriers, List<User> shippers) {
        Map<Long, List<Product>> byStore = new HashMap<>();
        products.forEach(p -> byStore.computeIfAbsent(p.getStore().getId(), k -> new ArrayList<>()).add(p));
        List<String> reviewTexts = List.of(
                "Sách cũ nhưng còn rất mới, gáy chắc chắn, không bị ố vàng nhiều. Shop đóng gói cẩn thận, giao hàng nhanh. Sẽ ủng hộ tiếp!",
                "Nội dung tuyệt vời, đọc một mạch không dứt ra được. Tình trạng sách đúng như mô tả của shop, giá lại rẻ hơn mua mới rất nhiều.",
                "Mua tặng em gái, bé rất thích. Sách có vài vết bút chì nhỏ nhưng tẩy được. Shop tư vấn nhiệt tình, nhắn tin trả lời rất nhanh.",
                "Đóng gói kỹ bằng bìa cứng và màng chống nước. Sách giữ gìn tốt so với tuổi đời, rất đáng tiền. Cảm ơn ReBook nhiều nha!",
                "Giao hơi chậm một chút so với dự kiến nhưng bù lại sách đẹp, giá hợp lý. Mình đã mua lần thứ ba ở shop này và vẫn rất hài lòng.");
        int count = 0;
        for (int i = 0; i < 360; i++) {
            Store store = stores.get(random.nextInt(stores.size()));
            List<Product> pool = byStore.get(store.getId());
            User customer = customers.get(i < 120 ? random.nextInt(6) : random.nextInt(customers.size()));
            int daysAgo = random.nextInt(365);
            OrderStatus status;
            if (daysAgo < 2) status = new OrderStatus[]{OrderStatus.NEW, OrderStatus.CONFIRMED, OrderStatus.SHIPPING}[random.nextInt(3)];
            else status = random.nextInt(100) < 88 ? OrderStatus.DELIVERED : (random.nextBoolean() ? OrderStatus.CANCELLED : OrderStatus.RETURNED);
            Carrier carrier = carriers.get(random.nextInt(carriers.size()));
            Address address = addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(customer.getId()).get(0);
            Order order = Order.builder()
                    .code("RB" + String.format("%06d", 100000 + i)).user(customer).store(store)
                    .receiverName(address.getReceiverName()).receiverPhone(address.getPhone()).shippingAddress(address.getFullAddress())
                    .carrier(carrier).status(status)
                    .paymentMethod(random.nextInt(4) == 0 ? PaymentMethod.VNPAY : PaymentMethod.COD)
                    .paymentStatus(PaymentStatus.UNPAID).shippingFee(carrier.getFee()).build();
            Set<Long> used = new HashSet<>();
            int lines = 1 + random.nextInt(3);
            long subtotal = 0;
            for (int l = 0; l < lines; l++) {
                Product p = pool.get(random.nextInt(pool.size()));
                if (!used.add(p.getId())) continue;
                int qty = 1 + (random.nextInt(5) == 0 ? 1 : 0);
                order.getItems().add(OrderItem.builder().order(order).product(p).productTitle(p.getTitle())
                        .price(p.getFinalPrice()).quantity(qty).build());
                subtotal += p.getFinalPrice() * qty;
                if (status == OrderStatus.DELIVERED) p.setSoldCount(p.getSoldCount() + qty);
            }
            order.setSubtotal(subtotal);
            order.setTotal(subtotal + order.getShippingFee());
            if (status != OrderStatus.NEW && status != OrderStatus.CANCELLED) order.setShipper(shippers.get(random.nextInt(shippers.size())));
            if (status == OrderStatus.DELIVERED) order.setPaymentStatus(PaymentStatus.PAID);
            if (status == OrderStatus.RETURNED) order.setPaymentStatus(PaymentStatus.REFUNDED);
            if (status == OrderStatus.NEW && order.getPaymentMethod() == PaymentMethod.VNPAY) order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);
            LocalDateTime created = LocalDateTime.now().minusDays(daysAgo).minusHours(random.nextInt(12));
            LocalDateTime delivered = status == OrderStatus.DELIVERED || status == OrderStatus.RETURNED
                    ? created.plusDays(2 + random.nextInt(3)) : null;
            if (delivered != null && delivered.isAfter(LocalDateTime.now())) delivered = LocalDateTime.now().minusHours(1);
            defer("update orders set created_at = ?, updated_at = ?, delivered_at = ? where id = ?",
                    Timestamp.valueOf(created), Timestamp.valueOf(delivered != null ? delivered : created),
                    delivered == null ? null : Timestamp.valueOf(delivered), order.getId());

            // Đánh giá cho khoảng 45% dòng đơn đã giao
            if (status == OrderStatus.DELIVERED) {
                for (OrderItem item : order.getItems()) {
                    if (random.nextInt(100) >= 45) continue;
                    int rating = random.nextInt(10) < 7 ? 5 : (random.nextBoolean() ? 4 : 3);
                    Review r = reviewRepository.save(Review.builder().user(customer).product(item.getProduct()).orderItem(item)
                            .rating(rating).content(reviewTexts.get(random.nextInt(reviewTexts.size()))).build());
                    defer("update reviews set created_at = ? where id = ?", Timestamp.valueOf(delivered.plusDays(1)), r.getId());
                    item.setReviewed(true);
                    Product p = item.getProduct();
                    double total = p.getRatingAvg() * p.getRatingCount() + rating;
                    p.setRatingCount(p.getRatingCount() + 1);
                    p.setRatingAvg(Math.round(total / p.getRatingCount() * 10) / 10.0);
                }
            }
            count++;
        }
        // Lượt xem, lượt thích mô phỏng
        for (Product p : products) {
            p.setViewCount(p.getSoldCount() * 9 + random.nextInt(80));
            p.setFavoriteCount(p.getSoldCount() / 3 + random.nextInt(10));
        }
        log.info("Đã tạo {} đơn hàng mẫu", count);
    }

    private User user(String name, String email, String phone, Role role, String pw, int daysAgo) {
        User u = userRepository.save(User.builder().fullName(name).email(email).phone(phone).password(pw)
                .role(role).enabled(true).locked(false).build());
        backdate("users", u.getId(), daysAgo);
        return u;
    }

    private void backdate(String table, Long id, int daysAgo) {
        defer("update " + table + " set created_at = ? where id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(Math.max(daysAgo, 0)).minusHours(random.nextInt(10))), id);
    }

    private void defer(String sql, Object... args) {
        Object[] row = new Object[args.length + 1];
        row[0] = sql;
        System.arraycopy(args, 0, row, 1, args.length);
        deferredUpdates.add(row);
    }

    private String describe(String title, String author, BookCondition cond, Category cat) {
        return "\"" + title + "\" của " + author + " là một trong những đầu sách được bạn đọc yêu thích ở mảng "
                + cat.getName().toLowerCase() + ".\n\nTình trạng: " + cond.getLabel()
                + ". Sách đã được ReBook kiểm tra từng trang, vệ sinh bìa và bọc plastic trước khi lên kệ."
                + "\nCam kết đổi trả trong 7 ngày nếu sách không đúng mô tả.";
    }
}

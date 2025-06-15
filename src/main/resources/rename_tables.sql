-- Đổi tên bảng
RENAME TABLE users TO NGUOIDUNG;
RENAME TABLE products TO SANPHAM;
RENAME TABLE orders TO DONHANG;
RENAME TABLE order_items TO CHITIETDONHANG;
RENAME TABLE payments TO THANHTOAN;
RENAME TABLE notifications TO THONGBAO;
RENAME TABLE chat_messages TO TINNHAN;
RENAME TABLE chat_rooms TO PHONGCHAT;
RENAME TABLE email_verification_tokens TO MAXACTHUC;

-- Đổi tên cột trong bảng NGUOIDUNG
ALTER TABLE NGUOIDUNG 
    CHANGE username tenDangNhap VARCHAR(255),
    CHANGE password matKhau VARCHAR(255),
    CHANGE email email VARCHAR(255),
    CHANGE full_name hoTen VARCHAR(255),
    CHANGE phone soDienThoai VARCHAR(255),
    CHANGE address diaChi TEXT,
    CHANGE role vaiTro VARCHAR(50),
    CHANGE active trangThai BOOLEAN,
    CHANGE email_verified daXacThucEmail BOOLEAN,
    CHANGE created_at ngayTao DATETIME;

-- Đổi tên cột trong bảng SANPHAM
ALTER TABLE SANPHAM
    CHANGE name tenSanPham VARCHAR(255),
    CHANGE description moTa TEXT,
    CHANGE price gia DECIMAL(10,2),
    CHANGE quantity soLuong INT,
    CHANGE category danhMuc VARCHAR(255),
    CHANGE brand thuongHieu VARCHAR(255),
    CHANGE image_url hinhAnh VARCHAR(255),
    CHANGE size kichThuoc VARCHAR(50),
    CHANGE pet_type loaiThuCung VARCHAR(50),
    CHANGE active trangThai BOOLEAN,
    CHANGE created_at ngayTao DATETIME,
    CHANGE updated_at ngayCapNhat DATETIME;

-- Đổi tên cột trong bảng DONHANG
ALTER TABLE DONHANG
    CHANGE order_number maDonHang VARCHAR(255),
    CHANGE user_id nguoiDungId BIGINT,
    CHANGE total_amount tongTien DECIMAL(10,2),
    CHANGE status trangThai VARCHAR(50),
    CHANGE payment_method phuongThucThanhToan VARCHAR(50),
    CHANGE shipping_address diaChiGiaoHang TEXT,
    CHANGE phone soDienThoai VARCHAR(255),
    CHANGE notes ghiChu TEXT,
    CHANGE created_at ngayTao DATETIME,
    CHANGE updated_at ngayCapNhat DATETIME;

-- Đổi tên cột trong bảng CHITIETDONHANG
ALTER TABLE CHITIETDONHANG
    CHANGE order_id donHangId BIGINT,
    CHANGE product_id sanPhamId BIGINT,
    CHANGE quantity soLuong INT,
    CHANGE price gia DECIMAL(10,2);

-- Đổi tên cột trong bảng THANHTOAN
ALTER TABLE THANHTOAN
    CHANGE order_id donHangId BIGINT,
    CHANGE payment_id maThanhToan VARCHAR(255),
    CHANGE transaction_id maGiaoDich VARCHAR(255),
    CHANGE amount soTien DECIMAL(10,2),
    CHANGE method phuongThuc VARCHAR(50),
    CHANGE status trangThai VARCHAR(50),
    CHANGE momo_order_id maDonHangMomo VARCHAR(255),
    CHANGE momo_request_id maYeuCauMomo VARCHAR(255),
    CHANGE momo_signature chuKyMomo VARCHAR(255),
    CHANGE response_code maTraLoi VARCHAR(50),
    CHANGE response_message thongDiepTraLoi TEXT,
    CHANGE payment_url urlThanhToan VARCHAR(255),
    CHANGE raw_response duLieuTraLoi TEXT,
    CHANGE created_at ngayTao DATETIME,
    CHANGE paid_at ngayThanhToan DATETIME,
    CHANGE updated_at ngayCapNhat DATETIME;

-- Đổi tên cột trong bảng THONGBAO
ALTER TABLE THONGBAO
    CHANGE user_id nguoiDungId BIGINT,
    CHANGE title tieuDe VARCHAR(255),
    CHANGE message noiDung TEXT,
    CHANGE type loaiThongBao VARCHAR(50),
    CHANGE is_read daDoc BOOLEAN,
    CHANGE created_at ngayTao DATETIME,
    CHANGE read_at ngayDoc DATETIME,
    CHANGE order_id donHangId BIGINT,
    CHANGE product_id sanPhamId BIGINT,
    CHANGE action_url urlHanhDong VARCHAR(255),
    CHANGE metadata duLieuBoSung TEXT;

-- Đổi tên cột trong bảng TINNHAN
ALTER TABLE TINNHAN
    CHANGE sender_id nguoiGuiId BIGINT,
    CHANGE receiver_id nguoiNhanId BIGINT,
    CHANGE content noiDung TEXT,
    CHANGE message_type loaiTinNhan VARCHAR(50),
    CHANGE created_at ngayTao DATETIME,
    CHANGE is_read daDoc BOOLEAN,
    CHANGE order_id donHangId BIGINT,
    CHANGE ticket_id maPhieuHoTro VARCHAR(255);

-- Đổi tên cột trong bảng PHONGCHAT
ALTER TABLE PHONGCHAT
    CHANGE room_id maPhong VARCHAR(255),
    CHANGE customer_id khachHangId BIGINT,
    CHANGE staff_id nhanVienId BIGINT,
    CHANGE room_type loaiPhong VARCHAR(50),
    CHANGE status trangThai VARCHAR(50),
    CHANGE created_at ngayTao DATETIME,
    CHANGE last_message_at tinNhanCuoiLuc DATETIME,
    CHANGE closed_at ngayDong DATETIME,
    CHANGE subject chuDe VARCHAR(255),
    CHANGE priority doUuTien VARCHAR(50);

-- Đổi tên cột trong bảng MAXACTHUC
ALTER TABLE MAXACTHUC
    CHANGE token maXacThuc VARCHAR(255),
    CHANGE user_id nguoiDungId BIGINT,
    CHANGE expiry_date ngayHetHan DATETIME,
    CHANGE used daSuDung BOOLEAN; 
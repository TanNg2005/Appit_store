// Script để import dữ liệu sản phẩm vào Firestore
// Yêu cầu: Đã cài đặt nodejs và chạy lệnh: npm install firebase-admin

const admin = require('firebase-admin');
const fs = require('fs');

// 1. Tải 'Service Account Key'
// Đảm bảo bạn đã tải file này từ Firebase Console -> Project Settings -> Service accounts
// và đổi tên nó thành 'serviceAccountKey.json' rồi để cùng thư mục với file này.
const serviceAccount = require('./serviceAccountKey.json');

// 2. Tải dữ liệu từ file products.json
// Đảm bảo file products.json nằm cùng thư mục
const data = JSON.parse(fs.readFileSync('./products.json', 'utf8'));

// 3. Khởi tạo Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// 4. Hàm để tải dữ liệu lên
async function uploadData() {
  const products = data.products;

  if (!products || products.length === 0) {
    console.log('Không tìm thấy sản phẩm nào trong file products.json');
    return;
  }

  console.log(`Bắt đầu tải lên ${products.length} sản phẩm...`);

  const productCollection = db.collection('products');

  for (const product of products) {
    try {
      // Sử dụng .doc(id).set(data) nếu bạn muốn giữ nguyên ID từ file json
      // Hoặc dùng .add(data) để Firebase tự sinh ID ngẫu nhiên.
      // Ở đây tôi dùng .add() vì ID trong file json (1, 2, 3...) chỉ để tham khảo.
      // Tuy nhiên, để dễ quản lý, ta có thể chuyển đổi id số thành string nếu muốn.
      
      // Cách 1: Để Firebase tự sinh ID (Khuyên dùng nếu ID của bạn chỉ là số thứ tự)
      await productCollection.add(product);

      // Cách 2: Nếu bạn muốn ID trên Firebase giống hệt ID trong file (ví dụ "1", "2")
      // await productCollection.doc(String(product.id)).set(product);

      console.log(`Đã thêm thành công: ${product.title}`);
    } catch (error) {
      console.error(`Lỗi khi thêm ${product.title}: `, error);
    }
  }

  console.log('Hoàn tất việc tải lên dữ liệu!');
}

// 5. Chạy hàm
uploadData();
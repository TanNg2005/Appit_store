const { exec } = require('child_process');

// ID dự án đã được điền sẵn
const projectId = 'appit-d5b5e';

const command = `firebase deploy --only firestore:indexes --project ${projectId}`;

console.log(`Đang thực thi lệnh: ${command}`);
console.log('Vui lòng đợi, quá trình này có thể mất vài phút...');

const deployProcess = exec(command);

deployProcess.stdout.on('data', (data) => {
  console.log(`${data}`);
});

deployProcess.stderr.on('data', (data) => {
  console.error(`Lỗi: ${data}`);
});

deployProcess.on('close', (code) => {
  if (code === 0) {
    console.log('\x1b[32m%s\x1b[0m', '✅ Triển khai chỉ mục thành công!');
    console.log('Chỉ mục có thể mất vài phút để xây dựng. Bây giờ bạn có thể chạy lại ứng dụng của mình.');
  } else {
    console.error('\x1b[31m%s\x1b[0m', `Lỗi: Quá trình triển khai thất bại với mã lỗi ${code}.`);
    console.error('Hãy đảm bảo bạn đã cài đặt Firebase CLI (`npm install -g firebase-tools`) và đã đăng nhập (`firebase login`).');
  }
});

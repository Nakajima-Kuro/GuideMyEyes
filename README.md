<div id="top"></div>

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/Nakajima-Kuro/GuideMyEyes">
    <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Logo" width="80" height="80">
  </a>

  <h3 align="center">Guide My Eyes</h3>

  <p align="center">
    Ứng dụng hỗ trợ người mù và người khiếm thị di chuyển trong môi trường xung quanh
  </p>
</div>

<!-- ABOUT THE PROJECT -->
## Tổng quan về sản phẩm

Guide My Eyes là một ứng dụng trợ giúp người mù và người khiếm thị có thể di chuyển trong môi trường đời sống bình thường một cách an toàn.
Mục tiêu của ứng dụng là có thể thay thế hoàn toàn gậy dò đường thông thường, khiến cho người khiếm thị có thể hòa nhập với cuộc sống bình thường

Cách thức hoạt động của ứng dụng:
* Sử dụng ARCore để lấy bản đồ độ sâu, từ đó quét tìm điểm có khả năng va chạm với người dùng nhất
* Với điểm kết quả lấy được, phát tín hiệu âm thanh để cảnh báo người dùng về vị trí vật cản
* Sử dụng TensorFlow Lite để nhận diện đối tượng có khả năng va chạm nhất và cảnh báo đến người dùng

## Được xây dựng bởi

Ứng dụng được xây dựng dựa trên những nền tảng sau:

* [Android](https://www.android.com/)
* [ARCore](https://developers.google.com/ar)
* [TensorFLow Lite](https://www.tensorflow.org/lite)
* [MobileNet-SSD](https://arxiv.org/pdf/1512.02325.pdf)

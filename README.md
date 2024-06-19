# EduPay
![logo](https://github.com/qaknc8/edupay-backend/assets/104825451/18aa99c4-3a47-4d3d-915d-67aec6b490dd)

학원비 청구 및 결제 자동화로 학원 운영의 효율성을 높이고, 학부모에게 편의를 주기 위한 사이트입니다.

이 서비스는 학원에서 강의와 원생을 관리하고, 이메일을 통해 결제 가능한 청구서를 발송할 수 있도록 지원합니다.

## 주요 기능
- **회원가입/로그인**
  - 사업자 정보를 입력하여 회원가입합니다.
- **원생 관리** 
  - 학원 원생의 등록, 수정, 삭제 및 조회할 수 있습니다.
- **강의 관리**
  - 강의를 등록, 수정, 삭제 및 조회할 수 있습니다.
- **포인트**
  - 청구서를 발송하기 위한 포인트 충전을 위해 portOne api를 사용합니다.
  - 포인트 충전 내역을 조회할 수 있습니다.
- **청구서 발송**
  - javaMailSender를 사용하여 청구서를 이메일로 발송합니다.
  - 청구서 결제를 위해 portOne api를 사용합니다.
- **수납 관리**
  - 전체, 수납과 미수납 내역을 조회할 수 있습니다.
  - 연도 및 월별의 전체 결제 건수와 총액 통계를 제공합니다.

## 팀 소개
<table>
  <tr> 
    <td><img src="https://api.dicebear.com/9.x/rings/svg?seed=Missy" alt="avatar" /><br /><sub><b>팀장 정지용</b></sub><br /></td>
    <td><img src="https://api.dicebear.com/9.x/rings/svg?seed=Cleo" alt="avatar" /><br /><sub><b>선주영</b></sub><br /></td>      
    <td><img src="https://api.dicebear.com/9.x/rings/svg?seed=Lola" alt="avatar" /><br /><sub><b>정진서</b></sub><br /></td>     
    <td><img src="https://api.dicebear.com/9.x/rings/svg?seed=Jasper" alt="avatar" /><br /><sub><b>이정수</b></sub><br /></td>     
    <td><img src="https://api.dicebear.com/9.x/rings/svg?seed=Abby" alt="avatar" /><br /><sub><b>이성민</b></sub><br /></td>     
    <td><img src="https://api.dicebear.com/9.x/rings/svg?seed=Garfield" alt="avatar" /><br /><sub><b>이영준</b></sub><br /></td>     
  </tr>
  <tr>  
    <td>
        <li>원생 관리</li>
        <li>강의 관리</li>
    </td>  
    <td>
        <li>로그인</li>
        <li>회원가입</li>
    </td>
    <td>
        <li>청구서 생성</li>
        <li>청구서 발송</li>
    </td>
    <td>
        <li>학원비 결제</li>
        <li>포인트 충전</li>
    </td>
    <td>
        <li>수납 관리</li>
        <li>매출 통계</li>
    </td>
    <td>
        <li>영수증 발급</li>
    </td>
  </tr>
</table>
프로젝트 기간 : 2024.05.20 ~ 2024.06.14

# 실시간 채팅 연동 가이드

1:1 채팅은 **REST로 보내고, STOMP(WebSocket)로 받는다.**

- 메시지 전송·조회·읽음 처리: 기존 REST API (`/chat/**`, Swagger 참고)
- 새 메시지·읽음 알림 수신: STOMP 개인 큐 `/user/queue/chat`

## 연결

| 항목 | 값 |
|------|-----|
| 엔드포인트 | `ws(s)://<서버>/ws-chat` (SockJS 아님, 순수 WebSocket) |
| 인증 | CONNECT 프레임 헤더 `Authorization: Bearer <accessToken>` |
| 구독 경로 | `/user/queue/chat` (이 경로만 허용) |
| 클라이언트 → 서버 SEND | 허용하지 않음 (보내기는 REST로) |
| 하트비트 | 10초 / 10초 |
| 허용 Origin | `app.cors.allowed-origins`와 같음 |

토큰이 없거나 틀리면 CONNECT가 ERROR 프레임으로 거부된다.

서버는 연결할 때 쓴 토큰의 만료 시각(`exp`)에 웹소켓을 **close code `4001`**(`token expired`)로 닫는다. 이 코드로 끊기면 토큰을 새로 받아 `connectHeaders`를 바꾼 뒤 다시 연결한다.

```ts
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'ws://localhost:8080/ws-chat',
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
  reconnectDelay: 3000,
  onConnect: () => {
    client.subscribe('/user/queue/chat', (frame) => {
      const event = JSON.parse(frame.body);
      // event.type === 'MESSAGE' | 'READ'
    });
  },
});
client.activate();
```

## 이벤트 형식

```json
{
  "type": "MESSAGE",
  "roomId": 1,
  "message": {
    "id": 250, "roomId": 1, "senderId": 2, "senderName": "김택배",
    "content": "택배 어디에 두셨나요?", "isMine": false, "isRead": false,
    "createdAt": "2026-11-16T14:30:00"
  },
  "readerId": null,
  "unreadCount": 1,
  "totalUnreadCount": 3
}
```

- 모든 값은 **이벤트를 받는 사람 기준**이다 (`isMine`, `unreadCount`, `totalUnreadCount`).
- `unreadCount`·`totalUnreadCount`가 `null`이면 이 이벤트로 내 안 읽은 수가 바뀌지 않은 것이다. 기존 값을 그대로 둔다.
  - `MESSAGE`: 받는 사람에게만 값이 있고, 보낸 사람에게는 `null`
  - `READ`: 읽은 사람에게만 값이 있고(이 방은 `0`), 상대에게는 `null`
- `MESSAGE`: 메시지가 저장되면 두 참여자 모두에게 간다. 보낸 사람도 받으므로 다른 탭·기기의 화면을 맞출 수 있다.
- `READ`: `PATCH /chat/rooms/{roomId}/read`로 한 건 이상 읽음 처리되면 두 참여자 모두에게 간다.
  `readerId`가 내가 아니면, 그 방에서 내가 보낸 메시지를 모두 읽음으로 표시한다.

## 화면별 처리

| 상황 | 처리 |
|------|------|
| 채팅방을 보고 있을 때 `MESSAGE` 수신 | 메시지를 목록에 추가하고 `PATCH /read` 호출 |
| 다른 화면에서 `MESSAGE` 수신 | 방 목록의 미리보기·순서와 안 읽은 수를 이벤트 값으로 갱신 |
| 재연결 직후 | 끊겨 있던 동안의 이벤트는 다시 오지 않는다. `GET /chat/rooms`와 열려 있는 방의 `GET /messages`를 다시 불러온다 |
| 중복·순서 | REST 응답과 이벤트로 같은 메시지를 두 번 받을 수 있으니 `message.id`로 중복을 제거하고 `id` 순으로 정렬한다 |

## 서버 운영 메모

- 브로커는 서버 메모리 안의 simple broker다. **서버를 2대 이상 띄우면** 다른 서버에 연결된 사용자에게 이벤트가 가지 않으므로, 그 전에 RabbitMQ/Redis 같은 외부 브로커로 바꿔야 한다.
- 리버스 프록시(nginx 등)를 쓴다면 `/ws-chat`에 `Upgrade`/`Connection` 헤더 전달과 긴 read timeout 설정이 필요하다.

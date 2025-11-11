package shop.chaekmate.gateway.dto;

public record MemberInfoResponse(
        Long memberId,
        String name,
        String role) {
}

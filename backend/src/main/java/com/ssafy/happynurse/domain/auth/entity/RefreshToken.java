package com.ssafy.happynurse.domain.auth.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RedisHash("RefreshToken")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    private String tokenValue;

    @Indexed
    private String sessionId;

    private Long practitionerId;
    private String employeeNumber;
    private String practitionerName;

    private Long organizationId;
    private Long wardId;
    private String roleCode;

    @TimeToLive(unit = TimeUnit.MILLISECONDS)
    private Long ttl;

    public static RefreshToken create(String sessionId,
                                       Long practitionerId,
                                       String employeeNumber,
                                       String practitionerName,
                                       long expirationMs,
                                       Long organizationId,
                                       Long wardId,
                                       String roleCode) {
        RefreshToken token = new RefreshToken();
        token.tokenValue = UUID.randomUUID().toString();
        token.sessionId = sessionId;
        token.practitionerId = practitionerId;
        token.employeeNumber = employeeNumber;
        token.practitionerName = practitionerName;
        token.organizationId = organizationId;
        token.wardId = wardId;
        token.roleCode = roleCode;
        token.ttl = expirationMs;
        return token;
    }
}

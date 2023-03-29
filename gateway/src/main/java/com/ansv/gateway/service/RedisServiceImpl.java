package com.ansv.gateway.service;

import com.ansv.gateway.dto.redis.AccessToken;
import com.ansv.gateway.dto.redis.RefreshToken;
import com.ansv.gateway.repository.RedisRepository;
import com.ansv.gateway.repository.redis.RedisTokenRepository;
import com.ansv.gateway.util.DataUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class RedisServiceImpl implements RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisServiceImpl.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String ACCESSTOKEN = "accessToken";
    private static final String REFRESHTOKEN = "refreshToken";

    private ObjectMapper objectMapper = new ObjectMapper();


    @Autowired
    private RedisRepository redisRepository;

    @Autowired
    private RedisTokenRepository redisTokenRepository;

    // using for extract token
    private static Optional<String> extractBearTokenHeader(@NonNull HttpServletRequest request) {
        try {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (DataUtils.notNull(authorization)) {
                if (authorization.startsWith(BEARER_PREFIX)) {
                    String token = authorization.substring(BEARER_PREFIX.length()).trim();
                    if (!token.isBlank()) {
                        return Optional.of(token);
                    }
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Optional.empty();
        }
    }


    @Override
    public void saveAccessToken(AccessToken token) {
        try {
            String jsonToken = objectMapper.writeValueAsString(token);
            redisRepository.saveToken(jsonToken, ACCESSTOKEN.trim(), token.getUuid(), token.getExpiredTime());
//            redisTokenRepository.save(token);
        } catch (Exception e) {
            logger.error("An error when save ACCESS token into redis db", e);
        }
    }

    @Override
    public void updateAccessToken(AccessToken token) {
        try {
            String jsonToken = objectMapper.writeValueAsString(token);
            redisRepository.updateToken(jsonToken, ACCESSTOKEN.trim(), token.getUuid(), token.getExpiredTime());
//            redisTokenRepository.save(token);
        } catch (Exception e) {
            logger.error("An error when update ACCESS token into redis db", e);
        }
    }

    @Override
    public Optional<AccessToken> getAccessToken(String uuid) {
        try {
            Object token = redisRepository.getTokenByObject(uuid, ACCESSTOKEN.trim());
            if (DataUtils.notNull(token)) {
                return Optional.of((AccessToken) token);
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("An error when get ACCESS token into redis db", e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteAccessToken(String uuid) {
        try {
            redisRepository.deleteToken(uuid, ACCESSTOKEN.trim());
        } catch (Exception e) {
            logger.error("An error when DELETE ACCESS token into redis db", e);
        }
    }

    @Override
    public void saveRefreshToken(RefreshToken token) {
        try {
            String jsonToken = objectMapper.writeValueAsString(token);
            redisRepository.saveToken(jsonToken, REFRESHTOKEN.trim(), token.getUuid(), token.getExpiredTime());
        } catch (Exception e) {
            logger.error("An error when SAVE REFRESH token into redis db", e);
        }
    }

    @Override
    public void updateRefreshToken(RefreshToken token) {
        try {
            String jsonToken = objectMapper.writeValueAsString(token);
            redisRepository.updateToken(jsonToken, REFRESHTOKEN.trim(), token.getUuid(), token.getExpiredTime());
        } catch (Exception e) {
            logger.error("An error when get token into redis db", e);
        }
    }

    @Override
    public Optional<RefreshToken> getRefreshToken(String uuid) {
        try {
            Object token = redisRepository.getTokenByObject(uuid, REFRESHTOKEN.trim());
            if (DataUtils.notNull(token)) {
                return Optional.of((RefreshToken) token);
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("An error when get REFRESH token into redis db", e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteRefreshToken(String uuid) {
        try {
            redisRepository.deleteToken(uuid, REFRESHTOKEN.trim());
        } catch (Exception e) {
            logger.error("An error when DELETE REFRESH token into redis db", e);
        }
    }

    @Override
    public String generateUUIDVersion1() {
        long most64SigBits = get64MostSignificantBitsForVersion1();
        long least64SigBits = get64LeastSignificantBitsForVersion1();
        return new UUID(most64SigBits, least64SigBits).toString();
    }

    private static long get64LeastSignificantBitsForVersion1() {
        Random random = new Random();
        long random63BitLong = random.nextLong() & 0x3FFFFFFFFFFFFFFFL;
        long variant3BitFlag = 0x8000000000000000L;
        return random63BitLong | variant3BitFlag;
    }

    private static long get64MostSignificantBitsForVersion1() {
        final long currentTimeMillis = System.currentTimeMillis();
        final long time_low = (currentTimeMillis & 0x0000_0000_FFFF_FFFFL) << 32;
        final long time_mid = ((currentTimeMillis >> 32) & 0xFFFF) << 16;
        final long version = 1 << 12;
        final long time_hi = ((currentTimeMillis >> 48) & 0x0FFF);
        return time_low | time_mid | version | time_hi;
    }


}

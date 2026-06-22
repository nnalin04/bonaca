package com.bonaca.backend.auth.service;

import com.bonaca.backend.auth.dto.AuthTokensResponse;
import com.bonaca.backend.auth.dto.MeResponse;
import com.bonaca.backend.auth.exception.InvalidOtpException;
import com.bonaca.backend.auth.exception.InvalidRefreshTokenException;
import com.bonaca.backend.auth.model.User;
import com.bonaca.backend.auth.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final OtpService otpService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public AuthService(
            OtpService otpService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository) {
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    public void requestOtp(String phoneNumber) {
        otpService.requestOtp(phoneNumber);
    }

    /**
     * noRollbackFor must be repeated here, not just on OtpService.verifyOtp: this method joins
     * the same physical transaction (default REQUIRED propagation), and Spring's rollback
     * decision is governed by the outermost @Transactional boundary that sees the exception —
     * an inner noRollbackFor is overridden if an outer layer doesn't also declare it.
     */
    @Transactional(noRollbackFor = InvalidOtpException.class)
    public AuthTokensResponse verifyOtp(String phoneNumber, String code) {
        User user = otpService.verifyOtp(phoneNumber, code);
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthTokensResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);
        User user = userRepository
                .findById(rotation.userId())
                .orElseThrow(() -> new InvalidRefreshTokenException("User no longer exists"));
        String accessToken = jwtService.issueAccessToken(user.getId(), user.getPhoneNumber());
        return new AuthTokensResponse(accessToken, rotation.newRefreshToken(), user.isProfileCompleted());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    public MeResponse getMe(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new InvalidRefreshTokenException("User no longer exists"));
        return new MeResponse(user.getId(), user.getPhoneNumber(), user.isProfileCompleted());
    }

    private AuthTokensResponse issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(user.getId(), user.getPhoneNumber());
        String refreshToken = refreshTokenService.issue(user.getId());
        return new AuthTokensResponse(accessToken, refreshToken, user.isProfileCompleted());
    }
}

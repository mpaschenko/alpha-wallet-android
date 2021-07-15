package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.AuthenticationCallback;
import com.alphawallet.app.entity.AuthenticationFailType;
import com.alphawallet.app.entity.Operation;

import java.util.concurrent.Executor;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

/**
 * Created by James on 7/06/2019.
 * Stormbird in Sydney
 */
public class SignTransactionDialog
{
    private final AuthenticationStrength authenticationStrength;
    private boolean isShowing;
    private BiometricPrompt biometricPrompt;

    public SignTransactionDialog(Context context)
    {
        isShowing = false;
        BiometricManager biometricManager = BiometricManager.from(context);
        if (biometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS)
        {
            authenticationStrength = AuthenticationStrength.STRONG_AUTHENTICATION;
        }
        else if (biometricManager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS)
        {
            authenticationStrength = AuthenticationStrength.WEAK_AUTHENTICATION;
        }
        else
        {
            authenticationStrength = AuthenticationStrength.NO_AUTHENTICATION;
        }
    }

    public void getAuthentication(AuthenticationCallback authCallback, @NonNull Activity context, Operation callbackId)
    {
        Executor executor = ContextCompat.getMainExecutor(context);
        biometricPrompt = new BiometricPrompt((FragmentActivity) context,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                isShowing = false;
                switch (errorCode)
                {
                    case BiometricPrompt.ERROR_CANCELED:
                        authCallback.authenticateFail("Cancelled", AuthenticationFailType.FINGERPRINT_ERROR_CANCELED, callbackId);
                        break;
                    case BiometricPrompt.ERROR_LOCKOUT:
                    case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                        authCallback.authenticateFail(context.getString(R.string.too_many_fails), AuthenticationFailType.FINGERPRINT_NOT_VALIDATED, callbackId);
                        break;
                    case BiometricPrompt.ERROR_USER_CANCELED:
                        authCallback.authenticateFail(context.getString(R.string.fingerprint_error_user_canceled), AuthenticationFailType.FINGERPRINT_ERROR_CANCELED, callbackId);
                        break;
                    case BiometricPrompt.ERROR_HW_NOT_PRESENT:
                    case BiometricPrompt.ERROR_HW_UNAVAILABLE:
                    case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                    case BiometricPrompt.ERROR_NO_BIOMETRICS:
                    case BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL:
                    case BiometricPrompt.ERROR_NO_SPACE:
                    case BiometricPrompt.ERROR_TIMEOUT:
                    case BiometricPrompt.ERROR_UNABLE_TO_PROCESS:
                    case BiometricPrompt.ERROR_VENDOR:
                        authCallback.authenticateFail(context.getString(R.string.fingerprint_authentication_failed), AuthenticationFailType.FINGERPRINT_NOT_VALIDATED, callbackId);
                        break;
                }
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                isShowing = false;
                authCallback.authenticatePass(callbackId);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                isShowing = false;
                authCallback.authenticateFail(context.getString(R.string.fingerprint_authentication_failed), AuthenticationFailType.FINGERPRINT_NOT_VALIDATED, callbackId);
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.unlock_private_key))
                .setAllowedAuthenticators(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
        isShowing = true;
    }

    public void close()
    {
        if (biometricPrompt != null)
        {
            try
            {
                biometricPrompt.cancelAuthentication();
            }
            catch (Exception e)
            {
                //
            }
        }
    }

    public boolean isShowing()
    {
        return isShowing;
    }

    private enum AuthenticationStrength
    {
        STRONG_AUTHENTICATION,
        WEAK_AUTHENTICATION,
        NO_AUTHENTICATION
    }
}
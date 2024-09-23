/*
 * This is WestJet source code and is for consideration as a pull request to ForgeRock.
 *
 * This fork was necessary to integrate with the F5® Distributed Cloud Defense Mobile SDK,
 * which protects API endpoints from automation attacks by collecting telemetry and adding
 * custom HTTP headers to requests. The response handling capability was built into the
 * ForgeRock SDK to ensure that the F5 Distributed Cloud Bot Defense Mobile SDK can inspect
 * and process response headers for its internal functionality.
 *
 * Dated: 2024
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

/**
 * Observes and modifies incoming responses from the SDK.
 * Interceptors can be used to add, remove, or transform headers, status codes, etc., on the response.
 */
public interface FRResponseInterceptor<T> extends ResponseInterceptor {

    default @NonNull
    Response intercept(@NonNull Response response) {
        return intercept(response, (T) response.getInternalRes().request().tag());
    }

    /**
     * Intercepts incoming responses from the SDK.
     *
     * @param response The original incoming response.
     * @param tag The tag associated with the response. The SDK tags outbound requests with {@link Action}.
     * @return The updated response.
     */
    @NonNull
    Response intercept(@NonNull Response response, T tag);
}
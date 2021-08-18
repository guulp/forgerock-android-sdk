/*
 * Copyright (c) 2020 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.VisibleForTesting;

import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.exception.OathMechanismException;
import org.forgerock.android.auth.util.TimeKeeper;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Represents an instance of a OATH authentication mechanism.
 */
public abstract class OathMechanism extends Mechanism {

    public enum TokenType {
        HOTP, TOTP
    }

    /** OATH type, must be either 'TOTP' or 'HOTP' */
    private TokenType oathType;
    /** The TimeKeeper **/
    TimeKeeper timeKeeper = new TimeKeeper();
    /** Algorithm of HMAC-based OTP */
    protected String algorithm;
    /** Digits as in Int for length of OTP credentials */
    protected int digits;

    protected OathMechanism(String mechanismUID, String issuer, String accountName, String type, TokenType oathType,
                            String algorithm, String secret, int digits, Calendar timeCreated) {
        super(mechanismUID, issuer, accountName, type, secret, timeCreated);
        this.oathType = oathType;
        this.algorithm = algorithm;
        this.digits = digits;
    }

    /**
     * Returns the number of digits that are in OTPs generated by this Token.
     * @return the OTP length as int
     */
    public int getDigits() {
        return digits;
    }

    /**
     * Returns the algorithm used by this OathMechanism.
     * @return algorithm value as String
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the token type (HOTPMechanism, TOTPMechanism).
     * @return The token type
     */
    public TokenType getOathType() {
        return oathType;
    }

    /**
     * Generates a new set of codes for this OathMechanism Token.
     * @return OathTokenCode object that contains the currently active token code
     * @throws OathMechanismException If an error occur on generating the OTP codes
     */
    public abstract OathTokenCode getOathTokenCode() throws OathMechanismException;

    /**
     * Used for Time Travel during testing.
     * @param timeKeeper The TimeKeeper implementation this Mechanism should use
     */
    @VisibleForTesting
    void setTimeKeeper(TimeKeeper timeKeeper) {
        this.timeKeeper = timeKeeper;
    }

    @Override
    public abstract String toJson();

    /**
     * Deserializes the specified Json into an object of the {@link OathMechanism} object.
     * @param jsonString the json string representing the object to be deserialized
     * @return an {@link OathMechanism} object from the string. Returns {@code null} if
     * {@code jsonString} is {@code null}, if {@code jsonString} is empty or not able to parse it
     */
    static OathMechanism deserialize(String jsonString) {
        OathMechanism oath = null;
        if (jsonString == null || jsonString.length() == 0) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            String type = jsonObject.getString("oathType").toUpperCase();
            if(TokenType.valueOf(type).equals(TokenType.HOTP)) {
                oath = HOTPMechanism.deserialize(jsonString);
            } else if(TokenType.valueOf(type).equals(TokenType.TOTP)) {
                oath = TOTPMechanism.deserialize(jsonString);
            }
        } catch (JSONException e) {
            return null;
        }
        return oath;
    }

    /**
     * Builder class responsible for producing a Token.
     */
    public abstract static class OathBuilder<T extends OathBuilder> {
        protected String mechanismUID;
        protected String issuer;
        protected String accountName;
        protected String algorithm;
        protected String secret;
        protected int digits;
        protected Calendar timeCreated;

        protected abstract T getThis();

        /**
         * Sets the mechanism unique Id.
         * @param mechanismUID the mechanism unique Id
         * @return The current builder
         */
        public T setMechanismUID(String mechanismUID) {
            this.mechanismUID = mechanismUID;
            return getThis();
        }

        /**
         * Sets the name of the IDP that issued this account.
         * @param issuer The IDP name
         * @return The current builder
         */
        public T setIssuer(String issuer) {
            this.issuer = issuer;
            return getThis();
        }

        /**
         * Sets the name of the account.
         * @param accountName The account name
         * @return The current builder
         */
        public T setAccountName(String accountName) {
            this.accountName = accountName;
            return getThis();
        }

        /**
         * Sets the algorithm used for generating the OTP.
         * Assumption: algorithm name is valid if a corresponding algorithm can be loaded.
         *
         * @param algorithm algorithm to assign
         * @return The current builder
         */
        public T setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
            return getThis();
        }

        /**
         * Sets the secret used for generating the OTP.
         * Base32 encoding based on: http://tools.ietf.org/html/rfc4648#page-8
         *
         * @param secret A Base32 encoded secret key
         * @return The current builder
         */
        public T setSecret(String secret) {
            this.secret = secret;
            return getThis();
        }

        /**
         * Sets the length of the OTP to generate.
         * @param digits Number of digits, either 6 or 8
         * @return The current builder
         */
        public T setDigits(int digits) {
            this.digits = digits;
            return getThis();
        }

        /**
         * Sets the Date and Time this mechanism was stored.
         * @param timeCreated when this mechanism was stored.
         */
        public T setTimeCreated(Calendar timeCreated) {
            this.timeCreated = timeCreated;
            return getThis();
        }

        /**
         * Produce the described OathMechanism Token.
         * @return The built Token
         * @throws MechanismCreationException If an issuer or accountName were not provided.
         */
        protected OathMechanism build() throws MechanismCreationException {
            if(issuer == null || issuer.isEmpty()) {
                throw new MechanismCreationException("issuer cannot be empty or null.");
            }
            if(accountName == null || accountName.isEmpty()) {
                throw new MechanismCreationException("accountName cannot be empty or null.");
            }
            return buildOath();
        }

        abstract OathMechanism buildOath();

    }

}

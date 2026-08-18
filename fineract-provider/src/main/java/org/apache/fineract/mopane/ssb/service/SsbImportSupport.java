/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.mopane.ssb.service;

import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.springframework.util.StringUtils;

public final class SsbImportSupport {

    private SsbImportSupport() {}

    public static String normalizeBureau(final String bureau) {
        if (!StringUtils.hasText(bureau)) {
            throw new PlatformDataIntegrityException("error.msg.ssb.bureau.required", "bureau is required (SSB or PENSION).", "bureau");
        }
        final String value = bureau.trim().toUpperCase();
        if (SsbConstants.BUREAU_SSB.equals(value) || SsbConstants.BUREAU_PENSION.equals(value)) {
            return value;
        }
        throw new PlatformDataIntegrityException("error.msg.ssb.bureau.invalid", "bureau must be SSB or PENSION.", "bureau", bureau);
    }

    public static String trimTo(final String value, final int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public static String trimReason(final String message, final String fallback) {
        if (message == null) {
            return fallback;
        }
        return trimTo(message, 255);
    }

    public static String normalizeType(final String type) {
        if (!StringUtils.hasText(type)) {
            return SsbConstants.TYPE_NEW;
        }
        final String value = type.trim().toUpperCase();
        if (SsbConstants.TYPE_NEW_SHORT.equals(value) || SsbConstants.TYPE_NEW.equals(value)) {
            return SsbConstants.TYPE_NEW;
        }
        if (SsbConstants.TYPE_CHANGE_SHORT.equals(value) || SsbConstants.TYPE_CHANGE.equals(value)) {
            return SsbConstants.TYPE_CHANGE;
        }
        if (SsbConstants.TYPE_DELETE_SHORT.equals(value) || SsbConstants.TYPE_DELETE.equals(value)) {
            return SsbConstants.TYPE_DELETE;
        }
        return value;
    }

    public static String normalizeBureauStatus(final String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        final String value = status.trim().toUpperCase();
        if (SsbConstants.BUREAU_STATUS_SUCCESS.equals(value) || "Y".equals(value) || "YES".equals(value) || "PROCESSED".equals(value)
                || "1".equals(value)) {
            return SsbConstants.BUREAU_STATUS_SUCCESS;
        }
        if (SsbConstants.BUREAU_STATUS_FAILED.equals(value) || "N".equals(value) || "NO".equals(value) || "REJECTED".equals(value)
                || "0".equals(value)) {
            return SsbConstants.BUREAU_STATUS_FAILED;
        }
        return value;
    }

    public static boolean isNewType(final String type) {
        final String normalized = normalizeType(type);
        return SsbConstants.TYPE_NEW.equals(normalized);
    }
}

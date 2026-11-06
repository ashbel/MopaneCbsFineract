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
package org.apache.fineract.infrastructure.core.domain;

import java.lang.reflect.Method;

import org.apache.fineract.infrastructure.security.domain.PlatformUser;
import org.apache.fineract.infrastructure.security.service.FineractSha256PasswordEncoder;
import org.apache.fineract.infrastructure.security.service.PlatformPasswordEncoder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service(value = "applicationPasswordEncoder")
@Scope("singleton")
public class DefaultPlatformPasswordEncoder implements PlatformPasswordEncoder {

    @Override
    public String encode(final PlatformUser appUser) {
        return FineractSha256PasswordEncoder.hash(appUser.getPassword(), idOf(appUser));
    }

    private static Long idOf(final PlatformUser appUser) {
        try {
            final Method getId = appUser.getClass().getMethod("getId");
            final Object id = getId.invoke(appUser);
            if (id == null) { return null; }
            if (id instanceof Long) { return (Long) id; }
            return Long.valueOf(id.toString());
        } catch (final ReflectiveOperationException e) {
            return null;
        }
    }
}

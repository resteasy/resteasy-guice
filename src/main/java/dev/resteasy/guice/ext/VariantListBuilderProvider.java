/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice.ext;

import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.ext.RuntimeDelegate;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class VariantListBuilderProvider implements Provider<Variant.VariantListBuilder> {
    private final RuntimeDelegate runtimeDelegate;

    @Inject
    public VariantListBuilderProvider(final RuntimeDelegate runtimeDelegate) {
        this.runtimeDelegate = runtimeDelegate;
    }

    public Variant.VariantListBuilder get() {
        return runtimeDelegate.createVariantListBuilder();
    }
}

package org.particleframework.discovery.cloud;

import org.particleframework.context.env.ComputePlatform;

import java.util.Optional;

public interface MetadataResolver {

    public Optional<? extends ComputeInstanceMetadata> resolve(ComputePlatform computePlatform);
}

package io.dropwizard.discovery.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import io.dropwizard.discovery.core.InstanceMetadata;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.curator.x.discovery.DownInstancePolicy;
import org.apache.curator.x.discovery.ProviderStrategy;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class DiscoveryClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DiscoveryClient.class);
    private final ServiceDiscovery<InstanceMetadata> discovery;
    private final ServiceProvider<InstanceMetadata> provider;
    private final ServiceCache<InstanceMetadata> cache;

    /**
     * Constructor
     * 
     * @param serviceName
     *            name of the service to monitor
     * @param discovery
     *            {@link ServiceDiscovery}
     * @param downInstancePolicy
     *            {@link DownInstancePolicy} to use when marking instances as
     *            down
     * @param providerStrategy
     *            {@link ProviderStrategy} to use when selecting an instance
     */
    public DiscoveryClient(@Nonnull final String serviceName,
            @Nonnull final ServiceDiscovery<InstanceMetadata> discovery,
            @Nonnull final DownInstancePolicy downInstancePolicy,
            @Nonnull final ProviderStrategy<InstanceMetadata> providerStrategy) {
        checkNotNull(serviceName);
        checkArgument(!serviceName.isEmpty(), "serviceName cannot be empty");
        checkNotNull(providerStrategy);

        this.discovery = checkNotNull(discovery);

        this.provider = discovery.serviceProviderBuilder()
                .serviceName(serviceName)
                .downInstancePolicy(downInstancePolicy)
                .providerStrategy(providerStrategy).build();

        this.cache = discovery.serviceCacheBuilder().name(serviceName).build();
    }

    /**
     * Return a list of discoverable services
     * 
     * @return Collection of service names
     */
    public Collection<String> getServices() throws Exception {
        return discovery.queryForNames();
    }

    /**
     * Return the running instances for the service.
     * 
     * @return Collection of service instances
     */
    public Collection<ServiceInstance<InstanceMetadata>> getInstances(
            @Nonnull final String serviceName) throws Exception {
        return discovery.queryForInstances(serviceName);
    }

    /**
     * Return a cached list of the running instances for the service.
     * 
     * @return Collection of service instances
     */
    public Collection<ServiceInstance<InstanceMetadata>> getInstances() {
        return cache.getInstances();
    }

    /**
     * Return an instance of this service.
     * 
     * @return ServiceInstance
     * @throws Exception
     */
    public ServiceInstance<InstanceMetadata> getInstance() throws Exception {
        return provider.getInstance();
    }

    /**
     * Note an error when connecting to a service instance.
     * 
     * @param instance
     *            {@link ServiceInstance} that is causing the error.
     */
    public void noteError(
            @Nonnull final ServiceInstance<InstanceMetadata> instance) {
        provider.noteError(instance);
    }

    /**
     * Start the internal {@link ServiceProvider} and {@link ServiceCache}
     * 
     * @throws Exception
     */
    public void start() throws Exception {
        provider.start();
        cache.start();
    }

    /**
     * Stop the internal {@link ServiceProvider} and {@link ServiceCache}
     */
    @Override
    public void close() {
        try {
            cache.close();
        } catch (final IOException e) {
            LOGGER.error("Unable to close cache", e);
        }
        try {
            provider.close();
        } catch (final IOException e) {
            LOGGER.error("Unable to close provider", e);
        }
    }
}

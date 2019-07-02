package no.difi.meldingsutveksling.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig extends CachingConfigurerSupport {

    @Override
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
                new CaffeineCache(ServiceRegistryLookup.CACHE_GET_SAS_KEY,
                        Caffeine.newBuilder()
                                .expireAfterWrite(1, TimeUnit.DAYS)
                                .build()),
                new CaffeineCache(ServiceRegistryLookup.CACHE_GET_SERVICE_RECORD,
                        Caffeine.newBuilder()
                                .maximumSize(100L)
                                .expireAfterWrite(1, TimeUnit.MINUTES)
                                .build()),
                new CaffeineCache(ServiceRegistryLookup.CACHE_GET_SERVICE_RECORDS,
                        Caffeine.newBuilder()
                                .maximumSize(100L)
                                .expireAfterWrite(1, TimeUnit.MINUTES)
                                .build()),
                new CaffeineCache(ServiceRegistryLookup.CACHE_GET_INFO_RECORD,
                        Caffeine.newBuilder()
                                .maximumSize(100L)
                                .expireAfterWrite(1, TimeUnit.MINUTES)
                                .build())
        ));
        return cacheManager;
    }

    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.error(String.format("Failed to get object(%s) from cache(%s)", key, cache.getName()), exception);
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key, @Nullable Object value) {
                log.error(String.format("Failed to put object(%s) into cache(%s)", key, cache.getName()), exception);
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.error(String.format("Failed to evict object(%s) from cache(%s)", key, cache.getName()), exception);
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
                log.error(String.format("Failed to clear cache(%s)", cache.getName()), exception);
            }
        };
    }
}
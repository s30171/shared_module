package idv.mark.share_module.asr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ASRProviderRegistry {
    private final Map<String, ASRProvider> providers;

    @Autowired
    public ASRProviderRegistry(List<ASRProvider> providerList) {
        this.providers = providerList.stream()
            .collect(Collectors.toMap(ASRProvider::getName, Function.identity()));
        log.info("Registered ASR providers: {}", providers.keySet());
    }

    public ASRProvider getProvider(String name) {
        return Optional.ofNullable(providers.get(name))
            .orElseThrow(() -> new IllegalArgumentException("Unknown ASR provider: " + name));
    }

    public ASRProvider autoSelect(String[] languages) {
        return providers.values().stream()
            .filter(p -> p.supportsLanguages(languages))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No suitable ASR provider for languages: " + Arrays.toString(languages)));
    }

    public List<String> listProviders() {
        return new ArrayList<>(providers.keySet());
    }
}

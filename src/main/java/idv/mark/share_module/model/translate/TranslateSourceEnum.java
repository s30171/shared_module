package idv.mark.share_module.model.translate;

import lombok.Getter;

@Getter
public enum TranslateSourceEnum {
    Papago,
    DeepL,
    Azure,
    Google,
    ChatGPT,
    ChatGPT_SRT,
    Local_LLM
    ;
}

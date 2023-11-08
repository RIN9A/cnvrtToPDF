package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadConvert {
    private boolean async;
    private String filetype;
    private String key;
    private String outputtype;
    private String title;
    private String url;
    private String token;
}

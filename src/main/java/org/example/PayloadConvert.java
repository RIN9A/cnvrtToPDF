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
    private String fileType;
    private String key;
    private String outputType;
    private String title;
    private String url;

}

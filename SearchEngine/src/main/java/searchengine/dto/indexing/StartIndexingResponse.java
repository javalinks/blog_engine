package searchengine.dto.indexing;

import lombok.*;

@Data
public class StartIndexingResponse {
    private boolean result;

    private String error;
}

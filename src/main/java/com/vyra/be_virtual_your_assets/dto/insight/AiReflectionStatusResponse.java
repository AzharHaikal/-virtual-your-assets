package com.vyra.be_virtual_your_assets.dto.insight;

import lombok.Data;

import java.time.LocalDate;

/**
 * Whether the member can create a reflection now. {@code available} = the server has a model configured;
 * {@code consentGiven} = AI_REFLECTION granted; {@code canCreate} = both, the week has data and no
 * reflection yet.
 */
@Data
public class AiReflectionStatusResponse {
    private boolean available;
    private boolean consentGiven;
    private boolean canCreate;
    /** The week a new reflection would cover: the last full week (Monday..Sunday). */
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private boolean alreadyCreated;
    private boolean hasData;
}

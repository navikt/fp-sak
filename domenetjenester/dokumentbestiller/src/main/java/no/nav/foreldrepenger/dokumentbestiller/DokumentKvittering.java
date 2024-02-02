package no.nav.foreldrepenger.dokumentbestiller;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DokumentKvittering(@NotNull UUID behandlingUuid,
                                 @NotNull UUID bestillingUuid,
                                 DokumentMalType malType, // midlertidig - trenges ikke i V3 og vil bli fjernet på sikt.
                                 @NotNull String journalpostId,
                                 @NotNull String dokumentId) {
}

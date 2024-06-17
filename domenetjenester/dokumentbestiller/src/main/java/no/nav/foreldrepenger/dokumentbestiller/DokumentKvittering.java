package no.nav.foreldrepenger.dokumentbestiller;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record DokumentKvittering(@NotNull UUID behandlingUuid, @NotNull UUID bestillingUuid, @NotNull String journalpostId,
                                 @NotNull String dokumentId) {
}

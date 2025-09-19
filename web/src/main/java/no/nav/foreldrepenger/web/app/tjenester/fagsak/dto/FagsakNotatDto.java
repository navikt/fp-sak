package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakNotat;

public record FagsakNotatDto(@NotNull String opprettetAv, @NotNull LocalDateTime opprettetTidspunkt, @NotNull String notat) {

    public static FagsakNotatDto fraNotat(FagsakNotat notat) {
        return new FagsakNotatDto(notat.getOpprettetAv(), notat.getOpprettetTidspunkt(), notat.getNotat());
    }
}

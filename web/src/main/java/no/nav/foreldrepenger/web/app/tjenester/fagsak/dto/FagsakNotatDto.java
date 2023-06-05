package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakNotat;

public record FagsakNotatDto(String opprettetAv, LocalDateTime opprettetTidspunkt, String notat) {

    public static FagsakNotatDto fraNotat(FagsakNotat notat) {
        return new FagsakNotatDto(notat.getOpprettetAv(), notat.getOpprettetTidspunkt(), notat.getNotat());
    }
}

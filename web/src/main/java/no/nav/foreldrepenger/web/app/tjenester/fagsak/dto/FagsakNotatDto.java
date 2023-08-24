package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakNotat;

import java.time.LocalDateTime;

public record FagsakNotatDto(String opprettetAv, LocalDateTime opprettetTidspunkt, String notat) {

    public static FagsakNotatDto fraNotat(FagsakNotat notat) {
        return new FagsakNotatDto(notat.getOpprettetAv(), notat.getOpprettetTidspunkt(), notat.getNotat());
    }
}

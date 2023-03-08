package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.datavarehus.domene.FagsakRelasjonDvh;

public class FagsakRelasjonDvhMapper {

    private FagsakRelasjonDvhMapper() {
    }

    public static FagsakRelasjonDvh map(FagsakRelasjon fagsakRelasjon) {
        return FagsakRelasjonDvh.builder()
            .fagsakNrEn(fagsakRelasjon.getFagsakNrEn().getId())
            .fagsakNrTo(fagsakRelasjon.getFagsakNrTo().map(Fagsak::getId).orElse(null))
            .dekningsgrad(fagsakRelasjon.getDekningsgrad())
            .avsluttningsdato(fagsakRelasjon.getAvsluttningsdato())
            .funksjonellTid(LocalDateTime.now())
            .endretAv(CommonDvhMapper.finnEndretAvEllerOpprettetAv(fagsakRelasjon))
            .build();
    }
}

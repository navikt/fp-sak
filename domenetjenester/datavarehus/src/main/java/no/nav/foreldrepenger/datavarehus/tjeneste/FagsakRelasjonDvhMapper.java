package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.datavarehus.domene.FagsakRelasjonDvh;

public class FagsakRelasjonDvhMapper {

    public static FagsakRelasjonDvh map(FagsakRelasjon fagsakRelasjon) {
        Long fagsakNrTo = null;

        if (fagsakRelasjon.getFagsakNrTo().isPresent()) {
            fagsakNrTo = fagsakRelasjon.getFagsakNrTo().get().getId();
        }

        return FagsakRelasjonDvh.builder()
            .fagsakNrEn(fagsakRelasjon.getFagsakNrEn().getId())
            .fagsakNrTo(fagsakNrTo)
            .dekningsgrad(fagsakRelasjon.getDekningsgrad())
            .avsluttningsdato(fagsakRelasjon.getAvsluttningsdato())
            .funksjonellTid(LocalDateTime.now())
            .endretAv(CommonDvhMapper.finnEndretAvEllerOpprettetAv(fagsakRelasjon))
            .build();
    }
}

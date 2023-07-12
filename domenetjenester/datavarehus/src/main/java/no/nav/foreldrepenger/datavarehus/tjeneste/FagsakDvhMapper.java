package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.datavarehus.domene.FagsakDvh;
import no.nav.foreldrepenger.domene.typer.AktørId;

class FagsakDvhMapper {

    private FagsakDvhMapper() {
    }

    static FagsakDvh map(Fagsak fagsak, Optional<AktørId> annenPartAktørId) {
        return FagsakDvh.builder()
            .fagsakId(fagsak.getId())
            .brukerId(fagsak.getNavBruker().getId())
            .brukerAktørId(fagsak.getAktørId().getId())
            .epsAktørId(annenPartAktørId.map(AktørId::getId))
            .opprettetDato(fagsak.getOpprettetTidspunkt().toLocalDate())
            .funksjonellTid(LocalDateTime.now())
            .endretAv(CommonDvhMapper.finnEndretAvEllerOpprettetAv(fagsak))
            .fagsakStatus(fagsak.getStatus().getKode())
            .fagsakYtelse(fagsak.getYtelseType().getKode())
            .saksnummer(fagsak.getSaksnummer() != null ? fagsak.getSaksnummer().getVerdi() : null)
            .build();
    }

}

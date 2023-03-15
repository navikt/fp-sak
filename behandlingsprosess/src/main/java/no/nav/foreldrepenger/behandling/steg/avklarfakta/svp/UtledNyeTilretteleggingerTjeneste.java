package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;

@ApplicationScoped
class UtledNyeTilretteleggingerTjeneste {

    private SvangerskapspengerRepository svangerskapspengerRepository;
    private UtledTilretteleggingerMedArbeidsgiverTjeneste utledTilretteleggingerMedArbeidsgiverTjeneste;

    UtledNyeTilretteleggingerTjeneste() {
        // CDI
    }

    @Inject
    UtledNyeTilretteleggingerTjeneste(SvangerskapspengerRepository svangerskapspengerRepository,
            UtledTilretteleggingerMedArbeidsgiverTjeneste utledTilretteleggingerMedArbeidsgiverTjeneste) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.utledTilretteleggingerMedArbeidsgiverTjeneste = utledTilretteleggingerMedArbeidsgiverTjeneste;
    }

    public List<SvpTilretteleggingEntitet> utled(Behandling behandling, Skjæringstidspunkt skjæringstidspunkt) {
        var svpGrunnlag = svangerskapspengerRepository.hentGrunnlag(behandling.getId()).orElseThrow(
                () -> new IllegalStateException("Fant ikke forventet grunnlag for behandling " + behandling.getId()));
        var opprinneligeTilrettelegginger = svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe();
        var tilretteleggingerUtenArbeidsgiver = utled(opprinneligeTilrettelegginger);
        var tilretteleggingerMedArbeidsgiver = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt,
                opprinneligeTilrettelegginger);
        return Stream.of(tilretteleggingerUtenArbeidsgiver, tilretteleggingerMedArbeidsgiver)
                .flatMap(Collection::stream)
                .toList();
    }

    static List<SvpTilretteleggingEntitet> utled(List<SvpTilretteleggingEntitet> opprinneligeTilrettelegginger) {
        return opprinneligeTilrettelegginger.stream()
                .filter(tlr -> tlr.getArbeidsgiver().isEmpty())
                .toList();
    }

}

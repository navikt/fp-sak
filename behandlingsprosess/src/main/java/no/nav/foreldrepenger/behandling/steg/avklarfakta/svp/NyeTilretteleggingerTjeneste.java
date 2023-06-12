package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;

@ApplicationScoped
class NyeTilretteleggingerTjeneste {

    private SvangerskapspengerRepository svangerskapspengerRepository;
    private UtledTilretteleggingerMedArbeidsgiverTjeneste utledTilretteleggingerMedArbeidsgiverTjeneste;

    NyeTilretteleggingerTjeneste() {
        // CDI
    }

    @Inject
    NyeTilretteleggingerTjeneste(SvangerskapspengerRepository svangerskapspengerRepository,
                                 UtledTilretteleggingerMedArbeidsgiverTjeneste utledTilretteleggingerMedArbeidsgiverTjeneste) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.utledTilretteleggingerMedArbeidsgiverTjeneste = utledTilretteleggingerMedArbeidsgiverTjeneste;
    }

    public void utledNyeTilretteleggingerLagreJustert(Behandling behandling, Skjæringstidspunkt skjæringstidspunkt) {
        var opprinneligeTilrettelegginger = svangerskapspengerRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Fant ikke forventet grunnlag for behandling " + behandling.getId()))
            .getOpprinneligeTilrettelegginger().getTilretteleggingListe();
        var justerteTilrettelegginger = utledJusterte(behandling, skjæringstidspunkt);
        if (likeTilrettelegginger(justerteTilrettelegginger, opprinneligeTilrettelegginger)) {
            return;
        }
        lagre(behandling, justerteTilrettelegginger);
    }

    static boolean likeTilrettelegginger(List<SvpTilretteleggingEntitet> ene, List<SvpTilretteleggingEntitet> andre) {
        return ene.size() == andre.size() && new HashSet<>(ene).containsAll(andre);
    }


    List<SvpTilretteleggingEntitet> utledJusterte(Behandling behandling, Skjæringstidspunkt skjæringstidspunkt) {
        var opprinneligeTilrettelegginger = svangerskapspengerRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Fant ikke forventet grunnlag for behandling " + behandling.getId()))
            .getOpprinneligeTilrettelegginger().getTilretteleggingListe();
        var tilretteleggingerUtenArbeidsgiver = utledUtenArbeidsgiver(opprinneligeTilrettelegginger);
        var tilretteleggingerMedArbeidsgiver = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt,
                opprinneligeTilrettelegginger);
        return Stream.of(tilretteleggingerUtenArbeidsgiver, tilretteleggingerMedArbeidsgiver)
                .flatMap(Collection::stream)
                .toList();
    }

    void lagre(Behandling behandling, List<SvpTilretteleggingEntitet> nyeTilrettelegginger) {
        var svpGrunnlag = svangerskapspengerRepository.hentGrunnlag(behandling.getId()).orElseThrow(
            () -> new IllegalStateException("Fant ikke forventet grunnlag for behandling " + behandling.getId()));
        var nyttSvpGrunnlag = new SvpGrunnlagEntitet.Builder(svpGrunnlag)
            .medOverstyrteTilrettelegginger(nyeTilrettelegginger)
            .build();
        svangerskapspengerRepository.lagreOgFlush(nyttSvpGrunnlag);
    }

    static List<SvpTilretteleggingEntitet> utledUtenArbeidsgiver(List<SvpTilretteleggingEntitet> opprinneligeTilrettelegginger) {
        return opprinneligeTilrettelegginger.stream()
                .filter(tlr -> tlr.getArbeidsgiver().isEmpty())
                .toList();
    }

}

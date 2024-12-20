package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.svp;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;

@ApplicationScoped
class NyeTilretteleggingerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(NyeTilretteleggingerTjeneste.class);

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
        var gjeldendeTilrettelegginger = svangerskapspengerRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Fant ikke forventet grunnlag for behandling " + behandling.getId()))
            .getGjeldendeVersjon().getTilretteleggingListe();
        var justerteTilrettelegginger = utledJusterte(behandling, skjæringstidspunkt, gjeldendeTilrettelegginger);
        if (!likeTilrettelegginger(justerteTilrettelegginger, gjeldendeTilrettelegginger)) {
            LOG.info("Differanse mellom gammel og justert tilrettelegging på behandling med id {}, gamle tilrettelegginger var {} mens nye var {}", behandling.getId(), gjeldendeTilrettelegginger, justerteTilrettelegginger);
            lagre(behandling, justerteTilrettelegginger);
        }
    }

    static boolean likeTilrettelegginger(List<SvpTilretteleggingEntitet> ene, List<SvpTilretteleggingEntitet> andre) {
        return ene.size() == andre.size() && ene.stream().allMatch(tlre -> andre.stream().anyMatch(tlre::erLik));
    }


    List<SvpTilretteleggingEntitet> utledJusterte(Behandling behandling, Skjæringstidspunkt skjæringstidspunkt, List<SvpTilretteleggingEntitet> gjeldendeTilrettelegginger) {
        var tilretteleggingerUtenArbeidsgiver = utledUtenArbeidsgiver(gjeldendeTilrettelegginger);
        var tilretteleggingerMedArbeidsgiver = utledTilretteleggingerMedArbeidsgiverTjeneste.utled(behandling, skjæringstidspunkt,
                gjeldendeTilrettelegginger);
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

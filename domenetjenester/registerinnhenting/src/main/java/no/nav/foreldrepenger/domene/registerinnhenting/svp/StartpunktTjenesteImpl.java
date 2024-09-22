package no.nav.foreldrepenger.domene.registerinnhenting.svp;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class StartpunktTjenesteImpl implements StartpunktTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(StartpunktTjenesteImpl.class);

    private Instance<StartpunktUtleder> utledere;
    private EndringsresultatSjekker endringsresultatSjekker;

    StartpunktTjenesteImpl() {
        // For CDI
    }

    @Inject
    public StartpunktTjenesteImpl(@Any Instance<StartpunktUtleder> utledere,
                                  EndringsresultatSjekker endringsresultatSjekker) {
        this.utledere = utledere;
        this.endringsresultatSjekker = endringsresultatSjekker;
    }

    @Override
    public StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering, Skjæringstidspunkt stp) {
        var origBehandlingId = revurdering.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Original behandling mangler på revurdering - skal ikke skje"));

        var snapshotOriginalBehandling = endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(origBehandlingId);
        var diff = endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(revurdering.behandlingId(), snapshotOriginalBehandling);
        LOG.info("Endringsresultat ved revurdering={} er: {}", revurdering.behandlingId(), diff);
        return getStartpunktType(revurdering, stp, diff, false);
    }

    @Override
    public StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, Skjæringstidspunkt stp, EndringsresultatDiff differanse) {
        return getStartpunktType(revurdering, stp, differanse, true);
    }

    private StartpunktType getStartpunktType(BehandlingReferanse revurdering, Skjæringstidspunkt stp, EndringsresultatDiff differanse, boolean normalDiff) {
        return Stream.of(InntektArbeidYtelseGrunnlag.class, PersonInformasjonEntitet.class, NesteSakGrunnlagEntitet.class)
            .map(differanse::hentDelresultat)
            .flatMap(Optional::stream)
            .filter(EndringsresultatDiff::erSporedeFeltEndret)
            .map(diff -> utledStartpunktForDelresultat(revurdering, stp, diff, normalDiff))
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private StartpunktType utledStartpunktForDelresultat(BehandlingReferanse revurdering, Skjæringstidspunkt stp, EndringsresultatDiff diff, boolean normalDiff) {
        if (!diff.erSporedeFeltEndret())
            return StartpunktType.UDEFINERT;
        var utleder = GrunnlagRef.Lookup.find(StartpunktUtleder.class, utledere, diff.getGrunnlag()).orElseThrow();
        return normalDiff ? utleder.utledStartpunkt(revurdering, stp, diff.getGrunnlagId1(), diff.getGrunnlagId2()) :
            utleder.utledInitieltStartpunktRevurdering(revurdering, stp, diff.getGrunnlagId1(), diff.getGrunnlagId2()).stream()
                .filter(s -> !StartpunktType.SØKERS_RELASJON_TIL_BARNET.equals(s) && StartpunktType.DEKNINGSGRAD.equals(s))
                .min(Comparator.comparing(StartpunktType::getRangering))
                .orElse(StartpunktType.UDEFINERT);
    }


}

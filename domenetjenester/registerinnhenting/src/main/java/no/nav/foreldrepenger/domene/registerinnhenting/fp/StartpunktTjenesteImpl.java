package no.nav.foreldrepenger.domene.registerinnhenting.fp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class StartpunktTjenesteImpl implements StartpunktTjeneste {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartpunktTjenesteImpl.class);

    private Instance<StartpunktUtleder> utledere;
    private EndringsresultatSjekker endringsresultatSjekker;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    StartpunktTjenesteImpl() {
        // For CDI
    }

    @Inject
    public StartpunktTjenesteImpl(@Any Instance<StartpunktUtleder> utledere,
                                  EndringsresultatSjekker endringsresultatSjekker,
                                  FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.utledere = utledere;
        this.endringsresultatSjekker = endringsresultatSjekker;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering) {
        Long origBehandlingId = revurdering.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Original behandling mangler på revurdering - skal ikke skje"));

        EndringsresultatSnapshot snapshotOriginalBehandling = endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(origBehandlingId);
        EndringsresultatDiff diff = endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(revurdering.getId(), snapshotOriginalBehandling);
        LOGGER.info("Endringsresultat ved revurdering={} er: {}", revurdering.getId(), diff);// NOSONAR //$NON-NLS-1$
        return getStartpunktType(revurdering, diff, false);

    }

    @Override
    public StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse) {
        return getStartpunktType(revurdering, differanse, true);
    }

    private StartpunktType getStartpunktType(BehandlingReferanse revurdering, EndringsresultatDiff differanse, boolean normalDiff) {
        List<StartpunktType> startpunkter = new ArrayList<>();
        // Denne skal oppstå selv om grunnlaget er uendret. Eneste kjente tidsfristavhengige
        FamilieHendelseGrunnlagEntitet grunnlagForBehandling = familieHendelseTjeneste.hentAggregat(revurdering.getBehandlingId());
        if (skalSjekkeForManglendeFødsel(grunnlagForBehandling))
            startpunkter.add(StartpunktType.SØKERS_RELASJON_TIL_BARNET);

        differanse.hentKunDelresultater().stream()
            .map(diff -> utledStartpunktForDelresultat(revurdering, diff, normalDiff))
            .forEach(startpunkter::add);

        return startpunkter.stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private StartpunktType utledStartpunktForDelresultat(BehandlingReferanse revurdering, EndringsresultatDiff diff, boolean normalDiff) {
        if (!diff.erSporedeFeltEndret())
            return StartpunktType.UDEFINERT;
        var utleder = GrunnlagRef.Lookup.find(StartpunktUtleder.class, utledere, diff.getGrunnlag()).orElseThrow();
        return normalDiff ? utleder.utledStartpunkt(revurdering, diff.getGrunnlagId1(), diff.getGrunnlagId2()) :
            utleder.utledInitieltStartpunktRevurdering(revurdering, diff.getGrunnlagId1(), diff.getGrunnlagId2());
    }

    private boolean skalSjekkeForManglendeFødsel(FamilieHendelseGrunnlagEntitet grunnlagForBehandling) {
        return familieHendelseTjeneste.getManglerFødselsRegistreringFristUtløpt(grunnlagForBehandling);
    }
}

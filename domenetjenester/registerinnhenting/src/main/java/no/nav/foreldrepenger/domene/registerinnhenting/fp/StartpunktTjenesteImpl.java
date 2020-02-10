package no.nav.foreldrepenger.domene.registerinnhenting.fp;

import java.util.Comparator;

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
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class StartpunktTjenesteImpl implements StartpunktTjeneste {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartpunktTjenesteImpl.class);

    private Instance<StartpunktUtleder> utledere;
    private EndringsresultatSjekker endringsresultatSjekker;

    StartpunktTjenesteImpl() {
        // For CDI
    }

    @Inject
    public StartpunktTjenesteImpl(@Any Instance<StartpunktUtleder> utledere, EndringsresultatSjekker endringsresultatSjekker) {
        this.utledere = utledere;
        this.endringsresultatSjekker = endringsresultatSjekker;
    }

    @Override
    public StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering) {
        Long origBehandlingId = revurdering.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Original behandling mangler på revurdering - skal ikke skje"));

        EndringsresultatSnapshot snapshotOriginalBehandling = endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(origBehandlingId);
        EndringsresultatDiff diff = endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(revurdering.getId(), snapshotOriginalBehandling);
        LOGGER.info("Endringsresultat ved revurdering={} er: {}", revurdering.getId(), diff);// NOSONAR //$NON-NLS-1$
        StartpunktType startpunktType = utledStartpunktForDiffBehandlingsgrunnlag(revurdering, diff);

        return startpunktType;
    }

    @Override
    public StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse) {
        StartpunktType startpunktType = differanse.hentKunDelresultater().stream()
            .map(diff -> {
                var utleder = finnUtleder(diff.getGrunnlag());
                return utleder.erBehovForStartpunktUtledning(diff) ? utleder.utledStartpunkt(revurdering, diff.getGrunnlagId1(), diff.getGrunnlagId2()) : StartpunktType.UDEFINERT;
            })
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
        return startpunktType;
    }

    private StartpunktUtleder finnUtleder(Class<?> aggregat) {
        return GrunnlagRef.Lookup.find(StartpunktUtleder.class, utledere, aggregat).orElseThrow();
    }

}

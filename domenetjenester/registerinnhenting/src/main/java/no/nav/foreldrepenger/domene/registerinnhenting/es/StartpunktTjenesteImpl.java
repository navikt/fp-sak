package no.nav.foreldrepenger.domene.registerinnhenting.es;

import java.util.Comparator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@FagsakYtelseTypeRef("ES")
public class StartpunktTjenesteImpl implements StartpunktTjeneste {

    private Instance<StartpunktUtleder> utledere;

    @Inject
    public StartpunktTjenesteImpl(@Any Instance<StartpunktUtleder> utledere) {
        this.utledere = utledere;
    }

    StartpunktTjenesteImpl() {
        // CDI
    }

    @Override
    public StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering) {
        throw new IllegalStateException("Utviklerfeil: Skal ikke kalle startpunkt mot original for Engangsstønad, sak: " + revurdering.getSaksnummer().getVerdi());
    }

    @Override
    public StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse) {
        StartpunktType startpunkt = differanse.hentKunDelresultater().stream()
            .map(diff -> utledStartpunktForDelresultat(revurdering, diff))
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
        return StartpunktType.inngangsVilkårStartpunkt().contains(startpunkt) ? startpunkt : StartpunktType.UDEFINERT;
    }

    private StartpunktType utledStartpunktForDelresultat(BehandlingReferanse revurdering, EndringsresultatDiff diff) {
        var utleder = GrunnlagRef.Lookup.find(StartpunktUtleder.class, utledere, diff.getGrunnlag()).orElseThrow();
        return utleder.erBehovForStartpunktUtledning(diff) ?
            utleder.utledStartpunkt(revurdering, diff.getGrunnlagId1(), diff.getGrunnlagId2()) : StartpunktType.UDEFINERT;
    }

}

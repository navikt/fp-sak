package no.nav.foreldrepenger.domene.registerinnhenting.fp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class StartpunktTjenesteImpl implements StartpunktTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(StartpunktTjenesteImpl.class);

    private Instance<StartpunktUtleder> utledere;
    private EndringsresultatSjekker endringsresultatSjekker;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;

    StartpunktTjenesteImpl() {
        // For CDI
    }

    @Inject
    public StartpunktTjenesteImpl(@Any Instance<StartpunktUtleder> utledere,
                                  EndringsresultatSjekker endringsresultatSjekker,
                                  FamilieHendelseTjeneste familieHendelseTjeneste,
                                  FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                  DekningsgradTjeneste dekningsgradTjeneste) {
        this.utledere = utledere;
        this.endringsresultatSjekker = endringsresultatSjekker;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    @Override
    public StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering) {
        var origBehandlingId = revurdering.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Original behandling mangler på revurdering - skal ikke skje"));

        var snapshotOriginalBehandling = endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(origBehandlingId);
        var diff = endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(revurdering.behandlingId(), snapshotOriginalBehandling);
        LOG.info("Endringsresultat ved revurdering={} er: {}", revurdering.behandlingId(), diff);
        return getStartpunktType(revurdering, diff, false);
    }

    @Override
    public StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse) {
        return getStartpunktType(revurdering, differanse, true);
    }

    private StartpunktType getStartpunktType(BehandlingReferanse revurdering, EndringsresultatDiff differanse, boolean normalDiff) {
        List<StartpunktType> startpunkter = new ArrayList<>();
        // Denne skal oppstå selv om grunnlaget er uendret. Eneste kjente tidsfristavhengige
        var grunnlagForBehandling = familieHendelseTjeneste.hentAggregat(revurdering.behandlingId());
        if (skalSjekkeForManglendeFødsel(grunnlagForBehandling)) {
            startpunkter.add(StartpunktType.SØKERS_RELASJON_TIL_BARNET);
        }
        if (fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(revurdering.fagsakId())
            .flatMap(fr -> Optional.ofNullable(fr.getDekningsgrad()))
            .filter(frDekningsgrad -> !Objects.equals(frDekningsgrad, dekningsgradTjeneste.finnGjeldendeDekningsgrad(revurdering)))
            .isPresent()) {
            startpunkter.add(StartpunktType.DEKNINGSGRAD);
        }

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
        return FamilieHendelseTjeneste.getManglerFødselsRegistreringFristUtløpt(grunnlagForBehandling);
    }
}

package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@GrunnlagRef(GrunnlagRef.YTELSE_FORDELING_GRUNNLAG)
class StartpunktUtlederYtelseFordeling implements StartpunktUtleder {

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private SøknadRepository søknadRepository;
    private BeregningTjeneste beregningTjeneste;
    StartpunktUtlederYtelseFordeling() {
        // For CDI
    }

    @Inject
    StartpunktUtlederYtelseFordeling(BehandlingRepositoryProvider repositoryProvider,
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                     BeregningTjeneste beregningTjeneste) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        // Ser på forhold fra endringssøknader og kun en gang - ved KOFAK siden mottak merger/henlegger ved ny søknad. (andre utledere ser på fødsel, mm.).
        var originalBehandling = ref.getOriginalBehandlingId().orElse(null);
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        if (originalBehandling == null || behandling.harSattStartpunkt()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "ikke revurdering el passert kofak", grunnlagId1, grunnlagId2);
            return StartpunktType.UTTAKSVILKÅR;
        }
        if (erSkjæringsdatoEndret(stp, originalBehandling)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT, "endring i skjæringsdato", grunnlagId1, grunnlagId2);
            return StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
        }
        if (erStartpunktBeregning(ref)) {
            if (beregningTjeneste.kanStartesISteg(ref, BehandlingStegType.VURDER_REF_BERGRUNN)) {
                FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.BEREGNING_REFUSJON, "Søkt om gradert periode", grunnlagId1, grunnlagId2);
                return StartpunktType.BEREGNING_REFUSJON;
            } else {
                FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.BEREGNING_REFUSJON, "Søkt om gradert periode", grunnlagId1, grunnlagId2);
                return StartpunktType.BEREGNING;
            }
        }
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "ingen endring i skjæringsdato", grunnlagId1, grunnlagId2);
        return StartpunktType.UTTAKSVILKÅR;
    }

    private boolean erSkjæringsdatoEndret(Skjæringstidspunkt stp, Long originalBehandlingId) {
        var nySkjæringsdato = stp.getSkjæringstidspunktHvisUtledet().orElse(null);
        var originalSkjæringsdato = skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandlingId).getSkjæringstidspunktHvisUtledet().orElse(null);
        return !Objects.equals(originalSkjæringsdato, nySkjæringsdato);
    }


    private boolean erStartpunktBeregning(BehandlingReferanse nyBehandlingRef){
        var perioderFraSøknad = ytelsesFordelingRepository.hentAggregat(nyBehandlingRef.behandlingId()).getOppgittFordeling().getPerioder();
        var gradertePerioderFraSøknad = finnGradertePerioder(perioderFraSøknad);

        if (gradertePerioderFraSøknad.isEmpty()){
            return false;
        }

        // Det ligger en endringssøknad på denne behandlingen. Pluss har gradering. Pluss ikke på gjenbesøk.
        return harBehandlingEndringssøknad(nyBehandlingRef);
    }


    private List<OppgittPeriodeEntitet> finnGradertePerioder(List<OppgittPeriodeEntitet> perioder) {
        if(perioder == null) {
            throw new IllegalStateException("Utviklerfeil: forventer at behandling har oppgitte perioder");
        }
        return perioder.stream()
            .filter(OppgittPeriodeEntitet::isGradert)
            .toList();
    }

    private Boolean harBehandlingEndringssøknad(BehandlingReferanse referanse) {
        return søknadRepository.hentSøknadFraGrunnlag(referanse.behandlingId())
            .map(SøknadEntitet::erEndringssøknad)
            .orElse(false);
    }
}

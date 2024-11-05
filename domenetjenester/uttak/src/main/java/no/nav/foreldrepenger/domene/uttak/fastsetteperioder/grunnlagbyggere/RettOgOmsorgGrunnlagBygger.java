package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RettOgOmsorg;

@ApplicationScoped
public class RettOgOmsorgGrunnlagBygger {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    RettOgOmsorgGrunnlagBygger() {
        // CDI
    }

    @Inject
    public RettOgOmsorgGrunnlagBygger(UttakRepositoryProvider uttakRepositoryProvider, ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.ytelsesFordelingRepository = uttakRepositoryProvider.getYtelsesFordelingRepository();
        this.uttakTjeneste = uttakTjeneste;
        this.behandlingsresultatRepository = uttakRepositoryProvider.getBehandlingsresultatRepository();
    }

    public RettOgOmsorg.Builder byggGrunnlag(UttakInput uttakInput) {
        var ref = uttakInput.getBehandlingReferanse();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());
        var annenpartsUttaksplan = hentAnnenpartsUttak(uttakInput);
        var samtykke = samtykke(ytelseFordelingAggregat);
        return new RettOgOmsorg.Builder()
                .aleneomsorg(ytelseFordelingAggregat.robustHarAleneomsorg(uttakInput.getBehandlingReferanse().relasjonRolle()))
                .farHarRett(farHarRett(ref, ytelseFordelingAggregat, annenpartsUttaksplan))
                .morHarRett(morHarRett(ref, ytelseFordelingAggregat, annenpartsUttaksplan))
                .morUføretrygd(morUføretrygd(uttakInput, ytelseFordelingAggregat))
                .morOppgittUføretrygd(morOppgittUføretrygd(uttakInput))
                .samtykke(samtykke)
                .harOmsorg(ytelseFordelingAggregat.harOmsorg())
            ;
    }

    private Optional<ForeldrepengerUttak> hentAnnenpartsUttak(UttakInput uttakInput) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var annenpart = fpGrunnlag.getAnnenpart();
        if (annenpart.isEmpty()) {
            return Optional.empty();
        }
        return uttakTjeneste.hentHvisEksisterer(annenpart.get().gjeldendeVedtakBehandlingId());
    }

    private boolean farHarRett(BehandlingReferanse ref, YtelseFordelingAggregat ytelseFordelingAggregat, Optional<ForeldrepengerUttak> annenpartsUttaksplan) {
        var relasjonsRolleType = ref.relasjonRolle();
        if (RelasjonsRolleType.erMor(relasjonsRolleType)) {
            return ytelseFordelingAggregat.harAnnenForelderRett(annenpartsUttaksplan.filter(ForeldrepengerUttak::harUtbetaling).isPresent());
        }
        if (RelasjonsRolleType.erFarEllerMedmor(relasjonsRolleType)) {
            return harSøkerRett(ref);
        }
        throw new IllegalStateException("Uventet foreldrerolletype " + relasjonsRolleType);
    }

    private boolean morHarRett(BehandlingReferanse ref, YtelseFordelingAggregat ytelseFordelingAggregat, Optional<ForeldrepengerUttak> annenpartsUttaksplan) {
        var relasjonsRolleType = ref.relasjonRolle();
        if (RelasjonsRolleType.erMor(relasjonsRolleType)) {
            return harSøkerRett(ref);
        }
        if (RelasjonsRolleType.erFarEllerMedmor(relasjonsRolleType)) {
            return ytelseFordelingAggregat.harAnnenForelderRett(annenpartsUttaksplan.filter(ForeldrepengerUttak::harUtbetaling).isPresent());
        }
        throw new IllegalStateException("Uventet foreldrerolletype " + relasjonsRolleType);
    }

    private boolean morUføretrygd(UttakInput uttakInput, YtelseFordelingAggregat ytelseFordelingAggregat) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        return ytelseFordelingAggregat.morMottarUføretrygd(fpGrunnlag.getUføretrygdGrunnlag().orElse(null));
    }

    private boolean morOppgittUføretrygd(UttakInput uttakInput) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        return fpGrunnlag.getUføretrygdGrunnlag().isPresent();
    }

    private boolean samtykke(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return ytelseFordelingAggregat.getOppgittFordeling().getErAnnenForelderInformert();
    }

    private boolean harSøkerRett(BehandlingReferanse ref) {
        return !behandlingsresultatRepository.hent(ref.behandlingId()).isInngangsVilkårAvslått();
    }
}

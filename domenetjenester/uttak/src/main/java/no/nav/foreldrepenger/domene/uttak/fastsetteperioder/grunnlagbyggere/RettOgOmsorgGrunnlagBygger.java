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
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RettighetType;

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
        var annenpartHarForeldrepengerUtbetaling = annenpartsUttaksplan.filter(ForeldrepengerUttak::harUtbetaling).isPresent();
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var rettighetType = map(ytelseFordelingAggregat.getRettighetType(annenpartHarForeldrepengerUtbetaling, ref.relasjonRolle(),
            ytelsespesifiktGrunnlag.getUføretrygdGrunnlag().orElse(null)));
        return new RettOgOmsorg.Builder()
            .rettighetType(rettighetType)
                .morOppgittUføretrygd(morOppgittUføretrygd(uttakInput))
                .samtykke(samtykke)
                .harOmsorg(ytelseFordelingAggregat.harOmsorg())
            ;
    }

    private RettighetType map(no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.RettighetType rettighetType) {
        return switch (rettighetType) {
            case ALENEOMSORG -> RettighetType.ALENEOMSORG;
            case BEGGE_RETT, BEGGE_RETT_EØS -> RettighetType.BEGGE_RETT;
            case BARE_SØKER_RETT -> RettighetType.BARE_SØKER_RETT;
            case BARE_FAR_RETT_MOR_UFØR -> RettighetType.BARE_FAR_RETT_MOR_UFØR;
        };
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

package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static java.lang.Boolean.TRUE;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
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
        var farHarRett = farHarRett(ref, ytelseFordelingAggregat, annenpartsUttaksplan);
        var morHarRett = morHarRett(ref, ytelseFordelingAggregat, annenpartsUttaksplan);
        var oppgittAleneomsorg = TRUE.equals(ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet());
        var oppgittAnnenForelderRett = TRUE.equals(ytelseFordelingAggregat.getOppgittRettighet().getHarAnnenForeldreRett());
        if (!oppgittAleneomsorg && oppgittAnnenForelderRett && !samtykke) {
            throw new IllegalStateException("Midlertidig feil. Søknad opplyser om manglende samtykke");
        }
        return new RettOgOmsorg.Builder()
                .aleneomsorg(aleneomsorg(ytelseFordelingAggregat))
                .farHarRett(farHarRett)
                .morHarRett(morHarRett)
                .morUføretrygd(morUføretrygd(uttakInput))
                .samtykke(samtykke);
    }

    private Optional<ForeldrepengerUttak> hentAnnenpartsUttak(UttakInput uttakInput) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var annenpart = fpGrunnlag.getAnnenpart();
        if (annenpart.isEmpty()) {
            return Optional.empty();
        }
        return uttakTjeneste.hentUttakHvisEksisterer(annenpart.get().gjeldendeVedtakBehandlingId());
    }

    private boolean aleneomsorg(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return UttakOmsorgUtil.harAleneomsorg(ytelseFordelingAggregat);
    }

    private boolean farHarRett(BehandlingReferanse ref, YtelseFordelingAggregat ytelseFordelingAggregat, Optional<ForeldrepengerUttak> annenpartsUttaksplan) {
        var relasjonsRolleType = ref.relasjonRolle();
        if (RelasjonsRolleType.erMor(relasjonsRolleType)) {
            return UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat, annenpartsUttaksplan);
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
            return UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat, annenpartsUttaksplan);
        }
        throw new IllegalStateException("Uventet foreldrerolletype " + relasjonsRolleType);
    }

    private boolean morUføretrygd(UttakInput uttakInput) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        return fpGrunnlag.getUføretrygdGrunnlag()
            .filter(UføretrygdGrunnlagEntitet::annenForelderMottarUføretrygd)
            .isPresent();
    }

    private boolean samtykke(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return ytelseFordelingAggregat.getOppgittFordeling().getErAnnenForelderInformert();
    }

    private boolean harSøkerRett(BehandlingReferanse ref) {
        return !behandlingsresultatRepository.hent(ref.behandlingId()).isVilkårAvslått();
    }
}

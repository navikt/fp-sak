package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import static java.lang.Boolean.TRUE;
import static no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil.annenForelderHarUttakMedUtbetaling;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.FaktaUttakAksjonspunktUtleder;

/**
 * Aksjonspunkt for Avklar Annen forelder har rett
 */
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class AnnenForelderHarRettAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    @Inject
    public AnnenForelderHarRettAksjonspunktUtleder(UttakRepositoryProvider repositoryProvider,
                                                   PersonopplysningTjeneste personopplysningTjeneste, ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.uttakTjeneste = uttakTjeneste;
    }

    AnnenForelderHarRettAksjonspunktUtleder() {
        // For CDI
    }

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        YtelseFordelingAggregat ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.getBehandlingId());

        if (!Objects.equals(ref.getBehandlingType(), BehandlingType.FØRSTEGANGSSØKNAD) ||
            oppgittAnnenpart(ref).isEmpty()) {
            return List.of();
        }

        var annenpartsGjeldendeUttaksplan = hentAnnenpartsUttak(input.getYtelsespesifiktGrunnlag());

        if (!oppgittHarAnnenForeldreRett(ytelseFordelingAggregat) &&
            !oppgittAleneomsorg(ytelseFordelingAggregat) &&
            !annenForelderHarUttakMedUtbetaling(annenpartsGjeldendeUttaksplan)) {
            ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
            if (fpGrunnlag.getAnnenpart().isPresent()) {
                boolean harAnnennartInnvilgetES = fpGrunnlag.getAnnenpart().get().harInnvilgetES();
                return harAnnennartInnvilgetES ? List.of() : aksjonspunkt();
            }
            return aksjonspunkt();
        }

        if (oppgittHarAnnenForeldreRett(ytelseFordelingAggregat) &&
            erFarMedmor(ref.getRelasjonsRolleType()) &&
            !annenForelderHarUttakMedUtbetaling(annenpartsGjeldendeUttaksplan)) {
            return aksjonspunkt();
        }

        return List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return false;
    }

    private Optional<ForeldrepengerUttak> hentAnnenpartsUttak(ForeldrepengerGrunnlag fpGrunnlag) {
        var annenpart = fpGrunnlag.getAnnenpart();
        if (annenpart.isPresent()) {
            return uttakTjeneste.hentUttakHvisEksisterer(annenpart.get().getGjeldendeVedtakBehandlingId());
        }
        return Optional.empty();
    }

    private List<AksjonspunktDefinisjon> aksjonspunkt() {
        return List.of(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT);
    }

    private Optional<AktørId> oppgittAnnenpart(BehandlingReferanse ref) {
        final PersonopplysningerAggregat personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        return personopplysningerAggregat.getOppgittAnnenPart().map(OppgittAnnenPartEntitet::getAktørId);
    }

    private boolean erFarMedmor(RelasjonsRolleType relasjonsRolleType) {
        return RelasjonsRolleType.erFarEllerMedmor(relasjonsRolleType);
    }

    private boolean oppgittAleneomsorg(YtelseFordelingAggregat ytelseFordelingAggregat) {
        Boolean aleneomsorg = ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet();
        return Objects.equals(TRUE, aleneomsorg);
    }

    public static boolean oppgittHarAnnenForeldreRett(YtelseFordelingAggregat ytelseFordelingAggregat) {
        Boolean harAnnenForeldreRett = ytelseFordelingAggregat.getOppgittRettighet().getHarAnnenForeldreRett();
        return harAnnenForeldreRett == null || Objects.equals(TRUE, harAnnenForeldreRett);
    }
}

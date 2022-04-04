package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeForbeholdtMor;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fakta.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

/**
 * Aksjonspunkter for Manuell kontroll av om bruker har Omsorg
 */
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class BrukerHarOmsorgAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private PersonopplysningerForUttak personopplysninger;

    @Inject
    public BrukerHarOmsorgAksjonspunktUtleder(UttakRepositoryProvider repositoryProvider,
                                              PersonopplysningerForUttak personopplysninger) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.personopplysninger = personopplysninger;
    }
    BrukerHarOmsorgAksjonspunktUtleder() {

    }

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var familieHendelser = fpGrunnlag.getFamilieHendelser();
        var familieHendelse = familieHendelser.getGjeldendeFamilieHendelse();
        var bekreftetFH = familieHendelser.getBekreftetFamilieHendelse();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.getBehandlingId());

        if (familieHendelser.getGjeldendeFamilieHendelse().erAlleBarnDøde()) {
            return List.of();
        }

        if (harOppgittOmsorgTilBarnetIHeleSøknadsperioden(ytelseFordelingAggregat) == Utfall.JA) {
            if (bekreftetFH.isPresent() && erBarnetFødt(bekreftetFH.get()) == Utfall.JA
                && !personopplysninger.barnHarSammeBosted(ref)) {
                return List.of(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
            }
        } else {
            if (familieHendelser.gjelderTerminFødsel()) {
                if (erBrukerMor(ref.getRelasjonsRolleType()) == Utfall.NEI ||
                    erSøknadsperiodenLengreEnnAntallUkerForbeholdtMorEtterFødselen(familieHendelse,
                        ytelseFordelingAggregat) == Utfall.JA) {
                    return List.of(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
                }
            } else {
                return List.of(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
            }
        }
        return List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return false;
    }

    private Utfall harOppgittOmsorgTilBarnetIHeleSøknadsperioden(YtelseFordelingAggregat ytelseFordelingAggregat) {
        var harOmsorgForBarnetIHelePerioden = ytelseFordelingAggregat.getOppgittRettighet()
            .getHarOmsorgForBarnetIHelePerioden();
        Objects.requireNonNull(harOmsorgForBarnetIHelePerioden,
            "harOmsorgForBarnetIHelePerioden må være sett"); //$NON-NLS-1$
        return harOmsorgForBarnetIHelePerioden ? Utfall.JA : Utfall.NEI;
    }

    private Utfall erBarnetFødt(FamilieHendelse bekreftet) {
        return !bekreftet.getBarna().isEmpty() ? Utfall.JA : Utfall.NEI;
    }

    private Utfall erBrukerMor(RelasjonsRolleType relasjonsRolleType) {
        return RelasjonsRolleType.MORA.equals(relasjonsRolleType) ? Utfall.JA : Utfall.NEI;
    }

    private Utfall erSøknadsperiodenLengreEnnAntallUkerForbeholdtMorEtterFødselen(FamilieHendelse familieHendelse,
                                                                                  YtelseFordelingAggregat ytelseFordelingAggregat) {

        var sisteSøknadsDato = finnSisteSøknadsDato(ytelseFordelingAggregat);

        if (sisteSøknadsDato.isEmpty()) {
            throw new IllegalArgumentException("Fant ikke siste søknads dato");
        }

        var familiehendelseDato = Stream.of(familieHendelse.getFødselsdato(), familieHendelse.getTermindato())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke familiehendelsedato"));

        return sisteSøknadsDato.filter(
            søknadsDato -> søknadsDato.isAfter(TidsperiodeForbeholdtMor.tilOgMed(familiehendelseDato)))
            .map(søknadsDato -> Utfall.JA)
            .orElse(Utfall.NEI);
    }

    private Optional<LocalDate> finnSisteSøknadsDato(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return ytelseFordelingAggregat.getGjeldendeSøknadsperioder()
            .getOppgittePerioder()
            .stream()
            .max(Comparator.comparing(OppgittPeriodeEntitet::getTom))
            .map(OppgittPeriodeEntitet::getTom);
    }

}

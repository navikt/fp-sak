package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.omsorg;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.FaktaUttakAksjonspunktUtleder;
import no.nav.vedtak.konfig.KonfigVerdi;

/**
 * Aksjonspunkter for Manuell kontroll av om bruker har Omsorg
 */
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class BrukerHarOmsorgAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private Period periodeForbeholdtMorEtterFødsel;

    BrukerHarOmsorgAksjonspunktUtleder() {
    }

    /**
     * @param periodeForbeholdtMorEtterFødsel - Antall uker forbeholdt for mor etter fødsel.
     */
    @Inject
    public BrukerHarOmsorgAksjonspunktUtleder(UttakRepositoryProvider repositoryProvider,
                                              PersonopplysningTjeneste personopplysningTjeneste,
                                              @KonfigVerdi(value = "fp.periode.forbeholdt.mor.etter.fødsel", defaultVerdi = "P6W") Period periodeForbeholdtMorEtterFødsel) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.periodeForbeholdtMorEtterFødsel = periodeForbeholdtMorEtterFødsel;
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
            var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
            if (bekreftetFH.isPresent() && erBarnetFødt(bekreftetFH.get()) == Utfall.JA && harBarnSammeBosted(personopplysningerAggregat) == Utfall.NEI) {
                return List.of(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
            }
        } else {
            if (familieHendelser.gjelderTerminFødsel()) {
                if (erBrukerMor(ref.getRelasjonsRolleType()) == Utfall.NEI ||
                    erSøknadsperiodenLengreEnnAntallUkerForbeholdtMorEtterFødselen(familieHendelse, ytelseFordelingAggregat) == Utfall.JA) {
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
        Boolean harOmsorgForBarnetIHelePerioden = ytelseFordelingAggregat.getOppgittRettighet().getHarOmsorgForBarnetIHelePerioden();
        Objects.requireNonNull(harOmsorgForBarnetIHelePerioden, "harOmsorgForBarnetIHelePerioden må være sett"); //$NON-NLS-1$
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

        Optional<LocalDate> sisteSøknadsDato = finnSisteSøknadsDato(ytelseFordelingAggregat);

        if (sisteSøknadsDato.isEmpty()) {
            throw new IllegalArgumentException("Fant ikke siste søknads dato");
        }

        LocalDate familiehendelseDato = Stream.of(familieHendelse.getFødselsdato(), familieHendelse.getTermindato())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke familiehendelsedato"));

        return sisteSøknadsDato.filter(søknadsDato -> søknadsDato.isAfter(familiehendelseDato.plus(periodeForbeholdtMorEtterFødsel).minusDays(1)))
            .map(søknadsDato -> Utfall.JA).orElse(Utfall.NEI);

    }

    private Optional<LocalDate> finnSisteSøknadsDato(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return ytelseFordelingAggregat.getGjeldendeSøknadsperioder()
            .getOppgittePerioder()
            .stream()
            .max(Comparator.comparing(OppgittPeriodeEntitet::getTom))
            .map(OppgittPeriodeEntitet::getTom);
    }

    private Utfall harBarnSammeBosted(PersonopplysningerAggregat personopplysningerAggregat) {
        List<PersonRelasjonEntitet> barnRelasjoner = personopplysningerAggregat.getSøkersRelasjoner().stream()
            .filter(familierelasjon -> familierelasjon.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .filter(rel -> Objects.nonNull(rel.getHarSammeBosted()))
            .filter(rel -> erIkkeDød(personopplysningerAggregat, rel))
            .collect(Collectors.toList());

        if (!barnRelasjoner.isEmpty()) {
            return barnRelasjoner.stream().allMatch(PersonRelasjonEntitet::getHarSammeBosted) ? JA : NEI;
        } else {
            return harSammeAdresseSomBarn(personopplysningerAggregat) ? JA : NEI;
        }
    }

    private boolean erIkkeDød(PersonopplysningerAggregat personopplysningerAggregat, PersonRelasjonEntitet rel) {
        return personopplysningerAggregat.getPersonopplysning(rel.getTilAktørId()).getDødsdato() == null;
    }

    private boolean harSammeAdresseSomBarn(PersonopplysningerAggregat personopplysningerAggregat) {
        for (PersonAdresseEntitet opplysningAdresseSøker : personopplysningerAggregat.getAdresserFor(personopplysningerAggregat.getSøker().getAktørId())) {
            for (PersonopplysningEntitet barn : personopplysningerAggregat.getBarna()) {
                if (harBarnetSammeAdresse(personopplysningerAggregat, opplysningAdresseSøker, barn)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean harBarnetSammeAdresse(PersonopplysningerAggregat personopplysningerAggregat,
                                          PersonAdresseEntitet opplysningAdresseSøker,
                                          PersonopplysningEntitet barn) {
        if (barn.getDødsdato() != null) {
            return true;
        }
        for (PersonAdresseEntitet opplysningAdresseBarn : personopplysningerAggregat.getAdresserFor(barn.getAktørId())) {
            if (Objects.equals(opplysningAdresseSøker.getAdresselinje1(), opplysningAdresseBarn.getAdresselinje1())
                && Objects.equals(opplysningAdresseSøker.getPostnummer(), opplysningAdresseBarn.getPostnummer())) {
                return true;
            }
        }
        return false;
    }
}

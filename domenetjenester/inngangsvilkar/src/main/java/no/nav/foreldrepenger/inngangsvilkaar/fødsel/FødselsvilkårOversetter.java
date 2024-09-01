package no.nav.foreldrepenger.inngangsvilkaar.fødsel;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårGrunnlag;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class FødselsvilkårOversetter {

    private static final Map<NavBrukerKjønn, RegelKjønn> MAP_KJØNN = Map.of(
        NavBrukerKjønn.KVINNE, RegelKjønn.KVINNE,
        NavBrukerKjønn.MANN, RegelKjønn.MANN
    );

    private static final Map<RelasjonsRolleType, RegelSøkerRolle> MAP_ROLLE_TYPE = Map.of(
        RelasjonsRolleType.MORA, RegelSøkerRolle.MORA,
        RelasjonsRolleType.MEDMOR, RegelSøkerRolle.MEDMOR,
        RelasjonsRolleType.FARA, RegelSøkerRolle.FARA
    );

    private FamilieHendelseRepository familieGrunnlagRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;

    private Period tidligstUtstedelseFørTermin;

    FødselsvilkårOversetter() {
        // for CDI proxy
    }

    /**
     * @param tidligsteUtstedelseAvTerminBekreftelse - Periode for tidligst utstedelse av terminbekreftelse før termindato
     */
    @Inject
    public FødselsvilkårOversetter(BehandlingRepositoryProvider repositoryProvider,
                                   PersonopplysningTjeneste personopplysningTjeneste,
                                   @KonfigVerdi(value = "terminbekreftelse.tidligst.utstedelse.før.termin", defaultVerdi = "P18W3D") Period tidligsteUtstedelseAvTerminBekreftelse) {
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.tidligstUtstedelseFørTermin = tidligsteUtstedelseAvTerminBekreftelse;
    }

    public FødselsvilkårGrunnlag oversettTilRegelModellFødsel(BehandlingReferanse ref, boolean utenMinsterett) {
        var medFarMedmorUttakRundtFødsel = !utenMinsterett;
        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(ref.behandlingId());
        var bekreftetFamilieHendelse = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon();
        var gjeldendeTerminbekreftelse = familieHendelseGrunnlag.getGjeldendeTerminbekreftelse();
        var kjønn = tilSøkerKjøenn(getSøkersKjønn(ref));
        var rolle = finnSoekerRolle(ref).orElse(null);
        var bekreftetFødselsDato = bekreftetFamilieHendelse.flatMap(FamilieHendelseEntitet::getFødselsdato).orElse(null);
        var gjeldendeTermindato = gjeldendeTerminbekreftelse.map(TerminbekreftelseEntitet::getTermindato).orElse(null);
        var gjeldendeUtstedtDato = gjeldendeTerminbekreftelse.map(TerminbekreftelseEntitet::getUtstedtdato).orElse(null);
        var antallbarn = bekreftetFamilieHendelse.map(FamilieHendelseEntitet::getAntallBarn).orElse(0);
        var fristRegistreringUtløpt = FamilieHendelseTjeneste.getManglerFødselsRegistreringFristUtløpt(familieHendelseGrunnlag);
        var morForSykVedFødsel = bekreftetFamilieHendelse.map(FamilieHendelseEntitet::erMorForSykVedFødsel).orElse(false);
        var søktOmTermin = erSøktOmTermin(familieHendelseGrunnlag.getSøknadVersjon());
        var behandlingsdatoEtterTidligsteDato = erBehandlingsdatoEtterTidligsteDato(gjeldendeTermindato);
        var terminbekreftelseUtstedtEtterTidligsteDato = erTerminbekreftelseUtstedtEtterTidligsteDato(gjeldendeTermindato, gjeldendeUtstedtDato);

        return new FødselsvilkårGrunnlag(kjønn, rolle, LocalDate.now(),
            bekreftetFødselsDato, gjeldendeTermindato,
            antallbarn,
            fristRegistreringUtløpt,
            morForSykVedFødsel, søktOmTermin,
            behandlingsdatoEtterTidligsteDato,
            terminbekreftelseUtstedtEtterTidligsteDato, medFarMedmorUttakRundtFødsel);
    }

    /**
     * Presisering fra fag: Fra og med man er i svangerskapsuke 22 kan man søke og få innvilget ES/FP.
     * Dette er tolket som FOMdato = termindato - 18uker - 3dager
     */
    private boolean erTerminbekreftelseUtstedtEtterTidligsteDato(LocalDate termindato, LocalDate utstedtDato) {
        if (termindato == null || utstedtDato == null) {
            return true;
        }
        var tidligstedatoMinusDag = termindato.minus(tidligstUtstedelseFørTermin).minusDays(1);
        return utstedtDato.isAfter(tidligstedatoMinusDag);
    }

    private boolean erBehandlingsdatoEtterTidligsteDato(LocalDate termindato) {
        if (termindato == null) {
            return true;
        }
        var tidligstedatoMinusDag = termindato.minus(tidligstUtstedelseFørTermin).minusDays(1);
        return LocalDate.now().isAfter(tidligstedatoMinusDag);
    }

    private static boolean erSøktOmTermin(FamilieHendelseEntitet familieHendelse) {
        var type = familieHendelse.getType();
        return FamilieHendelseType.TERMIN.equals(type);
    }

    private NavBrukerKjønn getSøkersKjønn(BehandlingReferanse ref) {
        try {
            return personopplysningTjeneste.hentPersonopplysninger(ref).getSøker().getKjønn();
        } catch (Exception e) {
            return NavBrukerKjønn.UDEFINERT;
        }
    }

    private Optional<RegelSøkerRolle> finnSoekerRolle(BehandlingReferanse ref) {
        return Optional.ofNullable(finnRelasjonRolle(ref)).map(MAP_ROLLE_TYPE::get);
    }

    private RelasjonsRolleType finnRelasjonRolle(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var hendelseGrunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        var familiehendelse = hendelseGrunnlag.getGjeldendeBekreftetVersjon();
        if (familiehendelse.isEmpty()) {
            // Kan ikke finne relasjonsrolle dersom fødsel ikke er bekreftet.
            return null;
        }
        var fødselsdato = familiehendelse.get().getFødselsdato();
        if (fødselsdato.isEmpty()) {
            return null;
        }

        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);

        var fødselIntervall = byggIntervall(fødselsdato.get(), fødselsdato.get());
        var alleBarnPåFødselsdato = personopplysninger.getAlleBarnFødtI(fødselIntervall);

        var søkerPersonopplysning = personopplysninger.getSøker();
        var søkersAktørId = søkerPersonopplysning.getAktørId();

        if (!alleBarnPåFødselsdato.isEmpty()) {
            // Forutsetter at barn som er født er tvillinger, og sjekker derfor bare første barn.
            var personRelasjon = personopplysninger.getRelasjoner()
                .stream()
                .filter(relasjon -> relasjon.getTilAktørId().equals(søkersAktørId))
                .filter(familierelasjon -> RelasjonsRolleType.erRegistrertForeldre(familierelasjon.getRelasjonsrolle()))
                .findFirst();

            return personRelasjon.map(PersonRelasjonEntitet::getRelasjonsrolle).orElse(ref.relasjonRolle());
        }
        // Har ingenting annet å gå på så benytter det søker oppgir.
        return ref.relasjonRolle();
    }

    private static SimpleLocalDateInterval byggIntervall(LocalDate fomDato, LocalDate tomDato) {
        return SimpleLocalDateInterval.fraOgMedTomNotNull(fomDato, tomDato);
    }

    private static RegelKjønn tilSøkerKjøenn(NavBrukerKjønn søkerKjønn) {
        return Optional.ofNullable(MAP_KJØNN.get(søkerKjønn))
            .orElseThrow(() -> new NullPointerException("Fant ingen kjønn for " + søkerKjønn.getKode()));
    }

}

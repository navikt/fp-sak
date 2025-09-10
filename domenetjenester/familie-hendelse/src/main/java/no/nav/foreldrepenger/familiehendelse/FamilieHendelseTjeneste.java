package no.nav.foreldrepenger.familiehendelse;

import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.TERMIN;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.familiehendelse.event.FamiliehendelseEventPubliserer;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class FamilieHendelseTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FamilieHendelseTjeneste.class);

    private static final Period REGISTRERING_FRIST_ETTER_TERMIN = Period.parse("P17D");
    private static final Period REGISTRERING_FRIST_ETTER_FØDSEL = Period.parse("P7D");

    private static final Period MATCH_INTERVAlL_TERMIN = Period.parse("P19W");
    private static final Period MATCH_INTERVAlL_FØDSEL = Period.parse("P6W");

    public static final Period VENT_FØDSELSREGISTRERING_AUTOPUNKT = Period.parse("P8D"); // Sikre evt aksjonspunkt

    private FamilieHendelseRepository familieGrunnlagRepository;
    private FamiliehendelseEventPubliserer familiehendelseEventPubliserer;

    FamilieHendelseTjeneste() {
        // CDI
    }

    @Inject
    public FamilieHendelseTjeneste(FamiliehendelseEventPubliserer familiehendelseEventPubliserer,
                                   FamilieHendelseRepository familieHendelseRepository) {
        this.familieGrunnlagRepository = familieHendelseRepository;
        this.familiehendelseEventPubliserer = familiehendelseEventPubliserer;
    }

    public List<LocalDateInterval> forventetFødselsIntervaller(BehandlingReferanse ref) {
        return forventetFødselsIntervaller(ref.behandlingId());
    }

    public List<LocalDateInterval> forventetFødselsIntervaller(Long behandlingId) {
        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        return utledPerioderForRegisterinnhenting(familieHendelseGrunnlag);
    }

    public boolean erHendelseDatoRelevantForBehandling(Long behandlingId, LocalDate hendelsedato) {
        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId).orElse(null);
        if (familieHendelseGrunnlag == null || !familieHendelseGrunnlag.getGjeldendeVersjon().getGjelderFødsel()) {
            return false;
        }
        return utledPerioderForRegisterinnhenting(familieHendelseGrunnlag).stream()
            .anyMatch(i -> i.encloses(hendelsedato));
    }

    public boolean matcherFødselsSøknadMedBehandling(FamilieHendelseGrunnlagEntitet grunnlag, LocalDate termindato, LocalDate fødselsdato) {
        // Finn behandling
        if (grunnlag == null || termindato == null && fødselsdato == null || !FamilieHendelseType.gjelderFødsel(
            grunnlag.getGjeldendeVersjon().getType())) {
            return false;
        }
        List<LocalDateSegment<Boolean>> søknadSegmenter = new ArrayList<>();
        if (termindato != null)
            søknadSegmenter.add(segmentForFødselsdato(termindato)); // Holder med ett utvidet intervall - det fra grunnlaget
        if (fødselsdato != null)
            søknadSegmenter.add(segmentForFødselsdato(fødselsdato));
        var tidslineSøknad = new LocalDateTimeline<>(søknadSegmenter, StandardCombinators::alwaysTrueForMatch).compress();
        return utledTidslineFraGrunnlag(grunnlag).intersects(tidslineSøknad);
    }

    public boolean matcherOmsorgsSøknadMedBehandling(FamilieHendelseGrunnlagEntitet grunnlag, LocalDate omsorgDato, List<LocalDate> omsorgFødselsdatoer) {
        // Finn behandling
        if (grunnlag == null || omsorgDato == null || !FamilieHendelseType.gjelderAdopsjon(grunnlag.getGjeldendeVersjon().getType())) {
            return false;
        }
        var omsorgdatoGrunnlag = grunnlag.getGjeldendeVersjon().getSkjæringstidspunkt();
        if (omsorgdatoGrunnlag == null || !segmentForFødselsdato(omsorgDato).overlapper(segmentForFødselsdato(omsorgdatoGrunnlag)))
            return false;
        var intervallerGrunnlag =  utledPerioderForRegisterinnhenting(grunnlag);
        var antallGrunnlag = grunnlag.getGjeldendeAntallBarn();
        return antallGrunnlag == omsorgFødselsdatoer.size() && omsorgFødselsdatoer.stream()
            .allMatch(f -> intervallerGrunnlag.stream().anyMatch(i -> i.encloses(f)));
    }

    public boolean matcherGrunnlagene(FamilieHendelseGrunnlagEntitet grunnlag1, FamilieHendelseGrunnlagEntitet grunnlag2) {
        // Finn behandling
        if (grunnlag1 == null || grunnlag2 == null || !kompatibleTyper(grunnlag1.getGjeldendeVersjon(), grunnlag2.getGjeldendeVersjon())) {
            return false;
        }
        if (grunnlag1.getGjeldendeVersjon().getGjelderAdopsjon() && grunnlag2.getGjeldendeVersjon().getGjelderAdopsjon()) {
            var omsorgIntervall1 = segmentForFødselsdato(grunnlag1.getGjeldendeVersjon().getSkjæringstidspunkt());
            var omsorgIntervall2 = segmentForFødselsdato(grunnlag2.getGjeldendeVersjon().getSkjæringstidspunkt());
            if (!omsorgIntervall1.overlapper(omsorgIntervall2))
                return false;
        }
        var intervallerGrunnlag1 =  utledPerioderForRegisterinnhenting(grunnlag1);
        var intervallerGrunnlag2 =  utledPerioderForRegisterinnhenting(grunnlag2);
        int antallGrunnlag1 = grunnlag1.getGjeldendeAntallBarn();
        int antallGrunnlag2 = grunnlag2.getGjeldendeAntallBarn();
        return antallGrunnlag1 == antallGrunnlag2 &&
            intervallerGrunnlag1.stream().allMatch(i1 -> intervallerGrunnlag2.stream().anyMatch(i1::overlaps)) &&
            intervallerGrunnlag2.stream().allMatch(i2 -> intervallerGrunnlag1.stream().anyMatch(i2::overlaps));
    }

    private boolean kompatibleTyper(FamilieHendelseEntitet hendelse1, FamilieHendelseEntitet hendelse2) {
        return hendelse1.getGjelderFødsel() && hendelse2.getGjelderFødsel() || gjelderStebarnsadopsjon(hendelse1, hendelse2) ||
            hendelse1.getType().equals(hendelse2.getType());
    }

    private boolean gjelderStebarnsadopsjon(FamilieHendelseEntitet hendelse1, FamilieHendelseEntitet hendelse2) {
        return hendelse1.getGjelderFødsel() && hendelse2.getGjelderAdopsjon() || hendelse1.getGjelderAdopsjon() && hendelse2.getGjelderFødsel();
    }

    public void oppdaterFødselPåGrunnlag(Long behandlingId, List<FødtBarnInfo> bekreftetPdl) {
        var tidligereRegistrertFødselsdato = hentRegisterFødselsdato(behandlingId).orElse(null);
        var gjeldendeTermin = hentGjeldendeTerminbekreftelse(behandlingId); // Kun overstyrt/bekreftet

        if (bekreftetPdl.isEmpty()) {
            if (tidligereRegistrertFødselsdato != null) {
                LOG.info("Ungt Barn Forsvunnet fra Register for sak, behandling {}", behandlingId);
            }
            return;
        }

        var hendelseBuilder = familieGrunnlagRepository.opprettBuilderForRegister(behandlingId).tilbakestillBarn();

        bekreftetPdl.forEach(barn -> hendelseBuilder.leggTilBarn(barn.fødselsdato(), barn.getDødsdato().orElse(null)));
        hendelseBuilder.medAntallBarn(bekreftetPdl.size());
        // Bruker gjeldende bekreftet termindato fra forrige behandling - ellers brukes den fra søknad implisitt
        gjeldendeTermin.ifPresent(t -> {
            var tBuilder = hendelseBuilder.getTerminbekreftelseBuilder()
                .medTermindato(t.getTermindato())
                .medUtstedtDato(t.getUtstedtdato())
                .medNavnPå(t.getNavnPå());
            hendelseBuilder.medTerminbekreftelse(tBuilder);
        });

        familieGrunnlagRepository.lagreRegisterHendelse(behandlingId, hendelseBuilder);

        var sisteRegistrertFødselsdato = hentRegisterFødselsdato(behandlingId).orElse(null);

        var familieHendelseGrunnlag = hentAggregat(behandlingId);
        if (TERMIN.equals(familieHendelseGrunnlag.getSøknadVersjon().getType()) &&
            familieHendelseGrunnlag.getBekreftetVersjon().map(FamilieHendelseEntitet::getType).map(FØDSEL::equals).orElse(Boolean.FALSE)) {
            familiehendelseEventPubliserer.fireEventTerminFødsel(behandlingId, tidligereRegistrertFødselsdato, sisteRegistrertFødselsdato);
        }
    }


    public FamilieHendelseBuilder opprettBuilderForOverstyring(Long behandlingId) {
        return familieGrunnlagRepository.opprettBuilderForOverstyring(behandlingId);
    }


    public void lagreOverstyrtHendelse(Long behandlingId, FamilieHendelseBuilder hendelse) {
        var tidligereGjeldendeFødselsdato = hentGjeldendeBekreftetFødselsdato(behandlingId).orElse(null);

        familieGrunnlagRepository.lagreOverstyrtHendelse(behandlingId, hendelse);

        var sisteGjeldendeFødselsdato = hentGjeldendeBekreftetFødselsdato(behandlingId).orElse(null);

        var familieHendelseGrunnlag = hentAggregat(behandlingId);
        if (TERMIN.equals(familieHendelseGrunnlag.getSøknadVersjon().getType()) &&
            familieHendelseGrunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getType).filter(FØDSEL::equals).isPresent()) {
            familiehendelseEventPubliserer.fireEventTerminFødsel(behandlingId, tidligereGjeldendeFødselsdato, sisteGjeldendeFødselsdato);
        }
    }

    public FamilieHendelseGrunnlagEntitet hentAggregat(Long behandlingId) {
        return familieGrunnlagRepository.hentAggregat(behandlingId);
    }

    public Optional<FamilieHendelseGrunnlagEntitet> finnAggregat(Long behandlingId) {
        return familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);
    }

    public FamilieHendelseGrunnlagEntitet hentGrunnlagPåId(Long grunnlagId) {
        return familieGrunnlagRepository.hentGrunnlagPåId(grunnlagId);
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        var funnetId = familieGrunnlagRepository.hentIdPåAktivFamiliehendelse(behandlingId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(FamilieHendelseGrunnlagEntitet.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(FamilieHendelseGrunnlagEntitet.class));
    }

    public List<PersonopplysningEntitet> finnBarnSøktStønadFor(BehandlingReferanse ref, PersonopplysningerAggregat personopplysninger) {
        var behandlingId = ref.behandlingId();
        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        var fødselsintervall = utledPerioderForRegisterinnhenting(familieHendelseGrunnlag);

        return personopplysninger.getRelasjoner().stream()
            .filter(rel -> rel.getAktørId().equals(ref.aktørId()) && rel.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(rel -> personopplysninger.getPersonopplysning(rel.getTilAktørId()))
            .filter(barn -> barn != null && erBarnRelatertTilSøknad(fødselsintervall, barn.getFødselsdato()))
            .toList();
    }

    private boolean erBarnRelatertTilSøknad(List<LocalDateInterval> relasjonsintervall, LocalDate dato) {
        return relasjonsintervall.stream()
            .anyMatch(periode -> periode.encloses(dato));
    }

    private Optional<TerminbekreftelseEntitet> hentGjeldendeTerminbekreftelse(Long behandlingId) {
        return familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId)
            .flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon)
            .flatMap(FamilieHendelseEntitet::getTerminbekreftelse);
    }

    private Optional<LocalDate> hentRegisterFødselsdato(Long behandlingId) {
        return familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId)
            .flatMap(FamilieHendelseGrunnlagEntitet::getBekreftetVersjon)
            .flatMap(FamilieHendelseEntitet::getFødselsdato);
    }

    private Optional<LocalDate> hentGjeldendeBekreftetFødselsdato(Long behandlingId) {
        return familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId)
            .flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon)
            .flatMap(FamilieHendelseEntitet::getFødselsdato);
    }


    public static boolean getManglerFødselsRegistreringFristUtløpt(FamilieHendelseGrunnlagEntitet grunnlag) {
        if (grunnlag == null || !grunnlag.getGjeldendeVersjon().getGjelderFødsel() || harBekreftetFødsel(grunnlag)) {
            return false;
        }
        var fhDato = grunnlag.finnGjeldendeFødselsdato();
        var fhDatoPlussFrist = FamilieHendelseType.FØDSEL.equals(grunnlag.getGjeldendeVersjon().getType()) ?
            REGISTRERING_FRIST_ETTER_FØDSEL : REGISTRERING_FRIST_ETTER_TERMIN;
        return LocalDate.now().isAfter(fhDato.plus(fhDatoPlussFrist));
    }

    private static boolean harBekreftetFødsel(FamilieHendelseGrunnlagEntitet grunnlag) {
        if (grunnlag.getOverstyrtVersjon().filter(FamilieHendelseTjeneste::harBekreftetFødsel).isPresent()) {
            return grunnlag.getOverstyrtVersjon().filter(FamilieHendelseTjeneste::harRegistrertFødteBarn).isPresent();
        }
        return grunnlag.getBekreftetVersjon().filter(FamilieHendelseTjeneste::harRegistrertFødteBarn).isPresent();
    }

    private static boolean harBekreftetFødsel(FamilieHendelseEntitet fh) {
        return FØDSEL.equals(fh.getType());
    }

    private static boolean harRegistrertFødteBarn(FamilieHendelseEntitet fh) {
        return FØDSEL.equals(fh.getType()) && !fh.getBarna().isEmpty();
    }

    static List<LocalDateInterval> utledPerioderForRegisterinnhenting(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        return utledTidslineFraGrunnlag(familieHendelseGrunnlag).getLocalDateIntervals().stream().toList();
    }

    static LocalDateTimeline<Boolean> utledTidslineFraGrunnlag(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var søknadVersjon = familieHendelseGrunnlag.getSøknadVersjon();
        // Tar med bekreftet / overstyrt barn hvis finnes
        List<LocalDateSegment<Boolean>> intervaller = new ArrayList<>(familieHendelseGrunnlag.getGjeldendeBekreftetVersjon()
            .map(FamilieHendelseTjeneste::intervallerForUidentifisertBarn).orElse(List.of()));

        if (FamilieHendelseType.FØDSEL.equals(søknadVersjon.getType())) {
            intervaller.addAll(intervallerForUidentifisertBarn(søknadVersjon));
        }
        if (FamilieHendelseType.TERMIN.equals(søknadVersjon.getType())) {
            var termindato = søknadVersjon.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElseThrow();
            intervaller.add(segmentForTermindato(termindato));
            // Tar med bekreftet termindato hvis finnes - men både den og søknadsdato kan ha feil (sic)
            familieHendelseGrunnlag.getGjeldendeTerminbekreftelse()
                .map(TerminbekreftelseEntitet::getTermindato)
                .filter(t -> !termindato.equals(t))
                .ifPresent(t -> intervaller.add(segmentForTermindato(t)));
        }
        if (FamilieHendelseType.ADOPSJON.equals(søknadVersjon.getType()) || FamilieHendelseType.OMSORG.equals(søknadVersjon.getType())) {
            intervaller.addAll(intervallerForUidentifisertBarn(søknadVersjon));
        }

        return intervaller.isEmpty() ? new LocalDateTimeline<>(List.of()) : new LocalDateTimeline<>(intervaller, StandardCombinators::alwaysTrueForMatch).compress();
    }

    private static List<LocalDateSegment<Boolean>> intervallerForUidentifisertBarn(FamilieHendelseEntitet familieHendelseEntitet) {
        return familieHendelseEntitet.getBarna().stream()
            .map(UidentifisertBarn::getFødselsdato)
            .map(dato -> new LocalDateSegment<>(dato.minus(MATCH_INTERVAlL_FØDSEL), dato.plus(MATCH_INTERVAlL_FØDSEL), Boolean.TRUE))
            .toList();
    }

    private static LocalDateSegment<Boolean> segmentForFødselsdato(LocalDate fødselsdato) {
        return new LocalDateSegment<>(fødselsdato.minus(MATCH_INTERVAlL_FØDSEL), fødselsdato.plus(MATCH_INTERVAlL_FØDSEL), Boolean.TRUE);
    }

    private static LocalDateSegment<Boolean> segmentForTermindato(LocalDate termindato) {
        return new LocalDateSegment<>(intervallForTermindato(termindato), Boolean.TRUE);
    }

    public static LocalDateInterval intervallForTermindato(LocalDate termindato) {
        return new LocalDateInterval(termindato.minus(MATCH_INTERVAlL_TERMIN), termindato.plus(MATCH_INTERVAlL_FØDSEL));
    }
}

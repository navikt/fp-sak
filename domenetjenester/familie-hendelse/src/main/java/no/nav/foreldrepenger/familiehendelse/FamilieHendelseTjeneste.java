package no.nav.foreldrepenger.familiehendelse;

import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.TERMIN;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
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
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.familiehendelse.event.FamiliehendelseEventPubliserer;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class FamilieHendelseTjeneste {

    private static final Period REGISTRERING_FRIST_ETTER_TERMIN = Period.parse("P25D");
    private static final Period REGISTRERING_FRIST_ETTER_FØDSEL = Period.parse("P14D");

    private static final Period MATCH_INTERVAlL_TERMIN = Period.parse("P19W");
    private static final Period MATCH_INTERVAlL_FØDSEL = Period.parse("P6W");

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
        Long behandlingId = ref.getBehandlingId();
        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        return utledPerioderForRegisterinnhenting(familieHendelseGrunnlag);
    }

    public boolean erFødselsHendelseRelevantFor(Long behandlingId, LocalDate fødselsdato) {
        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId).orElse(null);
        if (familieHendelseGrunnlag == null || !familieHendelseGrunnlag.getGjeldendeVersjon().getGjelderFødsel()) {
            return false;
        }
        return utledPerioderForRegisterinnhenting(familieHendelseGrunnlag).stream()
            .anyMatch(i -> i.encloses(fødselsdato));
    }

    public boolean matcherFødselsSøknadMedBehandling(FamilieHendelseGrunnlagEntitet grunnlag, LocalDate termindato, LocalDate fødselsdato) {
        // Finn behandling
        if (grunnlag == null || (termindato == null && fødselsdato == null) || !FamilieHendelseType.gjelderFødsel(grunnlag.getGjeldendeVersjon().getType())) {
            return false;
        }
        List<LocalDateSegment<Boolean>> søknadSegmenter = new ArrayList<>();
        if (termindato != null)
            søknadSegmenter.add(intervallForFødselsdato(termindato)); // Holder med ett utvidet intervall - det fra grunnlaget
        if (fødselsdato != null)
            søknadSegmenter.add(intervallForFødselsdato(fødselsdato));
        var tidslineSøknad = new LocalDateTimeline<>(søknadSegmenter, StandardCombinators::alwaysTrueForMatch).compress();
        return utledTidslineFraGrunnlag(grunnlag).intersects(tidslineSøknad);
    }

    public boolean matcherOmsorgsSøknadMedBehandling(FamilieHendelseGrunnlagEntitet grunnlag, LocalDate omsorgDato, List<LocalDate> omsorgFødselsdatoer) {
        // Finn behandling
        if (grunnlag == null || omsorgDato == null || !FamilieHendelseType.gjelderAdopsjon(grunnlag.getGjeldendeVersjon().getType())) {
            return false;
        }
        var omsorgdatoGrunnlag = grunnlag.getGjeldendeVersjon().getSkjæringstidspunkt();
        if (omsorgdatoGrunnlag == null || !intervallForFødselsdato(omsorgDato).overlapper(intervallForFødselsdato(omsorgdatoGrunnlag)))
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
            var omsorgIntervall1 = intervallForFødselsdato(grunnlag1.getGjeldendeVersjon().getSkjæringstidspunkt());
            var omsorgIntervall2 = intervallForFødselsdato(grunnlag2.getGjeldendeVersjon().getSkjæringstidspunkt());
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
        return (hendelse1.getGjelderFødsel() && hendelse2.getGjelderAdopsjon()) || (hendelse1.getGjelderAdopsjon() && hendelse2.getGjelderFødsel());
    }

    public void oppdaterFødselPåGrunnlag(Behandling behandling, List<FødtBarnInfo> bekreftetTps) {
        if (bekreftetTps.isEmpty()) {
            return;
        }

        LocalDate tidligereRegistrertFødselsdato = hentRegisterFødselsdato(behandling.getId()).orElse(null);

        final FamilieHendelseBuilder hendelseBuilder = familieGrunnlagRepository.opprettBuilderForregister(behandling)
            .tilbakestillBarn();

        bekreftetTps.forEach(barn -> hendelseBuilder.leggTilBarn(barn.getFødselsdato(), barn.getDødsdato().orElse(null)));
        hendelseBuilder.medAntallBarn(bekreftetTps.size());

        familieGrunnlagRepository.lagreRegisterHendelse(behandling, hendelseBuilder);

        LocalDate sisteRegistrertFødselsdato = hentRegisterFødselsdato(behandling.getId()).orElse(null);

        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = hentAggregat(behandling.getId());
        if (TERMIN.equals(familieHendelseGrunnlag.getSøknadVersjon().getType()) &&
            familieHendelseGrunnlag.getBekreftetVersjon().map(FamilieHendelseEntitet::getType).map(FØDSEL::equals).orElse(Boolean.FALSE)) {
            familiehendelseEventPubliserer.fireEventTerminFødsel(behandling, tidligereRegistrertFødselsdato, sisteRegistrertFødselsdato);
        }
    }


    public FamilieHendelseBuilder opprettBuilderFor(Behandling behandling) {
        return familieGrunnlagRepository.opprettBuilderFor(behandling);
    }


    public void lagreOverstyrtHendelse(Behandling behandling, FamilieHendelseBuilder hendelse) {
        LocalDate tidligereGjeldendeFødselsdato = hentGjeldendeBekreftetFødselsdato(behandling.getId()).orElse(null);

        familieGrunnlagRepository.lagreOverstyrtHendelse(behandling, hendelse);

        LocalDate sisteGjeldendeFødselsdato = hentGjeldendeBekreftetFødselsdato(behandling.getId()).orElse(null);

        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = hentAggregat(behandling.getId());
        if (TERMIN.equals(familieHendelseGrunnlag.getSøknadVersjon().getType()) &&
            familieHendelseGrunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getType).map(FØDSEL::equals).orElse(Boolean.FALSE) ){
            familiehendelseEventPubliserer.fireEventTerminFødsel(behandling, tidligereGjeldendeFødselsdato, sisteGjeldendeFødselsdato);
        }
    }


    public FamilieHendelseGrunnlagEntitet hentAggregat(BehandlingReferanse ref) {
        return familieGrunnlagRepository.hentAggregat(ref.getBehandlingId());
    }


    public FamilieHendelseGrunnlagEntitet hentAggregat(Long behandlingId) {
        return familieGrunnlagRepository.hentAggregat(behandlingId);
    }


    public Optional<FamilieHendelseGrunnlagEntitet> finnAggregat(Long behandlingId) {
        return familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);
    }


    public FamilieHendelseGrunnlagEntitet hentFamilieHendelserPåGrunnlagId(Long aggregatId) {
        return familieGrunnlagRepository.hentFamilieHendelserPåGrunnlagId(aggregatId);
    }


    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        Optional<Long> funnetId = familieGrunnlagRepository.hentIdPåAktivFamiliehendelse(behandlingId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(FamilieHendelseGrunnlagEntitet.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(FamilieHendelseGrunnlagEntitet.class));
    }


    public DiffResult diffResultat(EndringsresultatDiff idDiff, boolean kunSporedeEndringer) {
        FamilieHendelseGrunnlagEntitet grunnlag1 = familieGrunnlagRepository.hentFamilieHendelserPåGrunnlagId((Long) idDiff.getGrunnlagId1());
        FamilieHendelseGrunnlagEntitet grunnlag2 = familieGrunnlagRepository.hentFamilieHendelserPåGrunnlagId((Long) idDiff.getGrunnlagId2());
        return familieGrunnlagRepository.diffResultat(grunnlag1, grunnlag2, kunSporedeEndringer);
    }


    public boolean harBehandlingFamilieHendelseDato(LocalDate familieHendelseDato, Long behandlingId) {
        Optional<LocalDate> dato2 = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::finnGjeldendeFødselsdato);
        return dato2.isPresent() && familieHendelseDato.equals(dato2.get());
    }

    public List<PersonopplysningEntitet> finnBarnSøktStønadFor(BehandlingReferanse ref, PersonopplysningerAggregat personopplysninger) {
        Long behandlingId = ref.getBehandlingId();
        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        List<LocalDateInterval> fødselsintervall = utledPerioderForRegisterinnhenting(familieHendelseGrunnlag);

        return personopplysninger.getRelasjoner().stream()
            .filter(rel -> rel.getAktørId().equals(ref.getAktørId()) && rel.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(rel -> personopplysninger.getPersonopplysninger().stream().filter(person -> person.getAktørId().equals(rel.getTilAktørId())).findAny().orElse(null))
            .filter(barn -> barn != null && erBarnRelatertTilSøknad(fødselsintervall, barn.getFødselsdato()))
            .collect(toList());
    }

    private boolean erBarnRelatertTilSøknad(List<LocalDateInterval> relasjonsintervall, LocalDate dato) {
        return relasjonsintervall.stream()
            .anyMatch(periode -> periode.encloses(dato));
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


    public boolean getManglerFødselsRegistreringFristUtløpt(FamilieHendelseGrunnlagEntitet grunnlag) {
        if (!grunnlag.getGjeldendeVersjon().getGjelderFødsel() || grunnlag.getBekreftetVersjon().isPresent() ||
            grunnlag.getOverstyrtVersjon().map(fh -> FØDSEL.equals(fh.getType()) && !fh.getBarna().isEmpty()).orElse(Boolean.FALSE)) {
            return false;
        }
        LocalDate fhDato = grunnlag.finnGjeldendeFødselsdato();
        Period fhDatoPlussFrist = FamilieHendelseType.FØDSEL.equals(grunnlag.getGjeldendeVersjon().getType()) ? REGISTRERING_FRIST_ETTER_FØDSEL : REGISTRERING_FRIST_ETTER_TERMIN;
        return LocalDate.now().isAfter(fhDato.plus(fhDatoPlussFrist));
    }

    public void kopierGrunnlag(Long fraBehandlingId, Long tilBehandlingId) {
        familieGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(fraBehandlingId, tilBehandlingId);
    }

    static List<LocalDateInterval> utledPerioderForRegisterinnhenting(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        return utledTidslineFraGrunnlag(familieHendelseGrunnlag).getDatoIntervaller().stream().collect(Collectors.toUnmodifiableList());
    }

    static LocalDateTimeline<Boolean> utledTidslineFraGrunnlag(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        final FamilieHendelseEntitet søknadVersjon = familieHendelseGrunnlag.getSøknadVersjon();
        // Tar med bekreftet / overstyrt barn hvis finnes
        List<LocalDateSegment<Boolean>> intervaller = new ArrayList<>(familieHendelseGrunnlag.getGjeldendeBekreftetVersjon()
            .map(FamilieHendelseTjeneste::intervallerForUidentifisertBarn).orElse(List.of()));

        if (FamilieHendelseType.FØDSEL.equals(søknadVersjon.getType())) {
            intervaller.addAll(intervallerForUidentifisertBarn(søknadVersjon));
        }
        if (FamilieHendelseType.TERMIN.equals(søknadVersjon.getType())) {
            var termindato = søknadVersjon.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElseThrow();
            intervaller.add(intervallForTermindato(termindato));
            // Tar med bekreftet termindato hvis finnes - men både den og søknadsdato kan ha feil (sic)
            familieHendelseGrunnlag.getGjeldendeTerminbekreftelse()
                .map(TerminbekreftelseEntitet::getTermindato)
                .filter(t -> !termindato.equals(t))
                .ifPresent(t -> intervaller.add(intervallForTermindato(t)));
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
            .collect(toList());
    }

    private static LocalDateSegment<Boolean> intervallForFødselsdato(LocalDate fødselsdato) {
        return new LocalDateSegment<>(fødselsdato.minus(MATCH_INTERVAlL_FØDSEL), fødselsdato.plus(MATCH_INTERVAlL_FØDSEL), Boolean.TRUE);
    }

    private static LocalDateSegment<Boolean> intervallForTermindato(LocalDate termindato) {
        return new LocalDateSegment<>(termindato.minus(MATCH_INTERVAlL_TERMIN), termindato.plus(MATCH_INTERVAlL_FØDSEL), Boolean.TRUE);
    }
}

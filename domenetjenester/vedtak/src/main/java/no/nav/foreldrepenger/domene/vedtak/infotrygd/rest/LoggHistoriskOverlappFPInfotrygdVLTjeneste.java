package no.nav.foreldrepenger.domene.vedtak.infotrygd.rest;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygdRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumer;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.konfig.Tid;

/*
Når Foreldrepenger-sak, enten førstegang eller revurdering, innvilges sjekker vi for overlapp med pleiepenger eller sykepenger i Infortrygd på personen.
Overlapp er tilstede dersom noen av de vedtatte periodende i Infotrygd overlapper med noen av utbetalingsperiodene på iverksatt foreldrepenge-behandling
Ved overlapp lagres informasjonen til databasetabellen BEHANDLING_OVERLAPP_INFOTRYGD
Det er manuell håndtering av funnene videre.
Håndtering av overlapp av Foreldrepenger-foreldrepenger håndteres av klassen VurderOpphørAvYtelser som trigges av en prosesstask.
 */
@ApplicationScoped
public class LoggHistoriskOverlappFPInfotrygdVLTjeneste {
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private AktørConsumer aktørConsumer;
    private InfotrygdSVPGrunnlag infotrygdSVPGrTjeneste;
    private InfotrygdFPGrunnlag infotrygdFPGrTjeneste;
    private BehandlingOverlappInfotrygdRepository overlappRepository;
    private static final Logger log = LoggerFactory.getLogger(LoggHistoriskOverlappFPInfotrygdVLTjeneste.class);


    LoggHistoriskOverlappFPInfotrygdVLTjeneste() {
        // for CDI
    }

    @Inject
    public LoggHistoriskOverlappFPInfotrygdVLTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                                      BehandlingRepository behandlingRepository,
                                                      AktørConsumer aktørConsumer,
                                                      InfotrygdSVPGrunnlag infotrygdSVPGrTjeneste,
                                                      InfotrygdFPGrunnlag infotrygdFPGrTjeneste,
                                                      BehandlingOverlappInfotrygdRepository overlappRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.aktørConsumer = aktørConsumer;
        this.infotrygdSVPGrTjeneste = infotrygdSVPGrTjeneste;
        this.infotrygdFPGrTjeneste = infotrygdFPGrTjeneste;
        this.overlappRepository = overlappRepository;
    }

    public void vurderOglagreEventueltOverlapp(Long behandlingId, AktørId annenPart, LocalDate minFraQuery) {
        try {
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            LocalDate førsteUttaksDatoFP = finnFørsteUttaksDato(behandling.getId());
            if (!minFraQuery.equals(førsteUttaksDatoFP)) {
                log.info("FPSAK DETEKTOR DIFF DATO {} vs  {}", minFraQuery, førsteUttaksDatoFP);
                førsteUttaksDatoFP = minFraQuery.isBefore(førsteUttaksDatoFP) ? minFraQuery : førsteUttaksDatoFP;
            }
            førsteUttaksDatoFP = fomMandag(førsteUttaksDatoFP);
            //Henter alle utbetalingsperioder på behandling som er iverksatt
            LocalDateTimeline<Boolean> perioderFp = hentPerioderFp(behandling.getId(), førsteUttaksDatoFP);
            harFPYtelserSomOverlapperIT(behandling, førsteUttaksDatoFP, perioderFp, behandling.getAktørId(), "FP").forEach(behandlingOverlappInfotrygd -> overlappRepository.lagre(behandlingOverlappInfotrygd));
            if (RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType()) && annenPart != null && FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()))  {
                harFPYtelserSomOverlapperIT(behandling, førsteUttaksDatoFP, perioderFp, annenPart, "FPA").forEach(behandlingOverlappInfotrygd -> overlappRepository.lagre(behandlingOverlappInfotrygd));
            }
            if (RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType())) {
                harSVPYtelserSomOverlapperIT(behandling, førsteUttaksDatoFP, perioderFp).forEach(behandlingOverlappInfotrygd -> overlappRepository.lagre(behandlingOverlappInfotrygd));
            }
        } catch (Exception e) {
            log.info("Identifisering av overlapp i Infotrygd feilet ", e);
        }
    }


    private List<BehandlingOverlappInfotrygd> harFPYtelserSomOverlapperIT(Behandling behandling, LocalDate førsteUttaksdatoFp,
                                                                          LocalDateTimeline<Boolean> perioderFP, AktørId finnForAktørId, String tema) {
        List<BehandlingOverlappInfotrygd> overlappene = new ArrayList<>();
        var ident = getFnrFraAktørId(finnForAktørId);

        List<Grunnlag> infotrygdFPGrunnlag = infotrygdFPGrTjeneste.hentGrunnlag(ident.getIdent(), førsteUttaksdatoFp.minusWeeks(4), førsteUttaksdatoFp.plusYears(3));

        finnOverlappene(behandling, perioderFP, tema, infotrygdFPGrunnlag, overlappene);

        return overlappene;
    }

    private List<BehandlingOverlappInfotrygd> harSVPYtelserSomOverlapperIT(Behandling behandling, LocalDate førsteUttaksdatoFp,
                                                                           LocalDateTimeline<Boolean> perioderFP) {
        List<BehandlingOverlappInfotrygd> overlappene = new ArrayList<>();
        var ident = getFnrFraAktørId(behandling.getAktørId());

        List<Grunnlag> infotrygdSVPGrunnlag = infotrygdSVPGrTjeneste.hentGrunnlag(ident.getIdent(), førsteUttaksdatoFp.minusWeeks(4), førsteUttaksdatoFp.plusYears(3));

        finnOverlappene(behandling, perioderFP, "SVP", infotrygdSVPGrunnlag, overlappene);

        return overlappene;
    }

    private void finnOverlappene(Behandling behandling, LocalDateTimeline<Boolean> perioderFP, String tema,
                                 List<Grunnlag> infotrygdGrunnlaglist, List<BehandlingOverlappInfotrygd> overlappene) {
        LocalDateTimeline<Boolean> tlGrunnlag = finnTidslinjeFraGrunnlagene(infotrygdGrunnlaglist);

        tlGrunnlag.getDatoIntervaller()
            .forEach(grunnlagPeriode -> perioderFP.getDatoIntervaller().stream()
                .filter(grunnlagPeriode::overlaps)
                .map(vlPeriode -> opprettOverlappIT(behandling, tema, grunnlagPeriode, vlPeriode))
                .forEach(overlappene::add)
        );
    }

    private PersonIdent getFnrFraAktørId(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(PersonIdent::new).orElseThrow();
    }

    private LocalDateTimeline<Boolean> hentPerioderFp(Long behandlingId, LocalDate minUttakDato) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        List<LocalDateSegment<Boolean>> segments = new ArrayList<>();

        berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            //.filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom(), Boolean.TRUE))
            .forEach(segments::add);
        if (!segments.isEmpty()) {
            segments.add(new LocalDateSegment<>(minUttakDato, minUttakDato, Boolean.TRUE));
        }

        return helgeJusterTidslinje(new LocalDateTimeline<>(segments, StandardCombinators::alwaysTrueForMatch).compress());
    }

    private LocalDateTimeline<Boolean> finnTidslinjeFraGrunnlagene(List<Grunnlag> grunnlag) {
        List<LocalDateSegment<Boolean>> segmenter = grunnlag.stream()
            .map(Grunnlag::getVedtak)
            .flatMap(Collection::stream)
            .map(p-> new LocalDateSegment<>(p.getPeriode().getFom(), tomSøndag(p.getPeriode().getTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        return helgeJusterTidslinje(new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress());
    }

    private LocalDate finnFørsteUttaksDato(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        Optional<LocalDate> minFom = berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            //.filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder());
        return minFom.orElse(Tid.TIDENES_ENDE);
    }

    private BehandlingOverlappInfotrygd opprettOverlappIT(Behandling behandling, String tema, LocalDateInterval periodeInfotrygd, LocalDateInterval periodeVL) {
        return BehandlingOverlappInfotrygd.builder()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBehandlingId(behandling.getId())
            .medPeriodeInfotrygd(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periodeInfotrygd.getFomDato(), periodeInfotrygd.getTomDato()))
            .medPeriodeVL(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periodeVL.getFomDato(), periodeVL.getTomDato()))
            .medYtelseInfotrygd(tema)
            .build();
    }

    private LocalDateTimeline<Boolean> helgeJusterTidslinje(LocalDateTimeline<Boolean> tidslinje) {
        var segments = tidslinje.getDatoIntervaller().stream()
            .map(p -> new LocalDateSegment<>(fomMandag(p.getFomDato()), tomFredag(p.getTomDato()), Boolean.TRUE))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segments, StandardCombinators::alwaysTrueForMatch).compress();
    }

    private LocalDate fomMandag(LocalDate fom) {
        DayOfWeek ukedag = DayOfWeek.from(fom);
        if (DayOfWeek.SUNDAY.getValue() == ukedag.getValue())
            return fom.plusDays(1);
        if (DayOfWeek.SATURDAY.getValue() == ukedag.getValue())
            return fom.plusDays(2);
        return fom;
    }

    private LocalDate tomFredag(LocalDate tom) {
        DayOfWeek ukedag = DayOfWeek.from(tom);
        if (DayOfWeek.SUNDAY.getValue() == ukedag.getValue())
            return tom.minusDays(2);
        if (DayOfWeek.SATURDAY.getValue() == ukedag.getValue())
            return tom.minusDays(1);
        return tom;
    }

    // Utvidelser for å koble p1.tom/fredag og p2.fom/mandag
    private LocalDate tomSøndag(LocalDate fom) {
        DayOfWeek ukedag = DayOfWeek.from(fom);
        if (DayOfWeek.SATURDAY.getValue() == ukedag.getValue())
            return fom.plusDays(1);
        if (DayOfWeek.FRIDAY.getValue() == ukedag.getValue())
            return fom.plusDays(2);
        return fom;
    }
}

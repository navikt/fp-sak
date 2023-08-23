package no.nav.foreldrepenger.mottak.vedtak.overlapp;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.abakus.AbakusTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdPSGrunnlag;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdSPGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.spokelse.Spøkelse;
import no.nav.vedtak.felles.integrasjon.spokelse.SykepengeVedtak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

/*
Når Foreldrepenger-sak, enten førstegang eller revurdering, innvilges sjekker vi for overlapp med pleiepenger eller sykepenger i Infortrygd på personen.
Overlapp er tilstede dersom noen av de vedtatte periodende i Infotrygd overlapper med noen av utbetalingsperiodene på iverksatt foreldrepenge-behandling
Ved overlapp lagres informasjonen til databasetabellen BEHANDLING_OVERLAPP_INFOTRYGD
Det er manuell håndtering av funnene videre.
Håndtering av overlapp av Foreldrepenger-foreldrepenger håndteres av klassen VurderOpphørAvYtelser som trigges av en prosesstask.
 */
@ApplicationScoped
public class LoggOverlappEksterneYtelserTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(LoggOverlappEksterneYtelserTjeneste.class);
    private static final BigDecimal HUNDRE = new BigDecimal(100);

    private static final List<Duration> SPOKELSE_TIMEOUTS = List.of(Duration.ofMillis(100), Duration.ofMillis(500), Duration.ofMillis(2500),
        Duration.ofMillis(12), Duration.ofSeconds(60));

    private static final boolean IS_PROD = Environment.current().isProd();

    private BeregningTjeneste beregningTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private PersoninfoAdapter personinfoAdapter;
    private InfotrygdPSGrunnlag infotrygdPSGrTjeneste;
    private InfotrygdSPGrunnlag infotrygdSPGrTjeneste;
    private AbakusTjeneste abakusTjeneste;
    private Spøkelse spøkelse;
    private OverlappVedtakRepository overlappRepository;


    @Inject
    public LoggOverlappEksterneYtelserTjeneste(BeregningTjeneste beregningTjeneste,
                                               BeregningsresultatRepository beregningsresultatRepository,
                                               PersoninfoAdapter personinfoAdapter,
                                               InfotrygdPSGrunnlag infotrygdPSGrTjeneste,
                                               InfotrygdSPGrunnlag infotrygdSPGrTjeneste,
                                               AbakusTjeneste abakusTjeneste,
                                               Spøkelse spøkelse,
                                               OverlappVedtakRepository overlappRepository,
                                               BehandlingRepository behandlingRepository) {
        this.beregningTjeneste = beregningTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.infotrygdPSGrTjeneste = infotrygdPSGrTjeneste;
        this.infotrygdSPGrTjeneste = infotrygdSPGrTjeneste;
        this.abakusTjeneste = abakusTjeneste;
        this.spøkelse = spøkelse;
        this.overlappRepository = overlappRepository;
    }

    LoggOverlappEksterneYtelserTjeneste() {
        // for CDI
    }

    public void loggOverlappForVedtakFPSAK(Long behandlingId, Saksnummer saksnummer, AktørId aktørId) {
        try {
            loggOverlappendeYtelser(behandlingId, saksnummer, aktørId).stream()
                .map(b -> b.medHendelse(OverlappVedtak.HENDELSE_VEDTAK_FOR))
                .forEach(overlappRepository::lagre);
        } catch (Exception e) {
            LOG.info("Identifisering av overlapp for vedtak i VL feilet ", e);
        }
    }

    public void loggOverlappForAvstemming(String hendelse, Long behandlingId, Saksnummer saksnummer, AktørId aktørId) {
        loggOverlappendeYtelser(behandlingId, saksnummer, aktørId).stream()
            .map(b -> b.medHendelse(hendelse))
            .forEach(overlappRepository::lagre);
    }

    public void loggOverlappForVedtakK9SAK(YtelseV1 ytelse, List<Fagsak> sakerForBruker) {
        // OBS Flere av K9SAK-ytelsene har fom/tom i helg ... ikke bruk VirkedagUtil på dem.
        var ytelseTidslinje = lagTidslinjeforYtelseV1(ytelse);

        sakerForBruker.stream()
            .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
            .forEach(b -> {
                var fpTimeline = getTidslinjeForBehandling(b.getId(), b.getFagsakYtelseType());
                var overlapp = finnGradertOverlapp(fpTimeline, Fagsystem.K9SAK.getKode(), mapAbakusYtelseType(ytelse),
                    ytelse.getSaksnummer(), ytelseTidslinje);
                overlapp.stream()
                    .map(builder -> builder.medSaksnummer(b.getFagsak().getSaksnummer())
                        .medBehandlingId(b.getId())
                        .medHendelse(OverlappVedtak.HENDELSE_VEDTAK_OMS))
                    .forEach(overlappRepository::lagre);

            });
    }

    private String mapAbakusYtelseType(YtelseV1 ytelse) {
        if (ytelse.getYtelse() == null) {
            return "UKJENT";
        }
        return switch (ytelse.getYtelse()) {
            case PLEIEPENGER_SYKT_BARN -> "PSB";
            case PLEIEPENGER_NÆRSTÅENDE -> "PPN";
            default -> ytelse.getYtelse().name();
        };
    }

    private List<OverlappVedtak.Builder> loggOverlappendeYtelser(Long behandlingId,
                                                                 Saksnummer saksnummer,
                                                                 AktørId aktørId) {
        var ytelseType = behandlingRepository.hentBehandling(behandlingId).getFagsakYtelseType();
        var perioderFpGradert = getTidslinjeForBehandling(behandlingId, ytelseType);
        if (perioderFpGradert.isEmpty()) {
            return Collections.emptyList();
        }
        var tidligsteUttakFP = perioderFpGradert.getMinLocalDate();

        var ident = getFnrFraAktørId(aktørId);
        List<OverlappVedtak.Builder> overlappene = new ArrayList<>();

        vurderOmOverlappInfotrygd(ident, tidligsteUttakFP, perioderFpGradert, overlappene);
        vurderOmOverlappOMS(aktørId, tidligsteUttakFP, perioderFpGradert, overlappene);
        vurderOmOverlappSYK(ident, tidligsteUttakFP, perioderFpGradert, overlappene);
        return overlappene.stream()
            .map(b -> b.medSaksnummer(saksnummer).medBehandlingId(behandlingId))
            .toList();
    }

    private LocalDateTimeline<BigDecimal> getTidslinjeForBehandling(Long behandlingId, FagsakYtelseType ytelseType) {
        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
            return getTidslinjeForBehandlingFP(behandlingId);
        }
        return FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType) ? getTidslinjeForBehandlingSVP(
            behandlingId) : new LocalDateTimeline<>(Collections.emptyList());
    }

    private LocalDateTimeline<BigDecimal> getTidslinjeForBehandlingFP(Long behandlingId) {
        var segments = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .orElse(Collections.emptyList())
            .stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom(),
                p.getKalkulertUtbetalingsgrad()))
            .toList();

        return new LocalDateTimeline<>(segments, LoggOverlappEksterneYtelserTjeneste::max)
            .compress(this::like, this::kombiner);
    }

    private LocalDateTimeline<BigDecimal> getTidslinjeForBehandlingSVP(Long behandlingId) {
        var beregningsgrunnlag = beregningTjeneste.hent(behandlingId).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .orElse(null);
        if (beregningsgrunnlag == null) {
            return new LocalDateTimeline<>(Collections.emptyList());
        }
        var grunnlagUtbetGrad = beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .filter(p -> p.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsgrunnlagPeriodeFom(), p.getBeregningsgrunnlagPeriodeTom(),
                beregnGrunnlagUtbetGradSvp(p, beregningsgrunnlag.getGrunnbeløp().getVerdi())))
            .toList();
        var resultatsegments = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .orElse(Collections.emptyList())
            .stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(p -> finnUtbetalingsgradFor(p, grunnlagUtbetGrad))
            .filter(Objects::nonNull)
            .toList();
        return new LocalDateTimeline<>(resultatsegments, LoggOverlappEksterneYtelserTjeneste::max)
            .compress(this::like, this::kombiner);
    }

    private BigDecimal beregnGrunnlagUtbetGradSvp(BeregningsgrunnlagPeriode bgPeriode, BigDecimal grunnbeløp) {
        var seksG = new BigDecimal(6).multiply(grunnbeløp);
        var avkortet = bgPeriode.getBruttoPrÅr().compareTo(seksG) > 0 ? seksG : bgPeriode.getBruttoPrÅr();
        return BigDecimal.ZERO.compareTo(avkortet) == 0 ? BigDecimal.ZERO : BigDecimal.TEN.multiply(BigDecimal.TEN)
            .multiply(bgPeriode.getRedusertPrÅr())
            .divide(avkortet, RoundingMode.HALF_EVEN);
    }

    private LocalDateSegment<BigDecimal> finnUtbetalingsgradFor(BeregningsresultatPeriode periode,
                                                                List<LocalDateSegment<BigDecimal>> grader) {
        return grader.stream()
            .filter(d -> d.getLocalDateInterval()
                .encloses(periode.getBeregningsresultatPeriodeFom())) // Antar at BR-perioder ikke krysser BG-perioder
            .findFirst()
            .map(v -> new LocalDateSegment<>(periode.getBeregningsresultatPeriodeFom(),
                periode.getBeregningsresultatPeriodeTom(), v.getValue()))
            .orElse(null);
    }

    private LocalDateTimeline<BigDecimal> lagTidslinjeforYtelseV1(YtelseV1 ytelse) {
        var graderteSegments = ytelse.getAnvist()
            .stream()
            .map(p -> new LocalDateSegment<>(p.getPeriode().getFom(), p.getPeriode().getTom(),
                utbetalingsgradHundreHvisNull(p.getUtbetalingsgrad())))
            .filter(s -> s.getValue().compareTo(BigDecimal.ZERO) > 0)
            .toList();
        return new LocalDateTimeline<>(graderteSegments, LoggOverlappEksterneYtelserTjeneste::max).compress(this::like,
            this::kombiner);
    }

    public void vurderOmOverlappInfotrygd(PersonIdent ident,
                                          LocalDate førsteUttaksDatoFP,
                                          LocalDateTimeline<BigDecimal> perioderFp,
                                          List<OverlappVedtak.Builder> overlappene) {
        //sjekker om noen av vedtaksperiodene i Infotrygd på sykepenger eller pleiepenger overlapper med perioderFp
        var infotrygdPSGrunnlag = infotrygdPSGrTjeneste.hentGrunnlag(ident.getIdent(),
            førsteUttaksDatoFP.minusMonths(1), førsteUttaksDatoFP.plusYears(3));
        overlappene.addAll(finnGradertOverlapp(perioderFp, Fagsystem.INFOTRYGD.getKode(), "BS", null,
            finnTidslinjeFraGrunnlagene(infotrygdPSGrunnlag)));

        var infotrygdSPGrunnlag = infotrygdSPGrTjeneste.hentGrunnlag(ident.getIdent(),
            førsteUttaksDatoFP.minusMonths(1), førsteUttaksDatoFP.plusYears(3));
        overlappene.addAll(
            finnGradertOverlapp(perioderFp, Fagsystem.INFOTRYGD.getKode(), "SP", null,
                finnTidslinjeFraGrunnlagene(infotrygdSPGrunnlag)));
    }

    public void vurderOmOverlappOMS(AktørId aktørId,
                                    LocalDate førsteUttaksDatoFP,
                                    LocalDateTimeline<BigDecimal> perioderFp,
                                    List<OverlappVedtak.Builder> overlappene) {
        try {
            var request =  AbakusTjeneste.lagRequestForHentVedtakFom(aktørId, førsteUttaksDatoFP.minusYears(1),
                Set.of(Ytelser.PLEIEPENGER_SYKT_BARN, Ytelser.PLEIEPENGER_NÆRSTÅENDE, Ytelser.OMSORGSPENGER, Ytelser.OPPLÆRINGSPENGER, Ytelser.FRISINN));
            abakusTjeneste.hentVedtakForAktørId(request)
                .stream()
                .map(y -> (YtelseV1) y)
                .filter(y -> Kildesystem.K9SAK.equals(y.getKildesystem()))
                .forEach(y -> {
                    var ytelseTidslinje = lagTidslinjeforYtelseV1(y);
                    overlappene.addAll(finnGradertOverlapp(perioderFp, Fagsystem.K9SAK.getKode(),  mapAbakusYtelseType(y),
                        y.getSaksnummer(), ytelseTidslinje));
                });
        } catch (Exception e) {
            if (IS_PROD) {
                throw new TekniskException("FP-180125", "Tjeneste abakus gir feil", e);
            }
            LOG.info("Noe gikk galt mot abakus", e);
        }
    }

    public void vurderOmOverlappSYK(PersonIdent ident,
                                    LocalDate førsteUttaksDatoFP,
                                    LocalDateTimeline<BigDecimal> perioderFp,
                                    List<OverlappVedtak.Builder> overlappene) {
        hentSpøkelse(ident.getIdent(), førsteUttaksDatoFP).forEach(y -> {
            var graderteSegments = y.utbetalingerNonNull()
                .stream()
                .map(
                    p -> new LocalDateSegment<>(p.fom(), p.tom(), utbetalingsgradHundreHvisNull(p.gradScale2())))
                .filter(s -> s.getValue().compareTo(BigDecimal.ZERO) > 0)
                .toList();
            var ytelseTidslinje = new LocalDateTimeline<>(graderteSegments,
                LoggOverlappEksterneYtelserTjeneste::max).compress(this::like, this::kombiner);
            overlappene.addAll(
                finnGradertOverlapp(perioderFp, Fagsystem.VLSP.getKode(), "SP",
                    y.vedtaksreferanse(), ytelseTidslinje));
        });

    }

    private List<SykepengeVedtak> hentSpøkelse(String fnr, LocalDate førsteUttaksDatoFP) {
        if (!IS_PROD) return List.of();
        var it = SPOKELSE_TIMEOUTS.iterator();
        while (it.hasNext()) {
            try {
                var timeout = it.next();
                var før = System.nanoTime();
                var vedtak = spøkelse.hentGrunnlag(fnr, førsteUttaksDatoFP, timeout);
                var etter = System.nanoTime();
                LOG.info("Spøkelse antall {} timeout {} svartid {}", vedtak.size(), timeout, (etter-før) / 1000000); // Log millis
                return vedtak;
            } catch (Exception e) {
                if (!it.hasNext()) {
                    throw e;
                }
            }
        }
        return List.of();
    }

    private BigDecimal utbetalingsgradHundreHvisNull(Desimaltall anvistUtbetalingsprosent) {
        return anvistUtbetalingsprosent != null
            && anvistUtbetalingsprosent.getVerdi() != null ? anvistUtbetalingsprosent.getVerdi() : HUNDRE;
    }

    private BigDecimal utbetalingsgradHundreHvisNull(BigDecimal anvistUtbetalingsprosent) {
        return anvistUtbetalingsprosent != null ? anvistUtbetalingsprosent : HUNDRE;
    }

    private List<OverlappVedtak.Builder> finnGradertOverlapp(LocalDateTimeline<BigDecimal> perioderFP,
                                                             String fagsystem,
                                                             String ytelseType,
                                                             String referanse,
                                                             LocalDateTimeline<BigDecimal> tlGrunnlag) {
        var filter = perioderFP.intersection(tlGrunnlag, StandardCombinators::sum)
            .filterValue(v -> v.compareTo(HUNDRE) > 0);

        return filter.getLocalDateIntervals()
            .stream()
            .map(filter::getSegment)
            .map(s -> opprettOverlappBuilder(s.getLocalDateInterval(), s.getValue()).medFagsystem(fagsystem)
                .medYtelse(ytelseType)
                .medReferanse(referanse))
            .toList();
    }

    private LocalDateTimeline<BigDecimal> finnTidslinjeFraGrunnlagene(List<Grunnlag> grunnlag) {
        var segmenter = grunnlag.stream()
            .map(Grunnlag::vedtak)
            .flatMap(Collection::stream)
            .filter(v -> v.utbetalingsgrad() > 0)
            .map(p -> new LocalDateSegment<>(normertIntervall(p.periode().fom(), p.periode().tom()),
                new BigDecimal(p.utbetalingsgrad())))
            .toList();

        return new LocalDateTimeline<>(segmenter, LoggOverlappEksterneYtelserTjeneste::max).compress(this::like,
            this::kombiner);
    }

    private LocalDateInterval normertIntervall(LocalDate fom, LocalDate tom) {
        return tom.isBefore(fom) ? new LocalDateInterval(tom, fom) : new LocalDateInterval(fom, tom);
    }

    private OverlappVedtak.Builder opprettOverlappBuilder(LocalDateInterval periode, BigDecimal utbetaling) {
        return OverlappVedtak.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato()))
            .medUtbetalingsprosent(utbetaling.longValue());
    }

    private PersonIdent getFnrFraAktørId(AktørId aktørId) {
        return personinfoAdapter.hentFnr(aktørId).orElseThrow();
    }

    private static LocalDateSegment<BigDecimal> max(LocalDateInterval dateInterval,
                                                    LocalDateSegment<BigDecimal> lhs,
                                                    LocalDateSegment<BigDecimal> rhs) {
        if (lhs == null && rhs == null) {
            return null;
        }
        if (lhs == null || rhs == null) {
            return new LocalDateSegment<>(dateInterval, lhs == null ? rhs.getValue() : lhs.getValue());
        }
        return new LocalDateSegment<>(dateInterval,
            lhs.getValue().compareTo(rhs.getValue()) > 0 ? lhs.getValue() : rhs.getValue());
    }

    private boolean like(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return Objects.equals(a, b);
        }
        return a.compareTo(b) == 0;
    }

    private LocalDateSegment<BigDecimal> kombiner(LocalDateInterval i,
                                                  LocalDateSegment<BigDecimal> lhs,
                                                  LocalDateSegment<BigDecimal> rhs) {
        if (lhs == null) {
            return rhs;
        }
        if (rhs == null) {
            return lhs;
        }
        return new LocalDateSegment<>(i, lhs.getValue());
    }

}

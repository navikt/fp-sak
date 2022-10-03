package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperioderHelper;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class OppgittPeriodeTidligstMottattDatoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OppgittPeriodeTidligstMottattDatoTjeneste.class);

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FpUttakRepository uttakRepository;

    @Inject
    public OppgittPeriodeTidligstMottattDatoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste, FpUttakRepository uttakRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uttakRepository = uttakRepository;
    }

    OppgittPeriodeTidligstMottattDatoTjeneste() {
        //CDI
    }

    /**
     * Henter tidligst mottatt dato for periode fra original behandling hvis den finnes
     */
    public Optional<LocalDate> finnTidligstMottattDatoForPeriode(Behandling behandling, OppgittPeriodeEntitet periode) {
        var originalBehandling = behandling.getOriginalBehandlingId();
        if (originalBehandling.isEmpty()) {
            return Optional.empty();
        }

        var matchendePerioderIOriginalBehandling = finnMatchendePerioder(periode, originalBehandling.get());
        if (matchendePerioderIOriginalBehandling.isEmpty()) {
            return Optional.empty();
        }

        if (matchendePerioderIOriginalBehandling.size() > 1) {
            throw new IllegalStateException("Finner mer enn 1 matchende oppgitt periode i original behandling" +
                " for periode" + periode.getFom() + " - " + periode.getTom());
        }
        var matchendePeriode = matchendePerioderIOriginalBehandling.get(0);
        var tidligstMottattDato = matchendePeriode.getTidligstMottattDato().orElse(matchendePeriode.getMottattDato());
        LOG.info("Fant matchende periode for søknadsperiode {}. Matchet med periode {}. Setter mottatt dato på søknadsperiode {}",
            periode.getTidsperiode(), matchendePeriode.getTidsperiode(), tidligstMottattDato);
        return Optional.ofNullable(tidligstMottattDato);
    }

    private List<OppgittPeriodeEntitet> finnMatchendePerioder(OppgittPeriodeEntitet periode, Long originalBehandling) {
        return ytelseFordelingTjeneste.hentAggregat(originalBehandling)
            .getGjeldendeSøknadsperioder().getOppgittePerioder()
            .stream()
            .filter(p -> lik(periode, p))
            .collect(Collectors.toList());
    }

    private boolean lik(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        var like = erOmsluttetAv(periode1, periode2)
            && Objects.equals(periode1.getÅrsak(), periode2.getÅrsak())
            && Objects.equals(periode1.getPeriodeType(), periode2.getPeriodeType())
            && Objects.equals(periode1.getSamtidigUttaksprosent(), periode2.getSamtidigUttaksprosent());
        if (like && periode1.isGradert()) {
            return periode2.isGradert() &&
                periode1.isArbeidstaker() == periode2.isArbeidstaker() &&
                Objects.equals(periode1.getArbeidsprosent(), periode2.getArbeidsprosent()) &&
                Objects.equals(periode1.getArbeidsgiver(), periode2.getArbeidsgiver());
        }
        return like;
    }

    private boolean erOmsluttetAv(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        return !periode2.getFom().isAfter(periode1.getFom()) && !periode2.getTom().isBefore(periode1.getTom());
    }

    public void sammenlignLoggMottattDato(Behandling behandling, List<OppgittPeriodeEntitet> nysøknad) {
        var forrigesøknad = behandling.getOriginalBehandlingId()
            .map(ytelseFordelingTjeneste::hentAggregat)
            .map(YtelseFordelingAggregat::getGjeldendeSøknadsperioder)
            .map(OppgittFordelingEntitet::getOppgittePerioder).orElse(List.of());

        var tidslinjeSammenlignNy =  new LocalDateTimeline<>(nysøknad.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForMottat(p))).toList());
        var tidslinjeSammenlignGammel =  new LocalDateTimeline<>(forrigesøknad.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForMottat(p))).toList());
        var tidslinjeSammenfall = tidslinjeSammenlignNy.intersection(tidslinjeSammenlignGammel);

        var tidslinjeTidligstMottattNy = new LocalDateTimeline<>(nysøknad.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p.getTidligstMottattDato().orElseGet(p::getMottattDato))).toList());
        var tidslinjeTidligstMottattGammel = new LocalDateTimeline<>(forrigesøknad.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p.getTidligstMottattDato().orElseGet(p::getMottattDato))).toList());
        var gammelMottattDatoForSammenfallende = tidslinjeTidligstMottattGammel.intersection(tidslinjeSammenfall);
        var oppdatertTidsligstMottattGammel = tidslinjeTidligstMottattNy.combine(gammelMottattDatoForSammenfallende, StandardCombinators::min, LocalDateTimeline.JoinStyle.INNER_JOIN);

        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer).orElse(null);
        var tidligstedato = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElse(null);

        List<OppgittPeriodeEntitet> perioderURBekreft = forrigeUttak != null && tidligstedato != null ? VedtaksperioderHelper.opprettOppgittePerioder(forrigeUttak, List.of(), tidligstedato) : List.of();
        var tidslinjeSammenlignURB =  new LocalDateTimeline<>(perioderURBekreft.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForMottat(p))).toList());
        var tidslinjeSammenfallURB = tidslinjeSammenlignNy.intersection(tidslinjeSammenlignURB);
        var tidslinjeTidligstMottattURB = new LocalDateTimeline<>(perioderURBekreft.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p.getTidligstMottattDato().orElseGet(p::getMottattDato))).toList());
        var gammelMottattDatoForSammenfallendeURB = tidslinjeTidligstMottattURB.intersection(tidslinjeSammenfallURB);

        List<OppgittPeriodeEntitet> perioderURSøknad = forrigeUttak != null && tidligstedato != null ?  VedtaksperioderHelper.opprettOppgittePerioderSøknadverdier(forrigeUttak, List.of(), tidligstedato) : List.of();
        var tidslinjeSammenlignURS =  new LocalDateTimeline<>(perioderURSøknad.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForMottat(p))).toList());
        var tidslinjeSammenfallURS = tidslinjeSammenlignNy.intersection(tidslinjeSammenlignURS);
        var tidslinjeTidligstMottattURS = new LocalDateTimeline<>(perioderURSøknad.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), p.getTidligstMottattDato().orElseGet(p::getMottattDato))).toList());
        var gammelMottattDatoForSammenfallendeURS = tidslinjeTidligstMottattURS.intersection(tidslinjeSammenfallURS);

        var oppdatertTidsligstMottattURB = tidslinjeTidligstMottattNy.combine(gammelMottattDatoForSammenfallendeURB, StandardCombinators::min, LocalDateTimeline.JoinStyle.INNER_JOIN);
        var oppdatertTidsligstMottattURS = tidslinjeTidligstMottattNy.combine(gammelMottattDatoForSammenfallendeURS, StandardCombinators::min, LocalDateTimeline.JoinStyle.INNER_JOIN);
        var oppdatertTidsligstMottattUR = oppdatertTidsligstMottattURB.combine(oppdatertTidsligstMottattURS, StandardCombinators::min, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        var oppdatertTidsligstMottatt = oppdatertTidsligstMottattUR.combine(oppdatertTidsligstMottattGammel, StandardCombinators::min, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        var nysøknadFom = nysøknad.stream().collect(Collectors.toMap(OppgittPeriodeEntitet::getFom, Function.identity()));
        oppdatertTidsligstMottatt.toSegments().forEach(s -> {
            if (nysøknadFom.containsKey(s.getFom())) {
                var tidligst = nysøknadFom.get(s.getFom()).getTidligstMottattDato().orElseGet(() -> nysøknadFom.get(s.getFom()).getMottattDato());
                if (!tidligst.equals(s.getValue())) {
                    LOG.info("SØKNAD MOTTATT DATO funnet avvik mottatt dato behandling {} fom {} søknad {} forrige {}", behandling.getId(), s.getFom(), tidligst, s.getValue());
                } else if (!s.getTom().equals(nysøknadFom.get(s.getFom()).getTom())) {
                    LOG.info("SØKNAD MOTTATT DATO splitte mine perioder {} fom {} tom {} søknad {} forrige {}", behandling.getId(), s.getFom(), s.getTom(), tidligst, s.getValue());
                }
            } else {
                LOG.info("SØKNAD MOTTATT DATO splittet mine perioder {} fom {} tom {}", behandling.getId(), s.getFom(), s.getTom());
            }
        });
    }

    private record SammenligningPeriodeForMottat(Årsak årsak, UttakPeriodeType periodeType, SamtidigUttaksprosent samtidigUttaksprosent, SammenligningGraderingForMottatt gradering) {
        SammenligningPeriodeForMottat(OppgittPeriodeEntitet periode) {
            this(periode.getÅrsak(), periode.getPeriodeType(), periode.getSamtidigUttaksprosent(), periode.isGradert() ? new SammenligningGraderingForMottatt(periode) : null);
        }

    }

    private record SammenligningGraderingForMottatt(boolean erArbeidstaker, BigDecimal arbeidsprosent, Arbeidsgiver arbeidsgiver) {
        SammenligningGraderingForMottatt(OppgittPeriodeEntitet periode) {
            this(periode.isArbeidstaker(), periode.getArbeidsprosent(), periode.getArbeidsgiver());
        }
    }

    public void sjekkOmPerioderKanForkastesSomLike(Behandling behandling, List<OppgittPeriodeEntitet> nysøknad) {
        var tidslinjeSammenlignNy =  new LocalDateTimeline<>(nysøknad.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForOppgitt(p))).toList());

        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(uttakRepository::hentUttakResultatHvisEksisterer).orElse(null);
        var tidligstedato = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElse(null);

        List<OppgittPeriodeEntitet> perioderURBekreft = forrigeUttak != null && tidligstedato != null ? VedtaksperioderHelper.opprettOppgittePerioder(forrigeUttak, List.of(), tidligstedato) : List.of();
        var tidslinjeSammenlignURB =  new LocalDateTimeline<>(perioderURBekreft.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForOppgitt(p))).toList());

        var sammenfall = tidslinjeSammenlignNy.combine(tidslinjeSammenlignURB, this::leftIfEqualsRight, LocalDateTimeline.JoinStyle.INNER_JOIN);
        var nyheter = tidslinjeSammenlignNy.disjoint(sammenfall);
        var førsteNyhet = nyheter.getLocalDateIntervals().stream().map(LocalDateInterval::getFomDato).min(Comparator.naturalOrder());
        if (førsteNyhet.isPresent() &&!førsteNyhet.get().equals(tidligstedato)) {
            LOG.info("SØKNAD KAST PERIODER: behandling {} kan kaste perioder mellom {} og {}", behandling.getId(), tidligstedato, førsteNyhet.get());
        }

    }

    private LocalDateSegment<SammenligningPeriodeForOppgitt> leftIfEqualsRight(LocalDateInterval dateInterval,
                                                                               LocalDateSegment<SammenligningPeriodeForOppgitt> lhs,
                                                                               LocalDateSegment<SammenligningPeriodeForOppgitt> rhs) {
        return lhs != null && rhs != null && Objects.equals(lhs.getValue(), rhs.getValue()) ?
            new LocalDateSegment<>(dateInterval, lhs.getValue()) : null;
    }

    private record SammenligningPeriodeForOppgitt(Årsak årsak, UttakPeriodeType periodeType, SamtidigUttaksprosent samtidigUttaksprosent, SammenligningGraderingForOppgitt gradering, boolean flerbarnsdager, MorsAktivitet morsAktivitet) {
        SammenligningPeriodeForOppgitt(OppgittPeriodeEntitet periode) {
            this(periode.getÅrsak(), periode.getPeriodeType(), periode.getSamtidigUttaksprosent(), periode.isGradert() ? new SammenligningGraderingForOppgitt(periode) : null, periode.isFlerbarnsdager(), periode.getMorsAktivitet());
        }
    }

    private record SammenligningGraderingForOppgitt(boolean erArbeidstaker, BigDecimal arbeidsprosent, Arbeidsgiver arbeidsgiver) {
        SammenligningGraderingForOppgitt(OppgittPeriodeEntitet periode) {
            this(periode.isArbeidstaker(), periode.getArbeidsprosent(), periode.getArbeidsgiver());
        }
    }

}

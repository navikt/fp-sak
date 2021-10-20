package no.nav.foreldrepenger.domene.vedtak.observer;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class VedtattYtelseTjeneste {

    private BehandlingVedtakRepository vedtakRepository;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public VedtattYtelseTjeneste() {
    }

    @Inject
    public VedtattYtelseTjeneste(BehandlingVedtakRepository vedtakRepository,
                                 BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                 BeregningsresultatRepository tilkjentYtelseRepository,
                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.vedtakRepository = vedtakRepository;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public Ytelse genererYtelse(Behandling behandling, boolean mapArbeidsforhold) {
        final var vedtak = vedtakRepository.hentForBehandling(behandling.getId());
        var berResultat = tilkjentYtelseRepository.hentUtbetBeregningsresultat(behandling.getId());

        final var aktør = new Aktør();
        aktør.setVerdi(behandling.getAktørId().getId());
        final var ytelse = new YtelseV1();
        ytelse.setFagsystem(Fagsystem.FPSAK);
        ytelse.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        ytelse.setVedtattTidspunkt(vedtak.getVedtakstidspunkt());
        ytelse.setVedtakReferanse(behandling.getUuid().toString());
        ytelse.setAktør(aktør);
        ytelse.setType(map(behandling.getFagsakYtelseType()));
        ytelse.setStatus(map(behandling.getFagsak().getStatus()));

        ytelse.setPeriode(utledPeriode(vedtak, berResultat.orElse(null)));
        ytelse.setAnvist(map(behandling, berResultat.orElse(null), mapArbeidsforhold));
        return ytelse;
    }

    private List<Anvisning> map(Behandling behandling, BeregningsresultatEntitet tilkjentYtelse, boolean mapArbeidsforhold) {
        if (tilkjentYtelse == null) {
            return List.of();
        }
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            if (mapArbeidsforhold) {
                List<ArbeidsforholdReferanse> arbeidsforholdReferanser = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())
                    .flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon)
                    .stream()
                    .flatMap(a -> a.getArbeidsforholdReferanser().stream()).collect(Collectors.toList());
                return VedtattYtelseForeldrepengerMapper.medArbeidsforhold(arbeidsforholdReferanser)
                    .mapForeldrepenger(tilkjentYtelse);
            } else {
                return VedtattYtelseForeldrepengerMapper.utenArbeidsforhold()
                    .mapForeldrepenger(tilkjentYtelse);
            }
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
            return mapSvangerskapspenger(behandling, tilkjentYtelse); // TODO - følg med på TFP-2667 må finne ny metode når Beregning/SVP er skrevet om.
        }
        return List.of();
    }

    private List<Anvisning> mapSvangerskapspenger(Behandling behandling, BeregningsresultatEntitet tilkjent) {
        var beregningsgrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandling.getId()).orElse(null);
        if (beregningsgrunnlag == null) {
            return List.of();
        }
        var grunnlagSatsUtbetGrad = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(p -> p.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsgrunnlagPeriodeFom(), p.getBeregningsgrunnlagPeriodeTom(), beregnGrunnlagSatsUtbetGradSvp(p, beregningsgrunnlag.getGrunnbeløp().getVerdi())))
            .collect(Collectors.toList());
        var resultatTidslinje = new LocalDateTimeline<DagsatsUtbgradSVP>(tilkjent.getBeregningsresultatPerioder().stream()
            .filter(p -> p.getDagsats() > 0)
            .map(p -> finnKombinertDagsatsUtbetaling(p, grunnlagSatsUtbetGrad))
            .filter(Objects::nonNull)
            .collect(Collectors.toList()), StandardCombinators::coalesceLeftHandSide);

        return resultatTidslinje.compress(Objects::equals, StandardCombinators::leftOnly).toSegments().stream()
            .map(this::mapSvangerskapspengerPeriode)
            .collect(Collectors.toList());
    }

    private LocalDateSegment<DagsatsUtbgradSVP> finnKombinertDagsatsUtbetaling(BeregningsresultatPeriode periode, List<LocalDateSegment<DagsatsUtbgradSVP>> dagsatsGrader) {
        return dagsatsGrader.stream()
            .filter(d -> d.getLocalDateInterval().encloses(periode.getBeregningsresultatPeriodeFom())) // Antar at BR-perioder ikke krysser BG-perioder
            .findFirst()
            .map(v -> new LocalDateSegment<>(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom(), v.getValue()))
            .orElse(null);
    }

    private Anvisning mapSvangerskapspengerPeriode(LocalDateSegment<DagsatsUtbgradSVP> periode) {
        final var anvisning = new Anvisning();
        final var p = new Periode();
        p.setFom(periode.getFom());
        p.setTom(periode.getTom());
        anvisning.setPeriode(p);
        anvisning.setDagsats(new Desimaltall(new BigDecimal(periode.getValue().dagsats())));
        anvisning.setUtbetalingsgrad(new Desimaltall(new BigDecimal(periode.getValue().utbetalingsgrad())));
        return anvisning;
    }

    private DagsatsUtbgradSVP beregnGrunnlagSatsUtbetGradSvp(BeregningsgrunnlagPeriode bgPeriode, BigDecimal grunnbeløp) {
        var seksG = new BigDecimal(6).multiply(grunnbeløp);
        var avkortet = bgPeriode.getBruttoPrÅr().compareTo(seksG) > 0 ? seksG : bgPeriode.getBruttoPrÅr();
        var grad = BigDecimal.ZERO.compareTo(avkortet) == 0 ? 0 :
            BigDecimal.TEN.multiply(BigDecimal.TEN).multiply(bgPeriode.getRedusertPrÅr()).divide(avkortet, RoundingMode.HALF_EVEN).longValue();
        var dagsats = BigDecimal.ZERO.compareTo(bgPeriode.getRedusertPrÅr()) == 0 ? 0 :
            new BigDecimal(bgPeriode.getDagsats()).multiply(avkortet).divide(bgPeriode.getRedusertPrÅr(), RoundingMode.HALF_EVEN).longValue();
        return new DagsatsUtbgradSVP(dagsats, grad);
    }

    private Periode utledPeriode(BehandlingVedtak vedtak, BeregningsresultatEntitet beregningsresultat) {
        final var periode = new Periode();
        if (beregningsresultat != null) {
            var minFom = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder());
            var maxTom = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
                .max(Comparator.naturalOrder());
            if (minFom.isEmpty()) {
                periode.setFom(vedtak.getVedtaksdato());
                periode.setTom(vedtak.getVedtaksdato());
                return periode;
            }
            periode.setFom(minFom.get());
            if (maxTom.isPresent()) {
                periode.setTom(maxTom.get());
            } else {
                periode.setTom(Tid.TIDENES_ENDE);
            }
            return periode;
        }
        periode.setFom(vedtak.getVedtaksdato());
        periode.setTom(vedtak.getVedtaksdato());
        return periode;
    }


    private YtelseType map(FagsakYtelseType type) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(type)) {
            return YtelseType.ENGANGSTØNAD;
        }
        if (FagsakYtelseType.FORELDREPENGER.equals(type)) {
            return YtelseType.FORELDREPENGER;
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(type)) {
            return YtelseType.SVANGERSKAPSPENGER;
        }
        throw new IllegalStateException("Ukjent ytelsestype " + type);
    }

    private YtelseStatus map(FagsakStatus kode) {
        YtelseStatus typeKode;
        if (FagsakStatus.OPPRETTET.equals(kode)) {
            typeKode = YtelseStatus.OPPRETTET;
        } else if (FagsakStatus.UNDER_BEHANDLING.equals(kode)) {
            typeKode = YtelseStatus.UNDER_BEHANDLING;
        } else if (FagsakStatus.LØPENDE.equals(kode)) {
            typeKode = YtelseStatus.LØPENDE;
        } else if (FagsakStatus.AVSLUTTET.equals(kode)) {
            typeKode = YtelseStatus.AVSLUTTET;
        } else {
            typeKode = YtelseStatus.OPPRETTET;
        }
        return typeKode;
    }

    static record DagsatsUtbgradSVP(long dagsats, long utbetalingsgrad) {
    }

}

package no.nav.foreldrepenger.domene.vedtak.observer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class VedtattYtelseTjeneste {

    private BehandlingVedtakRepository vedtakRepository;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;

    public VedtattYtelseTjeneste() {
    }

    @Inject
    public VedtattYtelseTjeneste(BehandlingVedtakRepository vedtakRepository,
                                 BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                 BeregningsresultatRepository tilkjentYtelseRepository) {
        this.vedtakRepository = vedtakRepository;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    public Ytelse genererYtelse(Behandling behandling) {
        final BehandlingVedtak vedtak = vedtakRepository.hentForBehandling(behandling.getId());
        Optional<BeregningsresultatEntitet> berResultat = tilkjentYtelseRepository.hentBeregningsresultat(behandling.getId());

        final Aktør aktør = new Aktør();
        aktør.setVerdi(behandling.getAktørId().getId());
        final YtelseV1 ytelse = new YtelseV1();
        ytelse.setFagsystem(Fagsystem.FPSAK);
        ytelse.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        ytelse.setVedtattTidspunkt(vedtak.getVedtakstidspunkt());
        ytelse.setVedtakReferanse(behandling.getUuid().toString());
        ytelse.setAktør(aktør);
        ytelse.setType(map(behandling.getFagsakYtelseType()));
        ytelse.setStatus(map(behandling.getFagsak().getStatus()));

        ytelse.setPeriode(utledPeriode(vedtak, berResultat.orElse(null)));
        ytelse.setAnvist(map(behandling, berResultat.orElse(null)));
        return ytelse;
    }

    private List<Anvisning> map(Behandling behandling, BeregningsresultatEntitet tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            return List.of();
        }
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            return mapForeldrepenger(tilkjentYtelse);
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
            return mapSvangerskapspenger(behandling, tilkjentYtelse); // TODO - følg med på TFP-2667 må finne ny metode når Beregning/SVP er skrevet om.
        }
        return List.of();
    }

    private List<Anvisning> mapForeldrepenger(BeregningsresultatEntitet tilkjent) {
        return tilkjent.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getDagsats() > 0)
            .map(this::mapForeldrepengerPeriode)
            .collect(Collectors.toList());
    }

    private Anvisning mapForeldrepengerPeriode(BeregningsresultatPeriode periode) {
        final Anvisning anvisning = new Anvisning();
        final Periode p = new Periode();
        p.setFom(periode.getBeregningsresultatPeriodeFom());
        p.setTom(periode.getBeregningsresultatPeriodeTom());
        anvisning.setPeriode(p);
        anvisning.setDagsats(new Desimaltall(new BigDecimal(periode.getDagsatsFraBg())));
        anvisning.setUtbetalingsgrad(new Desimaltall(periode.getKalkulertUtbetalingsgrad()));
        return anvisning;
    }

    private List<Anvisning> mapSvangerskapspenger(Behandling behandling, BeregningsresultatEntitet tilkjent) {
        var beregningsgrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandling.getId()).orElse(null);
        if (beregningsgrunnlag == null) {
            return List.of();
        }
        List<LocalDateSegment<DagsatsUtbgradSVP>> grunnlagSatsUtbetGrad = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(p -> p.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsgrunnlagPeriodeFom(), p.getBeregningsgrunnlagPeriodeTom(), beregnGrunnlagSatsUtbetGradSvp(p, beregningsgrunnlag.getGrunnbeløp().getVerdi())))
            .collect(Collectors.toList());
        LocalDateTimeline<DagsatsUtbgradSVP> resultatTidslinje = new LocalDateTimeline<>(tilkjent.getBeregningsresultatPerioder().stream()
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
        final Anvisning anvisning = new Anvisning();
        final Periode p = new Periode();
        p.setFom(periode.getFom());
        p.setTom(periode.getTom());
        anvisning.setPeriode(p);
        anvisning.setDagsats(new Desimaltall(new BigDecimal(periode.getValue().getDagsats())));
        anvisning.setUtbetalingsgrad(new Desimaltall(new BigDecimal(periode.getValue().getUtbetalingsgrad())));
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
        final Periode periode = new Periode();
        if (beregningsresultat != null) {
            Optional<LocalDate> minFom = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder());
            Optional<LocalDate> maxTom = beregningsresultat.getBeregningsresultatPerioder().stream()
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
        } else {
            periode.setFom(vedtak.getVedtaksdato());
            periode.setTom(vedtak.getVedtaksdato());
        }
        return periode;
    }


    private YtelseType map(FagsakYtelseType type) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(type)) {
            return YtelseType.ENGANGSTØNAD;
        } else if (FagsakYtelseType.FORELDREPENGER.equals(type)) {
            return YtelseType.FORELDREPENGER;
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(type)) {
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

    private static class DagsatsUtbgradSVP {
        private long dagsats;
        private long utbetalingsgrad;

        DagsatsUtbgradSVP(long dagsats, long utbetalingsgrad) {
            this.dagsats = dagsats;
            this.utbetalingsgrad = utbetalingsgrad;
        }

        public long getDagsats() {
            return dagsats;
        }

        public long getUtbetalingsgrad() {
            return utbetalingsgrad;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DagsatsUtbgradSVP that = (DagsatsUtbgradSVP) o;
            return dagsats == that.dagsats &&
                utbetalingsgrad == that.utbetalingsgrad;
        }
    }
}

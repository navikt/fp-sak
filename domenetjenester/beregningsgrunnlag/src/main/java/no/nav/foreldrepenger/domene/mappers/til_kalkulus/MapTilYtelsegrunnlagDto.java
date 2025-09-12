package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.folketrygdloven.kalkulus.beregning.v1.besteberegning.Ytelseandel;
import no.nav.folketrygdloven.kalkulus.beregning.v1.besteberegning.Ytelsegrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.besteberegning.Ytelseperiode;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvistAndel;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class MapTilYtelsegrunnlagDto {
    private static final List<RelatertYtelseType> FPSAK_YTELSER = Arrays.asList(RelatertYtelseType.FORELDREPENGER,
        RelatertYtelseType.SVANGERSKAPSPENGER);

    private MapTilYtelsegrunnlagDto() {
        // Skjuler konstruktør
    }


    public static List<Saksnummer> saksnummerSomMåHentesFraFpsak(DatoIntervallEntitet periodeYtelserKanVæreRelevantForBB, YtelseFilter ytelseFilter) {
        var ytelserFraFpsak = ytelseFilter.filter(y -> FPSAK_YTELSER.contains(y.getRelatertYtelseType()))
            .filter(y -> y.getKilde().equals(Fagsystem.FPSAK))
            .filter(y -> y.getPeriode().overlapper(periodeYtelserKanVæreRelevantForBB))
            .getFiltrertYtelser();
        return ytelserFraFpsak.stream().map(Ytelse::getSaksnummer).toList();
    }

    public static Optional<Ytelsegrunnlag> mapFpsakYtelseTilYtelsegrunnlag(BeregningsresultatEntitet resultat,
                                                                           FagsakYtelseType ytelseType) {
        var ytelseperioder = resultat.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getDagsats() > 0)
            .map(MapTilYtelsegrunnlagDto::mapPeriode)
            .toList();
        return ytelseperioder.isEmpty()
            ? Optional.empty()
            : Optional.of(new Ytelsegrunnlag(mapTilGenerellYtelse(ytelseType), ytelseperioder));
    }

    private static YtelseType mapTilGenerellYtelse(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD, UDEFINERT -> throw new IllegalArgumentException("Skal ikke besteberegne basert på " + ytelseType.getKode());
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
        };
    }

    private static Ytelseperiode mapPeriode(BeregningsresultatPeriode periode) {
        var andeler = periode.getBeregningsresultatAndelList().stream()
            .map(MapTilYtelsegrunnlagDto::mapAndel)
            .toList();
        return new Ytelseperiode(new Periode(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom()), andeler);
    }

    private static Ytelseandel mapAndel(BeregningsresultatAndel a) {
        return new Ytelseandel(AktivitetStatus.fraKode(a.getAktivitetStatus().getKode()),
            Inntektskategori.fraKode(a.getInntektskategori().getKode()),
            null,
            (long) a.getDagsats());
    }

    private static Optional<Ytelseperiode> mapTilYtelsegrunnlag(Ytelse sp) {
        var arbeidskategori = sp.getYtelseGrunnlag()
            .flatMap(YtelseGrunnlag::getArbeidskategori);
        if (arbeidskategori.isEmpty() || harUgyldigTilstandForBesteberegning(arbeidskategori.get(), sp.getStatus())) {
            return Optional.empty();
        }
        var andel = new Ytelseandel(null, null, KodeverkTilKalkulusMapper.mapArbeidskategori(arbeidskategori.get()),
            null);
        return Optional.of(new Ytelseperiode(new Periode(sp.getPeriode().getFomDato(), sp.getPeriode().getTomDato()),
            Collections.singletonList(andel)));
    }

    private static boolean harUgyldigTilstandForBesteberegning(Arbeidskategori kategori, RelatertYtelseTilstand sp) {
        return Arbeidskategori.UGYLDIG.equals(kategori) && RelatertYtelseTilstand.ÅPEN.equals(sp);
    }

    public static Optional<Ytelsegrunnlag> mapEksterneYtelserTilBesteberegningYtelsegrunnlag(DatoIntervallEntitet periode,
                                                                                             YtelseFilter ytelseFilter,
                                                                                             RelatertYtelseType ytelse) {
        var ytelsevedtak = ytelseFilter.filter(y -> y.getRelatertYtelseType().equals(ytelse))
            .filter(y -> y.getPeriode().overlapper(periode))
            .getFiltrertYtelser();
        if (ytelse.equals(RelatertYtelseType.SYKEPENGER)) {
            var sykepengeperioder = ytelsevedtak.stream().map(MapTilYtelsegrunnlagDto::mapTilYtelsegrunnlag).flatMap(Optional::stream).toList();
            return sykepengeperioder.isEmpty() ? Optional.empty() : Optional.of(new Ytelsegrunnlag(YtelseType.SYKEPENGER, sykepengeperioder));
        } else {
            var alleAnvistePerioder = ytelsevedtak.stream().map(Ytelse::getYtelseAnvist).flatMap(Collection::stream).toList();
            var perioder = alleAnvistePerioder.stream()
                .map(p -> new Ytelseperiode(new Periode(p.getAnvistFOM(), p.getAnvistTOM()), mapAnvisteAndeler(p.getYtelseAnvistAndeler())))
                .toList();
            return perioder.isEmpty() ? Optional.empty() : Optional.of(new Ytelsegrunnlag(mapRelatertYtelseKode(ytelse), perioder));
        }
    }

    private static List<Ytelseandel> mapAnvisteAndeler(Set<YtelseAnvistAndel> ytelseAnvistAndeler) {
        return ytelseAnvistAndeler.stream().map(a -> new Ytelseandel(finnAktivitetstatus(a), mapInntektskategori(a.getInntektskategori()), null, mapDagsats(a.getDagsats()))).toList();
    }

    private static AktivitetStatus finnAktivitetstatus(YtelseAnvistAndel a) {
        if (a.getArbeidsgiver().isPresent()) {
            return AktivitetStatus.ARBEIDSTAKER;
        }
        return utledAktivitetstatusFraInntektskategori(a.getInntektskategori());
    }

    private static AktivitetStatus utledAktivitetstatusFraInntektskategori(no.nav.abakus.iaygrunnlag.kodeverk.Inntektskategori inntektskategori) {
        return switch (inntektskategori) {
            case FRILANSER -> AktivitetStatus.FRILANSER;
            case SELVSTENDIG_NÆRINGSDRIVENDE, DAGMAMMA, JORDBRUKER -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case DAGPENGER -> AktivitetStatus.DAGPENGER;
            case ARBEIDSAVKLARINGSPENGER -> AktivitetStatus.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER, ARBEIDSTAKER_UTEN_FERIEPENGER, SJØMANN, FISKER -> AktivitetStatus.ARBEIDSTAKER;
            case UDEFINERT -> AktivitetStatus.UDEFINERT;
        };
    }

    private static Inntektskategori mapInntektskategori(no.nav.abakus.iaygrunnlag.kodeverk.Inntektskategori inntektskategori) {
        if (inntektskategori == null) {
            return null;
        }
        return switch (inntektskategori) {
            case ARBEIDSTAKER -> Inntektskategori.ARBEIDSTAKER;
            case FRILANSER -> Inntektskategori.FRILANSER;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE;
            case DAGPENGER -> Inntektskategori.DAGPENGER;
            case ARBEIDSAVKLARINGSPENGER -> Inntektskategori.ARBEIDSAVKLARINGSPENGER;
            case SJØMANN -> Inntektskategori.SJØMANN;
            case DAGMAMMA -> Inntektskategori.DAGMAMMA;
            case JORDBRUKER -> Inntektskategori.JORDBRUKER;
            case FISKER -> Inntektskategori.FISKER;
            case ARBEIDSTAKER_UTEN_FERIEPENGER -> Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER;
            case UDEFINERT -> Inntektskategori.UDEFINERT;
        };
    }

    private static Long mapDagsats(Beløp dagsats) {
        if (dagsats == null || dagsats.erNullEllerNulltall()) {
            return null;
        }
        return dagsats.getVerdi().longValue();
    }

    private static YtelseType mapRelatertYtelseKode(RelatertYtelseType ytelse) {
        return switch (ytelse) {
            case PLEIEPENGER_SYKT_BARN ->  YtelseType.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> YtelseType.PLEIEPENGER_NÆRSTÅENDE;
            case OPPLÆRINGSPENGER -> YtelseType.OPPLÆRINGSPENGER;
            default -> throw new IllegalArgumentException("Ukjent ytelse ved mapping til besteberegning ytelsegrunnlag: " + ytelse);
        };
    }}

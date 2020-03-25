package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.KodeMapper;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public final class UttakHistorikkUtil {

    private HistorikkinnslagType historikkinnslagTypeSplitt;
    private HistorikkinnslagType historikkinnslagTypeEndring;

    private static final KodeMapper<OppholdÅrsak, StønadskontoType> oppholdÅrsakMapper = initOppholdÅrsakMapper();

    private UttakHistorikkUtil(HistorikkinnslagType historikkinnslagTypeSplitt, HistorikkinnslagType historikkinnslagTypeEndring) {
        this.historikkinnslagTypeSplitt = historikkinnslagTypeSplitt;
        this.historikkinnslagTypeEndring = historikkinnslagTypeEndring;
    }

    public static UttakHistorikkUtil forFastsetting() {
        return new UttakHistorikkUtil(HistorikkinnslagType.FASTSATT_UTTAK_SPLITT, HistorikkinnslagType.FASTSATT_UTTAK);
    }

    public List<Historikkinnslag> lagHistorikkinnslag(Behandling behandling,
                                                      List<UttakResultatPeriodeLagreDto> uttakResultat,
                                                      List<ForeldrepengerUttakPeriode> gjeldende) {
        List<Historikkinnslag> historikkinnslag = new ArrayList<>();
        historikkinnslag.addAll(lagHistorikkinnslagFraSplitting(behandling, uttakResultat, gjeldende));
        historikkinnslag.addAll(lagHistorikkinnslagFraPeriodeEndringer(behandling, uttakResultat, gjeldende));
        return historikkinnslag;
    }

    private List<Historikkinnslag> lagHistorikkinnslagFraPeriodeEndringer(Behandling behandling,
                                                                          List<UttakResultatPeriodeLagreDto> grupper,
                                                                          List<ForeldrepengerUttakPeriode> gjeldende) {
        return grupper
            .stream()
            .map(gruppe -> lagHistorikkinnslagForPeriode(behandling, gruppe, gjeldende))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private List<Historikkinnslag> lagHistorikkinnslagFraSplitting(Behandling behandling,
                                                                   List<UttakResultatPeriodeLagreDto> nyePerioder,
                                                                   List<ForeldrepengerUttakPeriode> gjeldende) {
        if (nyePerioder.size() == gjeldende.size()) {
            return Collections.emptyList();
        }
        List<UttakOverstyringsPeriodeSplitt> splittet = finnSplittet(nyePerioder, gjeldende);
        return splittet.stream().map(split -> lagHistorikkinnslag(behandling, split)).collect(Collectors.toList());
    }

    private Historikkinnslag lagHistorikkinnslag(Behandling behandling,
                                                 UttakOverstyringsPeriodeSplitt split) {
        Historikkinnslag historikkinnslag = new Historikkinnslag.Builder()
            .medType(historikkinnslagTypeSplitt)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .build();
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder()
            .medSkjermlenke(SkjermlenkeType.UTTAK)
            .medHendelse(historikkinnslagTypeSplitt);
        for (LocalDateInterval splittetPeriode : split.getSplittet()) {
            if (!Objects.equals(split.getOpprinnelig(), splittetPeriode)) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.UTTAK_SPLITT_TIDSPERIODE, split.getOpprinnelig(), splittetPeriode);
            }
        }
        historikkinnslag.setHistorikkinnslagDeler(tekstBuilder.build(historikkinnslag));
        return historikkinnslag;
    }

    private List<UttakOverstyringsPeriodeSplitt> finnSplittet(List<UttakResultatPeriodeLagreDto> perioder,
                                                              List<ForeldrepengerUttakPeriode> gjeldende) {

        Map<ForeldrepengerUttakPeriode, UttakOverstyringsPeriodeSplitt.Builder> map = new HashMap<>();
        for (UttakResultatPeriodeLagreDto periode : perioder) {
            LocalDateInterval periodeInterval = new LocalDateInterval(periode.getFom(), periode.getTom());
            var matchendeGjeldendePeriode = EndreUttakUtil.finnGjeldendePeriodeFor(gjeldende, periodeInterval);
            if (!likeTidsperioder(periode, matchendeGjeldendePeriode)) {
                map.computeIfAbsent(matchendeGjeldendePeriode, m -> new UttakOverstyringsPeriodeSplitt.Builder()
                    .medOpprinnelig(new LocalDateInterval(m.getFom(), m.getTom())));
                map.get(matchendeGjeldendePeriode).leggTil(periodeInterval);
            }
        }

        return map.values().stream().map(UttakOverstyringsPeriodeSplitt.Builder::build).collect(Collectors.toList());
    }

    private boolean likeTidsperioder(UttakResultatPeriodeLagreDto periode, ForeldrepengerUttakPeriode matchendeGjeldendePeriode) {
        return matchendeGjeldendePeriode.getFom().isEqual(periode.getFom()) &&
            matchendeGjeldendePeriode.getTom().isEqual(periode.getTom());
    }

    private List<Historikkinnslag> lagHistorikkinnslagForPeriode(Behandling behandling,
                                                                 UttakResultatPeriodeLagreDto periode,
                                                                 List<ForeldrepengerUttakPeriode> gjeldende) {
        List<Historikkinnslag> list = new ArrayList<>();
        if (erOppholdsPeriode(periode)) {
            if (periodeHarEndringer(gjeldende, periode)) {
                list.add(lagHistorikkinnslagForOppholdsperiode(behandling, gjeldende, periode));
            }
        }
        for (UttakResultatPeriodeAktivitetLagreDto aktivitet : periode.getAktiviteter()) {
            if (aktivitetHarEndringer(gjeldende, periode, aktivitet)) {
                list.add(lagHistorikkinnslag(behandling, gjeldende, periode, aktivitet));
            }
        }
        return list;
    }

    private Historikkinnslag lagHistorikkinnslag(Behandling behandling,
                                                 List<ForeldrepengerUttakPeriode> gjeldende,
                                                 UttakResultatPeriodeLagreDto nyGruppe,
                                                 UttakResultatPeriodeAktivitetLagreDto nyPeriode) {
        Historikkinnslag historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medType(historikkinnslagTypeEndring)
            .build();
        HistorikkInnslagTekstBuilder tekstBuilder = lagHistorikkinnslagTekst(gjeldende, nyGruppe, nyPeriode);
        historikkinnslag.setHistorikkinnslagDeler(tekstBuilder.build(historikkinnslag));
        return historikkinnslag;
    }

    private Historikkinnslag lagHistorikkinnslagForOppholdsperiode(Behandling behandling,
                                                                   List<ForeldrepengerUttakPeriode> gjeldende,
                                                                   UttakResultatPeriodeLagreDto nyGruppe) {
        Historikkinnslag historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medType(historikkinnslagTypeEndring)
            .build();
        HistorikkInnslagTekstBuilder tekstBuilder = lagHistorikkinnslagTekstForOppholdsperiode(gjeldende, nyGruppe);
        historikkinnslag.setHistorikkinnslagDeler(tekstBuilder.build(historikkinnslag));
        return historikkinnslag;
    }

    private boolean aktivitetHarEndringer(List<ForeldrepengerUttakPeriode> gjeldende,
                                          UttakResultatPeriodeLagreDto nyPeriode,
                                          UttakResultatPeriodeAktivitetLagreDto nyAktivitet) {
        if (nyPeriode.getBegrunnelse() == null || nyPeriode.getBegrunnelse().isEmpty()) {
            return false;
        }
        return lagHistorikkinnslagTekst(gjeldende, nyPeriode, nyAktivitet).antallEndredeFelter() != 0;
    }

    private boolean periodeHarEndringer(List<ForeldrepengerUttakPeriode> gjeldende,
                                        UttakResultatPeriodeLagreDto nyGruppe) {
        return lagHistorikkinnslagTekstForOppholdsperiode(gjeldende, nyGruppe).antallEndredeFelter() != 0;
    }

    private HistorikkInnslagTekstBuilder lagHistorikkinnslagTekst(List<ForeldrepengerUttakPeriode> gjeldende,
                                                                  UttakResultatPeriodeLagreDto nyPeriode,
                                                                  UttakResultatPeriodeAktivitetLagreDto nyAktivitet) {
        var gjeldendePeriode = EndreUttakUtil.finnGjeldendePeriodeFor(gjeldende,
            new LocalDateInterval(nyPeriode.getFom(), nyPeriode.getTom()));
        var gjeldendeAktivitet = EndreUttakUtil.finnGjeldendeAktivitetFor(gjeldendePeriode, nyAktivitet.getArbeidsgiver().orElse(null),
            nyAktivitet.getArbeidsforholdId(),
            nyAktivitet.getUttakArbeidType());
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medSkjermlenke(SkjermlenkeType.UTTAK)
            .medHendelse(historikkinnslagTypeEndring)
            .medBegrunnelse(nyPeriode.getBegrunnelse())
            .medOpplysning(HistorikkOpplysningType.UTTAK_PERIODE_FOM, nyPeriode.getFom())
            .medOpplysning(HistorikkOpplysningType.UTTAK_PERIODE_TOM, nyPeriode.getTom());

        Trekkdager gjeldendeAktivitetTrekkdager = gjeldendeAktivitet.getTrekkdager();
        Trekkdager nyAktivitetTrekkdager = nyAktivitet.getTrekkdagerDesimaler();

        builder.medEndretFelt(HistorikkEndretFeltType.UTTAK_TREKKDAGER,
            HistorikkInnslagTekstBuilder.formatString(gjeldendeAktivitetTrekkdager),
            HistorikkInnslagTekstBuilder.formatString(nyAktivitetTrekkdager));

        builder.medEndretFelt(HistorikkEndretFeltType.UTTAK_STØNADSKONTOTYPE, gjeldendeAktivitet.getTrekkonto(), nyAktivitet.getStønadskontoType());
        builder.medEndretFelt(HistorikkEndretFeltType.UTTAK_PERIODE_RESULTAT_TYPE, gjeldendePeriode.getResultatType(),
            nyPeriode.getPeriodeResultatType());
        builder.medEndretFelt(HistorikkEndretFeltType.UTTAK_PROSENT_UTBETALING, gjeldendeAktivitet.getUtbetalingsprosent(), nyAktivitet.getUtbetalingsgrad());
        builder.medEndretFelt(HistorikkEndretFeltType.UTTAK_PERIODE_RESULTAT_ÅRSAK, gjeldendePeriode.getResultatÅrsak(),
            nyPeriode.getPeriodeResultatÅrsak());
        builder.medEndretFelt(HistorikkEndretFeltType.UTTAK_TREKKDAGER_FLERBARN_KVOTE, gjeldendePeriode.isFlerbarnsdager(), nyPeriode.isFlerbarnsdager());
        builder.medEndretFelt(HistorikkEndretFeltType.UTTAK_SAMTIDIG_UTTAK, gjeldendePeriode.isSamtidigUttak(), nyPeriode.isSamtidigUttak());
        builder.medEndretFelt(HistorikkEndretFeltType.UTTAK_SAMTIDIG_UTTAK, gjeldendePeriode.getSamtidigUttaksprosent(), nyPeriode.getSamtidigUttaksprosent());
        builder.medEndretFelt(HistorikkEndretFeltType.UTTAK_GRADERING_ARBEIDSFORHOLD,
            gjeldendePeriode.isGraderingInnvilget() ? HistorikkEndretFeltVerdiType.GRADERING_OPPFYLT : HistorikkEndretFeltVerdiType.GRADERING_IKKE_OPPFYLT,
            nyPeriode.isGraderingInnvilget() ? HistorikkEndretFeltVerdiType.GRADERING_OPPFYLT : HistorikkEndretFeltVerdiType.GRADERING_IKKE_OPPFYLT);
        builder.medEndretFelt(HistorikkEndretFeltType.UTTAK_GRADERING_AVSLAG_ÅRSAK, gjeldendePeriode.getGraderingAvslagÅrsak(),
            nyPeriode.getGraderingAvslagÅrsak());
        return builder;
    }

    private HistorikkInnslagTekstBuilder lagHistorikkinnslagTekstForOppholdsperiode(List<ForeldrepengerUttakPeriode> gjeldende,
                                                                                    UttakResultatPeriodeLagreDto nyPeriode) {
        var gjeldendePeriode = EndreUttakUtil.finnGjeldendePeriodeFor(gjeldende, new LocalDateInterval(nyPeriode.getFom(), nyPeriode.getTom()));
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medSkjermlenke(SkjermlenkeType.UTTAK)
            .medHendelse(historikkinnslagTypeEndring)
            .medBegrunnelse(nyPeriode.getBegrunnelse())
            .medOpplysning(HistorikkOpplysningType.UTTAK_PERIODE_FOM, nyPeriode.getFom())
            .medOpplysning(HistorikkOpplysningType.UTTAK_PERIODE_TOM, nyPeriode.getTom());
        Optional<StønadskontoType> stønadskontoTypeOpt = oppholdÅrsakMapper.map(gjeldendePeriode.getOppholdÅrsak());
        Optional<StønadskontoType> nyStønadskontoTypeOpt = oppholdÅrsakMapper.map(nyPeriode.getOppholdÅrsak());
        if (stønadskontoTypeOpt.isPresent() && nyStønadskontoTypeOpt.isPresent()) {
            builder.medEndretFelt(HistorikkEndretFeltType.UTTAK_STØNADSKONTOTYPE, stønadskontoTypeOpt.get(), nyStønadskontoTypeOpt.get());
        }
        return builder;
    }

    public static UttakHistorikkUtil forOverstyring() {
        return new UttakHistorikkUtil(HistorikkinnslagType.OVST_UTTAK_SPLITT, HistorikkinnslagType.OVST_UTTAK);
    }

    private boolean erOppholdsPeriode(UttakResultatPeriodeLagreDto periode) {
        return !OppholdÅrsak.UDEFINERT.equals(periode.getOppholdÅrsak());
    }

    private static KodeMapper<OppholdÅrsak, StønadskontoType> initOppholdÅrsakMapper() {
        return KodeMapper
            .medMapping(OppholdÅrsak.KVOTE_FELLESPERIODE_ANNEN_FORELDER, StønadskontoType.FELLESPERIODE)
            .medMapping(OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER, StønadskontoType.MØDREKVOTE)
            .medMapping(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, StønadskontoType.FEDREKVOTE)
            .medMapping(OppholdÅrsak.KVOTE_FORELDREPENGER_ANNEN_FORELDER, StønadskontoType.FORELDREPENGER)
            .build();
    }
}

package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.konfig.Tid;

public class MapYtelseperioderTjeneste {

    private static final OpptjeningAktivitetType UDEFINERT = OpptjeningAktivitetType.UDEFINERT;
    private static final String UTEN_ORGNR = "UTENORGNR";

    private MapYtelseperioderTjeneste() {
    }

    public static List<OpptjeningsperiodeForSaksbehandling> mapYtelsePerioder(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderOpptjening, Skjæringstidspunkt skjæringstidspunkt) {
        var aktørId = behandlingReferanse.aktørId();
        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt.getUtledetSkjæringstidspunkt());
        List<OpptjeningsperiodeForSaksbehandling> ytelsePerioder = new ArrayList<>();
        filter.getFiltrertYtelser().stream()
                .filter(ytelse -> !(Fagsystem.INFOTRYGD.equals(ytelse.getKilde()) && RelatertYtelseTilstand.ÅPEN.equals(ytelse.getStatus())))
                .filter(ytelse -> !(ytelse.getKilde().equals(Fagsystem.FPSAK) && ytelse.getSaksnummer().equals(behandlingReferanse.saksnummer())))
                .filter(ytelse -> ytelse.getRelatertYtelseType().girOpptjeningsTid(behandlingReferanse.fagsakYtelseType()))
                .forEach(behandlingRelaterteYtelse -> {
                    var periode = mapYtelseAnvist(behandlingRelaterteYtelse, behandlingReferanse, skjæringstidspunkt, grunnlag,
                            vurderOpptjening);
                    ytelsePerioder.addAll(periode);
                });
        return slåSammenYtelseTimelines(ytelsePerioder);
    }

    private static List<OpptjeningsperiodeForSaksbehandling> mapYtelseAnvist(Ytelse ytelse, BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt,
            InntektArbeidYtelseGrunnlag iayGrunnlag,
            OpptjeningAktivitetVurdering vurderForSaksbehandling) {
        var type = mapYtelseType(ytelse);
        List<OpptjeningsperiodeForSaksbehandling> ytelserAnvist = new ArrayList<>();
        var grunnlagList = ytelse.getYtelseGrunnlag().map(YtelseGrunnlag::getYtelseStørrelse).orElse(Collections.emptyList());
        var orgnumre = grunnlagList.stream()
                .map(ys -> ys.getOrgnr().orElse(null))
                .filter(Objects::nonNull)
                .toList();

        // Aksepter Utbetprosent null, men ikke tallet 0
        ytelse.getYtelseAnvist().stream()
                .filter(ya -> ya.getUtbetalingsgradProsent().map(Stillingsprosent::getVerdi).map(p -> p.compareTo(BigDecimal.ZERO) > 0).orElse(true))
                .forEach(ytelseAnvist -> {
                    if (orgnumre.isEmpty()) {
                        var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                                .medPeriode(hentUtDatoIntervall(ytelse, ytelseAnvist))
                                .medOpptjeningAktivitetType(type)
                                .medVurderingsStatus(vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, skjæringstidspunkt, null, iayGrunnlag, false));
                        ytelserAnvist.add(builder.build());
                    } else {
                        orgnumre.forEach(orgnr -> {
                            var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                                    .medPeriode(hentUtDatoIntervall(ytelse, ytelseAnvist))
                                    .medOpptjeningAktivitetType(type)
                                    .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
                                    .medOpptjeningsnøkkel(Opptjeningsnøkkel.forOrgnummer(orgnr))
                                    .medVurderingsStatus(vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, skjæringstidspunkt, null, iayGrunnlag, false));
                            ytelserAnvist.add(builder.build());
                        });
                    }
                });
        return ytelserAnvist;
    }

    private static DatoIntervallEntitet hentUtDatoIntervall(Ytelse ytelse, YtelseAnvist ytelseAnvist) {
        var fom = ytelseAnvist.getAnvistFOM();
        if (Fagsystem.ARENA.equals(ytelse.getKilde()) && fom.isBefore(ytelse.getPeriode().getFomDato())) {
            // Kunne vært generell men er forsiktig pga at feil som gir fpsak-ytelser fom =
            // siste uttaksperiode (er rettet)
            // OBS: TOM kan ikke justeres tilsvarende pga konvensjon rundt satsjustering
            // ....
            fom = ytelse.getPeriode().getFomDato();
        }
        var tom = ytelseAnvist.getAnvistTOM();
        if (tom != null && !Tid.TIDENES_ENDE.equals(tom)) {
            if (Fagsystem.INFOTRYGD.equals(ytelse.getKilde()) && DayOfWeek.THURSDAY.getValue() < DayOfWeek.from(tom).getValue()) {
                tom = tom.plusDays((long) DayOfWeek.SUNDAY.getValue() - DayOfWeek.from(tom).getValue());
            }
            return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        }
        return DatoIntervallEntitet.fraOgMed(fom);
    }

    private static OpptjeningAktivitetType mapYtelseType(Ytelse ytelse) {
        return OpptjeningAktivitetType.hentFraRelatertYtelseTyper()
                .getOrDefault(ytelse.getRelatertYtelseType(), Collections.singleton(UDEFINERT)).stream().findFirst().orElse(UDEFINERT);
    }

    private static List<OpptjeningsperiodeForSaksbehandling> slåSammenYtelseTimelines(List<OpptjeningsperiodeForSaksbehandling> ytelser) {
        List<OpptjeningsperiodeForSaksbehandling> resultat = new ArrayList<>();
        var gruppering = ytelser.stream()
                .collect(Collectors.groupingBy(MapYtelseperioderTjeneste::finnYtelseDiskriminator));
        gruppering.forEach((k, v) -> resultat.addAll(slåSammenYtelseListe(v)));
        return resultat;
    }

    private static List<OpptjeningsperiodeForSaksbehandling> slåSammenYtelseListe(List<OpptjeningsperiodeForSaksbehandling> ytelser) {
        var tidslinje = new LocalDateTimeline<>(ytelser.stream()
                .map(s -> new LocalDateSegment<>(new LocalDateInterval(s.getPeriode().getFomDato(), s.getPeriode().getTomDato()), s))
                .toList(), MapYtelseperioderTjeneste::slåSammenToSegment);
        return tidslinje.compress((v1, v2) -> true, MapYtelseperioderTjeneste::slåSammenToSegment).toSegments().stream()
                .map(LocalDateSegment::getValue)
                .toList();
    }

    private static LocalDateSegment<OpptjeningsperiodeForSaksbehandling> slåSammenToSegment(LocalDateInterval i,
            LocalDateSegment<OpptjeningsperiodeForSaksbehandling> lhs,
            LocalDateSegment<OpptjeningsperiodeForSaksbehandling> rhs) {
        return new LocalDateSegment<>(i, OpptjeningsperiodeForSaksbehandling.Builder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(i.getFomDato(), i.getTomDato()))
                .medOpptjeningAktivitetType(lhs.getValue().getOpptjeningAktivitetType())
                .medVurderingsStatus(lhs.getValue().getVurderingsStatus())
                .medArbeidsgiver(lhs.getValue().getArbeidsgiver())
                .medOpptjeningsnøkkel(lhs.getValue().getOpptjeningsnøkkel())
                .build());
    }

    private record YtelseGruppering(OpptjeningAktivitetType type, String orgNummer) {}

    private static YtelseGruppering finnYtelseDiskriminator(OpptjeningsperiodeForSaksbehandling ytelse) {
        var retOrgnr = Optional.ofNullable(ytelse.getArbeidsgiver()).map(Arbeidsgiver::getOrgnr).orElse(UTEN_ORGNR);
        return new YtelseGruppering(ytelse.getOpptjeningAktivitetType(), retOrgnr);
    }
}

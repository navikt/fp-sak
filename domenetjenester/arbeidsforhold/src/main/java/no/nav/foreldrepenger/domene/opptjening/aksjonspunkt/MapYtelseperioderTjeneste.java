package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.util.Tuple;

public class MapYtelseperioderTjeneste {

    private static final OpptjeningAktivitetType UDEFINERT = OpptjeningAktivitetType.UDEFINERT;
    private static final String UTEN_ORGNR = "UTENORGNR";

    public MapYtelseperioderTjeneste() {
    }

    public List<OpptjeningsperiodeForSaksbehandling> mapYtelsePerioder(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag grunnlag, OpptjeningAktivitetVurdering vurderOpptjening, LocalDate skjæringstidspunkt) {
        AktørId aktørId = behandlingReferanse.getAktørId();
        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt);
        List<OpptjeningsperiodeForSaksbehandling> ytelsePerioder = new ArrayList<>();
        filter.getFiltrertYtelser().stream()
            .filter(ytelse -> !(Fagsystem.INFOTRYGD.equals(ytelse.getKilde()) && RelatertYtelseTilstand.ÅPEN.equals(ytelse.getStatus())))
            .filter(ytelse -> !(ytelse.getKilde().equals(Fagsystem.FPSAK) && ytelse.getSaksnummer().equals(behandlingReferanse.getSaksnummer())))
            .filter(ytelse -> ytelse.getRelatertYtelseType().girOpptjeningsTid(behandlingReferanse.getFagsakYtelseType()))
            .forEach(behandlingRelaterteYtelse -> {
                List<OpptjeningsperiodeForSaksbehandling> periode = mapYtelseAnvist(behandlingRelaterteYtelse, behandlingReferanse, grunnlag, vurderOpptjening);
                ytelsePerioder.addAll(periode);
            });
        return slåSammenYtelseTimelines(ytelsePerioder);
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapYtelseAnvist(Ytelse ytelse, BehandlingReferanse behandlingReferanse,
                                                                      InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                      OpptjeningAktivitetVurdering vurderForSaksbehandling) {
        OpptjeningAktivitetType type = mapYtelseType(ytelse);
        List<OpptjeningsperiodeForSaksbehandling> ytelserAnvist = new ArrayList<>();
        List<YtelseStørrelse> grunnlagList = ytelse.getYtelseGrunnlag().map(YtelseGrunnlag::getYtelseStørrelse).orElse(Collections.emptyList());
        List<String> orgnumre = grunnlagList.stream()
            .map(ys -> ys.getOrgnr().orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        ytelse.getYtelseAnvist().stream()
            .filter(ya -> ya.getUtbetalingsgradProsent().map(Stillingsprosent::getVerdi).map(p -> p.compareTo(BigDecimal.ZERO) > 0).orElse(true)) // Aksepter Utbetprosent null , men ikke tallet 0
            .forEach(ytelseAnvist -> {
            if (orgnumre.isEmpty()) {
                OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                    .medPeriode(hentUtDatoIntervall(ytelse, ytelseAnvist))
                    .medOpptjeningAktivitetType(type)
                    .medVurderingsStatus(vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, null, iayGrunnlag, false));
                ytelserAnvist.add(builder.build());
            } else {
                orgnumre.forEach(orgnr -> {
                    OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                        .medPeriode(hentUtDatoIntervall(ytelse, ytelseAnvist))
                        .medOpptjeningAktivitetType(type)
                        .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
                        .medOpptjeningsnøkkel(Opptjeningsnøkkel.forOrgnummer(orgnr))
                        .medVurderingsStatus(vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, null, iayGrunnlag, false));
                    ytelserAnvist.add(builder.build());
                });
            }
        });
        return ytelserAnvist;
    }

    private static DatoIntervallEntitet hentUtDatoIntervall(Ytelse ytelse, YtelseAnvist ytelseAnvist) {
        LocalDate fom = ytelseAnvist.getAnvistFOM();
        if (Fagsystem.ARENA.equals(ytelse.getKilde()) && fom.isBefore(ytelse.getPeriode().getFomDato())) {
            // Kunne vært generell men er forsiktig pga at feil som gir fpsak-ytelser fom = siste uttaksperiode (er rettet)
            // OBS: TOM kan ikke justeres tilsvarende pga konvensjon rundt satsjustering ....
            fom = ytelse.getPeriode().getFomDato();
        }
        LocalDate tom = ytelseAnvist.getAnvistTOM();
        if (tom != null && !Tid.TIDENES_ENDE.equals(tom)) {
            if (Fagsystem.INFOTRYGD.equals(ytelse.getKilde()) && DayOfWeek.THURSDAY.getValue() < DayOfWeek.from(tom).getValue()) {
                tom = tom.plusDays((long) DayOfWeek.SUNDAY.getValue() - DayOfWeek.from(tom).getValue());
            }
            return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        }
        return DatoIntervallEntitet.fraOgMed(fom);
    }

    private OpptjeningAktivitetType mapYtelseType(Ytelse ytelse) {
        if (RelatertYtelseType.PÅRØRENDESYKDOM.equals(ytelse.getRelatertYtelseType())) {
            return OpptjeningAktivitetType.hentFraTemaUnderkategori()
                .getOrDefault(ytelse.getBehandlingsTema(), Collections.singleton(UDEFINERT)).stream().findFirst().orElse(UDEFINERT);
        }
        return OpptjeningAktivitetType.hentFraRelatertYtelseTyper()
            .getOrDefault(ytelse.getRelatertYtelseType(), Collections.singleton(UDEFINERT)).stream().findFirst().orElse(UDEFINERT);
    }

    private List<OpptjeningsperiodeForSaksbehandling> slåSammenYtelseTimelines(List<OpptjeningsperiodeForSaksbehandling> ytelser) {
        List<OpptjeningsperiodeForSaksbehandling> resultat = new ArrayList<>();
        Map<Tuple<OpptjeningAktivitetType, String>, List<OpptjeningsperiodeForSaksbehandling>> gruppering = ytelser.stream()
            .collect(Collectors.groupingBy(this::finnYtelseDiskriminator));
        gruppering.forEach((k, v) -> resultat.addAll(slåSammenYtelseListe(v)));
        return resultat;
    }

    private List<OpptjeningsperiodeForSaksbehandling> slåSammenYtelseListe(List<OpptjeningsperiodeForSaksbehandling> ytelser) {
        var tidslinje = new LocalDateTimeline<>(ytelser.stream()
            .map(s -> new LocalDateSegment<>(new LocalDateInterval(s.getPeriode().getFomDato(), s.getPeriode().getTomDato()), s))
            .collect(Collectors.toList()), this::slåSammenToSegment);
        return tidslinje.compress((v1,v2) -> true, this::slåSammenToSegment).toSegments().stream()
            .map(LocalDateSegment::getValue)
            .collect(Collectors.toList());
    }

    private LocalDateSegment<OpptjeningsperiodeForSaksbehandling> slåSammenToSegment(LocalDateInterval i,
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

    private Tuple<OpptjeningAktivitetType, String> finnYtelseDiskriminator(OpptjeningsperiodeForSaksbehandling ytelse) {
        String retOrgnr = ytelse.getOrgnr() != null ? ytelse.getOrgnr() : UTEN_ORGNR;
        return new Tuple<>(ytelse.getOpptjeningAktivitetType(), retOrgnr);
    }
}

package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.threeten.extra.Interval;

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
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.util.Tuple;

public class MapYtelseperioderTjeneste {

    private static final OpptjeningAktivitetType UDEFINERT = OpptjeningAktivitetType.UDEFINERT;
    private static final String UTEN_ORGNR = "UTENORGNR";

    public MapYtelseperioderTjeneste() {
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

    private static DatoIntervallEntitet slåSammenOverlappendeDatoIntervall(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        LocalDate fom = periode1.getFomDato();
        if (periode2.getFomDato().isBefore(fom)) {
            fom = periode2.getFomDato();
        }
        LocalDate tom = periode2.getTomDato();
        if (periode1.getTomDato().isAfter(tom)) {
            tom = periode1.getTomDato();
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    private static boolean erTilgrensende(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        Interval p1 = periode1.tilIntervall();
        Interval p2 = periode2.tilIntervall();
        return p1.isConnected(p2) || p2.isConnected(p1) || periode1.getTomDato().plusDays(1).equals(periode2.getFomDato()) || periode2.getTomDato().plusDays(1).equals(periode1.getFomDato());
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
        return slåSammenYtelsePerioder(ytelsePerioder);
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

        ytelse.getYtelseAnvist().forEach(ytelseAnvist -> {
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

    private OpptjeningAktivitetType mapYtelseType(Ytelse ytelse) {
        if (RelatertYtelseType.PÅRØRENDESYKDOM.equals(ytelse.getRelatertYtelseType())) {
            return OpptjeningAktivitetType.hentFraTemaUnderkategori()
                .getOrDefault(ytelse.getBehandlingsTema(), Collections.singleton(UDEFINERT)).stream().findFirst().orElse(UDEFINERT);
        }
        return OpptjeningAktivitetType.hentFraRelatertYtelseTyper()
            .getOrDefault(ytelse.getRelatertYtelseType(), Collections.singleton(UDEFINERT)).stream().findFirst().orElse(UDEFINERT);
    }

    private List<OpptjeningsperiodeForSaksbehandling> slåSammenYtelsePerioder(List<OpptjeningsperiodeForSaksbehandling> ytelser) {
        List<OpptjeningsperiodeForSaksbehandling> resultat = new ArrayList<>();
        if (ytelser.isEmpty()) {
            return resultat;
        }
        Map<Tuple<OpptjeningAktivitetType, String>, List<OpptjeningsperiodeForSaksbehandling>> sortering = ytelser.stream()
            .collect(Collectors.groupingBy(this::finnYtelseDiskriminator));
        sortering.forEach((key, value) -> resultat.addAll(slåSammenYtelsePerioderSammeType(value)));
        return resultat;
    }

    private Tuple<OpptjeningAktivitetType, String> finnYtelseDiskriminator(OpptjeningsperiodeForSaksbehandling ytelse) {
        String retOrgnr = ytelse.getOrgnr() != null ? ytelse.getOrgnr() : UTEN_ORGNR;
        return new Tuple<>(ytelse.getOpptjeningAktivitetType(), retOrgnr);
    }

    private List<OpptjeningsperiodeForSaksbehandling> slåSammenYtelsePerioderSammeType(List<OpptjeningsperiodeForSaksbehandling> ytelser) {
        if (ytelser.size() < 2) {
            return ytelser;
        }
        List<OpptjeningsperiodeForSaksbehandling> sorterFom = ytelser.stream()
            .sorted(Comparator.comparing(opfs -> opfs.getPeriode().getFomDato()))
            .collect(Collectors.toList());
        List<OpptjeningsperiodeForSaksbehandling> fusjonert = new ArrayList<>();

        Iterator<OpptjeningsperiodeForSaksbehandling> iterator = sorterFom.iterator();
        OpptjeningsperiodeForSaksbehandling prev = iterator.next();
        OpptjeningsperiodeForSaksbehandling next;
        while (iterator.hasNext()) {
            next = iterator.next();
            if (erTilgrensende(prev.getPeriode(), next.getPeriode())) {
                prev = slåSammenToPerioder(prev, next);
            } else {
                fusjonert.add(prev);
                prev = next;
            }
        }
        fusjonert.add(prev);
        return fusjonert;
    }

    private OpptjeningsperiodeForSaksbehandling slåSammenToPerioder(OpptjeningsperiodeForSaksbehandling opp1, OpptjeningsperiodeForSaksbehandling opp2) {
        return OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(slåSammenOverlappendeDatoIntervall(opp1.getPeriode(), opp2.getPeriode()))
            .medOpptjeningAktivitetType(opp1.getOpptjeningAktivitetType())
            .medVurderingsStatus(opp1.getVurderingsStatus())
            .medArbeidsgiver(opp1.getArbeidsgiver())
            .medOpptjeningsnøkkel(opp1.getOpptjeningsnøkkel())
            .build();

    }
}

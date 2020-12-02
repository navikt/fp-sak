package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType.NÆRING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

@Dependent
public class OpptjeningsperioderUtenOverstyringTjeneste {

    private OpptjeningRepository opptjeningRepository;
    private MapYtelseperioderTjeneste mapYtelseperioderTjeneste;

    @Inject
    public OpptjeningsperioderUtenOverstyringTjeneste(OpptjeningRepository opptjeningRepository) {
        this.opptjeningRepository = opptjeningRepository;
        this.mapYtelseperioderTjeneste = new MapYtelseperioderTjeneste();
    }

    public List<OpptjeningsperiodeForSaksbehandling> mapPerioderForSaksbehandling(BehandlingReferanse behandlingReferanse,
            InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderOpptjening) {
        AktørId aktørId = behandlingReferanse.getAktørId();
        List<OpptjeningsperiodeForSaksbehandling> perioder = new ArrayList<>();
        LocalDate skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();

        var mapArbeidOpptjening = OpptjeningAktivitetType.hentFraArbeidTypeRelasjoner();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId))
                .før(skjæringstidspunkt);
        for (var yrkesaktivitet : filter.getYrkesaktiviteterForBeregning()) {
            var opptjeningsperioder = MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.mapYrkesaktivitet(
                    behandlingReferanse, yrkesaktivitet, grunnlag, vurderOpptjening, mapArbeidOpptjening, null);
            perioder.addAll(opptjeningsperioder);
        }

        var oppgittOpptjening = grunnlag.getOppgittOpptjening();
        perioder.addAll(mapOppgittOpptjening(mapArbeidOpptjening, oppgittOpptjening));
        perioder.addAll(mapYtelseperioderTjeneste.mapYtelsePerioder(behandlingReferanse, grunnlag, vurderOpptjening, skjæringstidspunkt));
        lagOpptjeningsperiodeForFrilansAktivitet(behandlingReferanse, oppgittOpptjening.orElse(null), grunnlag, perioder, skjæringstidspunkt,
                mapArbeidOpptjening).ifPresent(perioder::add);

        return perioder.stream().sorted(Comparator.comparing(OpptjeningsperiodeForSaksbehandling::getPeriode)).collect(Collectors.toList());
    }

    public Optional<Opptjening> hentOpptjeningHvisFinnes(Long behandlingId) {
        return opptjeningRepository.finnOpptjening(behandlingId);
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapOppgittOpptjening(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
            Optional<OppgittOpptjening> oppgittOpptjening) {
        List<OpptjeningsperiodeForSaksbehandling> oppgittOpptjeningPerioder = new ArrayList<>();
        if (oppgittOpptjening.isPresent()) {
            // map
            final OppgittOpptjening opptjening = oppgittOpptjening.get();
            for (Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet : opptjening.getAnnenAktivitet().stream()
                    .collect(Collectors.groupingBy(OppgittAnnenAktivitet::getArbeidType)).entrySet()) {
                oppgittOpptjeningPerioder.addAll(mapAnnenAktivitet(annenAktivitet, mapArbeidOpptjening));
            }
            opptjening.getOppgittArbeidsforhold() // .filter(utenlandskArbforhold ->
                                                  // utenlandskArbforhold.getArbeidType().equals(ArbeidType.UDEFINERT))
                    .forEach(oppgittArbeidsforhold -> oppgittOpptjeningPerioder.add(mapOppgittArbeidsforholdUtenOverstyring(oppgittArbeidsforhold,
                            mapArbeidOpptjening)));
            opptjening.getEgenNæring().forEach(egenNæring -> oppgittOpptjeningPerioder.add(mapEgenNæring(egenNæring)));
        }
        return oppgittOpptjeningPerioder;
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsforholdUtenOverstyring(OppgittArbeidsforhold oppgittArbeidsforhold,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, oppgittArbeidsforhold.getArbeidType());
        return mapOppgittArbeidsperiode(oppgittArbeidsforhold, type);
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsperiode(OppgittArbeidsforhold oppgittArbeidsforhold, OpptjeningAktivitetType type) {
        final OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
        DatoIntervallEntitet periode = oppgittArbeidsforhold.getPeriode();
        builder.medOpptjeningAktivitetType(type)
                .medPeriode(periode)
                .medArbeidsgiverUtlandNavn(oppgittArbeidsforhold.getUtenlandskVirksomhet().getNavn());
        return builder.build();
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapAnnenAktivitet(Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        OpptjeningAktivitetType opptjeningAktivitetType = utledOpptjeningType(mapArbeidOpptjening, annenAktivitet.getKey());
        List<OpptjeningsperiodeForSaksbehandling> annenAktivitetPerioder = new ArrayList<>();
        for (OppgittAnnenAktivitet aktivitet : annenAktivitet.getValue()) {
            OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
            builder.medPeriode(aktivitet.getPeriode())
                    .medOpptjeningAktivitetType(opptjeningAktivitetType);
            annenAktivitetPerioder.add(builder.build());
        }
        return annenAktivitetPerioder;
    }

    private OpptjeningAktivitetType utledOpptjeningType(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, ArbeidType arbeidType) {
        return mapArbeidOpptjening.get(arbeidType)
                .stream()
                .findFirst()
                .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private OpptjeningsperiodeForSaksbehandling mapEgenNæring(OppgittEgenNæring egenNæring) {
        final OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                .medOpptjeningAktivitetType(NÆRING);
        builder.medPeriode(egenNæring.getPeriode());
        if (egenNæring.getOrgnr() != null) {
            builder.medOpptjeningsnøkkel(new Opptjeningsnøkkel(null, egenNæring.getOrgnr(), null))
                    .medArbeidsgiver(Arbeidsgiver.virksomhet(egenNæring.getOrgnr()));
        }
        builder.medStillingsandel(new Stillingsprosent(BigDecimal.valueOf(100)));
        return builder.build();
    }

    private Optional<OpptjeningsperiodeForSaksbehandling> lagOpptjeningsperiodeForFrilansAktivitet(BehandlingReferanse behandlingReferanse,
            OppgittOpptjening oppgittOpptjening,
            InntektArbeidYtelseGrunnlag grunnlag,
            List<OpptjeningsperiodeForSaksbehandling> perioder,
            LocalDate skjæringstidspunkt,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        // Hvis oppgitt frilansaktivitet brukes perioden derfra og det er allerede laget
        // en OFS.
        if (((oppgittOpptjening != null)
                && oppgittOpptjening.getAnnenAktivitet().stream().anyMatch(oaa -> ArbeidType.FRILANSER.equals(oaa.getArbeidType()))) ||
                perioder.stream().anyMatch(oaa -> OpptjeningAktivitetType.FRILANS.equals(oaa.getOpptjeningAktivitetType()))) {
            return Optional.empty();
        }
        Optional<Opptjening> opptjeningOptional = opptjeningRepository.finnOpptjening(behandlingReferanse.getId());
        if (!opptjeningOptional.isPresent()) {
            return Optional.empty();
        }

        AktørId aktørId = behandlingReferanse.getAktørId();
        Optional<AktørArbeid> aktørArbeidFraRegister = grunnlag.getAktørArbeidFraRegister(aktørId);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), aktørArbeidFraRegister).før(skjæringstidspunkt);

        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(skjæringstidspunkt).filterPensjonsgivende();

        Collection<Yrkesaktivitet> frilansOppdrag = filter.getFrilansOppdrag();

        if (aktørArbeidFraRegister.isPresent() && !inntektFilter.getFiltrertInntektsposter().isEmpty() && !frilansOppdrag.isEmpty()) {
            var periode = opptjeningOptional.get().getOpptjeningPeriode();
            List<Yrkesaktivitet> frilansMedInntekt = frilansOppdrag.stream()
                    .filter(frilans -> harInntektFraVirksomhetForPeriode(frilans, inntektFilter, periode))
                    .collect(Collectors.toList());
            OpptjeningAktivitetType brukType = utledOpptjeningType(mapArbeidOpptjening, ArbeidType.FRILANSER);
            DatoIntervallEntitet brukPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(opptjeningOptional.get().getFom(),
                    opptjeningOptional.get().getTom());
            return frilansMedInntekt.isEmpty() ? Optional.empty()
                    : Optional.of(OpptjeningsperiodeForSaksbehandling.Builder.ny()
                            .medOpptjeningAktivitetType(brukType)
                            .medPeriode(brukPeriode)
                            .build());
        }
        return Optional.empty();
    }

    private boolean harInntektFraVirksomhetForPeriode(Yrkesaktivitet frilans, InntektFilter inntektFilter, DatoIntervallEntitet opptjeningsPeriode) {
        return inntektFilter
                .filter(i -> frilans.getArbeidsgiver().equals(i.getArbeidsgiver()))
                .anyMatchFilter((i, ip) -> harInntektpostForPeriode(ip, opptjeningsPeriode));
    }

    private boolean harInntektpostForPeriode(Inntektspost ip, DatoIntervallEntitet opptjeningsPeriode) {
        return opptjeningsPeriode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(ip.getPeriode().getFomDato(), ip.getPeriode().getTomDato()));
    }
}

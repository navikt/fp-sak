package no.nav.foreldrepenger.domene.opptjening;

import static no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType.NÆRING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
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
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderBekreftetOpptjening;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderOppgittOpptjening;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.MapYrkesaktivitetTilOpptjeningsperiodeTjeneste;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.MapYtelseperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningAktivitetVurderingAksjonspunkt;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningAktivitetVurderingVilkår;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

@ApplicationScoped
public class OpptjeningsperioderTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpptjeningRepository opptjeningRepository;
    private OpptjeningAktivitetVurdering vurderForSaksbehandling;
    private OpptjeningAktivitetVurdering vurderForVilkår;
    private MapYtelseperioderTjeneste mapYtelseperioderTjeneste;

    OpptjeningsperioderTjeneste() {
        // CDI
    }

    @Inject
    public OpptjeningsperioderTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
            OpptjeningRepository opptjeningRepository,
            AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening,
            AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.opptjeningRepository = opptjeningRepository;
        this.vurderForSaksbehandling = new OpptjeningAktivitetVurderingAksjonspunkt(vurderOppgittOpptjening, vurderBekreftetOpptjening);
        this.vurderForVilkår = new OpptjeningAktivitetVurderingVilkår(vurderOppgittOpptjening, vurderBekreftetOpptjening);
        this.mapYtelseperioderTjeneste = new MapYtelseperioderTjeneste();
    }

    /**
     * Hent alle opptjeningsaktiv og utleder om noen perioder trenger vurdering av
     * saksbehandler
     */
    public List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningAktiveterForSaksbehandling(BehandlingReferanse behandlingReferanse) {
        Optional<InntektArbeidYtelseGrunnlag> grunnlagOpt = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.getBehandlingId());
        if (grunnlagOpt.isPresent()) {
            return mapOpptjeningsperiodeForSaksbehandling(behandlingReferanse, grunnlagOpt.get(), vurderForSaksbehandling);
        }
        return Collections.emptyList();
    }

    /**
     * Hent alle opptjeningsaktiv og utleder om noen perioder trenger vurdering av
     * saksbehandler
     */
    public List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningAktiveterForSaksbehandling(BehandlingReferanse behandlingReferanse,
            Optional<InntektArbeidYtelseGrunnlag> grunnlagOpt) {
        if (grunnlagOpt.isPresent()) {
            return mapOpptjeningsperiodeForSaksbehandling(behandlingReferanse, grunnlagOpt.get(), vurderForSaksbehandling);
        }
        return Collections.emptyList();
    }

    public List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningAktiveterForVilkårVurdering(BehandlingReferanse behandlingReferanse) {
        Optional<InntektArbeidYtelseGrunnlag> grunnlagOpt = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.getBehandlingId());
        if (grunnlagOpt.isPresent()) {
            return mapOpptjeningsperiodeForSaksbehandling(behandlingReferanse, grunnlagOpt.get(), vurderForVilkår);
        }
        return Collections.emptyList();
    }

    /**
     * Hent alle opptjeningsaktiv fra et gitt grunnlag og utleder om noen perioder
     * trenger vurdering av saksbehandler
     */
    public List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningAktiveterForSaksbehandling(BehandlingReferanse behandlingReferanse,
            UUID iayGrunnlagUuid) {
        InntektArbeidYtelseGrunnlag grunnlag = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingReferanse.getBehandlingId(),
                iayGrunnlagUuid);
        return mapOpptjeningsperiodeForSaksbehandling(behandlingReferanse, grunnlag, vurderForSaksbehandling);
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapOpptjeningsperiodeForSaksbehandling(BehandlingReferanse behandlingReferanse,
            InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
            OpptjeningAktivitetVurdering vurderForSaksbehandling) {
        return mapPerioderForSaksbehandling(behandlingReferanse, inntektArbeidYtelseGrunnlag, vurderForSaksbehandling);
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapPerioderForSaksbehandling(BehandlingReferanse behandlingReferanse,
            InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderOpptjening) {
        AktørId aktørId = behandlingReferanse.getAktørId();
        List<OpptjeningsperiodeForSaksbehandling> perioder = new ArrayList<>();
        LocalDate skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId))
                .før(skjæringstidspunkt);

        Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening = OpptjeningAktivitetType.hentFraArbeidTypeRelasjoner();
        for (Yrkesaktivitet yrkesaktivitet : filter.getYrkesaktiviteter()) {
            mapYrkesaktivitet(behandlingReferanse, perioder, yrkesaktivitet, grunnlag, vurderOpptjening, mapArbeidOpptjening);
        }

        final Optional<OppgittOpptjening> oppgittOpptjening = grunnlag.getOppgittOpptjening();
        if (oppgittOpptjening.isPresent()) {
            // map
            final OppgittOpptjening opptjening = oppgittOpptjening.get();
            for (Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet : opptjening.getAnnenAktivitet().stream()
                    .collect(Collectors.groupingBy(OppgittAnnenAktivitet::getArbeidType)).entrySet()) {
                mapAnnenAktivitet(perioder, annenAktivitet, grunnlag, behandlingReferanse, vurderOpptjening, mapArbeidOpptjening);
            }
            opptjening.getOppgittArbeidsforhold() // .filter(utenlandskArbforhold ->
                                                  // utenlandskArbforhold.getArbeidType().equals(ArbeidType.UDEFINERT))
                    .forEach(oppgittArbeidsforhold -> perioder.add(mapOppgittArbeidsforhold(oppgittArbeidsforhold, grunnlag,
                            behandlingReferanse, vurderOpptjening, mapArbeidOpptjening)));

            opptjening.getEgenNæring().forEach(egenNæring -> {
                OpptjeningsperiodeForSaksbehandling periode = mapEgenNæring(egenNæring, grunnlag, behandlingReferanse, vurderOpptjening);
                perioder.add(periode);
            });
        }
        perioder.addAll(mapYtelseperioderTjeneste.mapYtelsePerioder(behandlingReferanse, grunnlag, vurderOpptjening, skjæringstidspunkt));

        var filterSaksbehandlet = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        håndterManueltLagtTilAktiviteter(behandlingReferanse, grunnlag, vurderOpptjening, perioder, filterSaksbehandlet, mapArbeidOpptjening,
                skjæringstidspunkt);

        lagOpptjeningsperiodeForFrilansAktivitet(behandlingReferanse, oppgittOpptjening.orElse(null), grunnlag, perioder, skjæringstidspunkt,
                mapArbeidOpptjening, vurderOpptjening).ifPresent(perioder::add);

        return perioder.stream().sorted(Comparator.comparing(OpptjeningsperiodeForSaksbehandling::getPeriode)).collect(Collectors.toList());
    }

    private void håndterManueltLagtTilAktiviteter(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderOpptjening,
            List<OpptjeningsperiodeForSaksbehandling> perioder, YrkesaktivitetFilter filter,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, LocalDate skjæringstidspunkt) {
        filter.getYrkesaktiviteter()
                .stream()
                .filter(Yrkesaktivitet::erArbeidsforhold)
                .forEach(yr -> filter.getAnsettelsesPerioder(yr).forEach(avtale -> {
                    if (perioder.stream().noneMatch(
                            p -> p.getOpptjeningAktivitetType().equals(utledOpptjeningType(mapArbeidOpptjening, yr.getArbeidType()))
                                    && p.getPeriode().equals(avtale.getPeriode()))) {
                        leggTilManuelleAktiviteter(yr, avtale, perioder, behandlingReferanse, grunnlag, vurderOpptjening, mapArbeidOpptjening,
                                skjæringstidspunkt);
                    }
                }));
        filter.getYrkesaktiviteter()
                .stream()
                .filter(yr -> !yr.erArbeidsforhold())
                .forEach(yr -> filter.getAktivitetsAvtalerForArbeid(yr).stream().filter(av -> perioder.stream()
                        .noneMatch(p -> p.getOpptjeningAktivitetType().equals(utledOpptjeningType(mapArbeidOpptjening, yr.getArbeidType())) &&
                                p.getPeriode().equals(av.getPeriode())))
                        .forEach(avtale -> leggTilManuelleAktiviteter(yr, avtale, perioder, behandlingReferanse, grunnlag, vurderOpptjening,
                                mapArbeidOpptjening, skjæringstidspunkt)));
    }

    private void leggTilManuelleAktiviteter(Yrkesaktivitet yr, AktivitetsAvtale avtale, List<OpptjeningsperiodeForSaksbehandling> perioder,
            BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderOpptjening, Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
            LocalDate skjæringstidspunkt) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, yr.getArbeidType());
        OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
        builder.medPeriode(avtale.getPeriode())
                .medOpptjeningAktivitetType(type)
                .medBegrunnelse(avtale.getBeskrivelse())
                .medVurderingsStatus(vurderOpptjening.vurderStatus(type, behandlingReferanse, yr, grunnlag, grunnlag.harBlittSaksbehandlet()));
        yr.getStillingsprosentFor(skjæringstidspunkt).ifPresent(builder::medStillingsandel);
        MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.settArbeidsgiverInformasjon(yr, builder);
        harSaksbehandlerVurdert(builder, type, behandlingReferanse, null, vurderOpptjening, grunnlag);
        builder.medErManueltRegistrert();
        perioder.add(builder.build());
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsforhold(OppgittArbeidsforhold oppgittArbeidsforhold,
            InntektArbeidYtelseGrunnlag grunnlag,
            BehandlingReferanse behandlingReferanse, OpptjeningAktivitetVurdering vurderOpptjening,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, oppgittArbeidsforhold.getArbeidType());

        AktørId aktørId = behandlingReferanse.getAktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        final Yrkesaktivitet overstyrt = finnTilsvarende(filter, oppgittArbeidsforhold.getArbeidType(), oppgittArbeidsforhold.getPeriode())
                .orElse(null);
        return mapOppgittArbeidsperiode(oppgittArbeidsforhold, grunnlag, behandlingReferanse, vurderOpptjening, type, overstyrt);
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsperiode(OppgittArbeidsforhold oppgittArbeidsforhold,
            InntektArbeidYtelseGrunnlag grunnlag,
            BehandlingReferanse behandlingReferanse, OpptjeningAktivitetVurdering vurderOpptjening,
            OpptjeningAktivitetType type, Yrkesaktivitet overstyrt) {
        final OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
        DatoIntervallEntitet periode = utledPeriode(oppgittArbeidsforhold.getPeriode(), overstyrt);
        builder.medOpptjeningAktivitetType(type)
                .medPeriode(periode)
                .medArbeidsgiverUtlandNavn(oppgittArbeidsforhold.getUtenlandskVirksomhet().getNavn())
                .medVurderingsStatus(vurderOpptjening.vurderStatus(type, behandlingReferanse, overstyrt, grunnlag, grunnlag.harBlittSaksbehandlet()));

        if (harEndretPåPeriode(oppgittArbeidsforhold.getPeriode(), overstyrt)) {
            builder.medErPeriodenEndret();
        }

        if (overstyrt != null) {
            new YrkesaktivitetFilter(null, List.of(overstyrt)).getAktivitetsAvtalerForArbeid()
                    .stream()
                    .filter(aa -> aa.getPeriode().equals(periode))
                    .findFirst()
                    .ifPresent(aa -> builder.medBegrunnelse(aa.getBeskrivelse()));
        }
        return builder.build();
    }

    private Optional<Yrkesaktivitet> finnTilsvarende(YrkesaktivitetFilter filter, Yrkesaktivitet registerAktivitet) {
        if (filter.getYrkesaktiviteter().isEmpty()) {
            return Optional.empty();
        }
        return filter.getYrkesaktiviteter().stream().filter(yr -> matcher(yr, registerAktivitet)).findFirst();
    }

    private Optional<Yrkesaktivitet> finnTilsvarende(YrkesaktivitetFilter filter, ArbeidType arbeidType, DatoIntervallEntitet periode) {
        return filter.getYrkesaktiviteter().stream()
                .filter(yr -> matcher(yr, arbeidType)
                        && inneholderPeriode(yr, periode))
                .findFirst();
    }

    private boolean inneholderPeriode(Yrkesaktivitet yr, DatoIntervallEntitet periode) {
        return new YrkesaktivitetFilter(null, List.of(yr)).getAktivitetsAvtalerForArbeid().stream()
                .anyMatch(aa -> aa.getPeriode().overlapper(periode));
    }

    private boolean matcher(Yrkesaktivitet saksbehandlet, Yrkesaktivitet registerAktivitet) {
        if (!saksbehandlet.getArbeidType().equals(registerAktivitet.getArbeidType())) {
            return false;
        }
        return saksbehandlet.gjelderFor(registerAktivitet.getArbeidsgiver(), registerAktivitet.getArbeidsforholdRef());
    }

    private boolean matcher(Yrkesaktivitet yr, ArbeidType type) {
        return yr.getArbeidType().equals(type);
    }

    private void mapYrkesaktivitet(BehandlingReferanse behandlingReferanse,
            List<OpptjeningsperiodeForSaksbehandling> perioder,
            Yrkesaktivitet registerAktivitet,
            InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderForSaksbehandling,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        AktørId aktørId = behandlingReferanse.getAktørId();
        var filter = new YrkesaktivitetFilter(null, grunnlag.getBekreftetAnnenOpptjening(aktørId));

        var overstyrtAktivitet = finnTilsvarende(filter, registerAktivitet).orElse(null);
        var opptjeningsperioderForSaksbehandling = MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.mapYrkesaktivitet(behandlingReferanse,
                registerAktivitet, grunnlag, vurderForSaksbehandling, mapArbeidOpptjening, overstyrtAktivitet);
        perioder.addAll(opptjeningsperioderForSaksbehandling);
    }

    private void harSaksbehandlerVurdert(OpptjeningsperiodeForSaksbehandling.Builder builder, OpptjeningAktivitetType type,
            BehandlingReferanse behandlingReferanse, Yrkesaktivitet registerAktivitet,
            OpptjeningAktivitetVurdering vurderForSaksbehandling, InntektArbeidYtelseGrunnlag grunnlag) {
        if (vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, registerAktivitet, grunnlag, false)
                .equals(VurderingsStatus.TIL_VURDERING)) {
            builder.medErManueltBehandlet();
        }
    }

    public Optional<Opptjening> hentOpptjeningHvisFinnes(Long behandlingId) {
        return opptjeningRepository.finnOpptjening(behandlingId);
    }

    private void mapAnnenAktivitet(List<OpptjeningsperiodeForSaksbehandling> perioder,
            Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet,
            InntektArbeidYtelseGrunnlag grunnlag, BehandlingReferanse behandlingReferanse,
            OpptjeningAktivitetVurdering vurderForSaksbehandling, Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        var opptjeningAktivitetType = utledOpptjeningType(mapArbeidOpptjening, annenAktivitet.getKey());

        AktørId aktørId = behandlingReferanse.getAktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        for (OppgittAnnenAktivitet aktivitet : annenAktivitet.getValue()) {
            var overstyrtAktivitet = finnTilsvarende(filter, aktivitet.getArbeidType(), aktivitet.getPeriode()).orElse(null);
            var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
            var status = vurderForSaksbehandling.vurderStatus(opptjeningAktivitetType, behandlingReferanse, overstyrtAktivitet, grunnlag,
                    grunnlag.harBlittSaksbehandlet());
            builder.medPeriode(utledPeriode(aktivitet.getPeriode(), overstyrtAktivitet))
                    .medOpptjeningAktivitetType(opptjeningAktivitetType)
                    .medVurderingsStatus(status);

            if (overstyrtAktivitet != null) {
                var aktivitetsAvtale = utledAktivitetAvtale(aktivitet.getPeriode(), overstyrtAktivitet);
                aktivitetsAvtale.ifPresent(aktivitetsAvtale1 -> builder.medBegrunnelse(aktivitetsAvtale1.getBeskrivelse()));
                builder.medErManueltBehandlet();
            }
            if (grunnlag.harBlittSaksbehandlet() && VurderingsStatus.UNDERKJENT.equals(status)) {
                builder.medErManueltBehandlet();
            }
            if (harEndretPåPeriode(aktivitet.getPeriode(), overstyrtAktivitet)) {
                builder.medErPeriodenEndret();
            }
            perioder.add(builder.build());
        }
    }

    private DatoIntervallEntitet utledPeriode(DatoIntervallEntitet periode, Yrkesaktivitet overstyrtAktivitet) {
        if (overstyrtAktivitet == null) {
            return periode;
        }
        return utledAktivitetAvtale(periode, overstyrtAktivitet).map(AktivitetsAvtale::getPeriode).orElse(periode);
    }

    private Optional<AktivitetsAvtale> utledAktivitetAvtale(DatoIntervallEntitet periode, Yrkesaktivitet overstyrtAktivitet) {
        return new YrkesaktivitetFilter(null, List.of(overstyrtAktivitet)).getAktivitetsAvtalerForArbeid()
                .stream()
                .filter(it -> it.getPeriode().overlapper(periode))
                .findFirst();
    }

    private OpptjeningAktivitetType utledOpptjeningType(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, ArbeidType arbeidType) {
        return mapArbeidOpptjening.get(arbeidType)
                .stream()
                .findFirst()
                .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private OpptjeningsperiodeForSaksbehandling mapEgenNæring(OppgittEgenNæring egenNæring, InntektArbeidYtelseGrunnlag grunnlag,
            BehandlingReferanse behandlingReferanse,
            OpptjeningAktivitetVurdering vurderForSaksbehandling) {
        final OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                .medOpptjeningAktivitetType(NÆRING);

        AktørId aktørId = behandlingReferanse.getAktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        final Yrkesaktivitet overstyrt = finnTilsvarende(filter, ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, egenNæring.getPeriode()).orElse(null);
        builder.medPeriode(utledPeriode(egenNæring.getPeriode(), overstyrt))
                .medArbeidsgiverUtlandNavn(egenNæring.getVirksomhet().getNavn());
        if (egenNæring.getOrgnr() != null) {
            builder.medOpptjeningsnøkkel(new Opptjeningsnøkkel(null, egenNæring.getOrgnr(), null))
                    .medArbeidsgiver(Arbeidsgiver.virksomhet(egenNæring.getOrgnr()));
        }

        builder.medVurderingsStatus(
                vurderForSaksbehandling.vurderStatus(NÆRING, behandlingReferanse, overstyrt, grunnlag, grunnlag.harBlittSaksbehandlet()));
        if (grunnlag.harBlittSaksbehandlet()) {
            builder.medErManueltBehandlet();
        }
        builder.medStillingsandel(new Stillingsprosent(BigDecimal.valueOf(100)));
        return builder.build();
    }

    private boolean harEndretPåPeriode(DatoIntervallEntitet periode, Yrkesaktivitet overstyrtAktivitet) {
        if (overstyrtAktivitet == null) {
            return false;
        }

        return new YrkesaktivitetFilter(null, List.of(overstyrtAktivitet)).getAktivitetsAvtalerForArbeid().stream().map(AktivitetsAvtale::getPeriode)
                .noneMatch(p -> p.equals(periode));
    }

    private Optional<OpptjeningsperiodeForSaksbehandling> lagOpptjeningsperiodeForFrilansAktivitet(BehandlingReferanse behandlingReferanse,
            OppgittOpptjening oppgittOpptjening,
            InntektArbeidYtelseGrunnlag grunnlag,
            List<OpptjeningsperiodeForSaksbehandling> perioder,
            LocalDate skjæringstidspunkt,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
            OpptjeningAktivitetVurdering vurderOpptjening) {
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
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId));

        var filterRegisterFør = filter.før(skjæringstidspunkt);
        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(skjæringstidspunkt).filterPensjonsgivende();

        if (!inntektFilter.getFiltrertInntektsposter().isEmpty() && !filterRegisterFør.getFrilansOppdrag().isEmpty()) {
            var periode = opptjeningOptional.get().getOpptjeningPeriode();

            var frilansMedInntekt = filterRegisterFør.getFrilansOppdrag().stream()
                    .filter(frilans -> harInntektFraVirksomhetForPeriode(frilans, inntektFilter, periode))
                    .collect(Collectors.toList());
            var brukType = utledOpptjeningType(mapArbeidOpptjening, ArbeidType.FRILANSER);
            var brukPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(opptjeningOptional.get().getFom(), opptjeningOptional.get().getTom());

            var filterSaksbehandlet = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
                    grunnlag.getBekreftetAnnenOpptjening(aktørId));
            var overstyrtAktivitet = finnTilsvarende(filterSaksbehandlet, ArbeidType.FRILANSER, brukPeriode).orElse(null);

            return frilansMedInntekt.isEmpty() ? Optional.empty()
                    : Optional.of(OpptjeningsperiodeForSaksbehandling.Builder.ny()
                            .medOpptjeningAktivitetType(brukType)
                            .medPeriode(brukPeriode)
                            .medVurderingsStatus(
                                    vurderOpptjening.vurderStatus(brukType, behandlingReferanse, overstyrtAktivitet, grunnlag,
                                            grunnlag.harBlittSaksbehandlet()))
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

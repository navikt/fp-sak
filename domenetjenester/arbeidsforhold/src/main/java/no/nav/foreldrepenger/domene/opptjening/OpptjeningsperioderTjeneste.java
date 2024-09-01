package no.nav.foreldrepenger.domene.opptjening;

import static no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType.FRILOPP;
import static no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType.NÆRING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
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
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderBekreftetOpptjening;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderOppgittOpptjening;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.MapYrkesaktivitetTilOpptjeningsperiodeTjeneste;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.MapYtelseperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningAktivitetVurderingAksjonspunkt;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningAktivitetVurderingVilkår;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class OpptjeningsperioderTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpptjeningRepository opptjeningRepository;
    private OpptjeningAktivitetVurdering vurderForSaksbehandling;
    private OpptjeningAktivitetVurdering vurderForVilkår;

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
    }

    /**
     * Hent alle opptjeningsaktiv og utleder om noen perioder trenger vurdering av
     * saksbehandler
     */
    public List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningAktiveterForSaksbehandling(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt) {
        return inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())
            .map(grunnlag -> mapOpptjeningsperiodeForSaksbehandling(behandlingReferanse, skjæringstidspunkt, grunnlag, vurderForSaksbehandling)).orElse(List.of());
    }

    public List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningAktiveterForVilkårVurdering(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt) {
        return inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())
            .map(grunnlag -> mapOpptjeningsperiodeForSaksbehandling(behandlingReferanse, skjæringstidspunkt, grunnlag, vurderForVilkår)).orElse(List.of());
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapOpptjeningsperiodeForSaksbehandling(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt,
            InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderOpptjening) {
        var aktørId = behandlingReferanse.aktørId();
        List<OpptjeningsperiodeForSaksbehandling> perioder = new ArrayList<>();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId))
                .før(skjæringstidspunkt.getUtledetSkjæringstidspunkt());

        var mapArbeidOpptjening = OpptjeningAktivitetType.hentFraArbeidTypeRelasjoner();
        for (var yrkesaktivitet : filter.getYrkesaktiviteter()) {
            mapYrkesaktivitet(behandlingReferanse, skjæringstidspunkt, perioder, yrkesaktivitet, grunnlag, vurderOpptjening, mapArbeidOpptjening);
        }

        var oppgittOpptjening = grunnlag.getGjeldendeOppgittOpptjening();
        if (oppgittOpptjening.isPresent()) {
            // map
            var opptjening = oppgittOpptjening.get();
            for (var annenAktivitet : opptjening.getAnnenAktivitet().stream()
                    .collect(Collectors.groupingBy(OppgittAnnenAktivitet::getArbeidType)).entrySet()) {
                mapAnnenAktivitet(perioder, annenAktivitet, grunnlag, behandlingReferanse, skjæringstidspunkt, vurderOpptjening, mapArbeidOpptjening);
            }
            opptjening.getOppgittArbeidsforhold() // .filter(utenlandskArbforhold ->
                                                  // utenlandskArbforhold.getArbeidType().equals(ArbeidType.UDEFINERT))
                    .forEach(oppgittArbeidsforhold -> perioder.add(mapOppgittArbeidsforhold(oppgittArbeidsforhold, grunnlag,
                            behandlingReferanse, skjæringstidspunkt, vurderOpptjening, mapArbeidOpptjening)));

            opptjening.getEgenNæring().forEach(egenNæring -> {
                var periode = mapEgenNæring(egenNæring, grunnlag, behandlingReferanse, skjæringstidspunkt, vurderOpptjening);
                perioder.add(periode);
            });
        }
        perioder.addAll(MapYtelseperioderTjeneste.mapYtelsePerioder(behandlingReferanse, grunnlag, vurderOpptjening, skjæringstidspunkt));

        var filterSaksbehandlet = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        håndterManueltLagtTilAktiviteter(behandlingReferanse, grunnlag, vurderOpptjening, perioder, filterSaksbehandlet, mapArbeidOpptjening,
                skjæringstidspunkt);

        for (var yrkesaktivitet : filter.getFrilansOppdrag()) {
            mapYrkesaktivitet(behandlingReferanse, skjæringstidspunkt, perioder, yrkesaktivitet, grunnlag, vurderOpptjening, mapArbeidOpptjening);
        }
        if (vurderOpptjening.skalInkludereAkkumulertFrilans()) {
            lagOpptjeningsperiodeForFrilansAktivitet(behandlingReferanse, oppgittOpptjening, grunnlag, perioder, skjæringstidspunkt,
                mapArbeidOpptjening, vurderOpptjening).ifPresent(perioder::add);
        }
        if (!vurderOpptjening.skalInkludereDetaljertFrilansOppdrag()) {
            return perioder.stream()
                .filter(oa -> !FRILOPP.equals(oa.getOpptjeningAktivitetType()))
                .sorted(Comparator.comparing(OpptjeningsperiodeForSaksbehandling::getPeriode))
                .toList();
        }
        return perioder.stream().sorted(Comparator.comparing(OpptjeningsperiodeForSaksbehandling::getPeriode)).toList();
    }

    private void håndterManueltLagtTilAktiviteter(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderOpptjening,
            List<OpptjeningsperiodeForSaksbehandling> perioder, YrkesaktivitetFilter filter,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, Skjæringstidspunkt skjæringstidspunkt) {
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
            Skjæringstidspunkt skjæringstidspunkt) {
        var type = utledOpptjeningType(mapArbeidOpptjening, yr.getArbeidType());
        var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
        builder.medPeriode(avtale.getPeriode())
                .medOpptjeningAktivitetType(type)
                .medBegrunnelse(avtale.getBeskrivelse())
                .medVurderingsStatus(vurderOpptjening.vurderStatus(type, behandlingReferanse, skjæringstidspunkt, yr, grunnlag, grunnlag.harBlittSaksbehandlet()));
        getStillingsprosentVedDato(yr, skjæringstidspunkt.getUtledetSkjæringstidspunkt()).ifPresent(builder::medStillingsandel);
        MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.settArbeidsgiverInformasjon(yr, builder);
        harSaksbehandlerVurdert(builder, type, behandlingReferanse, skjæringstidspunkt, vurderOpptjening, grunnlag);
        builder.medErManueltRegistrert();
        perioder.add(builder.build());
    }

    private static Optional<Stillingsprosent> getStillingsprosentVedDato(Yrkesaktivitet yrkesaktivitet, LocalDate dato) {
        return yrkesaktivitet.getAlleAktivitetsAvtaler()
            .stream()
            .filter(a -> !a.erAnsettelsesPeriode())
            .filter(a -> a.getPeriode().inkluderer(dato))
            .max(Comparator.comparing(a -> a.getPeriode().getFomDato()))
            .map(AktivitetsAvtale::getProsentsats);
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsforhold(OppgittArbeidsforhold oppgittArbeidsforhold,
            InntektArbeidYtelseGrunnlag grunnlag,
            BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt, OpptjeningAktivitetVurdering vurderOpptjening,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        var type = utledOpptjeningType(mapArbeidOpptjening, oppgittArbeidsforhold.getArbeidType());

        var aktørId = behandlingReferanse.aktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        var overstyrt = finnTilsvarende(filter, oppgittArbeidsforhold.getArbeidType(), oppgittArbeidsforhold.getPeriode()).orElse(null);
        return mapOppgittArbeidsperiode(oppgittArbeidsforhold, grunnlag, behandlingReferanse, skjæringstidspunkt, vurderOpptjening, type, overstyrt);
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsperiode(OppgittArbeidsforhold oppgittArbeidsforhold,
            InntektArbeidYtelseGrunnlag grunnlag,
            BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt, OpptjeningAktivitetVurdering vurderOpptjening,
            OpptjeningAktivitetType type, Yrkesaktivitet overstyrt) {
        var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
        var periode = utledPeriode(oppgittArbeidsforhold.getPeriode(), overstyrt);
        builder.medOpptjeningAktivitetType(type)
                .medPeriode(periode)
                .medArbeidsgiverUtlandNavn(oppgittArbeidsforhold.getUtenlandskVirksomhet().getNavn())
                .medVurderingsStatus(vurderOpptjening.vurderStatus(type, behandlingReferanse, skjæringstidspunkt, overstyrt, grunnlag, grunnlag.harBlittSaksbehandlet()));

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

    private void mapYrkesaktivitet(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt stp,
            List<OpptjeningsperiodeForSaksbehandling> perioder,
            Yrkesaktivitet registerAktivitet,
            InntektArbeidYtelseGrunnlag grunnlag,
            OpptjeningAktivitetVurdering vurderForSaksbehandling,
            Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        var aktørId = behandlingReferanse.aktørId();
        var filter = new YrkesaktivitetFilter(Optional.empty(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        var overstyrtAktivitet = finnTilsvarende(filter, registerAktivitet).orElse(null);
        var opptjeningsperioderForSaksbehandling = MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.mapYrkesaktivitet(behandlingReferanse,
                stp, registerAktivitet, grunnlag, vurderForSaksbehandling, mapArbeidOpptjening, overstyrtAktivitet);
        perioder.addAll(opptjeningsperioderForSaksbehandling);
    }

    private void harSaksbehandlerVurdert(OpptjeningsperiodeForSaksbehandling.Builder builder, OpptjeningAktivitetType type,
            BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt, OpptjeningAktivitetVurdering vurderForSaksbehandling, InntektArbeidYtelseGrunnlag grunnlag) {
        if (vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, skjæringstidspunkt, null, grunnlag, false)
                .equals(VurderingsStatus.TIL_VURDERING)) {
            builder.medErManueltBehandlet();
        }
    }

    public Optional<Opptjening> hentOpptjeningHvisFinnes(Long behandlingId) {
        return opptjeningRepository.finnOpptjening(behandlingId);
    }

    public Collection<FerdiglignetNæring> hentFerdiglignetNæring(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt stp) {
        return inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.behandlingId())
            .map(grunnlag -> grunnlag.getAktørInntektFraRegister(behandlingReferanse.aktørId()))
            .map(InntektFilter::new)
            .map(ai -> ai.før(stp.getUtledetSkjæringstidspunkt()))
            .map(InntektFilter::filterBeregnetSkatt)
            .map(f -> f.filter(InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE, InntektspostType.NÆRING_FISKE_FANGST_FAMBARNEHAGE))
            .map(f -> f.mapInntektspost(OpptjeningsperioderTjeneste::mapInntektspost)).orElse(List.of());
    }

    private static FerdiglignetNæring mapInntektspost(Inntekt inntekt, Inntektspost inntektspost) {
       return new FerdiglignetNæring(String.valueOf(inntektspost.getPeriode().getFomDato().getYear()),
           inntektspost.getBeløp().erNullEllerNulltall() ? 0L : inntektspost.getBeløp().getVerdi().longValue());
    }

    private void mapAnnenAktivitet(List<OpptjeningsperiodeForSaksbehandling> perioder,
            Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet,
            InntektArbeidYtelseGrunnlag grunnlag, BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt,
            OpptjeningAktivitetVurdering vurderForSaksbehandling, Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        var opptjeningAktivitetType = utledOpptjeningType(mapArbeidOpptjening, annenAktivitet.getKey());

        var aktørId = behandlingReferanse.aktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        for (var aktivitet : annenAktivitet.getValue()) {
            var overstyrtAktivitet = finnTilsvarende(filter, aktivitet.getArbeidType(), aktivitet.getPeriode()).orElse(null);
            var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
            var status = vurderForSaksbehandling.vurderStatus(opptjeningAktivitetType, behandlingReferanse, skjæringstidspunkt, overstyrtAktivitet, grunnlag,
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
            BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt,
            OpptjeningAktivitetVurdering vurderForSaksbehandling) {
        var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny().medOpptjeningAktivitetType(NÆRING);

        var aktørId = behandlingReferanse.aktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        var overstyrt = finnTilsvarende(filter, ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, egenNæring.getPeriode()).orElse(null);
        builder.medPeriode(utledPeriode(egenNæring.getPeriode(), overstyrt))
                .medArbeidsgiverUtlandNavn(egenNæring.getVirksomhet().getNavn());
        if (egenNæring.getOrgnr() != null) {
            builder.medOpptjeningsnøkkel(new Opptjeningsnøkkel(null, egenNæring.getOrgnr(), null))
                    .medArbeidsgiver(Arbeidsgiver.virksomhet(egenNæring.getOrgnr()));
        }

        builder.medVurderingsStatus(
                vurderForSaksbehandling.vurderStatus(NÆRING, behandlingReferanse, skjæringstidspunkt, overstyrt, grunnlag, grunnlag.harBlittSaksbehandlet()));
        if (grunnlag.harBlittSaksbehandlet()) {
            builder.medErManueltBehandlet();
        }
        builder.medStillingsandel(new Stillingsprosent(BigDecimal.valueOf(100)));
        var beskrivelseOpt = finnNæringBegrunnelseFraSaksbehandlet(grunnlag, aktørId);
        beskrivelseOpt.ifPresent(builder::medBegrunnelse);
        return builder.build();
    }

    private static Optional<String> finnNæringBegrunnelseFraSaksbehandlet(InntektArbeidYtelseGrunnlag grunnlag, AktørId aktørId) {
        var manueltBekreftetNæring = grunnlag.getSaksbehandletVersjon()
            .flatMap(sbh -> sbh.getAktørArbeid().stream().filter(aa -> Objects.equals(aa.getAktørId(), aktørId)).findFirst())
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList())
            .stream()
            .filter(ya -> ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(ya.getArbeidType()))
            .findFirst();
        return manueltBekreftetNæring
            .map(Yrkesaktivitet::getAlleAktivitetsAvtaler)
            .orElse(Collections.emptyList())
            .stream()
            .filter(aa -> aa.getBeskrivelse() != null)
            .findFirst()
            .map(AktivitetsAvtale::getBeskrivelse);
    }

    private boolean harEndretPåPeriode(DatoIntervallEntitet periode, Yrkesaktivitet overstyrtAktivitet) {
        if (overstyrtAktivitet == null) {
            return false;
        }

        return new YrkesaktivitetFilter(null, List.of(overstyrtAktivitet)).getAktivitetsAvtalerForArbeid().stream().map(AktivitetsAvtale::getPeriode)
                .noneMatch(p -> p.equals(periode));
    }

    private Optional<OpptjeningsperiodeForSaksbehandling> lagOpptjeningsperiodeForFrilansAktivitet(BehandlingReferanse ref,
        Optional<OppgittOpptjening> oppgittOpptjening,
        InntektArbeidYtelseGrunnlag grunnlag,
        List<OpptjeningsperiodeForSaksbehandling> perioder,
        Skjæringstidspunkt skjæringstidspunkt,
        Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
        OpptjeningAktivitetVurdering vurderOpptjening) {
        // Hvis oppgitt frilansaktivitet brukes perioden derfra og det er allerede laget
        // en OFS.
        if (oppgittOpptjening.map(OppgittOpptjening::getAnnenAktivitet).orElse(List.of()).stream().anyMatch(oaa -> ArbeidType.FRILANSER.equals(oaa.getArbeidType())) ||
            perioder.stream().anyMatch(oaa -> OpptjeningAktivitetType.FRILANS.equals(oaa.getOpptjeningAktivitetType()))) {
            return Optional.empty();
        }

        var opptjeningsperiode = opptjeningRepository.finnOpptjening(ref.behandlingId())
            .map(o -> DatoIntervallEntitet.fraOgMedTilOgMed(o.getFom(), o.getTom())).orElse(null);
        if (opptjeningsperiode == null) {
            return Optional.empty();
        }

        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(ref.aktørId())).før(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).filterPensjonsgivende();

        var aktivitetsperioder = perioder.stream()
            .filter(p -> OpptjeningAktivitetType.FRILOPP.equals(p.getOpptjeningAktivitetType()))
            .filter(p -> opptjeningsperiode.overlapper(p.getPeriode()))
            .filter(p -> harInntektFraVirksomhetForPeriode(p.getArbeidsgiver(), inntektFilter, opptjeningsperiode))
            .map(OpptjeningsperiodeForSaksbehandling::getPeriode)
            .map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), Boolean.TRUE))
            .toList();

        if (aktivitetsperioder.isEmpty()) return Optional.empty();
        var opptjeningsperiodeTimeline = new LocalDateTimeline<>(opptjeningsperiode.getFomDato(), opptjeningsperiode.getTomDato(), Boolean.TRUE);
        var timeline = new LocalDateTimeline<>(aktivitetsperioder, StandardCombinators::alwaysTrueForMatch).intersection(opptjeningsperiodeTimeline).compress();
        var mindato = timeline.getLocalDateIntervals().stream().map(LocalDateInterval::getFomDato).min(Comparator.naturalOrder()).orElseGet(skjæringstidspunkt::getUtledetSkjæringstidspunkt);
        var maxdato = timeline.getLocalDateIntervals().stream().map(LocalDateInterval::getTomDato).max(Comparator.naturalOrder()).orElseGet(skjæringstidspunkt::getUtledetSkjæringstidspunkt);

        if (mindato.equals(skjæringstidspunkt.getUtledetSkjæringstidspunkt()) || maxdato.equals(skjæringstidspunkt.getUtledetSkjæringstidspunkt())) return Optional.empty();
        var brukType = utledOpptjeningType(mapArbeidOpptjening, ArbeidType.FRILANSER);
        var filterSaksbehandlet = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(ref.aktørId()));
        var overstyrtAktivitet = finnTilsvarende(filterSaksbehandlet, ArbeidType.FRILANSER, opptjeningsperiode).orElse(null);
        var akkumulert = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medOpptjeningAktivitetType(brukType)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(mindato, maxdato))
            .medVurderingsStatus(vurderOpptjening.vurderStatus(brukType, ref, skjæringstidspunkt, overstyrtAktivitet, grunnlag, grunnlag.harBlittSaksbehandlet()))
            .build();
        return Optional.of(akkumulert);

    }

    private boolean harInntektFraVirksomhetForPeriode(Arbeidsgiver arbeidsgiver, InntektFilter inntektFilter, DatoIntervallEntitet opptjeningsPeriode) {
        return inntektFilter
            .filter(i -> arbeidsgiver.equals(i.getArbeidsgiver()))
            .anyMatchFilter((i, ip) -> opptjeningsPeriode.overlapper(ip.getPeriode()));
    }
}

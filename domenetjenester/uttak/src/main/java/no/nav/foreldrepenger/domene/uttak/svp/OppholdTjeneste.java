package no.nav.foreldrepenger.domene.uttak.svp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.svangerskapspenger.domene.felles.Arbeidsforhold;
import no.nav.svangerskapspenger.domene.søknad.Opphold;
import no.nav.svangerskapspenger.tjeneste.fastsettuttak.SvpOppholdÅrsak;

@ApplicationScoped
public class OppholdTjeneste {
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private static final Logger LOG = LoggerFactory.getLogger(OppholdTjeneste.class);

    @Inject
    public OppholdTjeneste(InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }
    OppholdTjeneste() {
        //CDI
    }
    public Map<Arbeidsforhold, List<Opphold>> finnOppholdFraTilretteleggingOgInntektsmelding(BehandlingReferanse behandlingRef, Skjæringstidspunkt stp, SvangerskapspengerGrunnlag svpGrunnlag) {
        Map<Arbeidsforhold, List<Opphold>> oppholdForAlleArbeidsforholdMap = new HashMap<>();
        svpGrunnlag.getGrunnlagEntitet()
            .map(SvpGrunnlagEntitet::hentTilretteleggingerSomSkalBrukes)
            .orElse(Collections.emptyList()).forEach(tilrettelegging -> {
            var aktivitetType = RegelmodellSøknaderMapper.mapTilAktivitetType(tilrettelegging.getArbeidType());
            var arbeidsforhold =  RegelmodellSøknaderMapper.lagArbeidsforhold(tilrettelegging.getArbeidsgiver(), tilrettelegging.getInternArbeidsforholdRef(), aktivitetType);

            //henter ferie- eller sykepenger-opphold registrert av saksbehandler
            var oppholdListeFraTIlr = hentOppholdListeFraTilrettelegging(tilrettelegging);
            List<Opphold> alleOppholdForArbeidsforhold = new ArrayList<>(oppholdListeFraTIlr);

            LOG.info("OppholdAnalyse: alleOppholdForArbeidsforhold fra tilrettelegging: {}, antall: {}", tilrettelegging,alleOppholdForArbeidsforhold.size());
            alleOppholdForArbeidsforhold.forEach(opphold -> LOG.info("OppholdAnalyse - fradato {},tildato {}, årsak {}",opphold.getFom(), opphold.getTom(), opphold.getÅrsak()));
            //henter ferier fra inntektsmelding meldt av arbeidsgiver
            tilrettelegging.getArbeidsgiver().ifPresent( arbeidsgiver -> {
                var oppholdListeArbeidsforholdFraInntektsmelding = finnOppholdFraIMForArbeidsgiver(behandlingRef, stp, arbeidsgiver, tilrettelegging.getInternArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()));
                LOG.info("Opphold fra Inntektsmelding for arbeidsforhold {}", arbeidsgiver);
                oppholdListeArbeidsforholdFraInntektsmelding.forEach(oppholdIm -> LOG.info("OppholdAnalyse - fradato {}, tildato {}, årsak {}",oppholdIm.getFom(), oppholdIm.getTom(), oppholdIm.getÅrsak()));

                alleOppholdForArbeidsforhold.addAll(oppholdListeArbeidsforholdFraInntektsmelding);

                LOG.info("OppholdAnalyse: alleOppholdForArbeidsforhold antall etter Inntektsmelding: {}", alleOppholdForArbeidsforhold.size());
            });
            oppholdForAlleArbeidsforholdMap.put(arbeidsforhold, alleOppholdForArbeidsforhold);
        });
        return oppholdForAlleArbeidsforholdMap;
    }

    private List<Opphold> hentOppholdListeFraTilrettelegging(SvpTilretteleggingEntitet tilrettelegging) {
        List<Opphold> oppholdListeFraSaksbehandler = new ArrayList<>();
        tilrettelegging.getAvklarteOpphold().forEach( avklartOpphold -> oppholdListeFraSaksbehandler.addAll(Opphold.opprett(avklartOpphold.getFom(), avklartOpphold.getTom(), mapOppholdÅrsak(avklartOpphold.getOppholdÅrsak()))));
        return oppholdListeFraSaksbehandler;
    }

    private List<Opphold> finnOppholdFraIMForArbeidsgiver(BehandlingReferanse behandlingRef, Skjæringstidspunkt stp, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internRef) {
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingRef, stp.getSkjæringstidspunktOpptjening());
        List<Opphold> oppholdListe = new ArrayList<>();
        inntektsmeldinger.stream()
            .filter(inntektsmelding -> inntektsmelding.getArbeidsgiver().equals(arbeidsgiver) && inntektsmelding.getArbeidsforholdRef()
                .gjelderFor(internRef))
            .flatMap(inntektsmelding -> inntektsmelding.getUtsettelsePerioder().stream())
            .filter(utsettelse -> UtsettelseÅrsak.FERIE.equals(utsettelse.getÅrsak()))
            .forEach(utsettelse -> oppholdListe.addAll(Opphold.opprett(utsettelse.getPeriode().getFomDato(), utsettelse.getPeriode().getTomDato(), SvpOppholdÅrsak.FERIE)));
        return oppholdListe;
    }

    private SvpOppholdÅrsak mapOppholdÅrsak(no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak oppholdÅrsak) {
        return switch (oppholdÅrsak) {
            case FERIE -> SvpOppholdÅrsak.FERIE;
            case SYKEPENGER -> SvpOppholdÅrsak.SYKEPENGER;
        };
    }

}

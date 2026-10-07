package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;

/**
 * Utleder hvilke arbeidsgivere vi krever inntektsmelding fra. Vurderingen gjøres pr arbeidsgiver: én inntektsmelding
 * fra en arbeidsgiver dekker alle arbeidsforhold hos arbeidsgiveren, uavhengig av arbeidsforholdId.
 */
@ApplicationScoped
public class InntektsmeldingRegisterTjeneste {

    private static final String VALID_REF = "behandlingReferanse";
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingRegisterTjeneste.class);

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private Instance<InntektsmeldingFilterYtelse> inntektsmeldingFiltere;

    InntektsmeldingRegisterTjeneste() {
        // CDI-runner
    }

    @Inject
    public InntektsmeldingRegisterTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
            InntektsmeldingTjeneste inntektsmeldingTjeneste, @Any Instance<InntektsmeldingFilterYtelse> inntektsmeldingFiltere) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.inntektsmeldingFiltere = inntektsmeldingFiltere;
    }

    /**
     * Alle arbeidsgivere vi krever inntektsmelding fra, uten å ta hensyn til om inntektsmeldingen har kommet eller ikke.
     * Filtrert for søknad (svp) og åpenbart passive arbeidsforhold.
     */
    public Set<Arbeidsgiver> utledPåkrevdeInntektsmeldinger(BehandlingReferanse referanse, Skjæringstidspunkt stp) {
        Objects.requireNonNull(referanse, VALID_REF);
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(referanse.behandlingId());
        return utledPåkrevdeInntektsmeldinger(referanse, stp, inntektArbeidYtelseGrunnlag);
    }

    /**
     * Arbeidsgivere vi krever inntektsmelding fra, men som ikke har sendt inntektsmelding og som saksbehandler
     * ikke har avklart at ikke trenger å sende.
     */
    public Set<Arbeidsgiver> utledManglendeInntektsmeldinger(BehandlingReferanse referanse, Skjæringstidspunkt stp) {
        Objects.requireNonNull(referanse, VALID_REF);
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(referanse.behandlingId());
        var manglende = new HashSet<>(utledPåkrevdeInntektsmeldinger(referanse, stp, inntektArbeidYtelseGrunnlag));
        if (!manglende.isEmpty()) {
            inntektsmeldingTjeneste.hentInntektsmeldinger(referanse, stp.getUtledetSkjæringstidspunkt()).stream()
                .map(Inntektsmelding::getArbeidsgiver)
                .forEach(manglende::remove);
            inntektArbeidYtelseGrunnlag.flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon)
                .map(ArbeidsforholdInformasjon::getOverstyringer)
                .orElse(List.of())
                .stream()
                .filter(ArbeidsforholdOverstyring::kreverIkkeInntektsmelding)
                .map(ArbeidsforholdOverstyring::getArbeidsgiver)
                .forEach(manglende::remove);
        }
        logInntektsmeldinger(referanse, manglende, "FILTRERT bort arbeidsgivere vi har mottatt inntektsmelding fra");
        return manglende;
    }

    private Set<Arbeidsgiver> utledPåkrevdeInntektsmeldinger(BehandlingReferanse referanse,
                                                             Skjæringstidspunkt stp,
                                                             Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        LOG.info("Utleder påkrevde inntektsmeldinger på skjæringstidspunkt {} for behandling {}", stp.getUtledetSkjæringstidspunkt(), referanse.behandlingId());
        var påkrevde = utledPåkrevdeInntektsmeldingerFraGrunnlag(referanse, stp, inntektArbeidYtelseGrunnlag);
        logInntektsmeldinger(referanse, påkrevde, "UFILTRERT");

        var filter = finnFilter(referanse);
        var søkteArbeidsgivere = filter.søknadsFilter(referanse, påkrevde);
        logInntektsmeldinger(referanse, søkteArbeidsgivere, "FILTRERT bort arbeidsgivere det ikke er søkt(svp) for");

        var aktiveArbeidsgivere = filter.aktiveArbeidsforholdFilter(referanse, stp, inntektArbeidYtelseGrunnlag, søkteArbeidsgivere);
        logInntektsmeldinger(referanse, aktiveArbeidsgivere, "FILTRERT bort inaktive arbeidsgivere");
        return aktiveArbeidsgivere;
    }

    private Set<Arbeidsgiver> utledPåkrevdeInntektsmeldingerFraGrunnlag(BehandlingReferanse referanse,
                                                                        Skjæringstidspunkt skjæringstidspunkt,
                                                                        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        return inntektArbeidYtelseGrunnlag.map(grunnlag -> {
            var stp = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
            var filterFør = RelevanteYrkesaktiviteterForInntektsmelding.lagFilter(grunnlag, referanse.aktørId(), stp);

            var yrkesaktiviteterNy = filterFør.getYrkesaktiviteterKunAnsettelsesperiode();
            var yrkesaktiviteterGammel = filterFør.getYrkesaktiviteter();
            if (yrkesaktiviteterGammel.size() != yrkesaktiviteterNy.size()) {
                LOG.info("IM_BEHOV_DIFF: Ny og gammel metode for utledning av hvilke arbeidsforhold vi trenger inntektsmelding fra gir ulike resultater."
                    + " Saksnummer {} med gammel liste: {} og ny liste {}", referanse.saksnummer(), yrkesaktiviteterGammel, yrkesaktiviteterNy);
            }

            var arbeidsgivere = RelevanteYrkesaktiviteterForInntektsmelding.finn(filterFør, stp).stream()
                .map(Yrkesaktivitet::getArbeidsgiver)
                .collect(Collectors.toSet());
            LOG.info("Relevante arbeidsgivere for inntektsmelding: {}", arbeidsgivere);
            return arbeidsgivere;
        }).orElseGet(Set::of);
    }

    private InntektsmeldingFilterYtelse finnFilter(BehandlingReferanse referanse) {
        return FagsakYtelseTypeRef.Lookup.find(inntektsmeldingFiltere, referanse.fagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + referanse.fagsakYtelseType().getKode()));
    }

    private static void logInntektsmeldinger(BehandlingReferanse referanse, Set<Arbeidsgiver> arbeidsgivere, String filtrert) {
        if (arbeidsgivere.isEmpty()) {
            LOG.info("{} påkrevdeInntektsmeldinger[{}]: TOM LISTE", filtrert, referanse.behandlingId());
            return;
        }
        var identifikatorer = arbeidsgivere.stream().map(ag -> tilMaskertNummer(ag.getIdentifikator())).collect(Collectors.joining(","));
        LOG.info("{} påkrevdeInntektsmeldinger[{}]: identifikatorer: {}", filtrert, referanse.behandlingId(), identifikatorer);
    }
}

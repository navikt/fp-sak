package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;

/**
 * Håndterer administrasjon(saksbehandlers input) vedrørende arbeidsforhold.
 */
@ApplicationScoped
public class ArbeidsforholdAdministrasjonTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    ArbeidsforholdAdministrasjonTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdAdministrasjonTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    /**
     * Oppretter en builder for å lagre ned overstyringen av arbeidsforhold
     *
     * @param behandlingId behandlingen sin ID
     * @return buildern
     */
    public ArbeidsforholdInformasjonBuilder opprettBuilderFor(Long behandlingId) {
        return ArbeidsforholdInformasjonBuilder.oppdatere(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId));
    }

    /**
     * Lagrer overstyringer i ArbeidsforholdInformasjon
     *
     * @param behandlingId behandlingId
     * @param builder      ArbeidsforholdsOverstyringene som skal lagrers
     */
    public void lagreOverstyring(Long behandlingId, ArbeidsforholdInformasjonBuilder builder) {
        inntektArbeidYtelseTjeneste.lagreOverstyrtArbeidsforhold(behandlingId, builder);
    }

    public void fjernOverstyringerGjortAvSaksbehandler(Long behandlingId) {
        var builder = opprettBuilderFor(behandlingId);
        builder.fjernAlleOverstyringer();
        inntektArbeidYtelseTjeneste.lagreOverstyrtArbeidsforhold(behandlingId, builder);
    }
}

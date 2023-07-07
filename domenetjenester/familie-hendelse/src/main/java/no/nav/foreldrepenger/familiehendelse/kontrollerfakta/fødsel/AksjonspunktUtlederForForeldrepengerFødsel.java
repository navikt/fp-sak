package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

/**
 * Aksjonspunkter for søknad om foreldrepenger for fødsel
 */
@ApplicationScoped
public class AksjonspunktUtlederForForeldrepengerFødsel extends AksjonspunktUtlederForFødsel {

    AksjonspunktUtlederForForeldrepengerFødsel() {
    }

    @Inject
    public AksjonspunktUtlederForForeldrepengerFødsel(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, FamilieHendelseTjeneste familieHendelseTjeneste) {
        super(inntektArbeidYtelseTjeneste, familieHendelseTjeneste);
    }

    @Override
    protected List<AksjonspunktResultat> utledAksjonspunkterForTerminbekreftelse(AksjonspunktUtlederInput param) {
        return erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param) == JA ?
            List.of() : opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE);
    }


}

package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

import java.util.List;

import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE;

/**
 * Aksjonspunkter for søknad om engangsstønad for fødsel
 */
@ApplicationScoped
public class AksjonspunktUtlederForEngangsstønadFødsel extends AksjonspunktUtlederForFødsel {

    AksjonspunktUtlederForEngangsstønadFødsel() {
        super();
    }

    @Inject
    public AksjonspunktUtlederForEngangsstønadFødsel(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, FamilieHendelseTjeneste familieHendelseTjeneste) {
        super(inntektArbeidYtelseTjeneste, familieHendelseTjeneste);
    }

    @Override
    protected List<AksjonspunktResultat> utledAksjonspunkterForTerminbekreftelse(AksjonspunktUtlederInput param) {
        return opprettListeForAksjonspunkt(AVKLAR_TERMINBEKREFTELSE);
    }
}

package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

/**
 * Aksjonspunkter for søknad om foreldrepenger for fødsel
 */
@ApplicationScoped
public class AksjonspunktUtlederForForeldrepengerFødsel extends AksjonspunktUtlederForFødsel {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();

    AksjonspunktUtlederForForeldrepengerFødsel() {
    }

    @Inject
    public AksjonspunktUtlederForForeldrepengerFødsel(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, FamilieHendelseTjeneste familieHendelseTjeneste) {
        super(inntektArbeidYtelseTjeneste, familieHendelseTjeneste);
    }

    @Override
    protected List<AksjonspunktResultat> utledAksjonspunkterForTerminbekreftelse(AksjonspunktUtlederInput param) {
        return !RelasjonsRolleType.erMor(param.getRelasjonsRolleType()) && param.getSkjæringstidspunkt().utenMinsterett() ?
            utledAksjonspunkterForFarMedmor() : utledAksjonspunkterForMor(param);
    }

    /**
     * Utleder aksjonspunkter for far/medmor som hovedsøker for tilfelle før Prop15L21/22
     */
    private List<AksjonspunktResultat> utledAksjonspunkterForFarMedmor() {
        List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();
        aksjonspunktResultater.add(opprettForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE));
        aksjonspunktResultater.add(opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_OM_VILKÅR_FOR_SYKDOM_OPPFYLT));
        return aksjonspunktResultater;
    }

    /**
     * Utleder aksjonspunkter for mor som hovedsøker
     */
    private List<AksjonspunktResultat> utledAksjonspunkterForMor(AksjonspunktUtlederInput param) {  // Tanken her er at funksjonell APU flyt i dok (PK-46654) skal samsvare med koden
        if (erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param) == JA) {
            return INGEN_AKSJONSPUNKTER;
        }
        return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE);
    }
}

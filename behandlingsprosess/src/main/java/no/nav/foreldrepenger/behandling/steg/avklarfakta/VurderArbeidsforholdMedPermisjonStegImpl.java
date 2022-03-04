package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdInntektsmeldingToggleTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "VURDER_ARB_FORHOLD_PERMISJON")
@BehandlingTypeRef
@FagsakYtelseTypeRef()
@ApplicationScoped
public class VurderArbeidsforholdMedPermisjonStegImpl implements VurderArbeidsforholdMedPermisjonSteg {

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private AksjonspunktUtlederForArbForholdMedPermisjoner utlederArbForholdMedPermisjoner;

    VurderArbeidsforholdMedPermisjonStegImpl() {
        // for CDI proxy
    }

    @Inject
    VurderArbeidsforholdMedPermisjonStegImpl(BehandlingRepository behandlingRepository,
                                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                             AksjonspunktUtlederForArbForholdMedPermisjoner utleder) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.utlederArbForholdMedPermisjoner = utleder;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (!ArbeidsforholdInntektsmeldingToggleTjeneste.erTogglePå()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        var aksjonspunktResultat = utlederArbForholdMedPermisjoner.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultat);
    }
}

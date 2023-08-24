package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt.AksjonspunktUtlederForArbForholdMedPermisjoner;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(BehandlingStegType.VURDER_ARB_FORHOLD_PERMISJON)
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
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        var aksjonspunktResultat = utlederArbForholdMedPermisjoner.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));

        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultat);
    }
}

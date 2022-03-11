package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt.AksjonspunktUtlederForVurderArbeidsforhold;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerArbeidsforholdSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdInntektsmeldingToggleTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "KOARB")
@BehandlingTypeRef
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class KontrollerArbeidsforholdStegImpl implements KontrollerArbeidsforholdSteg {

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private AksjonspunktUtlederForVurderArbeidsforhold utleder;

    public KontrollerArbeidsforholdStegImpl() {
        // for CDI proxy
    }

    @Inject
    public KontrollerArbeidsforholdStegImpl(BehandlingRepository behandlingRepository,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
            AksjonspunktUtlederForVurderArbeidsforhold utleder) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.utleder = utleder;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (ArbeidsforholdInntektsmeldingToggleTjeneste.erTogglePå()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        var aksjonspunktResultat = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultat);
    }
}

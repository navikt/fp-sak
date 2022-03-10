package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;
import static no.nav.foreldrepenger.domene.arbeidsforhold.impl.VurderPermisjonTjeneste.finnArbForholdMedPermisjonUtenSluttdatoMangel;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

@ApplicationScoped
public class AksjonspunktUtlederForArbForholdMedPermisjoner implements AksjonspunktUtleder {
    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();

    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;


    AksjonspunktUtlederForArbForholdMedPermisjoner() {
    }

    @Inject
    public AksjonspunktUtlederForArbForholdMedPermisjoner(BehandlingRepository behandlingRepository,
                                                          InntektArbeidYtelseTjeneste iayTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        if (SpesialBehandling.skalGrunnlagBeholdes(behandling)) {
            return INGEN_AKSJONSPUNKTER;
        }
        var iayGrunnlag = iayTjeneste.finnGrunnlag(param.getBehandlingId()).orElse(null);
        if (iayGrunnlag != null) {

            var arbForholdMedPermisjonUtenSluttdato = finnArbForholdMedPermisjonUtenSluttdatoMangel(param.getRef(),iayGrunnlag);

            if (!arbForholdMedPermisjonUtenSluttdato.isEmpty()) {
                return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO);
            }
        }
        return INGEN_AKSJONSPUNKTER;
    }
}


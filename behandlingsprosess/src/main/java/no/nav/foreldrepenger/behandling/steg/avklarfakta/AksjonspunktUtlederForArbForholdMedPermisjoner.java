package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;
import static no.nav.foreldrepenger.domene.arbeidsforhold.impl.VurderPermisjonTjeneste.hentArbForholdMedPermisjonUtenSluttdato;

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
        var iayGrunnlag = iayTjeneste.finnGrunnlag(param.getBehandlingId());
        if (iayGrunnlag.isPresent()) {
            var arbForholdMedPermisjonUtenSluttdato = hentArbeidsforholdMedPermisjonUtenSluttdato(param, iayGrunnlag.get());

            if (!arbForholdMedPermisjonUtenSluttdato.isEmpty()) {
                return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO);
            }
        }
        return INGEN_AKSJONSPUNKTER;
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> hentArbeidsforholdMedPermisjonUtenSluttdato(AksjonspunktUtlederInput param,
                                                                                                        InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return hentArbForholdMedPermisjonUtenSluttdato(param.getRef(), iayGrunnlag);
    }

}


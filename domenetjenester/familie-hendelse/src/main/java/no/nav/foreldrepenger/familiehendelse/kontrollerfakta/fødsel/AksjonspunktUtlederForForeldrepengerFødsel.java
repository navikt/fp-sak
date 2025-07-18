package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.fødsel;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

/**
 * Aksjonspunkter for søknad om foreldrepenger for fødsel
 */
@ApplicationScoped
public class AksjonspunktUtlederForForeldrepengerFødsel extends AksjonspunktUtlederForFødsel {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public AksjonspunktUtlederForForeldrepengerFødsel(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                      FamilieHendelseTjeneste familieHendelseTjeneste,
                                                      YtelsesFordelingRepository ytelsesFordelingRepository) {
        super(inntektArbeidYtelseTjeneste, familieHendelseTjeneste);
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    AksjonspunktUtlederForForeldrepengerFødsel() {
    }

    @Override
    protected List<AksjonspunktUtlederResultat> utledAksjonspunkterForTerminbekreftelse(AksjonspunktUtlederInput param) {
        if (farSøkerOgMorIkkeRett(param) || erSøkerRegistrertArbeidstakerMedLøpendeArbeidsforholdIAARegisteret(param) == NEI) {
            return AksjonspunktUtlederResultat.opprettListeForAksjonspunkt(AksjonspunktDefinisjon.SJEKK_TERMINBEKREFTELSE);
        }
        return List.of();
    }

    private boolean farSøkerOgMorIkkeRett(AksjonspunktUtlederInput param) {
        if (RelasjonsRolleType.erMor(param.getRelasjonsRolleType())) {
            return false;
        }
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(param.getBehandlingId());
        //setter false på annen parts utbetaling her, greit å få AP i de få casene der mor har uttak og far oppgitt at hun ikke har rett
        return !ytelseFordelingAggregat.harAnnenForelderRett() || ytelseFordelingAggregat.harAleneomsorg();
    }

}

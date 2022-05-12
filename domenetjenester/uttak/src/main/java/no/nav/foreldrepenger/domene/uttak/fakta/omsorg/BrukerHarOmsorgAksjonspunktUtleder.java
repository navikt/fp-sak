package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.fakta.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

/**
 * Aksjonspunkter for Manuell kontroll av om bruker har Omsorg
 */
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class BrukerHarOmsorgAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {
    private PersonopplysningerForUttak personopplysninger;

    @Inject
    public BrukerHarOmsorgAksjonspunktUtleder(PersonopplysningerForUttak personopplysninger) {
        this.personopplysninger = personopplysninger;
    }
    BrukerHarOmsorgAksjonspunktUtleder() {

    }

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var familieHendelser = fpGrunnlag.getFamilieHendelser();
        var bekreftetFH = familieHendelser.getBekreftetFamilieHendelse();

        if (familieHendelser.getGjeldendeFamilieHendelse().erAlleBarnDøde()) {
            return List.of();
        }
        if (bekreftetFH.isPresent() && erBarnetFødt(bekreftetFH.get()) == Utfall.JA
            && !personopplysninger.barnHarSammeBosted(ref)) {
            return List.of(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
        }

        return List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return false;
    }

    private Utfall erBarnetFødt(FamilieHendelse bekreftet) {
        return !bekreftet.getBarna().isEmpty() ? Utfall.JA : Utfall.NEI;
    }
}

package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.skjæringstidspunkt.SøknadsperiodeFristTjeneste;

@ApplicationScoped
@VilkårTypeRef(VilkårType.SØKNADSFRISTVILKÅRET)
public class InngangsvilkårEngangsstønadSøknadsfrist implements Inngangsvilkår {

    private static VilkårData OPPFYLT = new VilkårData(VilkårType.SØKNADSFRISTVILKÅRET, VilkårUtfallType.OPPFYLT, List.of());
    private static VilkårData MANUELL = new VilkårData(VilkårType.SØKNADSFRISTVILKÅRET, VilkårUtfallType.IKKE_VURDERT,
        List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET));

    private SøknadsperiodeFristTjeneste fristTjeneste;

    InngangsvilkårEngangsstønadSøknadsfrist() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårEngangsstønadSøknadsfrist(@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD) SøknadsperiodeFristTjeneste fristTjeneste) {
        this.fristTjeneste = fristTjeneste;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        return vurderSøknadsfristOppfyltAutomatisk(ref);
    }

    private VilkårData vurderSøknadsfristOppfyltAutomatisk(BehandlingReferanse ref) {
        var fristdata = fristTjeneste.finnSøknadsfrist(ref.behandlingId());

        var søknadMottattDato = fristdata.getSøknadMottattDato();
        var fristdato = fristdata.getUtledetSøknadsfrist();

        return søknadMottattDato.isAfter(fristdato) ? MANUELL : OPPFYLT;
    }
}

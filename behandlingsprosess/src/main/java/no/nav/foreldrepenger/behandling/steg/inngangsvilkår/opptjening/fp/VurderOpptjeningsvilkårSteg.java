package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.fp;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.MapTilOpptjeningAktiviteter;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles.VurderOpptjeningsvilkårStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;

@BehandlingStegRef(BehandlingStegType.VURDER_OPPTJENINGSVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class VurderOpptjeningsvilkårSteg extends VurderOpptjeningsvilkårStegFelles {

    @Inject
    public VurderOpptjeningsvilkårSteg(BehandlingRepositoryProvider repositoryProvider, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
    }

    @Override
    protected List<OpptjeningAktivitet> mapTilOpptjeningsaktiviteter(OpptjeningsvilkårResultat oppResultat) {
        List<OpptjeningAktivitet> aktiviteter = new ArrayList<>();
        aktiviteter.addAll(MapTilOpptjeningAktiviteter.map(oppResultat.getUnderkjentePerioder(), OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST));
        aktiviteter.addAll(
            MapTilOpptjeningAktiviteter.map(oppResultat.getAntattGodkjentePerioder(), OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT));
        aktiviteter.addAll(
            MapTilOpptjeningAktiviteter.map(oppResultat.getBekreftetGodkjentePerioder(), OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT));
        aktiviteter.addAll(MapTilOpptjeningAktiviteter.map(oppResultat.getAkseptertMellomliggendePerioder(),
            OpptjeningAktivitetKlassifisering.MELLOMLIGGENDE_PERIODE));
        return aktiviteter;
    }

    @Override
    protected BehandleStegResultat stegResultatVilkårIkkeOppfylt(RegelResultat regelResultat, Behandling behandling) {
        return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET));
    }
}

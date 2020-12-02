package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.svp;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.MapTilOpptjeningAktiviteter;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles.VurderOpptjeningsvilkårStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;

@BehandlingStegRef(kode = "VURDER_OPPTJ")
@BehandlingTypeRef
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class VurderOpptjeningsvilkårSteg extends VurderOpptjeningsvilkårStegFelles {

    @Inject
    public VurderOpptjeningsvilkårSteg(BehandlingRepositoryProvider repositoryProvider, OpptjeningRepository opptjeningRepository,
            InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
        super(repositoryProvider, opptjeningRepository, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
    }

    @Override
    protected List<OpptjeningAktivitet> mapTilOpptjeningsaktiviteter(MapTilOpptjeningAktiviteter mapper, OpptjeningsvilkårResultat oppResultat) {
        List<OpptjeningAktivitet> aktiviteter = new ArrayList<>();
        aktiviteter.addAll(mapper.map(oppResultat.getBekreftetGodkjentePerioder(), OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT));
        aktiviteter.addAll(mapper.map(oppResultat.getAntattGodkjentePerioder(), OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT));
        aktiviteter.addAll(mapper.map(oppResultat.getUnderkjentePerioder(), OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST));
        return aktiviteter;
    }
}

package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.mottatteopplysninger.TilknyttFagsakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.INNHENT_SØKNADOPP)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class TilknyttFagsakStegRevurdering implements TilknyttFagsakSteg {

    @Inject
    public TilknyttFagsakStegRevurdering() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}

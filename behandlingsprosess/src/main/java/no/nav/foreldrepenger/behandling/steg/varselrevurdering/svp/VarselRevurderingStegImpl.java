package no.nav.foreldrepenger.behandling.steg.varselrevurdering.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.varselrevurdering.VarselRevurderingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.VARSEL_REVURDERING)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class VarselRevurderingStegImpl implements VarselRevurderingSteg {

    private BehandlingRepository behandlingRepository;

    @Inject
    public VarselRevurderingStegImpl(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (SpesialBehandling.skalGrunnlagBeholdes(behandling)) {
            return BehandleStegResultat.langhopp(BehandlingStegType.KONTROLLER_FAKTA);
        }
        // DO nothing. Steget finnes for å kunne hoppe fram til Kontroller Fakta
        return BehandleStegResultat.utførtUtenAksjonspunkter();

    }
}

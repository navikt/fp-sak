package no.nav.foreldrepenger.behandling.steg.beregnytelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.ytelse.beregning.BeregnYtelseTjeneste;

/**
 * Felles steg for å beregne tilkjent ytelse for foreldrepenger og
 * svangerskapspenger (ikke engangsstønad)
 */

@BehandlingStegRef(BehandlingStegType.BEREGN_YTELSE)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class BeregneYtelseStegImpl implements BeregneYtelseSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BeregnYtelseTjeneste beregnYtelseTjeneste;

    protected BeregneYtelseStegImpl() {
        // for proxy
    }

    @Inject
    public BeregneYtelseStegImpl(BehandlingRepository behandlingRepository,
                                 BeregningsresultatRepository beregningsresultatRepository,
                                 BeregnYtelseTjeneste beregnYtelseTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.beregnYtelseTjeneste = beregnYtelseTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        // Beregn ytelse
        var beregningsresultat = beregnYtelseTjeneste.beregnYtelse(ref);

        // Lagre beregningsresultat
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
    }
}

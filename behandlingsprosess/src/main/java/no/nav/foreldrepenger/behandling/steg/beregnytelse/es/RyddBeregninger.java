package no.nav.foreldrepenger.behandling.steg.beregnytelse.es;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

// TODO (Safir): FLytt logikk til BeregningRepository
class RyddBeregninger {

    private BehandlingRepository behandlingRepository;
    private final BehandlingskontrollKontekst kontekst;

    RyddBeregninger(BehandlingRepository behandlingRepository, BehandlingskontrollKontekst kontekst) {
        this.behandlingRepository = behandlingRepository;
        this.kontekst = kontekst;
    }

    void ryddBeregninger(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat == null || behandlingsresultat.getBeregningResultat() == null) {
            return;
        }

        behandlingRepository.slettTidligereBeregningerES(behandling, kontekst.getSkriveLås());

        if (behandlingsresultat.getBeregningResultat().getBeregninger() != null) {
            LegacyESBeregningsresultat.builderFraEksisterende(behandlingsresultat.getBeregningResultat())
                .nullstillBeregninger()
                .buildFor(behandling, behandlingsresultat);
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        }
    }

    public void ryddBeregningerHvisIkkeOverstyrt(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat == null || behandlingsresultat.getBeregningResultat() == null) {
            return;
        } else if (!behandlingsresultat.getBeregningResultat().isOverstyrt()) {
            ryddBeregninger(behandling, behandlingsresultat);
        }
    }
}

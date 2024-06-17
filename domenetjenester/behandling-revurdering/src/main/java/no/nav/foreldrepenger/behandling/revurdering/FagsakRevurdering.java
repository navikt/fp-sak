package no.nav.foreldrepenger.behandling.revurdering;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

class FagsakRevurdering {

    private BehandlingRepository behandlingRepository;

    public FagsakRevurdering(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        if (harÅpenBehandling(fagsak)) {
            return false;
        }
        var behandling = hentBehandlingMedVedtak(fagsak);
        return behandling.filter(this::kanVilkårRevurderes).isPresent();
    }

    private boolean kanVilkårRevurderes(Behandling behandling) {
        return behandling.getBehandlingsresultat().getVilkårResultat().getVilkårene().stream().noneMatch(this::erAvslagPåManglendeDokumentasjon);
    }

    private boolean erAvslagPåManglendeDokumentasjon(Vilkår vilkår) {
        return vilkår.getVilkårType().equals(VilkårType.SØKERSOPPLYSNINGSPLIKT) && vilkår.getGjeldendeVilkårUtfall()
            .equals(VilkårUtfallType.IKKE_OPPFYLT);
    }

    private Optional<Behandling> hentBehandlingMedVedtak(Fagsak fagsak) {
        return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId());
    }

    private boolean harÅpenBehandling(Fagsak fagsak) {
        return !behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId()).isEmpty();
    }

    static class BehandlingAvsluttetDatoComparator implements Comparator<Behandling>, Serializable {
        @Override
        public int compare(Behandling behandling, Behandling otherBehandling) {
            return otherBehandling.getAvsluttetDato() != null && behandling.getAvsluttetDato() != null ? otherBehandling.getAvsluttetDato()
                .compareTo(behandling.getAvsluttetDato()) : otherBehandling.getOpprettetDato().compareTo(behandling.getOpprettetDato());
        }
    }
}

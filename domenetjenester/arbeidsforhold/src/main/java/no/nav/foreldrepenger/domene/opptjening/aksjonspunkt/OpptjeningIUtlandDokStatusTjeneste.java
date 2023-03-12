package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandMarkering;

@ApplicationScoped
public class OpptjeningIUtlandDokStatusTjeneste {

    private OpptjeningIUtlandDokStatusRepository repository;
    private FagsakEgenskapRepository fagsakEgenskapRepository;

    @Inject
    public OpptjeningIUtlandDokStatusTjeneste(OpptjeningIUtlandDokStatusRepository repository,
                                              FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.repository = repository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    OpptjeningIUtlandDokStatusTjeneste() {
        // CDI
    }

    public void lagreStatus(Long behandlingId, OpptjeningIUtlandDokStatus status) {
        repository.lagre(new OpptjeningIUtlandDokStatusEntitet(behandlingId, status));
    }

    public Optional<OpptjeningIUtlandDokStatus> hentStatus(Behandling behandling) {
        return fagsakEgenskapRepository.finnUtlandDokumentasjonStatus(behandling.getFagsakId())
            .map(s -> OpptjeningIUtlandDokStatus.valueOf(s.name()))
            .or(() -> repository.hent(behandling.getId()).map(OpptjeningIUtlandDokStatusEntitet::getDokStatus));
    }

    public Optional<UtlandMarkering> hentUtlandMarkering(Behandling behandling) {
        return fagsakEgenskapRepository.finnUtlandMarkering(behandling.getFagsakId());
    }

    public boolean harUtlandsMarkering(Behandling behandling) {
        var legacyMarkering = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE)
            .map(Aksjonspunkt::getBegrunnelse).filter(b -> !UtlandMarkering.NASJONAL.equals(UtlandMarkering.valueOf(b))).isPresent();
        return legacyMarkering || fagsakEgenskapRepository.finnUtlandMarkering(behandling.getFagsakId())
            .filter(e -> !UtlandMarkering.NASJONAL.equals(e)).isPresent();
    }

    public void deaktiverStatus(Long behandlingId) {
        repository.deaktiverStatus(behandlingId);
    }
}

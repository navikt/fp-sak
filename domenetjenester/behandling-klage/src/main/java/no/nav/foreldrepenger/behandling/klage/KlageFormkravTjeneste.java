package no.nav.foreldrepenger.behandling.klage;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class KlageFormkravTjeneste {

    private KlageRepository klageRepository;
    private BehandlingRepository behandlingRepository;

    KlageFormkravTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KlageFormkravTjeneste(BehandlingRepository behandlingRepository,
                                 KlageRepository klageRepository) {
        this.klageRepository = klageRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public void opprettKlage(Behandling klageBehandling) {
        klageRepository.hentEvtOpprettKlageResultat(klageBehandling);
    }

    public void oppdaterKlageMedPåklagetBehandling(Long klageBehandlingId, Long påklagetBehandlingId) {
        Behandling klageBehandling = behandlingRepository.hentBehandling(klageBehandlingId);
        if (påklagetBehandlingId == null) {
            klageRepository.settPåklagdBehandling(klageBehandling, null);
            return;
        }
        Behandling påklagetBehandling = behandlingRepository.hentBehandling(påklagetBehandlingId);
        klageRepository.settPåklagdBehandling(klageBehandling, påklagetBehandling);
    }

    public void oppdaterKlageMedPåklagetEksternBehandlingUuid(Long klageBehandlingId, UUID påklagetEksternBehandlingUuid){
        Behandling klageBehandling = behandlingRepository.hentBehandling(klageBehandlingId);
        klageRepository.settPåklagdEksternBehandlingUuid(klageBehandling,påklagetEksternBehandlingUuid);
    }

    public void lagreFormkrav(KlageFormkravAdapter dto) {
        Behandling behandling = behandlingRepository.hentBehandling(dto.getKlageBehandlingId());
        KlageResultatEntitet klageResultat = klageRepository.hentEvtOpprettKlageResultat(behandling);
        KlageVurdertAv klageVurdertAv = dto.getKlageVurdertAvKode();
        KlageFormkravEntitet.Builder builder = new KlageFormkravEntitet.Builder();
        builder.medErKlagerPart(dto.isErKlagerPart());
        builder.medErFristOverholdt(dto.isErFristOverholdt());
        builder.medErKonkret(dto.isErKonkret());
        builder.medErSignert(dto.isErSignert());
        builder.medGjelderVedtak(dto.gjelderVedtak());
        builder.medBegrunnelse(dto.getBegrunnelse());
        builder.medKlageResultat(klageResultat);
        builder.medKlageVurdertAv(klageVurdertAv);

        klageRepository.lagreFormkrav(behandling, builder);
    }

}

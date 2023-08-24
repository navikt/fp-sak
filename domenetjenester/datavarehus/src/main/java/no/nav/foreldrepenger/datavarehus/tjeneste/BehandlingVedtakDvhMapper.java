package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingVedtakDvh;

import java.time.LocalDateTime;

class BehandlingVedtakDvhMapper {

    private BehandlingVedtakDvhMapper() {
    }

    static BehandlingVedtakDvh map(BehandlingVedtak behandlingVedtak, Behandling behandling) {
        return BehandlingVedtakDvh.builder()
                .ansvarligBeslutter(behandling.getAnsvarligBeslutter())
                .ansvarligSaksbehandler(behandlingVedtak.getAnsvarligSaksbehandler())
                .behandlingId(behandling.getId())
                .endretAv(CommonDvhMapper.finnEndretAvEllerOpprettetAv(behandlingVedtak))
                .funksjonellTid(LocalDateTime.now())
                .godkjennendeEnhet(behandling.getBehandlendeEnhet())
                .iverksettingStatus(behandlingVedtak.getIverksettingStatus().getKode())
                .opprettetDato(
                        behandlingVedtak.getOpprettetTidspunkt() == null ? null : behandlingVedtak.getOpprettetTidspunkt().toLocalDate())
                .vedtakDato(behandlingVedtak.getVedtaksdato())
                .vedtakId(behandlingVedtak.getId())
                .vedtakResultatTypeKode(behandlingVedtak.getVedtakResultatType().getKode())
                .build();
    }
}

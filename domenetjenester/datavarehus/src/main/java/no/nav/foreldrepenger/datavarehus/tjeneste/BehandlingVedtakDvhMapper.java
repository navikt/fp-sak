package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingVedtakDvh;

public class BehandlingVedtakDvhMapper {

    public BehandlingVedtakDvh map(BehandlingVedtak behandlingVedtak, Behandling behandling) {
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

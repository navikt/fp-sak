package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingVedtakDvh;

class BehandlingVedtakDvhMapper {

    private BehandlingVedtakDvhMapper() {
    }

    static BehandlingVedtakDvh map(BehandlingVedtak behandlingVedtak, Behandling behandling, LocalDate utbetaltTid) {
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
                .vedtakTid(behandlingVedtak.getVedtakstidspunkt())
                .vedtakId(behandlingVedtak.getId())
                .vedtakResultatTypeKode(behandlingVedtak.getVedtakResultatType().getKode())
                .utbetaltTid(utbetaltTid)
                .build();
    }

    static BehandlingVedtakDvh mapInnsynRepop(BehandlingVedtak behandlingVedtak, Behandling behandling) {
        return BehandlingVedtakDvh.builder()
            .ansvarligBeslutter(behandling.getAnsvarligBeslutter())
            .ansvarligSaksbehandler(behandlingVedtak.getAnsvarligSaksbehandler())
            .behandlingId(behandling.getId())
            .endretAv(CommonDvhMapper.finnEndretAvEllerOpprettetAv(behandlingVedtak))
            .funksjonellTid(behandlingVedtak.getVedtakstidspunkt())
            .godkjennendeEnhet(behandling.getBehandlendeEnhet())
            .iverksettingStatus(behandlingVedtak.getIverksettingStatus().getKode())
            .opprettetDato(
                behandlingVedtak.getOpprettetTidspunkt() == null ? null : behandlingVedtak.getOpprettetTidspunkt().toLocalDate())
            .vedtakDato(behandlingVedtak.getVedtaksdato())
            .vedtakTid(behandlingVedtak.getVedtakstidspunkt())
            .vedtakId(behandlingVedtak.getId())
            .vedtakResultatTypeKode(behandlingVedtak.getVedtakResultatType().getKode())
            .build();
    }
}

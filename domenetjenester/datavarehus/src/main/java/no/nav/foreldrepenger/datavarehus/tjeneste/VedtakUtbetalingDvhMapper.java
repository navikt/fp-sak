package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.datavarehus.domene.VedtakUtbetalingDvh;

import java.time.LocalDateTime;

class VedtakUtbetalingDvhMapper {

    private VedtakUtbetalingDvhMapper() {
    }

    static VedtakUtbetalingDvh map(String xmlClob, Behandling behandling, BehandlingVedtak behandlingVedtak, FamilieHendelseType hendelseType) {
        return VedtakUtbetalingDvh.builder()
            .xmlClob(xmlClob)
            .fagsakId(behandling.getFagsakId())
            .behandlingId(behandling.getId())
            .vedtakId(behandlingVedtak.getId())
            .vedtakDato(behandlingVedtak.getVedtaksdato())
            .behandlingType(behandling.getType().getOffisiellKode())
            .fagsakType(behandling.getFagsakYtelseType().getKode())
            .søknadType(hendelseType.getKode())
            .funksjonellTid(LocalDateTime.now())
            .endretAv(CommonDvhMapper.finnEndretAvEllerOpprettetAv(behandlingVedtak))
            .build();
    }
}

package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import org.junit.jupiter.api.Test;

import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

class BehandlingVedtakDvhMapperTest {


    @Test
    void skal_mappe_til_behandling_vedtak_dvh() {
        var behandling = byggBehandling();
        var vedtak = BehandlingVedtak.builder()
                .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
                .medIverksettingStatus(IVERKSETTING_STATUS)
                .medVedtakstidspunkt(VEDTAK_DATO)
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .build();

        var dvh = BehandlingVedtakDvhMapper.map(vedtak, behandling);
        assertThat(dvh).isNotNull();
        assertThat(dvh.getAnsvarligBeslutter()).isEqualTo(ANSVARLIG_BESLUTTER);
        assertThat(dvh.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(dvh.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(dvh.getGodkjennendeEnhet()).isEqualTo(BEHANDLENDE_ENHET);
        assertThat(dvh.getIverksettingStatus()).isEqualTo(IVERKSETTING_STATUS.getKode());
        assertThat(dvh.getVedtakDato()).isEqualTo(VEDTAK_DATO.toLocalDate());
        assertThat(dvh.getVedtakResultatTypeKode()).isEqualTo(VEDTAK_RESULTAT_TYPE.getKode());
    }

    private Behandling byggBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = scenario.lagMocked();
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET, null));
        return behandling;
    }

}

package no.nav.foreldrepenger.datavarehus.tjeneste;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.SØKERS_RELASJON_TIL_BARN;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus.OPPRETTET;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak.UDEFINERT;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.AKSJONSPUNKT_DEF;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_BESLUTTER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_SAKSBEHANDLER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLENDE_ENHET;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLING_STEG_ID;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLING_STEG_TYPE;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.OPPRETTET_AV;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.OPPRETTET_TID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;

@ExtendWith(MockitoExtension.class)
class AksjonspunktDvhMapperTest {

    @Mock
    private Aksjonspunkt aksjonspunkt;
    private Behandling behandling;

    @BeforeEach
    public void beforeEach() {
        behandling = behandling();
    }

    @Test
    void skal_mappe_til_aksjonspunkt_dvh() {

        when(aksjonspunkt.getAksjonspunktDefinisjon()).thenReturn(AKSJONSPUNKT_DEF);

        var punkt = behandling.getAksjonspunktMedDefinisjonOptional(AKSJONSPUNKT_DEF).get();
        var dvh = AksjonspunktDvhMapper.map(punkt, behandling, byggBehandlingStegTilstand(), true);

        assertThat(dvh).isNotNull();
        assertThat(dvh.getAksjonspunktDef()).isEqualTo(AKSJONSPUNKT_DEF.getKode());
        assertThat(dvh.getAksjonspunktId()).isEqualTo(punkt.getId());
        assertThat(dvh.getAksjonspunktStatus()).isEqualTo(OPPRETTET.getKode());
        assertThat(dvh.getAnsvarligBeslutter()).isEqualTo(ANSVARLIG_BESLUTTER);
        assertThat(dvh.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(dvh.getBehandlendeEnhetKode()).isEqualTo(BEHANDLENDE_ENHET);
        assertThat(dvh.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(dvh.getBehandlingStegId()).isEqualTo(BEHANDLING_STEG_ID);
        assertThat(dvh.getEndretAv()).isEqualTo(OPPRETTET_AV);
    }

    @Test
    void skal_mappe_behandlingsteg_null() {

        when(aksjonspunkt.getFristTid()).thenReturn(OPPRETTET_TID.plusWeeks(3));
        when(aksjonspunkt.getAksjonspunktDefinisjon()).thenReturn(AKSJONSPUNKT_DEF);

        var punkt = behandling.getAksjonspunktMedDefinisjonOptional(AKSJONSPUNKT_DEF).get();
        var dvh = AksjonspunktDvhMapper.map(punkt, behandling, Optional.empty(), true);

        assertThat(dvh).isNotNull();
        assertThat(dvh.getBehandlingStegId()).isNull();
        assertThat(dvh.getVenteårsak()).isNull();
        assertThat(dvh.getFristTid()).isEqualTo(OPPRETTET_TID.plusWeeks(3));
    }

    private Optional<BehandlingStegTilstand> byggBehandlingStegTilstand() {
        var tilstand = new BehandlingStegTilstand(behandling, BEHANDLING_STEG_TYPE);
        tilstand.setId(BEHANDLING_STEG_ID);
        return Optional.of(tilstand);
    }

    private Behandling behandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, SØKERS_RELASJON_TIL_BARN);
        var behandling = scenario.lagMocked();
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET, null));
        var tilstand = new BehandlingStegTilstand(behandling, BEHANDLING_STEG_TYPE);
        tilstand.setId(BEHANDLING_STEG_ID);
        behandling.setBehandlingStegTilstander(List.of(tilstand));
        when(aksjonspunkt.getOpprettetAv()).thenReturn(OPPRETTET_AV);
        when(aksjonspunkt.getVenteårsak()).thenReturn(UDEFINERT);
        when(aksjonspunkt.getStatus()).thenReturn(OPPRETTET);
        var spy = spy(behandling);
        when(spy.getAksjonspunktMedDefinisjonOptional(eq(AKSJONSPUNKT_DEF))).thenReturn(Optional.of(aksjonspunkt));
        return spy;
    }

}

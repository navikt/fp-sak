package no.nav.foreldrepenger.datavarehus.tjeneste;

import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_BESLUTTER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_SAKSBEHANDLER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLENDE_ENHET;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.IVERKSETTING_STATUS;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.VEDTAK_DATO;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.VEDTAK_RESULTAT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.datavarehus.domene.VilkårVerdiDvh;

class BehandlingVedtakDvhMapperTest {


    @Test
    void skal_mappe_til_behandling_vedtak_dvh() {
        var behandling = byggBehandling();
        var utbetaltTid = VEDTAK_DATO.toLocalDate().plusWeeks(1);
        var vedtak = BehandlingVedtak.builder()
                .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
                .medIverksettingStatus(IVERKSETTING_STATUS)
                .medVedtakstidspunkt(VEDTAK_DATO)
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .build();

        var dvh = BehandlingVedtakDvhMapper.map(vedtak, behandling, utbetaltTid, Set.of());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getAnsvarligBeslutter()).isEqualTo(ANSVARLIG_BESLUTTER);
        assertThat(dvh.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(dvh.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(dvh.getGodkjennendeEnhet()).isEqualTo(BEHANDLENDE_ENHET);
        assertThat(dvh.getIverksettingStatus()).isEqualTo(IVERKSETTING_STATUS.getKode());
        assertThat(dvh.getVedtakDato()).isEqualTo(VEDTAK_DATO.toLocalDate());
        assertThat(dvh.getVedtakResultatTypeKode()).isEqualTo(VEDTAK_RESULTAT_TYPE.getKode());
        assertThat(dvh.getVedtakTid()).isEqualTo(VEDTAK_DATO);
        assertThat(dvh.getUtbetaltTid()).isEqualTo(utbetaltTid);
        assertThat(dvh.getVilkårIkkeOppfylt()).isNull();
    }

    @Test
    void skal_mappe_til_avslag_behandling_vedtak_dvh() {
        var behandling = byggBehandling();
        var utbetaltTid = VEDTAK_DATO.toLocalDate().plusWeeks(1);
        var vedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medIverksettingStatus(IVERKSETTING_STATUS)
            .medVedtakstidspunkt(VEDTAK_DATO)
            .medVedtakResultatType(VedtakResultatType.AVSLAG)
            .build();

        var dvh = BehandlingVedtakDvhMapper.map(vedtak, behandling, utbetaltTid, Set.of(VilkårType.FØDSELSVILKÅRET_MOR, VilkårType.SØKERSOPPLYSNINGSPLIKT));
        assertThat(dvh).isNotNull();
        assertThat(dvh.getAnsvarligBeslutter()).isEqualTo(ANSVARLIG_BESLUTTER);
        assertThat(dvh.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(dvh.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(dvh.getGodkjennendeEnhet()).isEqualTo(BEHANDLENDE_ENHET);
        assertThat(dvh.getIverksettingStatus()).isEqualTo(IVERKSETTING_STATUS.getKode());
        assertThat(dvh.getVedtakDato()).isEqualTo(VEDTAK_DATO.toLocalDate());
        assertThat(dvh.getVedtakResultatTypeKode()).isEqualTo(VedtakResultatType.AVSLAG.getKode());
        assertThat(dvh.getVedtakTid()).isEqualTo(VEDTAK_DATO);
        assertThat(dvh.getUtbetaltTid()).isEqualTo(utbetaltTid);
        assertThat(dvh.getVilkårIkkeOppfylt()).isEqualTo(VilkårVerdiDvh.ENGANSSTØNAD.name());
    }

    @Test
    void skal_mappe_til_avslag_opplysningsplikt_behandling_vedtak_dvh() {
        var behandling = byggBehandling();
        var utbetaltTid = VEDTAK_DATO.toLocalDate().plusWeeks(1);
        var vedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medIverksettingStatus(IVERKSETTING_STATUS)
            .medVedtakstidspunkt(VEDTAK_DATO)
            .medVedtakResultatType(VedtakResultatType.AVSLAG)
            .build();

        var dvh = BehandlingVedtakDvhMapper.map(vedtak, behandling, utbetaltTid, Set.of(VilkårType.SØKERSOPPLYSNINGSPLIKT));
        assertThat(dvh).isNotNull();
        assertThat(dvh.getAnsvarligBeslutter()).isEqualTo(ANSVARLIG_BESLUTTER);
        assertThat(dvh.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(dvh.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(dvh.getGodkjennendeEnhet()).isEqualTo(BEHANDLENDE_ENHET);
        assertThat(dvh.getIverksettingStatus()).isEqualTo(IVERKSETTING_STATUS.getKode());
        assertThat(dvh.getVedtakDato()).isEqualTo(VEDTAK_DATO.toLocalDate());
        assertThat(dvh.getVedtakResultatTypeKode()).isEqualTo(VedtakResultatType.AVSLAG.getKode());
        assertThat(dvh.getVedtakTid()).isEqualTo(VEDTAK_DATO);
        assertThat(dvh.getUtbetaltTid()).isEqualTo(utbetaltTid);
        assertThat(dvh.getVilkårIkkeOppfylt()).isEqualTo(VilkårVerdiDvh.OPPLYSNINGSPLIKT.name());
    }

    @Test
    void skal_mappe_til_opphør_opplysningsplikt_behandling_vedtak_dvh() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET, null));
        var utbetaltTid = VEDTAK_DATO.toLocalDate().plusWeeks(1);
        var vedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medIverksettingStatus(IVERKSETTING_STATUS)
            .medVedtakstidspunkt(VEDTAK_DATO)
            .medVedtakResultatType(VedtakResultatType.OPPHØR)
            .build();

        var dvh = BehandlingVedtakDvhMapper.map(vedtak, behandling, utbetaltTid, Set.of(VilkårType.OPPTJENINGSVILKÅRET));
        assertThat(dvh).isNotNull();
        assertThat(dvh.getAnsvarligBeslutter()).isEqualTo(ANSVARLIG_BESLUTTER);
        assertThat(dvh.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(dvh.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(dvh.getGodkjennendeEnhet()).isEqualTo(BEHANDLENDE_ENHET);
        assertThat(dvh.getIverksettingStatus()).isEqualTo(IVERKSETTING_STATUS.getKode());
        assertThat(dvh.getVedtakDato()).isEqualTo(VEDTAK_DATO.toLocalDate());
        assertThat(dvh.getVedtakResultatTypeKode()).isEqualTo(VedtakResultatType.OPPHØR.getKode());
        assertThat(dvh.getVedtakTid()).isEqualTo(VEDTAK_DATO);
        assertThat(dvh.getUtbetaltTid()).isEqualTo(utbetaltTid);
        assertThat(dvh.getVilkårIkkeOppfylt()).isEqualTo(VilkårVerdiDvh.FORELDREPENGER_OPPTJENING.name());
    }

    @Test
    void skal_mappe_til_avslag_uttak_behandling_vedtak_dvh() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET, null));
        var utbetaltTid = VEDTAK_DATO.toLocalDate().plusWeeks(1);
        var vedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medIverksettingStatus(IVERKSETTING_STATUS)
            .medVedtakstidspunkt(VEDTAK_DATO)
            .medVedtakResultatType(VedtakResultatType.AVSLAG)
            .build();

        var dvh = BehandlingVedtakDvhMapper.map(vedtak, behandling, utbetaltTid, Set.of());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getAnsvarligBeslutter()).isEqualTo(ANSVARLIG_BESLUTTER);
        assertThat(dvh.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(dvh.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(dvh.getGodkjennendeEnhet()).isEqualTo(BEHANDLENDE_ENHET);
        assertThat(dvh.getIverksettingStatus()).isEqualTo(IVERKSETTING_STATUS.getKode());
        assertThat(dvh.getVedtakDato()).isEqualTo(VEDTAK_DATO.toLocalDate());
        assertThat(dvh.getVedtakResultatTypeKode()).isEqualTo(VedtakResultatType.AVSLAG.getKode());
        assertThat(dvh.getVedtakTid()).isEqualTo(VEDTAK_DATO);
        assertThat(dvh.getUtbetaltTid()).isEqualTo(utbetaltTid);
        assertThat(dvh.getVilkårIkkeOppfylt()).isEqualTo(VilkårVerdiDvh.FORELDREPENGER_UTTAK.name());
    }

    private Behandling byggBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = scenario.lagMocked();
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET, null));
        return behandling;
    }

}

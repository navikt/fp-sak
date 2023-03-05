package no.nav.foreldrepenger.datavarehus.tjeneste;

import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_BESLUTTER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_SAKSBEHANDLER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLENDE_ENHET;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.IVERKSETTING_STATUS;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.VEDTAK_DATO;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;

class VedtakUtbetalingDvhMapperTest {

    public static final String XML = "xml";

    private FamilieHendelseRepository familieHendelseRepository;


    @Test
    void skal_mappe_til_VedtakUtbetalingDvh(){
        var behandling = byggBehandling();
        var vedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medIverksettingStatus(IVERKSETTING_STATUS)
            .medVedtakstidspunkt(VEDTAK_DATO)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .build();


        final var familieHendelseGrunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
        var mapped = VedtakUtbetalingDvhMapper.map(XML, behandling, vedtak, familieHendelseGrunnlag.getGjeldendeVersjon().getType());
        assertThat(mapped.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(mapped.getXmlClob()).isEqualTo(XML);
        assertThat(mapped.getVedtakDato()).isEqualTo(VEDTAK_DATO.toLocalDate());
        assertThat(mapped.getFagsakId()).isEqualTo(behandling.getFagsakId());
        assertThat(mapped.getFagsakType()).isEqualTo(behandling.getFagsak().getYtelseType().getKode());
        assertThat(mapped.getSøknadType()).isEqualTo(familieHendelseGrunnlag.getGjeldendeVersjon().getType().getKode());
    }


    private Behandling byggBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = scenario.lagMocked();
        familieHendelseRepository = scenario.mockBehandlingRepositoryProvider().getFamilieHendelseRepository();
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET, null));
        return behandling;
    }
}

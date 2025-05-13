package no.nav.foreldrepenger.behandlingskontroll.impl;

import static org.assertj.core.api.Assertions.assertThat;

import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModellTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(CdiAwareExtension.class)
class BehandlingModellTjenesteTest {

    private BehandlingStegType steg2;
    private BehandlingStegType steg3;
    private BehandlingStegType steg4;


    @BeforeEach
    void setUp() {
        var modell = BehandlingModellRepository.getModell(BehandlingType.FØRSTEGANGSSØKNAD, FagsakYtelseType.ENGANGSTØNAD);
        steg2 = modell.hvertSteg().map(BehandlingStegModell::getBehandlingStegType).toList().get(8);
        steg3 = modell.finnNesteSteg(steg2).getBehandlingStegType();
        steg4 = modell.finnNesteSteg(steg3).getBehandlingStegType();
    }

    @Test
    void steg_rekkefølger() {
        var behandlingModell = new BehandlingModellTjeneste();
        assertThat(behandlingModell.inneholderSteg(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR)).isTrue();
        assertThat(behandlingModell.erStegAFørStegB(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD,
            steg2, steg3)).isTrue();
        assertThat(behandlingModell.erStegAFørStegB(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD,
            steg4, steg3)).isFalse();
        assertThat(behandlingModell.erStegAFørStegB(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD,
            steg3, steg3)).isFalse();

        assertThat(behandlingModell.erStegAEtterStegB(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD,
            steg2, steg3)).isFalse();
        assertThat(behandlingModell.erStegAEtterStegB(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD,
            steg4, steg3)).isTrue();
        assertThat(behandlingModell.erStegAEtterStegB(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD,
            steg3, steg3)).isFalse();
    }


    @Test
    void skal_returnere_true_når_aksjonspunktet_skal_løses_etter_angitt_steg() {
        assertThat(new BehandlingModellTjeneste().skalAksjonspunktLøsesIEllerEtterSteg(FagsakYtelseType.ENGANGSTØNAD,
                BehandlingType.FØRSTEGANGSSØKNAD, steg3, AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET)).isTrue();
    }

    @Test
    void skal_returnere_true_når_aksjonspunktet_skal_løses_i_angitt_steg() {
        assertThat(new BehandlingModellTjeneste().skalAksjonspunktLøsesIEllerEtterSteg(FagsakYtelseType.ENGANGSTØNAD,
                BehandlingType.FØRSTEGANGSSØKNAD, steg2, AksjonspunktDefinisjon.AVKLAR_VERGE))
                        .isTrue();
    }

    @Test
    void skal_returnere_false_når_aksjonspunktet_skal_løses_før_angitt_steg() {
        assertThat(new BehandlingModellTjeneste().skalAksjonspunktLøsesIEllerEtterSteg(FagsakYtelseType.ENGANGSTØNAD,
                BehandlingType.FØRSTEGANGSSØKNAD, steg4, AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD))
                        .isFalse();
    }

    @Test
    void skal_returnere_true_ved_senere_steg() {
        assertThat(new BehandlingModellTjeneste().skalAksjonspunktLøsesIEllerEtterSteg(FagsakYtelseType.ENGANGSTØNAD,
            BehandlingType.FØRSTEGANGSSØKNAD, steg4, AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD))
            .isFalse();
    }


}

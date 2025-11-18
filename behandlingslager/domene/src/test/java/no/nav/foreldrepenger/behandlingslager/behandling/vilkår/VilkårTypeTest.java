package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

class VilkårTypeTest {

    @Test
    void skal_hente_ut_riktig_lovreferanse_basert_på_fagsakYtelseType_engangsstønad() {
        assertThat(VilkårType.SØKERSOPPLYSNINGSPLIKT.getLovReferanse(FagsakYtelseType.ENGANGSTØNAD)).isEqualTo("§ 21-3");
    }

    @Test
    void skal_hente_ut_riktig_lovreferanse_basert_på_fagsakYtelseType_foreldrepenger() {
        assertThat(VilkårType.BEREGNINGSGRUNNLAGVILKÅR.getLovReferanse(FagsakYtelseType.FORELDREPENGER)).isEqualTo("§ 14-7");
    }

}

package no.nav.foreldrepenger.domene.tid;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class VirkedagUtilTest {

    @Test
    void lørdagSøndagTilMandag() {
        //Mandag
        assertThat(VirkedagUtil.lørdagSøndagTilMandag(LocalDate.of(2021, 12, 13))).isEqualTo(LocalDate.of(2021, 12, 13));
        //Tirsdag
        assertThat(VirkedagUtil.lørdagSøndagTilMandag(LocalDate.of(2021, 12, 14))).isEqualTo(LocalDate.of(2021, 12, 14));
        //Onsdag
        assertThat(VirkedagUtil.lørdagSøndagTilMandag(LocalDate.of(2021, 12, 15))).isEqualTo(LocalDate.of(2021, 12, 15));
        //Torsdag
        assertThat(VirkedagUtil.lørdagSøndagTilMandag(LocalDate.of(2021, 12, 16))).isEqualTo(LocalDate.of(2021, 12, 16));
        //Fredag
        assertThat(VirkedagUtil.lørdagSøndagTilMandag(LocalDate.of(2021, 12, 17))).isEqualTo(LocalDate.of(2021, 12, 17));
        //Lørdag
        assertThat(VirkedagUtil.lørdagSøndagTilMandag(LocalDate.of(2021, 12, 18))).isEqualTo(LocalDate.of(2021, 12, 20));
        //Søndag
        assertThat(VirkedagUtil.lørdagSøndagTilMandag(LocalDate.of(2021, 12, 19))).isEqualTo(LocalDate.of(2021, 12, 20));
    }

}

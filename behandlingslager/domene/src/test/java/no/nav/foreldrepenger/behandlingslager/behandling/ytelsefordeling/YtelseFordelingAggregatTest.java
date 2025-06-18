package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;

class YtelseFordelingAggregatTest {

    @Test
    void rettighetstype_aleneomsorg() {
        var yfa = YtelseFordelingAggregat.oppdatere(Optional.empty()).medOppgittRettighet(OppgittRettighetEntitet.aleneomsorg()).build();

        assertThat(yfa.getGjeldendeRettighetstype(false, RelasjonsRolleType.MORA, null)).isEqualTo(Rettighetstype.ALENEOMSORG);
    }

    @Test
    void rettighetstype_overstyrt_type() {
        var yfa = YtelseFordelingAggregat.oppdatere(Optional.empty())
            .medOverstyrtRettighetstype(Rettighetstype.BEGGE_RETT)
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .build();

        assertThat(yfa.getGjeldendeRettighetstype(false, RelasjonsRolleType.FARA, null)).isEqualTo(Rettighetstype.BEGGE_RETT);
    }

    @Test
    void rettighetstype_annenpart_har_foreldrepenger_utbetaling() {
        var yfa = YtelseFordelingAggregat.oppdatere(Optional.empty())
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .build();

        assertThat(yfa.getGjeldendeRettighetstype(true, RelasjonsRolleType.FARA, null)).isEqualTo(Rettighetstype.BEGGE_RETT);
    }

    @Test
    void rettighetstype_avklart_annen_forelder_har_rett_eøs() {
        var yfa = YtelseFordelingAggregat.oppdatere(Optional.empty())
            .medAvklartRettighet(new OppgittRettighetEntitet(false, false, false, true, true))
            .build();

        assertThat(yfa.getGjeldendeRettighetstype(false, RelasjonsRolleType.FARA, null)).isEqualTo(Rettighetstype.BEGGE_RETT_EØS);
    }

    @Test
    void rettighetstype_mor_mottar_uforetrygd() {
        var uføretrygdGrunnlag = UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty()).medRegisterUføretrygd(true, LocalDate.now().minusYears(1), LocalDate.now().minusYears(1)).build();
        var yfa = YtelseFordelingAggregat.oppdatere(Optional.empty())
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .build();

        assertThat(yfa.getGjeldendeRettighetstype(false, RelasjonsRolleType.FARA, uføretrygdGrunnlag)).isEqualTo(Rettighetstype.BARE_FAR_RETT_MOR_UFØR);
    }

    @Test
    void rettighetstype_mor_mottar_uforetrygd_rolle_mor() {
        var uføretrygdGrunnlag = UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty()).medRegisterUføretrygd(true, LocalDate.now().minusYears(1), LocalDate.now().minusYears(1)).build();
        var yfa = YtelseFordelingAggregat.oppdatere(Optional.empty())
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .build();

        assertThat(yfa.getGjeldendeRettighetstype(false, RelasjonsRolleType.MORA, uføretrygdGrunnlag)).isEqualTo(Rettighetstype.BARE_MOR_RETT);
    }

    @Test
    void rettighetstype_bare_far_rett() {
        var yfa = YtelseFordelingAggregat.oppdatere(Optional.empty())
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .build();

        assertThat(yfa.getGjeldendeRettighetstype(false, RelasjonsRolleType.FARA, null)).isEqualTo(Rettighetstype.BARE_FAR_RETT);
    }

    @Test
    void rettighetstype_bare_mor_rett() {
        var yfa = YtelseFordelingAggregat.oppdatere(Optional.empty())
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .build();

        assertThat(yfa.getGjeldendeRettighetstype(false, RelasjonsRolleType.MORA, null)).isEqualTo(Rettighetstype.BARE_MOR_RETT);
    }

}

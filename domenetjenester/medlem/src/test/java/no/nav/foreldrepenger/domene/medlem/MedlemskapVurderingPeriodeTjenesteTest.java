package no.nav.foreldrepenger.domene.medlem;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.skjæringstidspunkt.es.BotidCore2024;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class MedlemskapVurderingPeriodeTjenesteTest {

    private static final LocalDate IKRAFT = LocalDate.now().minusWeeks(2);
    private static final Period OVERGANG = Period.parse("P18W3D");
    private static final Period BOSATT_TILBAKE = Period.ofMonths(12);

    private static final Period ES_MEDLEMSKAP = Period.ofMonths(12);
    private static final BotidCore2024 BOTID_CORE = new BotidCore2024(IKRAFT, OVERGANG);

    @Test
    void engangsstønad_termin_uten_botid() {
        // Arrange
        var termindato = IKRAFT.plus(OVERGANG).minusDays(1); // Dagen før ny ordning

        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagMocked();

        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(termindato)
            .medUttaksintervall(new LocalDateInterval(termindato, termindato))
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(termindato, null))
            .build();

        // Act/Assert
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).bosattVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(LocalDate.now().minus(BOSATT_TILBAKE), termindato));
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).lovligOppholdVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(termindato, termindato));
    }

    @Test
    void engangsstønad_termin_fødsel_uten_botid() {
        // Arrange
        var termindato = IKRAFT.minusWeeks(1);
        var fødselsdato = IKRAFT.minusWeeks(1).minusDays(2);

        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagMocked();

        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medUttaksintervall(new LocalDateInterval(fødselsdato, fødselsdato))
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(termindato, fødselsdato))
            .build();

        // Act/Assert
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).bosattVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(fødselsdato.minus(BOSATT_TILBAKE), fødselsdato));
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).lovligOppholdVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(fødselsdato, fødselsdato));
    }

    @Test
    void engangsstønad_fødsel_uten_botid() {
        // Arrange
        var fødselsdato = IKRAFT.minusDays(2);

        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagMocked();

        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medUttaksintervall(new LocalDateInterval(fødselsdato, fødselsdato))
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(null, fødselsdato))
            .build();

        // Act/Assert
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).bosattVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(fødselsdato.minus(BOSATT_TILBAKE), fødselsdato));
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).lovligOppholdVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(fødselsdato, fødselsdato));
    }

    @Test
    void engangsstønad_adopsjon_uten_botid() {
        // Arrange
        var omsorgsdato = IKRAFT.minusWeeks(2);

        var behandling = ScenarioMorSøkerEngangsstønad.forAdopsjon()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagMocked();

        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(omsorgsdato)
            .medUttaksintervall(new LocalDateInterval(omsorgsdato, omsorgsdato))
            .medFamilieHendelseDato(FamilieHendelseDato.forAdopsjonOmsorg(omsorgsdato))
            .build();

        // Act/Assert
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).bosattVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(omsorgsdato.minus(BOSATT_TILBAKE), omsorgsdato));
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).lovligOppholdVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(omsorgsdato, omsorgsdato));
    }


    @Test
    void engangsstønad_termin_med_botid() {
        // Arrange
        var termindato = IKRAFT.plus(OVERGANG).plusWeeks(1);

        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagMocked();

        var ref = BehandlingReferanse.fra(behandling);
        var stp= Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(termindato)
            .medUttaksintervall(new LocalDateInterval(termindato, termindato))
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(termindato, null))
            .build();

        // Act/Assert
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).bosattVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(LocalDate.now().minus(ES_MEDLEMSKAP), termindato));
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).lovligOppholdVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(LocalDate.now().minus(ES_MEDLEMSKAP), termindato));
    }

    @Test
    void engangsstønad_termin_fødsel_med_botid() {
        // Arrange
        var termindato = IKRAFT.plus(OVERGANG).plusWeeks(1);
        var fødselsdato = IKRAFT.plus(OVERGANG);

        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagMocked();

        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medUttaksintervall(new LocalDateInterval(fødselsdato, fødselsdato))
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(termindato, fødselsdato))
            .build();

        // Act/Assert
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).bosattVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(LocalDate.now().minus(ES_MEDLEMSKAP), termindato));
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).lovligOppholdVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(LocalDate.now().minus(ES_MEDLEMSKAP), termindato));
    }

    @Test
    void engangsstønad_fødsel_med_botid() {
        // Arrange
        var fødselsdato = IKRAFT.plusWeeks(1);

        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagMocked();

        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medUttaksintervall(new LocalDateInterval(fødselsdato, fødselsdato))
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(null, fødselsdato))
            .build();

        // Act/Assert
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).bosattVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(fødselsdato.minus(ES_MEDLEMSKAP), fødselsdato));
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).lovligOppholdVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(fødselsdato.minus(ES_MEDLEMSKAP), fødselsdato));
    }

    @Test
    void engangsstønad_adopsjon_med_botid() {
        // Arrange
        var omsorgsdato = IKRAFT.plusWeeks(3);

        var behandling = ScenarioMorSøkerEngangsstønad.forAdopsjon()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagMocked();

        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(omsorgsdato)
            .medUttaksintervall(new LocalDateInterval(omsorgsdato, omsorgsdato))
            .medFamilieHendelseDato(FamilieHendelseDato.forAdopsjonOmsorg(omsorgsdato))
            .build();

        // Act/Assert
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).bosattVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(LocalDate.now().minus(ES_MEDLEMSKAP), omsorgsdato));
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).lovligOppholdVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(LocalDate.now().minus(ES_MEDLEMSKAP), omsorgsdato));
    }

    @Test
    void foreldrepenger_termin() {
        // Arrange
        var termindato = LocalDate.now().plusWeeks(4);
        var skjæringstidspunkt = termindato.minusWeeks(3);
        var maxdato = termindato.plusWeeks(15);

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagMocked();

        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(skjæringstidspunkt)
            .medUttaksintervall(new LocalDateInterval(skjæringstidspunkt, maxdato))
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(termindato, null))
            .build();

        // Act/Assert
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).bosattVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(LocalDate.now().minus(BOSATT_TILBAKE), maxdato));
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).lovligOppholdVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(skjæringstidspunkt, maxdato));
    }

    @Test
    void foreldrepenger_adopsjon() {
        // Arrange
        var omsorgsdato = LocalDate.now().minusDays(3);
        var maxdato = omsorgsdato.plusWeeks(15);

        var behandling = ScenarioMorSøkerForeldrepenger.forAdopsjon()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagMocked();

        var ref = BehandlingReferanse.fra(behandling);
        var stp =  Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(omsorgsdato)
            .medUttaksintervall(new LocalDateInterval(omsorgsdato, maxdato))
            .medFamilieHendelseDato(FamilieHendelseDato.forAdopsjonOmsorg(omsorgsdato))
            .build();

        // Act/Assert
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).bosattVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(omsorgsdato.minus(BOSATT_TILBAKE), maxdato));
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).lovligOppholdVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(omsorgsdato, maxdato));
    }

    @Test
    void svangerskapspenger_termin() {
        // Arrange
        var termindato = LocalDate.now().plusWeeks(12);
        var skjæringstidspunkt = LocalDate.now().minusWeeks(1);
        var maxdato = termindato.minusWeeks(3);

        var behandling = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagMocked();

        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(skjæringstidspunkt)
            .medUttaksintervall(new LocalDateInterval(skjæringstidspunkt, maxdato))
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(termindato, null))
            .build();

        // Act/Assert
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).bosattVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(skjæringstidspunkt.minus(BOSATT_TILBAKE), maxdato));
        assertThat(new MedlemskapVurderingPeriodeTjeneste(BOTID_CORE).lovligOppholdVurderingsintervall(ref, stp))
            .isEqualTo(new LocalDateInterval(skjæringstidspunkt, maxdato));
    }

}

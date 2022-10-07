package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.AvklarFaktaUttakValidator.GRADERING;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.AvklarFaktaUttakValidator.KREV_MINST_EN_SØKNADSPERIODE;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.AvklarFaktaUttakValidator.OVERLAPPENDE_PERIODER;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.AvklarFaktaUttakValidator.PERIODE_FØR_FØRSTE_UTTAKSDATO;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.KontrollerFaktaPeriode;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BekreftetOppgittPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FaktaUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaPeriodeLagreDto;

public class AvklarFaktaUttakValidatorTest {

    private static final LocalDate DATO = LocalDate.of(2019, 3, 25);

    @Test
    public void skal_validere_finnes_ingen_søknadsperiode() {
        FaktaUttakDto dto = new FaktaUttakDto.FaktaUttakPerioderDto();

        var feltFeil = AvklarFaktaUttakValidator.validerSøknadsperioder(dto.getBekreftedePerioder(), Optional.of(DATO));
        assertThat(feltFeil).isPresent();
        assertThat(feltFeil.get().getMelding()).isEqualTo(KREV_MINST_EN_SØKNADSPERIODE);
    }

    @Test
    public void skal_validere_overlappende_perioder() {
        FaktaUttakDto dto = new FaktaUttakDto.FaktaUttakPerioderDto();
        var bekreftetOppgittPeriodeDto_1 = getBekreftetUttakPeriodeDto(DATO.minusDays(20), DATO.minusDays(10));
        var bekreftetOppgittPeriodeDto_2 = getBekreftetUttakPeriodeDto(DATO.minusDays(11), DATO);
        dto.setBekreftedePerioder(List.of(bekreftetOppgittPeriodeDto_1, bekreftetOppgittPeriodeDto_2));

        var feltFeil = AvklarFaktaUttakValidator.validerSøknadsperioder(dto.getBekreftedePerioder(),
                Optional.of(bekreftetOppgittPeriodeDto_1.getBekreftetPeriode().getFom()));
        assertThat(feltFeil).isPresent();
        assertThat(feltFeil.get().getMelding()).isEqualTo(OVERLAPPENDE_PERIODER);
    }

    @Test
    public void skal_validere_arbeidsgiver_ved_arbeidstaker_og_gradering() {
        var bekreftet = OppgittPeriodeBuilder.ny().medPeriode(DATO.minusDays(20), DATO)
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medArbeidsgiver(null)
                .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
                .medArbeidsprosent(BigDecimal.TEN)
                .build();
        var bekreftetOppgittPeriodeDto = new BekreftetOppgittPeriodeDto();
        bekreftetOppgittPeriodeDto.setBekreftetPeriode(new KontrollerFaktaPeriodeLagreDto.Builder(KontrollerFaktaPeriode.ubekreftet(bekreftet), null)
                .build());
        FaktaUttakDto dto = new FaktaUttakDto.FaktaUttakPerioderDto();
        dto.setBekreftedePerioder(Collections.singletonList(bekreftetOppgittPeriodeDto));

        var feltFeil = AvklarFaktaUttakValidator.validerSøknadsperioder(dto.getBekreftedePerioder(),
                Optional.of(bekreftet.getFom()));
        assertThat(feltFeil.get().getMelding()).isEqualTo(GRADERING);
    }

    @Test
    public void ikke_mulig_å_ha_periode_før_første_uttaksdato() {
        FaktaUttakDto dto = new FaktaUttakDto.FaktaUttakPerioderDto();
        var periode = getBekreftetUttakPeriodeDto(DATO.minusDays(20), DATO.minusDays(10));
        dto.setBekreftedePerioder(List.of(periode));

        var feltFeil = AvklarFaktaUttakValidator.validerSøknadsperioder(dto.getBekreftedePerioder(),
                Optional.of(periode.getBekreftetPeriode().getFom().plusDays(1)));
        assertThat(feltFeil).isPresent();
        assertThat(feltFeil.get().getMelding()).isEqualTo(PERIODE_FØR_FØRSTE_UTTAKSDATO);
    }

    @Test
    public void mulig_å_ha_periode_på_første_uttaksdato() {
        FaktaUttakDto dto = new FaktaUttakDto.FaktaUttakPerioderDto();
        var periode = getBekreftetUttakPeriodeDto(DATO.minusDays(20), DATO.minusDays(10));
        dto.setBekreftedePerioder(List.of(periode));

        var feltFeil = AvklarFaktaUttakValidator.validerSøknadsperioder(dto.getBekreftedePerioder(),
                Optional.of(periode.getBekreftetPeriode().getFom()));
        assertThat(feltFeil).isEmpty();
    }

    private BekreftetOppgittPeriodeDto getBekreftetUttakPeriodeDto(LocalDate fom, LocalDate tom) {
        var bekreftetOppgittPeriodeDto = new BekreftetOppgittPeriodeDto();
        var bekreftetperiode = OppgittPeriodeBuilder.ny()
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .build();
        bekreftetOppgittPeriodeDto
                .setBekreftetPeriode(new KontrollerFaktaPeriodeLagreDto.Builder(KontrollerFaktaPeriode.ubekreftet(bekreftetperiode), null)
                        .build());
        return bekreftetOppgittPeriodeDto;
    }

}

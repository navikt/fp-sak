package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringValidatorTekster;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.EgenVirksomhetDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.FrilansDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.VirksomhetDto;

class ManuellRegistreringSvangerskapspengerValidatorTest {

    @Test
    void skal_ikke_gi_valideringsfeil_dersom_alt_er_utfylt() {
        var dto = lagStandardDtoUtenValideringsfeil();

        var feltFeilDtos = ManuellRegistreringSvangerskapspengerValidator.validerOpplysninger(dto);

        assertThat(feltFeilDtos).isEmpty();
    }

    @Test
    void skal_rapportere_valideringsfeil_dersom_termindato_mangler() {
        var dto = lagStandardDtoUtenValideringsfeil();
        dto.setTermindato(null);

        var feltFeilDtos = ManuellRegistreringSvangerskapspengerValidator.validerOpplysninger(dto);

        assertThat(feltFeilDtos).hasSize(1);
        assertThat(feltFeilDtos.get(0).getNavn()).isEqualTo("terminDato");
        assertThat(feltFeilDtos.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PAAKREVD_FELT);
    }

    @Test
    void skal_rapportere_valideringsfeil_dersom_mottatdato_mangler() {
        var dto = lagStandardDtoUtenValideringsfeil();
        dto.setMottattDato(null);

        var feltFeilDtos = ManuellRegistreringSvangerskapspengerValidator.validerOpplysninger(dto);

        assertThat(feltFeilDtos).hasSize(1);
        assertThat(feltFeilDtos.get(0).getNavn()).isEqualTo("mottattDato");
        assertThat(feltFeilDtos.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PAAKREVD_FELT);
    }

    @Test
    void skal_rapportere_valideringsfeil_dersom_frilans_mangler_perioder() {
        var dto = lagStandardDtoUtenValideringsfeil();
        dto.getFrilans().setHarSøkerPeriodeMedFrilans(true);
        dto.getFrilans().setPerioder(Collections.emptyList());

        var feltFeilDtos = ManuellRegistreringSvangerskapspengerValidator.validerOpplysninger(dto);

        assertThat(feltFeilDtos).hasSize(1);
        assertThat(feltFeilDtos.get(0).getNavn()).isEqualTo("frilans");
        assertThat(feltFeilDtos.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PERIODER_MANGLER);
    }

    @Test
    void skal_rapportere_valideringsfeil_dersom_egen_virksomhet_mangler_virksomhetnavn() {
        var dto = lagStandardDtoUtenValideringsfeil();
        dto.getEgenVirksomhet().setHarArbeidetIEgenVirksomhet(true);
        var virksomhet = new VirksomhetDto();
        virksomhet.setNavn(null);
        virksomhet.setVirksomhetRegistrertINorge(true);
        virksomhet.setLandJobberFra("Tyskland");
        virksomhet.setOrganisasjonsnummer("123456789");
        virksomhet.setFom(LocalDate.now());
        dto.getEgenVirksomhet().setVirksomheter(Collections.singletonList(virksomhet));

        var feltFeilDtos = ManuellRegistreringSvangerskapspengerValidator.validerOpplysninger(dto);

        assertThat(feltFeilDtos).hasSize(1);
        assertThat(feltFeilDtos.get(0).getNavn()).isEqualTo("virksomhetNavn");
        assertThat(feltFeilDtos.get(0).getMelding()).isEqualTo(ManuellRegistreringValidatorTekster.PAAKREVD_FELT);
    }

    private ManuellRegistreringSvangerskapspengerDto lagStandardDtoUtenValideringsfeil() {
        var dto = new ManuellRegistreringSvangerskapspengerDto();

        var egenVirksomhetDto = new EgenVirksomhetDto();
        egenVirksomhetDto.setHarArbeidetIEgenVirksomhet(false);
        dto.setEgenVirksomhet(egenVirksomhetDto);

        var frilansDto = new FrilansDto();
        frilansDto.setHarSøkerPeriodeMedFrilans(false);
        dto.setFrilans(frilansDto);

        var termindato = LocalDate.of(2019, 12, 24);
        dto.setTermindato(LocalDate.of(2019, 12, 24));
        dto.setMottattDato(termindato.minusMonths(5));

        return dto;
    }

}
